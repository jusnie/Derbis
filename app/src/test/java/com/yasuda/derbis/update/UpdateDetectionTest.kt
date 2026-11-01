package com.yasuda.derbis.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDetectionTest {

    private fun release(
        tag: String,
        version: String,
        track: DerbisUpdateTrack,
        publishedAtMs: Long = 0L,
        apk: Boolean = true
    ) = UpdateRelease(
        tag = tag,
        versionName = version,
        track = track,
        buildId = buildIdOf(tag),
        title = tag,
        notes = "",
        htmlUrl = "https://github.com/jusnie/Derbis/releases/tag/$tag",
        publishedAtMs = publishedAtMs,
        assets = if (apk) {
            listOf(
                UpdateReleaseAsset(
                    name = "derbis.apk",
                    downloadUrl = "https://example.invalid/derbis.apk",
                    contentType = "application/vnd.android.package-archive",
                    sizeBytes = 1L
                )
            )
        } else {
            emptyList()
        }
    )

    @Test
    fun classifiesTagsIntoReleaseLines() {
        assertEquals(DerbisUpdateTrack.Stable, updateTrackOf("v1.1.0", prerelease = false))
        assertEquals(DerbisUpdateTrack.Test, updateTrackOf("prebuild-1.6.0-prebuild.1-d2b6d7c", prerelease = true))
        // A prebuild tag is a test build even if GitHub did not mark the release as a pre-release.
        assertEquals(DerbisUpdateTrack.Test, updateTrackOf("prebuild-1.5.0-4b764ac", prerelease = false))
        // A v-tag published as a pre-release is a test build, not a stable one.
        assertEquals(DerbisUpdateTrack.Test, updateTrackOf("v1.2.0", prerelease = true))
        // Not a phone build at all.
        assertNull(updateTrackOf("desktop-v1.0.3", prerelease = false))
        assertNull(updateTrackOf("random-tag", prerelease = false))
        assertNull(updateTrackOf("", prerelease = false))
    }

    @Test
    fun readsTheVersionOutOfATag() {
        assertEquals("1.1.0", versionOf("v1.1.0"))
        assertEquals("1.6.0-prebuild.1", versionOf("prebuild-1.6.0-prebuild.1-d2b6d7c"))
        assertEquals("1.5.0", versionOf("prebuild-1.5.0-4b764ac"))
        // A prebuild without the counter still drops only the commit.
        assertEquals("1.5.0", versionOf("prebuild-1.5.0-abcdef0"))
        assertNull(versionOf("vnext"))
    }

    @Test
    fun readsTheBuildIdOnlyFromPrebuildTags() {
        assertEquals("d2b6d7c", buildIdOf("prebuild-1.6.0-prebuild.1-d2b6d7c"))
        assertEquals("4b764ac", buildIdOf("prebuild-1.5.0-4b764ac"))
        assertEquals("", buildIdOf("v1.1.0"))
    }

    @Test
    fun aTestBuildIsOfferedTheNewerTestRelease() {
        val releases = listOf(
            release("prebuild-1.6.0-prebuild.1-d2b6d7c", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 2_000L),
            release("v1.1.0", "1.1.0", DerbisUpdateTrack.Stable, 1_000L)
        )
        val selected = selectUpdateRelease(releases, DerbisUpdateTrack.Test)
        assertEquals("prebuild-1.6.0-prebuild.1-d2b6d7c", selected?.tag)
        assertTrue(isNewerThan(selected!!, "1.5.0"))
        // ...and it is not newer than itself.
        assertFalse(isNewerThan(selected, "1.6.0-prebuild.1"))
    }

    @Test
    fun aStableBuildNeverSeesATestRelease() {
        val releases = listOf(
            release("prebuild-1.6.0-prebuild.1-d2b6d7c", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 2_000L),
            release("v1.1.0", "1.1.0", DerbisUpdateTrack.Stable, 1_000L)
        )
        val selected = selectUpdateRelease(releases, DerbisUpdateTrack.Stable)
        assertEquals("v1.1.0", selected?.tag)
        assertFalse(isNewerThan(selected!!, "1.1.0"))
    }

    @Test
    fun aTestBuildFallsBackToStableWhenTheTestLineIsEmpty() {
        val releases = listOf(release("v1.1.0", "1.1.0", DerbisUpdateTrack.Stable, 1_000L))
        assertEquals("v1.1.0", selectUpdateRelease(releases, DerbisUpdateTrack.Test)?.tag)
    }

    @Test
    fun releasesWithoutAnApkAreNeverSelected() {
        val releases = listOf(
            release("prebuild-1.7.0-prebuild.1-aaaaaaa", "1.7.0-prebuild.1", DerbisUpdateTrack.Test, 3_000L, apk = false),
            release("prebuild-1.6.0-prebuild.1-d2b6d7c", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 2_000L)
        )
        assertEquals(
            "prebuild-1.6.0-prebuild.1-d2b6d7c",
            selectUpdateRelease(releases, DerbisUpdateTrack.Test)?.tag
        )
    }

    @Test
    fun theHighestVersionWinsRegardlessOfPublishOrder() {
        val releases = listOf(
            release("prebuild-1.4.0-prebuild.9-1111111", "1.4.0-prebuild.9", DerbisUpdateTrack.Test, 9_000L),
            release("prebuild-1.6.0-prebuild.1-d2b6d7c", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 2_000L)
        )
        assertEquals(
            "prebuild-1.6.0-prebuild.1-d2b6d7c",
            selectUpdateRelease(releases, DerbisUpdateTrack.Test)?.tag
        )
    }

    @Test
    fun equalVersionsPreferTheLaterPublication() {
        val releases = listOf(
            release("prebuild-1.6.0-prebuild.1-aaaaaaa", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 1_000L),
            release("prebuild-1.6.0-prebuild.1-bbbbbbb", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 5_000L)
        )
        assertEquals(
            "prebuild-1.6.0-prebuild.1-bbbbbbb",
            selectUpdateRelease(releases, DerbisUpdateTrack.Test)?.tag
        )
    }

    @Test
    fun anInstalledBuildAheadOfTheStableLineIsTreatedAsATestBuild() {
        val releases = listOf(
            release("prebuild-1.6.0-prebuild.1-d2b6d7c", "1.6.0-prebuild.1", DerbisUpdateTrack.Test, 2_000L),
            release("v1.1.0", "1.1.0", DerbisUpdateTrack.Stable, 1_000L)
        )
        // A hand-built 1.5.0 test APK carries an ordinary version name, so the build flag says
        // stable; being ahead of every published stable release is what gives it away.
        assertEquals(
            DerbisUpdateTrack.Test,
            resolveInstalledTrack(DerbisUpdateTrack.Stable, releases, "1.5.0")
        )
        // The published stable build stays on the stable line.
        assertEquals(
            DerbisUpdateTrack.Stable,
            resolveInstalledTrack(DerbisUpdateTrack.Stable, releases, "1.1.0")
        )
        // An explicitly configured test build is never demoted.
        assertEquals(
            DerbisUpdateTrack.Test,
            resolveInstalledTrack(DerbisUpdateTrack.Test, releases, "1.1.0")
        )
    }

    @Test
    fun dismissalKeysOnTheTagNotTheVersion() {
        val first = release("prebuild-1.5.0-4b764ac", "1.5.0", DerbisUpdateTrack.Test)
        val second = release("prebuild-1.5.0-9f3e1c2", "1.5.0", DerbisUpdateTrack.Test)
        assertEquals("prebuild-1.5.0-4b764ac", dismissalKeyOf(first))
        assertTrue(dismissalKeyOf(first) != dismissalKeyOf(second))
    }
}
