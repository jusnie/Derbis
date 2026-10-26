package com.luc4n3x.derbis.viewmodel

import com.luc4n3x.derbis.data.local.DEFAULT_QUEUE_SPACE_ID
import com.luc4n3x.derbis.data.locallibrary.LocalLibraryCatalog
import com.luc4n3x.derbis.player.queue.QueueSpaceSummary
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.luc4n3x.derbis.data.VaultPreview
import com.luc4n3x.derbis.domain.ArtistExclusions
import com.luc4n3x.derbis.domain.ArtistHit
import com.luc4n3x.derbis.domain.ArtistProfile
import com.luc4n3x.derbis.domain.ExcludedArtist
import com.luc4n3x.derbis.domain.ExploreCategory
import com.luc4n3x.derbis.domain.RecommendationFeedback
import com.luc4n3x.derbis.domain.DerbisAmbientSettings
import com.luc4n3x.derbis.domain.PlaylistHitPreview
import com.luc4n3x.derbis.domain.PlaylistTag
import com.luc4n3x.derbis.domain.AlbumHit
import com.luc4n3x.derbis.domain.AlbumDetail
import com.luc4n3x.derbis.domain.CacheReport
import com.luc4n3x.derbis.domain.AppUpdateInfo
import com.luc4n3x.derbis.domain.ChartRegion
import com.luc4n3x.derbis.domain.DownloadedTrack
import com.luc4n3x.derbis.domain.FollowedArtist
import com.luc4n3x.derbis.domain.HighQualityAudioMode
import com.luc4n3x.derbis.domain.DerbisAudioQuality
import com.luc4n3x.derbis.domain.HomeSection
import com.luc4n3x.derbis.domain.DerbisTab
import com.luc4n3x.derbis.domain.DerbisAudioSettings
import com.luc4n3x.derbis.domain.DerbisDownloadSettings
import com.luc4n3x.derbis.domain.DerbisSmartOfflineSettings
import com.luc4n3x.derbis.domain.DerbisInterfaceSettings
import com.luc4n3x.derbis.domain.DerbisHomeStudioSettings
import com.luc4n3x.derbis.domain.DerbisLyricsStudioSettings
import com.luc4n3x.derbis.domain.DerbisAutomationSettings
import com.luc4n3x.derbis.domain.DerbisBackupSettings
import com.luc4n3x.derbis.domain.PlaylistImportFailureKind
import com.luc4n3x.derbis.domain.DerbisVaultStatus
import com.luc4n3x.derbis.domain.DerbisIntelligenceSummary
import com.luc4n3x.derbis.domain.BatchDownload
import com.luc4n3x.derbis.domain.OfflineDownloadTask
import com.luc4n3x.derbis.domain.ListeningDna
import com.luc4n3x.derbis.domain.ListeningDnaPeriod
import com.luc4n3x.derbis.domain.recap.ListeningRecapPeriod
import com.luc4n3x.derbis.domain.recap.ListeningRecapSummary
import com.luc4n3x.derbis.domain.DerbisMixDefaults
import com.luc4n3x.derbis.domain.DerbisMixSummary
import com.luc4n3x.derbis.domain.VideoQualityRung
import com.luc4n3x.derbis.domain.VideoQualityTarget
import com.luc4n3x.derbis.domain.LyricsProviderOrdering
import com.luc4n3x.derbis.domain.ListeningPulse
import com.luc4n3x.derbis.domain.LyricLine
import com.luc4n3x.derbis.domain.LyricSection
import com.luc4n3x.derbis.domain.LyricsTranslationState
import com.luc4n3x.derbis.domain.Mood
import com.luc4n3x.derbis.domain.ReleaseRadarEntry
import com.luc4n3x.derbis.domain.RepeatMode
import com.luc4n3x.derbis.domain.SearchFilter
import com.luc4n3x.derbis.domain.SearchResults
import com.luc4n3x.derbis.domain.SmartMusicProfile
import com.luc4n3x.derbis.domain.Taste
import com.luc4n3x.derbis.domain.Track
import com.luc4n3x.derbis.domain.ResonanceCommentSnippet
import com.luc4n3x.derbis.domain.YoutubeEngagementState
import com.luc4n3x.derbis.data.LyricsRepository
import com.luc4n3x.derbis.data.LyricsLatencyProfiles
import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisNetworkSettingsError
import com.luc4n3x.derbis.domain.DerbisNetworkTestOutcome
import com.luc4n3x.derbis.feature.jam.JamUiState
import com.luc4n3x.derbis.feature.motion.MotionArtwork
import com.luc4n3x.derbis.feature.recognition.RecognitionHistoryEntry
import com.luc4n3x.derbis.feature.sharedmedia.SharedMediaPreview
import com.luc4n3x.derbis.feature.radio.RadioStation
import com.luc4n3x.derbis.ui.theme.DerbisThemes

enum class DetailReturnTarget {
    None,
    Album,
    Artist
}

@Immutable
data class LocalLibraryUiState(
    val permissionGranted: Boolean = false,
    val scanning: Boolean = false,
    val lastScanAt: Long = 0L,
    val message: String = "",
    val excludedFolders: Set<String> = emptySet(),
    val catalog: LocalLibraryCatalog = LocalLibraryCatalog()
)

@Immutable
data class VideoQualityUiState(
    val ladder: List<VideoQualityRung> = emptyList(),
    val activeLabel: String? = null,
    val switching: Boolean = false
) {
    val available: Boolean
        get() = ladder.isNotEmpty()
}

@Immutable
data class QueuePlaylistDraft(
    val sourceSpaceId: String,
    val tracks: List<Track>
)

data class DerbisUiState(
    val selectedTab: DerbisTab = DerbisTab.Home,
    val moods: List<Mood> = emptyList(),
    val tastes: List<Taste> = emptyList(),
    val quickPickSeeds: List<Track> = emptyList(),
    val showOnboarding: Boolean = false,
    val youtubeMusicIntroPending: Boolean = false,
    val isVideoMode: Boolean = false,
    val pendingVideoMode: Boolean? = null,
    val selectedVideoSubtitleId: String? = null,
    val videoQuality: VideoQualityUiState = VideoQualityUiState(),
    val videoQualityTarget: VideoQualityTarget = VideoQualityTarget.AUTO,
    val isSamplesOpen: Boolean = false,
    val showSettings: Boolean = false,
    val animationsEnabled: Boolean = true,
    val motionArtworkEnabled: Boolean = false,
    val recognitionAvailable: Boolean = true,
    val lastFmConfigured: Boolean = false,
    val lastFmAuthorizationPending: Boolean = false,
    val listenBrainzConfigured: Boolean = false,
    val audDConfigured: Boolean = false,
    val recognitionState: com.luc4n3x.derbis.feature.recognition.RecognitionState =
        com.luc4n3x.derbis.feature.recognition.RecognitionState.Idle,
    val showRecognition: Boolean = false,
    val recognitionHistory: List<RecognitionHistoryEntry> = emptyList(),
    val recognitionMatch: Track? = null,
    val recognitionMatching: Boolean = false,
    val recognitionDeviceCaptureSupported: Boolean = false,
    val jam: JamUiState = JamUiState(),
    val jamDisplayName: String = "",
    val showJam: Boolean = false,
    val networkSettings: DerbisNetworkSettings = DerbisNetworkSettings(),
    val networkProxyPasswordSet: Boolean = false,
    val networkTesting: Boolean = false,
    val networkTestOutcome: DerbisNetworkTestOutcome? = null,
    val networkErrors: List<DerbisNetworkSettingsError> = emptyList(),
    val dynamicColor: Boolean = true,
    val userName: String = "",
    val profilePhotoPath: String = "",
    val profilePhotoVersion: Long = 0L,
    val languageCode: String = "en",
    val selectedMood: Mood? = null,
    val tracks: List<Track> = emptyList(),
    val queue: List<Track> = emptyList(),
    val queueCurrentIndex: Int = -1,
    val queueSpaces: List<QueueSpaceSummary> = emptyList(),
    val activeQueueSpaceId: String = DEFAULT_QUEUE_SPACE_ID,
    val queuePlaylistLoadingSpaceId: String? = null,
    val queuePlaylistDraft: QueuePlaylistDraft? = null,
    val pendingQueueAddTracks: List<Track> = emptyList(),
    val queueSwitching: Boolean = false,
    /** Related tracks held only as a rolling endless-listening playback buffer, never a saved playlist. */
    val automaticQueueTrackKeys: Set<String> = emptySet(),
    val queueUnavailableUris: Set<String> = emptySet(),
    val localLibrary: LocalLibraryUiState = LocalLibraryUiState(),
    val queueUndoAvailable: Boolean = false,
    val queueHistoryCount: Int = 0,
    val radioEnabled: Boolean = true,
    val searchResults: List<Track> = emptyList(),
    val recentSearches: List<Track> = emptyList(),
    val personalOrbitTracks: List<Track> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val charts: List<Track> = emptyList(),
    val chartRegions: List<ChartRegion> = emptyList(),
    val selectedChartId: String = "it",
    val isLoadingCharts: Boolean = false,
    val homeSections: List<HomeSection> = emptyList(),
    val homeAlbums: List<AlbumHit> = emptyList(),
    val homeArtists: List<ArtistHit> = emptyList(),
    val homeResonanceTracks: List<Track> = emptyList(),
    val homeResonanceUpdatedAt: Long = 0L,
    val homeResonanceComments: Map<String, ResonanceCommentSnippet> = emptyMap(),
    val homeArtistsLoading: Boolean = false,
    val homeAlbumsLoading: Boolean = false,
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    val isDeviceOffline: Boolean = false,
    val showAlbum: Boolean = false,
    val albumLoading: Boolean = false,
    val albumError: String? = null,
    val albumDetail: AlbumDetail? = null,
    val albumMotionArtwork: MotionArtwork? = null,
    val detailReturnTarget: DetailReturnTarget = DetailReturnTarget.None,
    val favorites: List<Track> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val favoriteTimestamps: Map<String, Long> = emptyMap(),
    val forgottenFavorites: List<Track> = emptyList(),
    val playlists: List<com.luc4n3x.derbis.domain.Playlist> = emptyList(),
    val playlistTags: List<PlaylistTag> = emptyList(),
    val openPlaylist: com.luc4n3x.derbis.domain.Playlist? = null,
    val currentTrack: Track? = null,
    val liveRadioStation: RadioStation? = null,
    val liveRadioNowPlaying: String = "",
    val liveRadioReconnectAttempt: Int = 0,
    val motionArtwork: MotionArtwork? = null,
    val motionArtworkLoading: Boolean = false,
    val youtubeEngagement: YoutubeEngagementState = YoutubeEngagementState(),
    val similarSongs: List<Track> = emptyList(),
    val similarSongsLoading: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsSections: List<LyricSection> = emptyList(),
    val activeLyric: LyricLine? = null,
    val showLyrics: Boolean = false,
    val lyricsLoading: Boolean = false,
    val lyricsSynced: Boolean = false,
    val lyricsProvider: String = "",
    val lyricsConfidence: Int = 0,
    val lyricsCached: Boolean = false,
    val lyricsVersions: List<LyricsRepository.LyricsVersion> = emptyList(),
    val lyricsVersionsLoading: Boolean = false,
    val lyricsManualSelection: Boolean = false,
    val lyricsTranslationEnabled: Boolean = false,
    val lyricsTranslationState: LyricsTranslationState = LyricsTranslationState.DISABLED,
    val lyricsProviderOrdering: LyricsProviderOrdering = LyricsProviderOrdering(),
    val lyricsLatencyProfiles: LyricsLatencyProfiles = LyricsLatencyProfiles(),
    val lyricsStudioSettings: DerbisLyricsStudioSettings = DerbisLyricsStudioSettings(),
    val showLyricsStudio: Boolean = false,
    val homeStudioSettings: DerbisHomeStudioSettings = DerbisHomeStudioSettings(),
    val showHomeStudio: Boolean = false,
    val smartProfile: SmartMusicProfile = SmartMusicProfile(),
    val cacheReport: CacheReport = CacheReport(0, 0, 0, 0),
    val query: String = "",
    val isPlaying: Boolean = false,
    val isSearching: Boolean = false,
    val searchPending: Boolean = false,
    val isResolving: Boolean = false,
    val searchError: String? = null,
    val playerError: String? = null,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val smartScore: Int = 94,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1f,
    val audioQuality: String = DerbisAudioQuality.DEFAULT,
    val highQualityAudioMode: HighQualityAudioMode = HighQualityAudioMode.PREFER_320,
    val showAudioQualityPanel: Boolean = false,
    val audioNormalization: Boolean = false,
    val audioSettings: DerbisAudioSettings = DerbisAudioSettings(),
    val sleepTimerMinutes: Int = 0,
    val sleepTimerEndOfTrack: Boolean = false,
    val sleepTimerDeadlineElapsedRealtimeMs: Long = 0L,
    val sleepTimerTotalMs: Long = 0L,
    val sleepTimerFadeMs: Long = 0L,
    val showSleepTimer: Boolean = false,
    val sponsorBlockEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val showQueue: Boolean = false,
    val isOfflineExporting: Boolean = false,
    val offlineExportMessage: String? = null,
    val embeddedMetadataWriterReady: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val isCheckingUpdates: Boolean = false,
    val updateMessage: String? = null,
    val showUpdatePrompt: Boolean = false,
    val downloads: List<DownloadedTrack> = emptyList(),
    val downloadStorageBytes: Long = 0L,
    val exploreZoneId: String? = null,
    val exploreOpenRequest: String? = null,
    val exploreCategoryParams: String? = null,
    val exploreCategories: List<ExploreCategory> = emptyList(),
    val exploreCategoryArtwork: Map<String, String> = emptyMap(),
    val exploreTracks: List<Track> = emptyList(),
    val exploreFreshTracks: List<Track> = emptyList(),
    val exploreWorldFreshTracks: List<Track> = emptyList(),
    val exploreNewReleases: List<AlbumHit> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val exploreSamples: List<Track> = emptyList(),
    val isFreshCurrentsLoading: Boolean = false,
    val isNewReleasesLoading: Boolean = false,
    val newReleasesLoadFailed: Boolean = false,
    val isSamplesLoading: Boolean = false,
    val samplesLoadFailed: Boolean = false,
    val isExploreLoading: Boolean = false,
    val isExploreCategoriesLoading: Boolean = false,
    val downloadingTrackIds: Set<String> = emptySet(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadProgressByTrackId: Map<String, Int> = emptyMap(),
    val downloadTitleByTrackId: Map<String, String> = emptyMap(),
    val offlineQueueSize: Int = 0,
    val downloadQueue: List<OfflineDownloadTask> = emptyList(),
    val downloadBatches: List<BatchDownload> = emptyList(),
    val showArtist: Boolean = false,
    val artistLoading: Boolean = false,
    val artistError: String? = null,
    val artistProfile: ArtistProfile? = null,
    val artistMotionArtwork: MotionArtwork? = null,
    val artistListStateKey: String = "",
    val artistRequestName: String = "",
    val searchData: SearchResults = SearchResults(),
    val searchFilter: SearchFilter = SearchFilter.All,
    val searchSectionContinuations: Map<SearchFilter, String> = emptyMap(),
    val searchSectionLoading: Set<SearchFilter> = emptySet(),
    val themePreset: String = DerbisThemes.COSMIC,
    val themeAccent: Int = 0,
    val showThemeStudio: Boolean = false,
    val interfaceSettings: DerbisInterfaceSettings = DerbisInterfaceSettings(),
    val downloadSettings: DerbisDownloadSettings = DerbisDownloadSettings(),
    val smartOfflineSettings: DerbisSmartOfflineSettings = DerbisSmartOfflineSettings(),
    val backupSettings: DerbisBackupSettings = DerbisBackupSettings(),
    val automationSettings: DerbisAutomationSettings = DerbisAutomationSettings(),
    val vaultStatus: DerbisVaultStatus = DerbisVaultStatus.Idle,
    val backupPreview: VaultPreview? = null,
    val pendingRestoreUri: Uri? = null,
    val lastBackupAtMs: Long = 0L,
    val backupLocationUri: String? = null,
    val preUpdateBackupFailed: Boolean = false,
    val sharedMediaPreview: SharedMediaPreview? = null,
    val playlistHitPreview: PlaylistHitPreview? = null,
    val intelligenceSummary: DerbisIntelligenceSummary = DerbisIntelligenceSummary(),
    val backupMessage: String? = null,
    val playbackDiagnostics: String = "",
    val listeningPulse: ListeningPulse = ListeningPulse(),
    val recentListens: List<Track> = emptyList(),
    val mostPlayedTracks: List<Track> = emptyList(),
    val followedArtists: List<FollowedArtist> = emptyList(),
    val followedArtistKeys: Set<String> = emptySet(),
    val speedDialPins: List<com.luc4n3x.derbis.domain.SpeedDialPin> = emptyList(),
    val excludedArtists: List<ExcludedArtist> = emptyList(),
    val artistExclusions: ArtistExclusions = ArtistExclusions.Empty,
    val recommendationFeedback: RecommendationFeedback = RecommendationFeedback.Empty,
    val showAmbient: Boolean = false,
    val ambientSettings: DerbisAmbientSettings = DerbisAmbientSettings(),
    val releaseRadar: List<ReleaseRadarEntry> = emptyList(),
    val similarArtists: List<ArtistHit> = emptyList(),
    val mixFamiliarity: Float = DerbisMixDefaults.Familiarity,
    val mixLoading: Boolean = false,
    val activeMix: DerbisMixSummary? = null,
    val mixMessage: String? = null,
    val showYourSound: Boolean = false,
    val listeningDnaPeriod: ListeningDnaPeriod = ListeningDnaPeriod.Month,
    val listeningDna: ListeningDna = ListeningDna(),
    val listeningDnaLoading: Boolean = false,
    val showListeningInsights: Boolean = false,
    val showListeningRecap: Boolean = false,
    val listeningRecapPeriod: ListeningRecapPeriod = ListeningRecapPeriod.Days30,
    val listeningRecap: ListeningRecapSummary = ListeningRecapSummary(period = ListeningRecapPeriod.Days30),
    val listeningRecapLoading: Boolean = false
)

data class MiniPlayerStudioState(
    val settings: com.luc4n3x.derbis.domain.DerbisMiniPlayerStudioSettings = com.luc4n3x.derbis.domain.DerbisMiniPlayerStudioSettings(),
    val visible: Boolean = false
)

data class NavigationStudioState(
    val settings: com.luc4n3x.derbis.domain.DerbisNavigationStudioSettings = com.luc4n3x.derbis.domain.DerbisNavigationStudioSettings(),
    val visible: Boolean = false
)
