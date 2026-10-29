package com.yasuda.derbis.domain

enum class DerbisCanvasQuality {
    Auto,
    DataSaver,
    High;

    companion object {
        fun from(value: String): DerbisCanvasQuality =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Auto
    }
}

/** Kept as a one-value type so old preference/backup values migrate without a source picker. */
enum class DerbisCanvasSource {
    Auto;

    companion object {
        fun from(value: String): DerbisCanvasSource = Auto
    }
}

enum class DerbisVisualPerformance {
    Full,
    Auto,
    Smooth;

    companion object {
        fun from(value: String): DerbisVisualPerformance =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Full
    }
}

enum class LibrarySort {
    Recent,
    Title,
    Artist,
    Album,
    Duration;

    val defaultDirection: LibrarySortDirection
        get() = when (this) {
            Recent, Duration -> LibrarySortDirection.Descending
            Title, Artist, Album -> LibrarySortDirection.Ascending
        }

    companion object {
        fun from(value: String): LibrarySort =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Recent
    }
}

enum class LibrarySortDirection {
    Ascending,
    Descending;

    val inverted: LibrarySortDirection
        get() = if (this == Ascending) Descending else Ascending

    fun orient(comparison: Int): Int = if (this == Descending) -comparison else comparison

    companion object {
        fun from(value: String, fallback: LibrarySortDirection): LibrarySortDirection =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback
    }
}

enum class PlayerVisualMode {
    Artwork,
    CanvasCard,
    CanvasImmersive,
    AppleMusic13,
    TidalMusic,
    Pulse,
    Halo,
    Folio,
    Flow;

    companion object {
        /**
         * Bloom/Expressive was retired in favour of Apple Music 13.  Map old
         * saved preferences here so an existing listener's selected deck never
         * becomes an unavailable, invisible choice.
         */
        fun from(value: String): PlayerVisualMode {
            return when (value.trim().lowercase()) {
                "expressive", "bloom", "editorial", "apple_music_13", "applemusic13", "apple_music" -> AppleMusic13
                "tidal_music", "tidalmusic", "tidal_player" -> TidalMusic
                else -> entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: when (value.trim().lowercase()) {
                        "canvas_card", "card" -> CanvasCard
                        "canvas_immersive", "immersive" -> CanvasImmersive
                        else -> Artwork
                    }
            }
        }
    }
}

enum class PlayerColorMode {
    Dynamic,
    Theme,
    Custom;

    companion object {
        const val DEFAULT_ACCENT = 0xFF7C8CFF.toInt()

        fun from(value: String): PlayerColorMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Dynamic
    }
}

enum class PlayerBackgroundMode {
    Dynamic,
    Blur,
    Dark,
    PureBlack,
    Aurora,
    Gradient,
    ReactiveAura;

    companion object {
        fun from(value: String): PlayerBackgroundMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: when (value.trim().lowercase()) {
                    "adaptive", "dynamic" -> Dynamic
                    "blur" -> Blur
                    "dark" -> Dark
                    "pure_black", "pureblack", "black" -> PureBlack
                    "reactive_aura", "reactive-aura" -> ReactiveAura
                    else -> Dynamic
                }
    }
}

enum class ReactiveAuraPreset {
    Calm,
    Flow,
    Pulse;

    companion object {
        val Recommended: ReactiveAuraPreset = Flow

        fun from(value: String): ReactiveAuraPreset =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Recommended
    }
}

enum class PlayerDoubleTapAction {
    Seek,
    PlayPause,
    Favorite,
    Disabled;

    companion object {
        fun from(value: String): PlayerDoubleTapAction =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Seek
    }
}

enum class PlayerLongPressAction {
    Speed,
    Favorite,
    Queue,
    Lyrics,
    Disabled;

    companion object {
        fun from(value: String): PlayerLongPressAction =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Speed
    }
}

enum class PlayerVerticalSwipeAction {
    BrightnessAndVolume,
    Volume,
    Disabled;

    companion object {
        fun from(value: String): PlayerVerticalSwipeAction =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BrightnessAndVolume
    }
}

enum class DerbisSkin {
    Default,
    Nocturne78,
    LiquidGlass,
    Torque,
    Ivory,
    SpotifyStyle,
    AppleMusicStyle,
    TidalStyle,
    YouTubeMusicStyle;

    companion object {
        /** Prism was the temporary backing name for Liquid Glass builds. Keep old preferences valid. */
        fun from(value: String): DerbisSkin = when (value) {
            "Prism" -> LiquidGlass
            else -> entries.firstOrNull { it.name == value } ?: Default
        }
    }
}

data class DerbisInterfaceSettings(
    val compactHome: Boolean = false,
    val showPersonalOrbit: Boolean = true,
    val showResonance: Boolean = true,
    val showNewReleases: Boolean = true,
    val releaseNotificationsEnabled: Boolean = false,
    val showAlbumsForYou: Boolean = true,
    val showTrendingArtists: Boolean = true,
    val showCharts: Boolean = true,
    val fontPreset: DerbisFontPreset = DerbisFontPreset.Outfit,
    val playerGesturesEnabled: Boolean = true,
    val swipeTrackChangeEnabled: Boolean = true,
    val doubleTapAction: PlayerDoubleTapAction = PlayerDoubleTapAction.Seek,
    val doubleTapSeekSeconds: Int = 10,
    val longPressAction: PlayerLongPressAction = PlayerLongPressAction.Speed,
    val longPressSpeed: Float = 2f,
    val verticalSwipeAction: PlayerVerticalSwipeAction = PlayerVerticalSwipeAction.BrightnessAndVolume,
    val canvasQuality: DerbisCanvasQuality = DerbisCanvasQuality.Auto,
    val canvasSource: DerbisCanvasSource = DerbisCanvasSource.Auto,
    val visualPerformance: DerbisVisualPerformance = DerbisVisualPerformance.Full,
    val liquidGlassEnabled: Boolean = true,
    val liteMode: Boolean = false,
    val forceHighRefreshRate: Boolean = false,
    val glassBlur: Int = DEFAULT_GLASS_LEVEL,
    val glassTransparency: Int = DEFAULT_GLASS_LEVEL,
    val glassTint: Int = DEFAULT_GLASS_LEVEL,
    val glassRefraction: Boolean = true,
    /** Suppresses non-essential network work while preserving user-requested playback and search. */
    val dataSaverEnabled: Boolean = false,
    val motionArtworkWifiOnly: Boolean = false,
    val enhanceVideoMetadata: Boolean = false,
    /** Hides tracks and videos explicitly identified as AI-generated. */
    val blockAiContent: Boolean = false,
    val pureBlack: Boolean = false,
    val hapticFeedback: Boolean = true,
    val playerVisualMode: PlayerVisualMode = PlayerVisualMode.Flow,
    val playerBackground: PlayerBackgroundMode = PlayerBackgroundMode.Dynamic,
    val reactiveAuraPreset: ReactiveAuraPreset = ReactiveAuraPreset.Recommended,
    val reactiveAuraIntensity: Int = DEFAULT_REACTIVE_AURA_INTENSITY,
    val playerColorMode: PlayerColorMode = PlayerColorMode.Dynamic,
    val playerColorAccent: Int = PlayerColorMode.DEFAULT_ACCENT,
    val librarySort: LibrarySort = LibrarySort.Recent,
    val librarySortDirection: LibrarySortDirection = LibrarySort.Recent.defaultDirection,
    // New installs open with the Apple Music-inspired player requested for Derbis.
    val skin: DerbisSkin = DerbisSkin.AppleMusicStyle,
    val prism: PrismSettings = PrismSettings(),
    val torque: TorqueSettings = TorqueSettings(),
    val ivory: IvorySettings = IvorySettings()
) {
    fun normalized(): DerbisInterfaceSettings = copy(
        doubleTapSeekSeconds = doubleTapSeekSeconds.coerceIn(5, 30),
        longPressSpeed = longPressSpeed.coerceIn(1.25f, 3f),
        glassBlur = glassBlur.coerceIn(0, 100),
        glassTransparency = glassTransparency.coerceIn(0, 100),
        glassTint = glassTint.coerceIn(0, 100),
        reactiveAuraIntensity = reactiveAuraIntensity.coerceIn(0, 100),
        playerColorAccent = playerColorAccent or 0xFF000000.toInt()
    )

    companion object {
        const val DEFAULT_GLASS_LEVEL = 50
        const val DEFAULT_REACTIVE_AURA_INTENSITY = 50
    }
}

enum class DerbisDownloadPreset {
    Automatic,
    HighQuality,
    DataSaver;

    companion object {
        fun from(value: String): DerbisDownloadPreset = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Automatic
    }
}

enum class DerbisDownloadFolderMode {
    Flat,
    Artist,
    ArtistAlbum;

    companion object {
        fun from(value: String): DerbisDownloadFolderMode = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ArtistAlbum
    }
}

data class DerbisDownloadSettings(
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val resumable: Boolean = true,
    val maxConcurrentDownloads: Int = 2,
    val preset: DerbisDownloadPreset = DerbisDownloadPreset.Automatic,
    val folderMode: DerbisDownloadFolderMode = DerbisDownloadFolderMode.ArtistAlbum,
    val destinationTreeUri: String = "",
    val maxRateKbps: Int = 0,
    val embedMetadata: Boolean = true,
    val embedArtwork: Boolean = true,
    val verifyFile: Boolean = true,
    val skipExisting: Boolean = true
) {
    fun normalized(): DerbisDownloadSettings = copy(
        maxConcurrentDownloads = maxConcurrentDownloads.coerceIn(1, 4),
        destinationTreeUri = destinationTreeUri.trim(),
        maxRateKbps = maxRateKbps.takeIf { it in setOf(0, 512, 1024, 2048, 4096, 8192) } ?: 0
    )

    val effectiveRateKbps: Int
        get() = maxRateKbps.coerceAtLeast(0)

    val maxParallelFragments: Int
        get() = when (preset) {
            DerbisDownloadPreset.HighQuality -> 24
            DerbisDownloadPreset.Automatic -> 20
            DerbisDownloadPreset.DataSaver -> 16
        }

    val resolverAudioQuality: String?
        get() = when (preset) {
            DerbisDownloadPreset.HighQuality -> "High"
            DerbisDownloadPreset.DataSaver -> "Low"
            DerbisDownloadPreset.Automatic -> null
        }

    val storedPresetKey: String
        get() = preset.name

    fun storedQualityKey(automaticQuality: String = "Auto"): String {
        return resolverAudioQuality ?: automaticQuality.trim().ifBlank { "Auto" }
    }
}

enum class DerbisBackupFrequency(val intervalDays: Long) {
    Daily(1L),
    Weekly(7L),
    Monthly(30L);

    companion object {
        fun from(value: String): DerbisBackupFrequency = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: Weekly
    }
}

data class DerbisBackupSettings(
    val enabled: Boolean = false,
    val frequency: DerbisBackupFrequency = DerbisBackupFrequency.Weekly,
    val retentionCount: Int = 5,
    val chargingOnly: Boolean = true,
    val preUpdate: Boolean = true
) {
    fun normalized(): DerbisBackupSettings = copy(retentionCount = retentionCount.coerceIn(1, 12))
}

enum class DerbisVaultStatus { Idle, Running, Completed, Error }

internal fun DerbisDownloadSettings.shouldSkipExistingDownload(
    trackId: String,
    downloadedTrackIds: Set<String>
): Boolean {
    return skipExisting && trackId.isNotBlank() && trackId in downloadedTrackIds
}

data class DerbisIntelligenceSummary(
    val overview: String = "",
    val mood: String = "",
    val themes: List<String> = emptyList(),
    val repeatedPhrases: List<String> = emptyList(),
    val lexicalDensity: Int = 0,
    val lineCount: Int = 0,
    val wordCount: Int = 0,
    val localOnly: Boolean = true
) {
    val available: Boolean
        get() = overview.isNotBlank() || themes.isNotEmpty() || repeatedPhrases.isNotEmpty()
}

data class OfflineDownloadTask(
    val taskKey: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val state: String,
    val progress: Int,
    val error: String
)
