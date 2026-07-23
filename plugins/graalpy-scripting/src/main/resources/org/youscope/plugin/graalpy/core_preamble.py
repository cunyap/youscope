#*******************************************************************************
# * Copyright (c) 2026 Andreas P. Cuny
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the GNU Public License v2.0
# * which accompanies this distribution, and is available at
# * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
# *
# * Contributors:
# *     Andreas P. Cuny - initial API and implementation
# ******************************************************************************/

# Evaluated by GraalPyScriptEngine Phase 1 (guaranteed to run).
# Self-injects all public names into __main__.__dict__ at bottom.

import os as _os
import struct as _struct
import tempfile as _tf
import json as _json
import sys as _sys


_YOUSCOPE_BINDINGS = [
    'youscopeServer', 'youscopeClient', 'microscope',
    'measurementContext', 'jobs', 'evaluationNumber',
    'well', 'position', 'executionInformation', 'scriptInterface']

def _refresh_bindings():
    import builtins as _b
    import sys as _rs
    _g = _rs.modules['__main__'].__dict__
    for _name in _YOUSCOPE_BINDINGS:
        try:
            import polyglot as _pl
            _val = _pl.import_value(_name)
            if _val is not None:
                _g[_name] = _val
                setattr(_b, _name, _val)
        except BaseException:
            pass

# CPython bridge 
def _run_cpython(script_path, **kwargs):
    import java as _java
    _ys_dir = _java.type('java.lang.System').getProperty('user.dir', '.')
    _cfg = _os.path.join(_ys_dir, 'graalpy_config.txt')
    _cpython = None
    if _os.path.exists(_cfg):
        for _line in open(_cfg):
            if _line.startswith('cpython_executable='):
                _cpython = _line.strip().split('=', 1)[1]
                break
    if not _cpython or not _os.path.exists(_cpython):
        print('[YouScope] CPython not found. Run setup_graalpy_venv.bat')
        return {}
    _args_file = _tf.mktemp(suffix='.json')
    _res_file  = _tf.mktemp(suffix='.json')
    kwargs['result_file'] = _res_file
    with open(_args_file, 'w') as _f:
        _json.dump(kwargs, _f)
    import subprocess as _sp
    try:
        _proc = _sp.run([_cpython, script_path],
                        env={**_os.environ, 'YOUSCOPE_ARGS_FILE': _args_file},
                        capture_output=True, text=True, timeout=300)
        if _proc.stdout.strip():
            print(_proc.stdout.strip())
        if _proc.stderr.strip():
            print('[YouScope CPython stderr]', _proc.stderr.strip()[:500])
        if _proc.returncode != 0:
            print('[YouScope CPython] Exit code:', _proc.returncode)
    except _sp.TimeoutExpired:
        print('[YouScope CPython] Timeout after 300s')
    except Exception as _e:
        print('[YouScope] CPython bridge error:', _e)
    _result = {}
    if _os.path.exists(_res_file):
        with open(_res_file) as _f:
            _result = _json.load(_f)
    for _p in (_args_file, _res_file):
        try: _os.remove(_p)
        except: pass
    return _result

# exec_in_cpython / run_in_cpython 
_EXEC_SCRIPT = (
    "import os, json\n"
    "af = os.environ.get('YOUSCOPE_ARGS_FILE', '')\n"
    "data = json.load(open(af)) if af else {}\n"
    "code = data['code']\n"
    "returns = data.get('returns') or []\n"
    "ctx = data.get('context') or {}\n"
    "ns = dict(ctx)\n"
    "exec(compile(code, '<cpython_exec>', 'exec'), ns)\n"
    "out = {}\n"
    "for k in returns:\n"
    "    v = ns.get(k)\n"
    "    if hasattr(v, 'tolist'): v = v.tolist()\n"
    "    elif hasattr(v, 'item'): v = v.item()\n"
    "    out[k] = v\n"
    "res = data.get('result_file', '')\n"
    "if res: open(res, 'w').write(json.dumps(out))\n"
)


def exec_in_cpython(code, returns=None, **context):
    """Execute Python code in CPython env. Returns dict of named results."""
    import java as _java
    _ys = _java.type('java.lang.System').getProperty('user.dir', '.')
    _script = _os.path.join(_ys, 'scripts', '_exec_in_cpython.py')
    if not _os.path.exists(_script):
        _os.makedirs(_os.path.dirname(_script), exist_ok=True)
        with open(_script, 'w') as _f:
            _f.write(_EXEC_SCRIPT)
    return _run_cpython(_script, code=code, returns=returns or [], context=context)


def run_in_cpython(script_path, **kwargs):
    """Run a Python script file in the CPython environment."""
    return _run_cpython(script_path, **kwargs)

# Core imaging helpers
def takeImage(server, channelGroup, channel, exposureMS, cameraName=None):
    """Take an image via youscopeServer.getMicroscope().
    
    Args:
        server:       youscopeServer
        channelGroup: channel group name (e.g. "Channels")
        channel:      channel name (e.g. "FITC", "BF", "DAPI")
        exposureMS:   exposure time in milliseconds
        cameraName:   optional camera device name; uses default camera if None
    """
    _micro  = server.getMicroscope()
    _camera = (_micro.getCameraDevice(cameraName)
               if cameraName is not None
               else _micro.getCameraDevice())
    return _camera.makeImage(channelGroup, channel, float(exposureMS))


def toNumpyImage(imageEvent):
    """Convert ImageEvent to list-of-lists (uint8, no numpy needed)."""
    import array as _arr
    _w, _h  = imageEvent.getWidth(), imageEvent.getHeight()
    _bpp    = imageEvent.getBytesPerPixel()
    _bd     = imageEvent.getBitDepth()
    # Java byte[] is signed (-128..127); Python bytes needs 0..255
    _jbytes = imageEvent.getImageData()
    _raw    = bytes([b & 0xFF for b in _jbytes])
    _fmt    = 'B' if _bpp == 1 else 'H'
    _flat   = _arr.array(_fmt, _raw)
    _mx     = float((1 << _bd) - 1) if _bd > 0 else 65535.0
    if _mx <= 0: _mx = 65535.0
    _rows   = []
    for _r in range(_h):
        _row = _flat[_r * _w:(_r + 1) * _w]
        if _bpp == 1:
            _rows.append(list(_row))
        else:
            # clamp to [0,255] to guard against fp rounding
            _rows.append([min(255, max(0, int(v * 255.0 / _mx))) for v in _row])
    if imageEvent.isTransposeY(): _rows = _rows[::-1]
    if imageEvent.isTransposeX(): _rows = [r[::-1] for r in _rows]
    return _rows


def displayImage(img_or_event, title='YouScope Image'):
    """Display image via matplotlib in CPython env."""
    import java as _java
    _ys = _java.type('java.lang.System').getProperty('user.dir', '.')
    if hasattr(img_or_event, 'getWidth'):
        _ev = img_or_event
        _w, _h, _bpp = _ev.getWidth(), _ev.getHeight(), _ev.getBytesPerPixel()
        _jbytes = _ev.getImageData()
        _raw = bytes([b & 0xFF for b in _jbytes])
    else:
        _rows = img_or_event; _h = len(_rows); _w = len(_rows[0]) if _rows else 0; _bpp = 1
        import array as _a2; _raw = bytes(_a2.array('B', [v for r in _rows for v in r]))
    _tmp_bin = _tf.mktemp(suffix='.bin')
    with open(_tmp_bin, 'wb') as _f:
        _f.write(_struct.pack('>III', _w, _h, _bpp)); _f.write(_raw)
    _script = _os.path.join(_ys, 'scripts', '_display_image.py')
    if not _os.path.exists(_script):
        _os.makedirs(_os.path.dirname(_script), exist_ok=True)
        _disp_src = (
            "import os,json,struct,numpy as np\n"
            "import matplotlib; matplotlib.use('TkAgg')\n"
            "import matplotlib.pyplot as plt\n"
            "af=os.environ.get('YOUSCOPE_ARGS_FILE','')\n"
            "args=json.load(open(af)) if af else {}\n"
            "src=args['image_bin'];title=args.get('title','Image')\n"
            "res=args.get('result_file','')\n"
            "with open(src,'rb') as fh: w,h,bpp=struct.unpack('>III',fh.read(12));raw=fh.read()\n"
            "img=np.frombuffer(raw,dtype=np.uint8 if bpp==1 else np.uint16).reshape(h,w)\n"
            "if bpp==2 and img.max()>0: img=(img.astype(np.float32)*255/img.max()).astype(np.uint8)\n"
            "plt.figure(figsize=(8,8));plt.imshow(img,cmap='gray');plt.title(title)\n"
            "plt.axis('off');plt.tight_layout();plt.show()\n"
            "if res: open(res,'w').write('{\"ok\":true}')\n"
        )
        with open(_script, 'w') as _f:
            _f.write(_disp_src)
    _run_cpython(_script, image_bin=_tmp_bin, title=title)
    try: _os.remove(_tmp_bin)
    except: pass


def saveImage(imageEvent, filepath):
    """Save ImageEvent as TIFF via CPython (numpy + tifffile)."""
    import java as _java
    _ys = _java.type('java.lang.System').getProperty('user.dir', '.')
    _w, _h, _bpp = imageEvent.getWidth(), imageEvent.getHeight(), imageEvent.getBytesPerPixel()
    _jbytes = imageEvent.getImageData()
    _raw = bytes([b & 0xFF for b in _jbytes])
    _tmp_bin = _tf.mktemp(suffix='.bin')
    with open(_tmp_bin, 'wb') as _f:
        _f.write(_struct.pack('>III', _w, _h, _bpp)); _f.write(_raw)
    _script = _os.path.join(_ys, 'scripts', '_save_image.py')
    if not _os.path.exists(_script):
        _os.makedirs(_os.path.dirname(_script), exist_ok=True)
        _save_src = (
            "import os,json,struct,numpy as np,tifffile\n"
            "af=os.environ.get('YOUSCOPE_ARGS_FILE','')\n"
            "args=json.load(open(af)) if af else {}\n"
            "src=args['image_bin'];dst=args['output_path'];res=args.get('result_file','')\n"
            "with open(src,'rb') as fh: w,h,bpp=struct.unpack('>III',fh.read(12));raw=fh.read()\n"
            "img=np.frombuffer(raw,dtype=np.uint8 if bpp==1 else np.uint16).reshape(h,w)\n"
            "tifffile.imwrite(dst,img)\n"
            "if res: open(res,'w').write('{\"saved\":\"'+dst+'\"}')\n"
        )
        with open(_script, 'w') as _f:
            _f.write(_save_src)
    _result = _run_cpython(_script, image_bin=_tmp_bin, output_path=filepath)
    try: _os.remove(_tmp_bin)
    except: pass
    print('Saved:', filepath)
    return _result

# CPython import proxy
# Intercepts amu failed import and returns a proxy object.

class _CPythonProxy(object):
    """Proxy for a CPython module or attribute."""
    def __init__(self, mod, attr=None):
        self.__dict__['_mod']  = mod
        self.__dict__['_attr'] = attr

    def __repr__(self):
        _n = self.__dict__['_mod']
        _a = self.__dict__['_attr']
        return '<CPython:' + (_n + '.' + _a if _a else _n) + '>'

    def __call__(self, *args, **kwargs):
        _mod  = self.__dict__['_mod']
        _attr = self.__dict__['_attr']
        if not _attr:
            return None
        _main = _sys.modules.get('__main__')
        _fn   = getattr(_main, 'exec_in_cpython', None) if _main else None
        if _fn:
            try:
                _expr = _mod + '.' + _attr + '(*_args, **_kw)'
                _r = _fn('_result = ' + _expr,
                          returns=['_result'], _args=list(args), _kw=kwargs)
                return _r.get('_result') if isinstance(_r, dict) else _r
            except Exception:
                pass
        return None

    def __getattr__(self, name):
        if name.startswith('__'):
            raise AttributeError(name)
        _mod = self.__dict__['_mod']
        return _CPythonProxy(_mod, name)


class _CPythonImportHook(object):
    """Last-resort import hook: proxies failed imports to CPython."""
    def find_module(self, name, path=None):
        return self if name not in _sys.modules else None

    def load_module(self, name):
        if name in _sys.modules:
            return _sys.modules[name]
        proxy = _CPythonProxy(name)
        _sys.modules[name] = proxy
        return proxy


# Register hook last on sys.meta_path
_sys.meta_path = [h for h in _sys.meta_path
                  if type(h).__name__ != '_CPythonImportHook']
_sys.meta_path.append(_CPythonImportHook())

# Background execution for GUI apps

def exec_in_cpython_background(code, **context):
    """Run code in CPython in a background process (non-blocking).
    Use for GUI apps: napari, matplotlib interactive, Qt apps.
    The YouScope UI stays responsive while the viewer is open.

    Usage:
        exec_in_cpython_background(
            "import napari, numpy as np\n"
            "v = napari.Viewer()\n"
            "v.add_image(np.array(image, dtype='uint8'))\n"
            "napari.run()\n",
            image=image)
    """
    import java as _java
    import subprocess as _sp
    _ys = _java.type('java.lang.System').getProperty('user.dir', '.')
    _script = _os.path.join(_ys, 'scripts', '_exec_in_cpython.py')
    if not _os.path.exists(_script):
        with open(_script, 'w') as _f:
            _f.write(_EXEC_SCRIPT)
    _cfg = _os.path.join(_ys, 'graalpy_config.txt')
    _cpython = None
    if _os.path.exists(_cfg):
        for _line in open(_cfg):
            if _line.startswith('cpython_executable='):
                _cpython = _line.strip().split('=', 1)[1]
                break
    if not _cpython or not _os.path.exists(_cpython):
        print('[YouScope] CPython not found.')
        return None
    _args_file = _tf.mktemp(suffix='.json')
    _args_data = {'code': code, 'returns': [], 'context': context}
    with open(_args_file, 'w') as _f:
        _json.dump(_args_data, _f)
    # Use pythonw.exe on Windows (no console window, proper GUI session)
    # Fall back to python.exe if pythonw.exe not present
    _cpython_gui = _cpython
    if _os.name == 'nt':
        _pythonw = _cpython.replace('python.exe', 'pythonw.exe')
        if _os.path.exists(_pythonw):
            _cpython_gui = _pythonw

    # Set PYTHONPATH and ensure Qt can find the display
    _env = {**_os.environ, 'YOUSCOPE_ARGS_FILE': _args_file}
    # On Windows ensure the subprocess runs in an interactive desktop context
    _env.pop('QT_QPA_PLATFORM', None)  # let Qt auto-detect

    # Popen = non-blocking so YouScope UI stays responsive
    _proc = _sp.Popen(
        [_cpython_gui, _script],
        env=_env,
        stdout=_sp.PIPE, stderr=_sp.PIPE,
    )
    print('[YouScope] CPython GUI started (PID %d) using %s' % (
        _proc.pid, _os.path.basename(_cpython_gui)))

    # Check immediately if process died (e.g. import error)
    import time as _time
    _time.sleep(0.5)
    if _proc.poll() is not None:
        _out, _err = _proc.communicate()
        print('[YouScope] CPython GUI exited immediately (exit code %d)' % _proc.returncode)
        if _out: print('[stdout]', _out.decode('utf-8', errors='replace')[:500])
        if _err: print('[stderr]', _err.decode('utf-8', errors='replace')[:500])
    else:
        print('[YouScope] Process running. Close the viewer window when done.')

    return _proc


# Self-inject into __main__ 

_main = _sys.modules['__main__'].__dict__
_inject = {
    '_YOUSCOPE_BINDINGS': _YOUSCOPE_BINDINGS,
    '_refresh_bindings':  _refresh_bindings,
    '_run_cpython':       _run_cpython,
    '_EXEC_SCRIPT':       _EXEC_SCRIPT,
    'exec_in_cpython':            exec_in_cpython,
    'exec_in_cpython_background': exec_in_cpython_background,
    'run_in_cpython':     run_in_cpython,
    'takeImage':          takeImage,
    'toNumpyImage':       toNumpyImage,
    'displayImage':       displayImage,
    'saveImage':          saveImage,
    '_CPythonProxy':      _CPythonProxy,
    '_CPythonImportHook': _CPythonImportHook,
    '_os':                _os,
    '_struct':            _struct,
    '_tf':                _tf,
    '_json':              _json,
    '_sys':               _sys,
}
_main.update(_inject)
_main.pop('_main', None)
_main.pop('_inject', None)