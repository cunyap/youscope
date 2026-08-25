# /*******************************************************************************
#  * Copyright (c) 2026 Andreas P. Cuny.
#  * All rights reserved. This program and the accompanying materials
#  * are made available under the terms of the GNU Public License v2.0
#  * which accompanies this distribution, and is available at
#  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
#  * 
#  * Contributors:
#  *     Andreas P. Cuny - initial API and implementation
#  ******************************************************************************/

#!/usr/bin/env python3
"""
The YouScope Cellpose plugin runs a persistent Python "server" that keeps the
cpsam model loaded between detections. Normally the plugin manages it, but a
server left over from a crashed run (or an older plugin version) can linger and
interfere. Run this script to shut them all down.

USAGE (from the YouScope cpython env, or any Python 3):
    python shutdown_cellpose.py                 # scans ports 59712-59721
    python shutdown_cellpose.py 59712 59713     # only these ports

It sends the graceful {"cmd":"shutdown"} command that v0.4.10+ servers understand.
Servers older than v0.4.10 do not understand it and will not stop. For those,
kill the Python processes directly:

    Windows : taskkill /F /IM python.exe        (kills ALL python; blunt but sure)
              or find the one on the port:
              netstat -ano | findstr 59712      Note the PID
              taskkill /F /PID <pid>
    Linux/Mac: pkill -f cellpose_server.py
"""
import socket
import struct
import sys
import json


def shutdown_port(host, port, timeout=3.0):
    try:
        with socket.create_connection((host, port), timeout=timeout) as s:
            s.settimeout(timeout)
            req = json.dumps({"cmd": "shutdown"}).encode("utf-8")
            s.sendall(struct.pack(">I", len(req)) + req)
            raw_len = b""
            while len(raw_len) < 4:
                chunk = s.recv(4 - len(raw_len))
                if not chunk:
                    break
                raw_len += chunk
            if len(raw_len) < 4:
                return "no response (maybe an old server that ignores shutdown)"
            n = struct.unpack(">I", raw_len)[0]
            buf = b""
            while len(buf) < n:
                chunk = s.recv(n - len(buf))
                if not chunk:
                    break
                buf += chunk
            info = json.loads(buf.decode("utf-8"))
            return "shut down (was version %s)" % info.get("script_version", "?")
    except (ConnectionRefusedError, OSError):
        return None  # nothing listening here
    except Exception as e:
        return "error: %s" % e


def main():
    host = "127.0.0.1"
    ports = [int(p) for p in sys.argv[1:]] or list(range(59712, 59722))
    found = 0
    for port in ports:
        result = shutdown_port(host, port)
        if result is not None:
            found += 1
            print("port %d: %s" % (port, result))
    if found == 0:
        print("No Cellpose servers found on ports %s." % (ports,))
    else:
        print("Done. If any reported 'no response', it is an old server - kill the "
              "python process manually (see the header of this script).")


if __name__ == "__main__":
    main()
