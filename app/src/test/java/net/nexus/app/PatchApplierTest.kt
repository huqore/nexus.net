package net.nexus.app

import net.nexus.app.update.PatchApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PatchApplierTest {
    private fun res(name: String) = File(javaClass.classLoader!!.getResource("patch/$name")!!.toURI())

    @Test fun appliesPythonMadePatchByteForByte() {
        val out = File.createTempFile("nxp", ".bin").apply { deleteOnExit() }
        var last = 0f
        PatchApplier.apply(res("old.bin"), res("old_to_new.nxpatch"), out) { last = it }
        assertEquals(PatchApplier.sha256(res("new.bin")), PatchApplier.sha256(out))
        assertEquals(1f, last, 0.0001f)
    }

    @Test fun corruptPatchIsRejected() {
        val bad = File.createTempFile("nxp", ".nxpatch").apply { deleteOnExit() }
        val bytes = res("old_to_new.nxpatch").readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 0x5A).toByte()
        bad.writeBytes(bytes)
        val out = File.createTempFile("nxp", ".bin").apply { deleteOnExit() }
        val failed = runCatching { PatchApplier.apply(res("old.bin"), bad, out) }.isFailure ||
            PatchApplier.sha256(out) != PatchApplier.sha256(res("new.bin"))
        assertTrue("a corrupted patch must not produce the target file", failed)
    }

    @Test fun wrongMagicIsRejected() {
        val bad = File.createTempFile("nxp", ".nxpatch").apply { deleteOnExit(); writeBytes(ByteArray(64)) }
        val out = File.createTempFile("nxp", ".bin").apply { deleteOnExit() }
        assertTrue(runCatching { PatchApplier.apply(res("old.bin"), bad, out) }.isFailure)
    }

    /** Real APKs, if they are on this machine: 4.2.0 + patch must give the exact 4.3.0 file. */
    @Test fun realApkPatchIfPresent() {
        val old = File("../apks/release/nexus-net-v4.2.0.apk")
        val patch = File(System.getProperty("nx.realPatch") ?: "")
        val target = File("../apks/release/nexus-net-v4.3.0.apk")
        if (!old.exists() || !patch.exists() || !target.exists()) return
        val out = File.createTempFile("nxp", ".apk").apply { deleteOnExit() }
        val t = System.currentTimeMillis()
        PatchApplier.apply(old, patch, out)
        println("real patch applied in ${System.currentTimeMillis() - t} ms")
        assertEquals(PatchApplier.sha256(target), PatchApplier.sha256(out))
    }
}
