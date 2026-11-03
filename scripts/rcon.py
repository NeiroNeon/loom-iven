#!/usr/bin/env python3
"""rcon.py — минимальный RCON-клиент для smoke-тестов Loom Iven.
Использование: rcon.py <команда> [команда2 ...]
"""
import os
import socket
import struct
import sys

HOST = os.environ.get("RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("RCON_PORT", "25575"))
PASS = os.environ.get("RCON_PASS", "CHANGE_ME_RCON_PASSWORD")


def pkt(rid, ptype, payload: bytes) -> bytes:
    body = struct.pack("<ii", rid, ptype) + payload + b"\x00\x00"
    return struct.pack("<i", len(body)) + body


def recv_pkt(sock) -> tuple[int, int, bytes]:
    raw = b""
    while len(raw) < 4:
        chunk = sock.recv(4 - len(raw))
        if not chunk:
            raise ConnectionError("closed")
        raw += chunk
    (length,) = struct.unpack("<i", raw)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError("closed")
        data += chunk
    rid, ptype = struct.unpack("<ii", data[:8])
    return rid, ptype, data[8:-2]


def main() -> int:
    sock = socket.create_connection((HOST, PORT), timeout=10)
    sock.sendall(pkt(1, 3, PASS.encode()))
    rid, _, _ = recv_pkt(sock)
    if rid == -1:
        print("AUTH FAILED")
        return 1
    rc = 0
    for cmd in sys.argv[1:]:
        try:
            sock.sendall(pkt(2, 2, cmd.encode("utf-8")))
            _, _, payload = recv_pkt(sock)
            text = payload.decode("utf-8", errors="replace")
            print(f"$ {cmd}\n{text}\n")
        except Exception as ex:
            print(f"$ {cmd}\nОШИБКА RCON: {ex}\n")
            rc = 1
    sock.close()
    return rc


if __name__ == "__main__":
    sys.exit(main())
