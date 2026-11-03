# /*******************************************************************************
#  * Copyright © 2026 ETH Zürich
#  *
#  * This program and the accompanying materials are made available under the
#  * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
#  * License, Version 2.1 (or at your option any later version).
#  *
#  * You may choose which license you wish to use when copying, modifying,
#  * or distributing this software.
#  *
#  * Licenses are available at:
#  * - Apache 2.0: http://apache.org
#  * - LGPL 2.1:   https://gnu.org
#  *
#  * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
#  *
#  * Contributors:
#  *     Andreas P. Cuny - initial API and implementation
#  ******************************************************************************/

#!/usr/bin/env python3
"""Local CORS-enabled static file server for the FAIR export plugin.

Serves a folder (the FAIR-export output directory) over HTTP so that browser-based
OME-Zarr tools (the OME-NGFF validator, vizarr, neuroglancer, vol-e) can fetch
the exported `*.ome.zarr` store by URL. Those tools run in the browser and fetch
the data with `fetch()`, which is blocked cross-origin unless the server sends
`Access-Control-Allow-Origin`, so a plain `python -m http.server` will not work.

Beyond static serving it provides control endpoints, used by the bundled
self-hosted viewer page so that "open in napari" opens napari for real (the
GitHub-hosted validator can only show a `napari <url>` command, because a web page
cannot spawn a desktop app while this local server can):

 - GET /__stores            -> JSON list of the `*.ome.zarr` stores under the root.
 - GET/POST /__napari?source=<rel>  -> launches napari on that store (resolved
                            strictly inside the root) via --napari-script, and
                            returns a small JSON status. Fire-and-forget.
 - GET /__viewer            -> a self-hosted launcher page listing the stores with,
                            per store: "Open in napari" (calls /__napari), a link to
                            the OME-NGFF validator, and onward links to vizarr /
                            neuroglancer / vol-e prefilled with the local data URL.
 - /__validator/...         -> a self-hosted ome-ngff-validator build, if --validator-dir
                            is given (offline validation).

Static behaviour:
 - permissive CORS headers on every response (incl. preflight OPTIONS),
 - whole files with HTTP range support (some viewers issue ranged reads),
 - no directory traversal above the served root,
 - shuts itself down after `--idle-timeout` seconds without a request (0 = never),
 - prints one line `FAIR_VIZ_SERVER ... listening on http://<host>:<port>/` once
   bound, so the launching Java process can parse the base URL, then serves forever.

Args: --root DIR  --host 127.0.0.1  --port 0 (0 = pick a free port)
      --idle-timeout 1800  [--napari-script PATH]  [--validator-dir DIR]
      [--validator-base URL]
"""
SCRIPT_VERSION = "v0.2.0"

import os
import sys
import json
import time
import argparse
import threading
import functools
import subprocess
from urllib.parse import urlparse, parse_qs, quote
from http.server import SimpleHTTPRequestHandler, HTTPServer
from socketserver import ThreadingMixIn

_last_activity = time.time()

# Populated in main() so the handler (constructed per request) can see them.
_ROOT = None
_NAPARI_SCRIPT = None
_VALIDATOR_DIR = None
_VIZARR_DIR = None
_VALIDATOR_BASE = "https://ome.github.io/ome-ngff-validator/"


def _find_stores(root):
    """Return the names of `*.ome.zarr` directories directly under root, sorted."""
    try:
        names = [
            name for name in os.listdir(root)
            if name.lower().endswith(".ome.zarr") and os.path.isdir(os.path.join(root, name))
        ]
    except OSError:
        names = []
    names.sort()
    return names


def _safe_store_path(root, rel):
    """Resolve `rel` inside root, returning an absolute path only if it is a
    `*.ome.zarr` directory strictly inside root; otherwise None (no traversal)."""
    if not rel:
        return None
    rel = rel.replace("\\", "/").lstrip("/")
    root_abs = os.path.abspath(root)
    candidate = os.path.abspath(os.path.join(root_abs, rel))
    if not (candidate == root_abs or candidate.startswith(root_abs + os.sep)):
        return None
    # walk up to the `.ome.zarr` component so a link to a sub-array still opens the store
    parts = candidate[len(root_abs):].lstrip(os.sep).split(os.sep)
    acc = root_abs
    for part in parts:
        acc = os.path.join(acc, part)
        if part.lower().endswith(".ome.zarr") and os.path.isdir(acc):
            return acc
    if os.path.isdir(candidate) and candidate.lower().endswith(".ome.zarr"):
        return candidate
    return None


def _launch_napari(store_path):
    """Spawn napari on store_path via the configured napari script. Fire-and-forget.
    Returns (ok, message)."""
    if not _NAPARI_SCRIPT or not os.path.isfile(_NAPARI_SCRIPT):
        return False, "napari launcher script not available on the server"
    try:
        log_path = os.path.join(os.path.dirname(_NAPARI_SCRIPT) or ".", "napari_launch.log")
        with open(log_path, "ab", buffering=0) as log:
            subprocess.Popen(
                [sys.executable, _NAPARI_SCRIPT, store_path],
                stdout=log, stderr=log,
                cwd=os.path.dirname(_NAPARI_SCRIPT) or None,
            )
        return True, "napari launching on %s" % os.path.basename(store_path)
    except Exception as exc:  # noqa: BLE001 - report any spawn failure to the page
        return False, "could not launch napari: %r" % (exc,)


_HOSTED_VALIDATOR = "https://ome.github.io/ome-ngff-validator/"


def _looks_like_validator(base):
    """A usable validator base is an http(s) URL with a path (e.g. .../ome-ngff-validator/).
    A bare host such as http://127.0.0.1 or http://localhost:5000 is NOT a validator (it is
    typically the data server mis-entered), so we fall back to the hosted validator for it."""
    if not base:
        return False
    try:
        u = urlparse(base)
    except Exception:  # noqa: BLE001
        return False
    if u.scheme not in ("http", "https") or not u.netloc:
        return False
    return u.path not in ("", "/")


def _validator_url(base_url, data_url):
    base = base_url or _VALIDATOR_BASE
    if not _looks_like_validator(base):
        base = _HOSTED_VALIDATOR
    sep = "&" if "?" in base else "?"
    return "%s%ssource=%s" % (base, sep, quote(data_url, safe=""))


def _viewer_page(host_url):
    """Build the self-hosted launcher HTML for the stores under root."""
    stores = _find_stores(_ROOT)
    # Prefer locally-served builds (offline) when present, else the hosted ones.
    validator_base = _VALIDATOR_BASE if _looks_like_validator(_VALIDATOR_BASE) else _HOSTED_VALIDATOR
    if _VALIDATOR_DIR:
        validator_base = host_url + "__validator/"
    vizarr_base = "https://hms-dbmi.github.io/vizarr/"
    if _VIZARR_DIR:
        vizarr_base = host_url + "__vizarr/"
    rows = []
    for name in stores:
        data_url = host_url + quote(name)
        validator = _validator_url(validator_base, data_url)
        vizarr = vizarr_base + "?source=" + quote(data_url, safe="")
        neuroglancer = "https://neuroglancer-demo.appspot.com/#!" + quote(
            json.dumps({"layers": [{"source": "zarr://" + data_url, "name": name}]}), safe="")
        vole = "https://volumeviewer.hms.harvard.edu/?url=" + quote(data_url, safe="")
        napari_ep = "/__napari?source=" + quote(name, safe="")
        rows.append(_STORE_ROW.format(
            name=name, data_url=data_url, validator=validator, vizarr=vizarr,
            neuroglancer=neuroglancer, vole=vole, napari_ep=napari_ep))
    if not rows:
        rows.append('<p>No <code>*.ome.zarr</code> stores found in the served folder.</p>')
    return _VIEWER_HTML.format(rows="\n".join(rows), version=SCRIPT_VERSION, base=host_url)


class Handler(SimpleHTTPRequestHandler):
    # keep logging quiet; we only care about the bound URL line
    def log_message(self, fmt, *args):
        pass

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS, POST")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Expose-Headers", "*")
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_OPTIONS(self):
        global _last_activity
        _last_activity = time.time()
        self.send_response(204)
        self.end_headers()

    # Control endpoints

    def _host_base(self):
        host = self.headers.get("Host")
        if not host:
            host = "127.0.0.1:%d" % self.server.server_address[1]
        return "http://%s/" % host

    def _send_json(self, code, obj):
        body = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle_control(self, method):
        """Returns True if the path was a control endpoint and was handled."""
        parsed = urlparse(self.path)
        path = parsed.path
        if path == "/__stores":
            self._send_json(200, {"stores": _find_stores(_ROOT)})
            return True
        if path == "/__napari":
            params = parse_qs(parsed.query)
            source = (params.get("source") or [""])[0]
            store_path = _safe_store_path(_ROOT, source)
            if store_path is None:
                self._send_json(400, {"ok": False, "error": "unknown or unsafe source: %r" % source})
                return True
            ok, message = _launch_napari(store_path)
            self._send_json(200 if ok else 500, {"ok": ok, "message": message})
            return True
        if path in ("/__viewer", "/__viewer/"):
            body = _viewer_page(self._host_base()).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            if method == "GET":
                self.wfile.write(body)
            return True
        if _VALIDATOR_DIR and path.startswith("/__validator"):
            self._serve_static_app(parsed, method, _VALIDATOR_DIR, "/__validator")
            return True
        if _VIZARR_DIR and path.startswith("/__vizarr"):
            self._serve_static_app(parsed, method, _VIZARR_DIR, "/__vizarr")
            return True
        return False

    def _serve_static_app(self, parsed, method, app_dir, prefix):
        rel = parsed.path[len(prefix):].lstrip("/")
        base = os.path.abspath(app_dir)
        target = os.path.abspath(os.path.join(base, rel)) if rel else os.path.join(base, "index.html")
        if os.path.isdir(target):
            target = os.path.join(target, "index.html")
        if not (target == base or target.startswith(base + os.sep)) or not os.path.isfile(target):
            self.send_error(404, "Not found in validator build")
            return
        try:
            with open(target, "rb") as handle:
                data = handle.read()
        except OSError:
            self.send_error(404, "Not found")
            return
        self.send_response(200)
        self.send_header("Content-Type", self.guess_type(target))
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if method == "GET":
            self.wfile.write(data)

    def do_POST(self):
        global _last_activity
        _last_activity = time.time()
        if self._handle_control("POST"):
            return
        self.send_error(405, "Method not allowed")

    def do_GET(self):
        global _last_activity
        _last_activity = time.time()
        if self._handle_control("GET"):
            return
        rng = self.headers.get("Range")
        if rng:
            self._serve_range(rng)
        else:
            super().do_GET()

    def do_HEAD(self):
        global _last_activity
        _last_activity = time.time()
        parsed = urlparse(self.path)
        if parsed.path in ("/__stores", "/__napari", "/__viewer", "/__viewer/") \
                or (_VALIDATOR_DIR and parsed.path.startswith("/__validator")) \
                or (_VIZARR_DIR and parsed.path.startswith("/__vizarr")):
            if self._handle_control("HEAD"):
                return
        super().do_HEAD()

    def _serve_range(self, rng):
        """Minimal single-range handler (bytes=start-end)."""
        path = self.translate_path(self.path)
        if not os.path.isfile(path):
            self.send_error(404, "File not found")
            return
        try:
            size = os.path.getsize(path)
            unit, _, spec = rng.partition("=")
            if unit.strip() != "bytes":
                raise ValueError("unsupported range unit")
            start_s, _, end_s = spec.partition("-")
            start = int(start_s) if start_s else 0
            end = int(end_s) if end_s else size - 1
            end = min(end, size - 1)
            if start > end or start >= size:
                self.send_response(416)
                self.send_header("Content-Range", "bytes */%d" % size)
                self.end_headers()
                return
            length = end - start + 1
            self.send_response(206)
            ctype = self.guess_type(path)
            self.send_header("Content-Type", ctype)
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
            self.send_header("Content-Length", str(length))
            self.end_headers()
            with open(path, "rb") as f:
                f.seek(start)
                remaining = length
                while remaining > 0:
                    chunk = f.read(min(65536, remaining))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    remaining -= len(chunk)
        except (ValueError, OSError):
            super().do_GET()


class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = (os.name != "nt")


# Self-hosted launcher page templates

_STORE_ROW = """
<div class="store">
  <h3>{name}</h3>
  <div class="url"><code>{data_url}</code></div>
  <div class="actions">
    <button class="napari" data-endpoint="{napari_ep}">Open in napari</button>
    <a class="btn" href="{validator}" target="_blank" rel="noopener">OME-NGFF validator</a>
    <a class="btn" href="{vizarr}" target="_blank" rel="noopener">vizarr</a>
    <a class="btn" href="{neuroglancer}" target="_blank" rel="noopener">neuroglancer</a>
    <a class="btn" href="{vole}" target="_blank" rel="noopener">vol-e</a>
  </div>
</div>
"""

_VIEWER_HTML = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>YouScope FAIR export OME-Zarr viewers</title>
<style>
  body {{ font-family: system-ui, sans-serif; margin: 2rem auto; max-width: 820px; color: #1a1a1a; }}
  h1 {{ font-size: 1.4rem; }}
  .store {{ border: 1px solid #ddd; border-radius: 8px; padding: 1rem 1.2rem; margin: 1rem 0; }}
  .store h3 {{ margin: 0 0 .4rem; font-size: 1.1rem; }}
  .url code {{ font-size: .82rem; color: #444; word-break: break-all; }}
  .actions {{ margin-top: .8rem; display: flex; flex-wrap: wrap; gap: .5rem; }}
  .btn, button.napari {{
     display: inline-block; padding: .4rem .7rem; border-radius: 6px; border: 1px solid #bbb;
     background: #f6f6f6; color: #1a1a1a; text-decoration: none; font-size: .9rem; cursor: pointer;
  }}
  button.napari {{ background: #2d6cdf; border-color: #2456b8; color: #fff; }}
  button.napari:hover {{ background: #2456b8; }}
  .status {{ margin-top: .5rem; font-size: .85rem; color: #2456b8; min-height: 1.1em; }}
  footer {{ margin-top: 2rem; color: #888; font-size: .8rem; }}
</style>
</head>
<body>
  <h1>OME-Zarr export &mdash; validate &amp; view</h1>
  <p>Served locally from <code>{base}</code>. "Open in napari" launches napari on this
     machine (like the button in YouScope); the viewer links open in your browser.</p>
  {rows}
  <div class="status" id="status"></div>
  <footer>FAIR export local server {version}. This page and napari launching work offline;
     the browser viewers need internet unless self-hosted.</footer>
<script>
  document.querySelectorAll('button.napari').forEach(function (button) {{
    button.addEventListener('click', function () {{
      var status = document.getElementById('status');
      status.textContent = 'Launching napari...';
      fetch(button.getAttribute('data-endpoint'), {{ method: 'POST' }})
        .then(function (r) {{ return r.json(); }})
        .then(function (data) {{
          status.textContent = (data && data.message) ? data.message :
            (data && data.ok ? 'napari launching' : 'could not launch napari');
        }})
        .catch(function (err) {{ status.textContent = 'napari request failed: ' + err; }});
    }});
  }});
</script>
</body>
</html>
"""


def main():
    global _ROOT, _NAPARI_SCRIPT, _VALIDATOR_DIR, _VIZARR_DIR, _VALIDATOR_BASE
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--idle-timeout", type=int, default=1800)
    parser.add_argument("--napari-script", default=None,
                        help="path to napari_launch.py, enabling the /__napari endpoint")
    parser.add_argument("--validator-dir", default=None,
                        help="path to a self-hosted ome-ngff-validator build, served at /__validator/")
    parser.add_argument("--vizarr-dir", default=None,
                        help="path to a self-hosted vizarr build, served at /__vizarr/")
    parser.add_argument("--validator-base", default=None,
                        help="validator base URL used by the /__viewer page links")
    args = parser.parse_args()

    root = os.path.abspath(args.root)
    if not os.path.isdir(root):
        sys.stderr.write("root is not a directory: %s\n" % root)
        sys.exit(2)

    _ROOT = root
    _NAPARI_SCRIPT = os.path.abspath(args.napari_script) if args.napari_script else None
    _VALIDATOR_DIR = os.path.abspath(args.validator_dir) if args.validator_dir else None
    _VIZARR_DIR = os.path.abspath(args.vizarr_dir) if args.vizarr_dir else None
    if args.validator_base:
        _VALIDATOR_BASE = args.validator_base

    handler = functools.partial(Handler, directory=root)
    httpd = ThreadingHTTPServer((args.host, args.port), handler)
    host, port = httpd.server_address
    shown_host = "127.0.0.1" if host in ("0.0.0.0", "::", "") else host
    print("FAIR_VIZ_SERVER %s listening on http://%s:%d/" % (SCRIPT_VERSION, shown_host, port), flush=True)

    if args.idle_timeout and args.idle_timeout > 0:
        def _watch():
            while True:
                time.sleep(10)
                if time.time() - _last_activity > args.idle_timeout:
                    print("FAIR_VIZ_SERVER idle for >%ds - exiting" % args.idle_timeout, flush=True)
                    httpd.shutdown()
                    return
        threading.Thread(target=_watch, daemon=True).start()

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    print("FAIR_VIZ_SERVER stopped", flush=True)


if __name__ == "__main__":
    main()
