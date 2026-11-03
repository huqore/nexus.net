package net.nexus.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testParseGitHubReleasesFormat() {
        val json = """
            [
              {
                "tag_name": "v3.1.0",
                "name": "v3.1.0",
                "body": "Changelog text",
                "published_at": "2026-11-03T18:42:13Z",
                "draft": false,
                "assets": [
                  {
                    "name": "nexus-net-v3.1.0.apk",
                    "browser_download_url": "https://github.com/huqore/nexus.net/releases/download/v3.1.0/nexus-net-v3.1.0.apk",
                    "size": 71568673
                  }
                ]
              }
            ]
        """.trimIndent()

        val release = UpdateChecker.parseReleaseCandidates(json)
        assertNotNull(release)
        assertEquals("v3.1.0", release!!.tagName)
        assertEquals("3.1.0", release.versionName)
        assertEquals("Changelog text", release.changelog)
        assertEquals("nexus-net-v3.1.0.apk", release.apkFileName)
        assertEquals("https://github.com/huqore/nexus.net/releases/download/v3.1.0/nexus-net-v3.1.0.apk", release.downloadUrl)
        assertEquals(71568673L, release.apkSize)
    }

    @Test
    fun testParseUnghFormat() {
        val json = """
            {
              "releases": [
                {
                  "id": 433083993,
                  "tag": "v3.1.0",
                  "name": "v3.1.0",
                  "draft": false,
                  "markdown": "Ungh changelog",
                  "publishedAt": "2026-11-03T18:42:13Z",
                  "assets": [
                    {
                      "downloadUrl": "https://github.com/huqore/nexus.net/releases/download/v3.1.0/nexus-net-v3.1.0.apk",
                      "size": 71568673
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val release = UpdateChecker.parseReleaseCandidates(json)
        assertNotNull(release)
        assertEquals("v3.1.0", release!!.tagName)
        assertEquals("3.1.0", release.versionName)
        assertEquals("Ungh changelog", release.changelog)
        assertEquals("nexus-net-v3.1.0.apk", release.apkFileName)
        assertEquals("https://github.com/huqore/nexus.net/releases/download/v3.1.0/nexus-net-v3.1.0.apk", release.downloadUrl)
        assertEquals(71568673L, release.apkSize)
    }

    @Test
    fun testIsVersionNewer() {
        assertTrue(UpdateChecker.isVersionNewer("3.1.0", "3.0.1"))
        assertTrue(UpdateChecker.isVersionNewer("3.1.0", "3.1.0-test"))
        assertTrue(UpdateChecker.isVersionNewer("3.1.1", "3.1.0"))
        assertFalse(UpdateChecker.isVersionNewer("3.1.0", "3.1.0"))
        assertFalse(UpdateChecker.isVersionNewer("3.0.0", "3.1.0"))
    }
}
