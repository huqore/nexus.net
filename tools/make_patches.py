"""Builds delta patches (NXP1 format) from older release APKs to a new one, plus the manifest the app reads.

usage: python tools/make_patches.py --new apks/release/nexus-net-v4.4.0.apk --old apks/release/nexus-net-v4.3.0.apk [...] --out apks/patches

NXP1 = bsdiff with zlib-compressed blocks instead of bzip2, so Android applies it with java.util.zip alone:
    "NXP1" | u64 newSize | u64 ctrlLen | u64 diffLen | u64 extraLen | zlib(ctrl) | zlib(diff) | zlib(extra)
ctrl is bsdiff's sequence of (x, y, z) triples, each int64 in bsdiff's sign-magnitude "offtin" encoding.

Every patch is applied back in Python and checked against the new APK's SHA-256 before it is written.
Needs: pip install bsdiff4
"""
import argparse, hashlib, json, os, re, struct, sys, zlib
import bsdiff4.core as core

MAGIC = b"NXP1"


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def version_of(path):
    m = re.search(r"(\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?)", os.path.basename(path))
    if not m:
        sys.exit(f"cannot read a version from the file name: {path}")
    return m.group(1)


def offtin(b, i):
    y = int.from_bytes(b[i:i + 7], "little") | ((b[i + 7] & 0x7F) << 56)
    return -y if b[i + 7] & 0x80 else y


def build(old_bytes, new_bytes):
    ctrl, diff, extra = core.diff(old_bytes, new_bytes)
    c, d, e = (zlib.compress(x, 9) for x in (ctrl, diff, extra))
    return MAGIC + struct.pack("<QQQQ", len(new_bytes), len(c), len(d), len(e)) + c + d + e


def apply(old_bytes, patch):
    """reference implementation, mirrors PatchApplier.kt"""
    if patch[:4] != MAGIC:
        raise ValueError("bad magic")
    new_size, lc, ld, le = struct.unpack("<QQQQ", patch[4:36])
    p = 36
    ctrl = zlib.decompress(patch[p:p + lc]); p += lc
    diff = zlib.decompress(patch[p:p + ld]); p += ld
    extra = zlib.decompress(patch[p:p + le])
    import numpy as np
    old_np = np.frombuffer(old_bytes, np.uint8)
    diff_np = np.frombuffer(diff, np.uint8)
    out = bytearray(); op = 0; dp = 0; ep = 0
    for i in range(0, len(ctrl), 24):
        x, y, z = offtin(ctrl, i), offtin(ctrl, i + 8), offtin(ctrl, i + 16)
        oo = np.zeros(x, np.uint8)
        lo, hi = max(0, op), min(len(old_bytes), op + x)
        if hi > lo:
            oo[lo - op:hi - op] = old_np[lo:hi]
        out += (diff_np[dp:dp + x] + oo).tobytes()   # uint8 addition wraps mod 256, like bsdiff
        dp += x; op += x
        out += extra[ep:ep + y]; ep += y; op += z
    if len(out) != new_size:
        raise ValueError("size mismatch")
    return bytes(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--new", required=True)
    ap.add_argument("--old", nargs="+", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    new_ver = version_of(a.new)
    new_bytes = open(a.new, "rb").read()
    target = {"name": os.path.basename(a.new), "size": len(new_bytes), "sha256": sha256(a.new)}
    patches = []
    for old in a.old:
        old_ver = version_of(old)
        if old_ver == new_ver:
            continue
        old_bytes = open(old, "rb").read()
        print(f"{old_ver} -> {new_ver} ...", flush=True)
        patch = build(old_bytes, new_bytes)
        if hashlib.sha256(apply(old_bytes, patch)).hexdigest() != target["sha256"]:
            sys.exit(f"self-check failed for {old}")
        name = f"nexus-net-v{new_ver}-from-v{old_ver}.nxpatch"
        with open(os.path.join(a.out, name), "wb") as f:
            f.write(patch)
        patches.append({"from_version": old_ver, "from_sha256": sha256(old), "from_size": len(old_bytes),
                        "name": name, "size": len(patch), "sha256": hashlib.sha256(patch).hexdigest()})
        print(f"  {name}: {len(patch) / 1e6:.1f} MB (full APK {len(new_bytes) / 1e6:.1f} MB), verified")
    manifest = {"format": "NXP1", "version": new_ver, "target": target, "patches": patches}
    mname = f"nexus-net-v{new_ver}.patches.json"
    with open(os.path.join(a.out, mname), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print("manifest:", mname)


if __name__ == "__main__":
    main()
