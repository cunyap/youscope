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
"""

The Java side (`UseqWriter` / `UseqPayloadBuilder`) maps YouScope's parsed
`ExperimentMetadata` to a JSON payload of the fields `useq.MDASequence` accepts
(channels, time_plan, z_plan, stage_positions, metadata). This script turns that
payload into a *schema-guaranteed* acquisition plan: it constructs a real
`MDASequence`, so the field names and value ranges are validated by useq itself,
then serialises it to `sequence.useq.json` and (if PyYAML is present)
`sequence.useq.yaml`.

Doing the construction in Python is the whole point: the schema lives in the
`useq-schema` package, so we never hand-roll the output format and we never drift
from the spec. Anything the Java mapper could not express was already dropped (and
warned about) upstream; this script only validates and serialises what it is given.

Usage:
    useq_export.py --payload <payload.json> --out-dir <dir> [--basename sequence.useq]

Reads the payload from --payload (or stdin if that is "-"), writes
<basename>.json and, when possible, <basename>.yaml into --out-dir. Prints one
line "USEQ_WRITTEN <path>" per file written, and "USEQ_ERROR <message>" on failure
(exit code non-zero), so the launching Java process can report the outcome.

Requires the `useq-schema` package in the env pointed at by
`fairexport_cpython_executable` (falling back to the shared cell-detection /
cpython env). PyYAML is optional; without it only the JSON form is written.
"""
import argparse
import json
import os
import sys


def _load_payload(path):
    if path == "-":
        return json.load(sys.stdin)
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--payload", required=True,
                        help="path to the JSON payload, or '-' for stdin")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--basename", default="sequence.useq")
    args = parser.parse_args()

    try:
        payload = _load_payload(args.payload)
    except Exception as exc:  # noqa: BLE001 - report any read/parse problem uniformly
        sys.stderr.write("USEQ_ERROR could not read payload: %r\n" % (exc,))
        sys.exit(2)

    try:
        from useq import MDASequence
    except Exception as exc:  # pragma: no cover - depends on the env
        sys.stderr.write(
            "USEQ_ERROR useq-schema is not available in this environment (%r). "
            "Install it with:  pip install useq-schema\n" % (exc,))
        sys.exit(3)

    try:
        # Construct through the schema so field names and value ranges are validated
        # by useq rather than by us. A bad payload raises here instead of writing a
        # malformed file.
        sequence = MDASequence(**payload)
    except Exception as exc:  # noqa: BLE001 - surface validation errors verbatim
        sys.stderr.write("USEQ_ERROR MDASequence rejected the payload: %r\n" % (exc,))
        sys.exit(4)

    out_dir = os.path.abspath(args.out_dir)
    try:
        os.makedirs(out_dir, exist_ok=True)
    except OSError as exc:
        sys.stderr.write("USEQ_ERROR could not create output dir %s: %r\n" % (out_dir, exc))
        sys.exit(5)

    written = []

    # JSON is always written (model_dump_json is guaranteed by useq/pydantic).
    json_path = os.path.join(out_dir, args.basename + ".json")
    try:
        # exclude_unset keeps the file to what the acquisition actually specified,
        # rather than padding it with every schema default.
        try:
            text = sequence.model_dump_json(exclude_unset=True, indent=2)
        except TypeError:
            # older pydantic without indent kwarg
            text = sequence.model_dump_json(exclude_unset=True)
        with open(json_path, "w", encoding="utf-8") as handle:
            handle.write(text)
            if not text.endswith("\n"):
                handle.write("\n")
        written.append(json_path)
    except Exception as exc:  # noqa: BLE001
        sys.stderr.write("USEQ_ERROR could not write %s: %r\n" % (json_path, exc))
        sys.exit(6)

    # YAML is best-effort: only if PyYAML is installed. useq's own yaml() may exist
    # in some versions; fall back to dumping the parsed JSON so the two files agree.
    yaml_path = os.path.join(out_dir, args.basename + ".yaml")
    try:
        import yaml  # noqa: WPS433 - optional dependency
        try:
            data = json.loads(sequence.model_dump_json(exclude_unset=True))
        except Exception:  # noqa: BLE001 - fall back to full dump
            data = json.loads(sequence.model_dump_json())
        with open(yaml_path, "w", encoding="utf-8") as handle:
            yaml.safe_dump(data, handle, sort_keys=False, default_flow_style=False)
        written.append(yaml_path)
    except ImportError:
        sys.stderr.write("USEQ_NOTE PyYAML not installed; wrote JSON only.\n")
    except Exception as exc:  # noqa: BLE001 - a yaml failure must not lose the json
        sys.stderr.write("USEQ_NOTE could not write YAML (%r); JSON was written.\n" % (exc,))

    for path in written:
        print("USEQ_WRITTEN %s" % path, flush=True)


if __name__ == "__main__":
    main()
