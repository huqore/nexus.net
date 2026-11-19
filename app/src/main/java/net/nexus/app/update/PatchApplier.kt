package net.nexus.app.update

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.InflaterInputStream

/**
 * Applies an NXP1 delta patch (made by tools/make_patches.py) to the installed APK.
 *
 * NXP1 = bsdiff whose three blocks are zlib-compressed:
 *   "NXP1" | u64 newSize | u64 ctrlLen | u64 diffLen | u64 extraLen | zlib(ctrl) | zlib(diff) | zlib(extra)
 *
 * Streams everything: the old APK is read through a RandomAccessFile and the new one is written
 * sequentially, so memory stays at a few buffers even for a 70 MB APK.
 */
object PatchApplier {

    class PatchException(msg: String) : Exception(msg)

    private const val MAGIC = "NXP1"
    private const val HEADER = 36

    /** Writes the patched file to [out]. [onProgress] gets 0..1 of the new file written. */
    fun apply(old: File, patch: File, out: File, onProgress: (Float) -> Unit = {}) {
        val header = ByteArray(HEADER)
        DataInputStream(FileInputStream(patch)).use { it.readFully(header) }
        if (String(header, 0, 4, Charsets.US_ASCII) != MAGIC) throw PatchException("bad patch header")
        val newSize = u64(header, 4)
        val ctrlLen = u64(header, 12)
        val diffLen = u64(header, 20)
        val extraLen = u64(header, 28)
        if (newSize <= 0 || ctrlLen < 0 || diffLen < 0 || extraLen < 0 ||
            HEADER + ctrlLen + diffLen + extraLen != patch.length()
        ) throw PatchException("patch length mismatch")

        val ctrl = block(patch, HEADER.toLong(), ctrlLen)
        val diff = block(patch, HEADER + ctrlLen, diffLen)
        val extra = block(patch, HEADER + ctrlLen + diffLen, extraLen)
        val triple = ByteArray(24)
        val buf = ByteArray(64 * 1024)
        val oldBuf = ByteArray(64 * 1024)
        var newPos = 0L
        var oldPos = 0L
        var lastReport = -1

        try {
            RandomAccessFile(old, "r").use { oldFile ->
                val oldLen = oldFile.length()
                BufferedOutputStream(FileOutputStream(out), 256 * 1024).use { w ->
                    while (newPos < newSize) {
                        if (!readFully(ctrl, triple)) throw PatchException("truncated control block")
                        val x = offtin(triple, 0)
                        val y = offtin(triple, 8)
                        val z = offtin(triple, 16)
                        if (x < 0 || y < 0 || newPos + x + y > newSize) throw PatchException("bad control triple")

                        // x bytes: diff + old
                        var left = x
                        while (left > 0) {
                            val n = minOf(left, buf.size.toLong()).toInt()
                            if (!readFully(diff, buf, n)) throw PatchException("truncated diff block")
                            java.util.Arrays.fill(oldBuf, 0, n, 0)
                            val from = oldPos
                            val lo = maxOf(0L, from)
                            val hi = minOf(oldLen, from + n)
                            if (hi > lo) {
                                oldFile.seek(lo)
                                oldFile.readFully(oldBuf, (lo - from).toInt(), (hi - lo).toInt())
                            }
                            for (i in 0 until n) buf[i] = (buf[i] + oldBuf[i]).toByte()
                            w.write(buf, 0, n)
                            left -= n; oldPos += n; newPos += n
                        }
                        // y bytes copied from extra
                        left = y
                        while (left > 0) {
                            val n = minOf(left, buf.size.toLong()).toInt()
                            if (!readFully(extra, buf, n)) throw PatchException("truncated extra block")
                            w.write(buf, 0, n)
                            left -= n; newPos += n
                        }
                        oldPos += z
                        val pct = (newPos * 100 / newSize).toInt()
                        if (pct != lastReport) { lastReport = pct; onProgress(pct / 100f) }
                    }
                }
            }
        } finally {
            ctrl.close(); diff.close(); extra.close()
        }
        if (out.length() != newSize) throw PatchException("result size mismatch")
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val b = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(b)
                if (n < 0) break
                md.update(b, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun block(file: File, offset: Long, length: Long): InputStream {
        val fis = FileInputStream(file)
        fis.channel.position(offset)
        return InflaterInputStream(BufferedInputStream(Bounded(fis, length), 64 * 1024), java.util.zip.Inflater(), 64 * 1024)
    }

    private fun readFully(input: InputStream, b: ByteArray, len: Int = b.size): Boolean {
        var off = 0
        while (off < len) {
            val n = input.read(b, off, len - off)
            if (n < 0) return false
            off += n
        }
        return true
    }

    private fun u64(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        return v
    }

    /** bsdiff "offtin": little-endian magnitude, sign in the top bit of the last byte. */
    private fun offtin(b: ByteArray, off: Int): Long {
        var y = (b[off + 7].toLong() and 0x7F)
        for (i in 6 downTo 0) y = (y shl 8) or (b[off + i].toLong() and 0xFF)
        return if (b[off + 7].toInt() and 0x80 != 0) -y else y
    }

    private class Bounded(private val inner: InputStream, private var left: Long) : InputStream() {
        override fun read(): Int {
            if (left <= 0) return -1
            val r = inner.read(); if (r >= 0) left--; return r
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (left <= 0) return -1
            val n = inner.read(b, off, minOf(len.toLong(), left).toInt())
            if (n > 0) left -= n
            return n
        }
        override fun close() = inner.close()
    }
}
