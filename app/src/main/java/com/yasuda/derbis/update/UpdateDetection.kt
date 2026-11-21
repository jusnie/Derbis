/*
 * Which published release an installed Derbis should be told about.
 *
 * The old detector asked one question - "is there a non-prerelease release whose tag starts
 * with v?" - and it was wrong in both directions. Derbis publishes stable tags (v1.1.0) and
 * test builds (prebuild-1.6.0-prebuild.1-d2b6d7c), and every test build was skipped twice
 * over: once because a prebuild tag is not a v-tag, and again because the workflow marks them
 * as pre-releases. An installed 1.5.0 test build therefore sat next to a published 1.6.0 and
 * was told it was up to date.
 *
 * Here a release is parsed into a track, a version and a build id, and the installed build's
 * own track decides which pool it may see. Nothing in this file touches Android or the
 * network, so the ordering rules are unit-tested directly.
 */
package com.yasuda.derbis.update

import com.yasuda.derbis.nexus.update.DerbisVersionComparator

/**
 * How long a successful automatic check is good for.
 *
 * The old detector ran a full 30-release fetch on every cold start and had no memory of having
 * done it. Six hours keeps a phone that is opened ten times a day from asking GitHub ten times,
 * without ever leaving a device more than half a day behind a release.
 */
const val AUTO_UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

/** Which publishing line a release belongs to. */
enum class DerbisUpdateTrack(val key: String) {
    /** `v1.1.0` - a release advertised to everyone. */
    Stable("stable"),

    /** `prebuild-1.6.0-prebuild.1-d2b6d7c` - a test build, opted into by an installed test build. */
    Test("test");

    companion object {
        fun fromKey(value: String): DerbisUpdateTrack =
            entries.firstOrNull { it.key.equals(value.trim(), ignoreCase = true) } ?: Stable
    }
}

/** One downloadable file attached to a release. */
data class UpdateReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val contentType: String,
    val sizeBytes: Long,
    val abi: String = "universal"
) {
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true) ||
            contentType.contains("android.package-archive", ignoreCase = true)
}

/** A GitHub release, reduced to the fields the detector actually reasons about. */
data class UpdateRelease(
    val tag: String,
    val versionName: String,
    val track: DerbisUpdateTrack,
    /** Short commit the build was cut from, when the tag carries one. */
    val buildId: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val publishedAtMs: Long,
    val assets: List<UpdateReleaseAsset>
) {
    /** A release with no APK attached can be described but never installed. */
    val installable: Boolean get() = assets.any { it.isApk }
}

private val STABLE_TAG = Regex("^v[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?$")
private val VERSION_LEAD = Regex("^[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?$")
private val SHORT_SHA = Regex("^[0-9a-f]{7,40}$")

/**
 * Classifies a release tag. Returns null for tags that are not Derbis Android builds at all
 * (`desktop-v1.0.3`), which is how the Windows releases are kept out of the phone's update
 * list without maintaining a separate endpoint.
 */
fun updateTrackOf(tag: String, prerelease: Boolean): DerbisUpdateTrack? {
    val clean = tag.trim()
    if (clean.isEmpty()) return null
    if (clean.startsWith("prebuild-", ignoreCase = true)) return DerbisUpdateTrack.Test
    if (clean.startsWith("desktop-", ignoreCase = true)) return null
    if (!STABLE_TAG.matches(clean)) return null
    return if (prerelease) DerbisUpdateTrack.Test else DerbisUpdateTrack.Stable
}

/**
 * The version a tag advertises, without the tag's own decoration.
 *
 * `v1.1.0` -> `1.1.0`, and `prebuild-1.6.0-prebuild.1-d2b6d7c` -> `1.6.0-prebuild.1`: the
 * trailing segment is dropped only when it looks like the commit sha the workflow appends,
 * because the visible prebuild counter sits in the same dash-separated position right before
 * it.
 */
fun versionOf(tag: String): String? {
    val clean = tag.trim()
    if (clean.startsWith("prebuild-", ignoreCase = true)) {
        val rest = clean.substring("prebuild-".length)
        val parts = rest.split('-')
        val last = parts.lastOrNull().orEmpty()
        val withoutBuild = if (parts.size > 1 && SHORT_SHA.matches(last)) {
            parts.dropLast(1).joinToString("-")
        } else {
            rest
        }
        return withoutBuild.takeIf { VERSION_LEAD.matches(it) }
    }
    val version = clean.removePrefix("v").removePrefix("V")
    return version.takeIf { VERSION_LEAD.matches(it) }
}

/** The commit a prebuild tag was cut from, or "" for tags that do not carry one. */
fun buildIdOf(tag: String): String {
    val clean = tag.trim()
    if (!clean.startsWith("prebuild-", ignoreCase = true)) return ""
    val rest = clean.substring("prebuild-".length)
    val parts = rest.split('-')
    val last = parts.lastOrNull().orEmpty()
    return if (parts.size > 1 && SHORT_SHA.matches(last)) last else ""
}

/**
 * The release an installed build on [track] should be comparing itself against.
 *
 * Ordering is the shared version comparator (so `1.6.0-prebuild.2` outranks
 * `1.6.0-prebuild.1` and `1.6.0` leaves every `1.6.0-...` behind), with the publish time as
 * the tie-break for two releases that claim the same version.
 *
 * A test build falls back to the stable line when no test build has ever been published, so a
 * device cannot be stranded on a channel that went quiet.
 */
fun selectUpdateRelease(
    releases: List<UpdateRelease>,
    track: DerbisUpdateTrack
): UpdateRelease? {
    val usable = releases.filter { it.installable }
    val pool = when (track) {
        DerbisUpdateTrack.Stable -> usable.filter { it.track == DerbisUpdateTrack.Stable }
        DerbisUpdateTrack.Test -> usable.filter { it.track == DerbisUpdateTrack.Test }
            .ifEmpty { usable.filter { it.track == DerbisUpdateTrack.Stable } }
    }
    var best: UpdateRelease? = null
    for (release in pool) {
        val current = best
        if (current == null) {
            best = release
            continue
        }
        val comparison = DerbisVersionComparator.compare(release.versionName, current.versionName)
        if (comparison > 0 || (comparison == 0 && release.publishedAtMs > current.publishedAtMs)) {
            best = release
        }
    }
    return best
}

/**
 * Which line the installed build is really on.
 *
 * The build flag says what a build *meant* to be, and for the release and prebuild workflows it
 * is right. A hand-assembled test build is the case it misses: Derbis test APKs are assembled
 * with an ordinary version name (`-PderbisVersionName=1.5.0`), so nothing in the build says
 * "test". What it does say is that the installed version is ahead of every published stable
 * release, and a build that is ahead of the stable line came from somewhere else - which is
 * exactly what a test install is. That keeps those devices off the stable pool they would
 * otherwise be stranded on, without asking anyone to flip a flag.
 */
fun resolveInstalledTrack(
    configured: DerbisUpdateTrack,
    releases: List<UpdateRelease>,
    installedVersionName: String
): DerbisUpdateTrack {
    if (configured == DerbisUpdateTrack.Test) return DerbisUpdateTrack.Test
    val newestStable = selectUpdateRelease(releases, DerbisUpdateTrack.Stable)
        ?: return DerbisUpdateTrack.Stable
    val ahead = DerbisVersionComparator.compare(installedVersionName, newestStable.versionName) > 0
    return if (ahead) DerbisUpdateTrack.Test else DerbisUpdateTrack.Stable
}

/** True when [release] is ahead of the installed version. */
fun isNewerThan(release: UpdateRelease, installedVersionName: String): Boolean =
    DerbisVersionComparator.compare(release.versionName, installedVersionName) > 0

/**
 * Identity used to remember a dismissal. The tag is unique per build; the version name is not,
 * which is why two different prebuilds called 1.5.0 used to share one dismissal.
 */
fun dismissalKeyOf(release: UpdateRelease): String =
    release.tag.ifBlank { release.versionName }
