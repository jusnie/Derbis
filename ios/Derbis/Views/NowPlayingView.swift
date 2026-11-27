import SwiftUI
import UIKit

struct NowPlayingView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @ObservedObject private var haptics = HapticsAndGesturesService.shared
    @ObservedObject private var jam = JamService.shared
    @Environment(\.dismiss) private var dismiss

    @State private var showingLyrics: Bool = false
    @State private var showingQueue: Bool = false
    @State private var showingPlaylistSheet: Bool = false
    @State private var showingSpotifyCanvasSheet: Bool = false
    @State private var showingAudioStudioSheet: Bool = false
    @State private var showingJamSheet: Bool = false
    @State private var showingStudioConsole: Bool = false
    @State private var showingArtistSheet: Bool = false
    @State private var showingAlbumSheet: Bool = false
    @State private var showingSimilarSheet: Bool = false
    @State private var selectedGeniusMeaning: GeniusAnnotationEntry?
    @State private var selectedGeniusLineText: String = ""

    @State private var isScrubbing: Bool = false
    @State private var scrubTime: Double = 0
    @State private var lastScrubNotch: Int = -1

    @State private var controlsCollapsedInLyrics: Bool = false
    @State private var autoHideTask: Task<Void, Never>?

    @State private var gestureFeedbackBadge: (icon: String, label: String)?
    @State private var badgeClearTask: Task<Void, Never>?
    @State private var artworkDragOffset: CGFloat = 0
    @State private var dismissDragOffset: CGFloat = 0
    @State private var holdLensLocation: CGPoint? = nil
    @State private var holdLensRevealed: Bool = false
    @State private var holdLensTask: Task<Void, Never>? = nil

    private var activeMotionAsset: MotionCanvasAsset? {
        guard theme.motionCanvasEnabled,
              let asset = player.currentMotionCanvas,
              asset.trackId == player.currentTrack?.id else {
            return nil
        }
        return asset
    }

    private var isImmersiveCanvas: Bool {
        theme.backdropStyle == .animated && activeMotionAsset != nil && theme.motionCanvasStyle == .immersive
    }

    private var isFillBackdrop: Bool {
        theme.backdropStyle == .fill && !theme.isLightSkin
    }

    // Press-and-hold lens: the blurred-lyric droplet that wakes under the finger.
    private var lensLines: [LyricLine] {
        if let lines = player.currentLyrics?.lines, !lines.isEmpty {
            return lines
        }
        return [LyricLine(id: -1, startTimeSec: 0, text: "Instrumental")]
    }

    private var artworkHoldGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                holdLensLocation = value.location
                let travel = hypot(value.translation.width, value.translation.height)
                if travel > 12 {
                    // A real swipe — cancel the hold, the stage gestures own it.
                    holdLensTask?.cancel()
                    holdLensTask = nil
                    return
                }
                guard !holdLensRevealed, holdLensTask == nil else { return }
                holdLensTask = Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 150_000_000)
                    guard !Task.isCancelled else { return }
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    withAnimation(.interactiveSpring(response: 0.3, dampingFraction: 0.78)) {
                        holdLensRevealed = true
                    }
                }
            }
            .onEnded { _ in
                holdLensTask?.cancel()
                holdLensTask = nil
                withAnimation(.spring(response: 0.32, dampingFraction: 0.8)) {
                    holdLensRevealed = false
                }
                holdLensLocation = nil
            }
    }

    private var fgPrimary: Color {
        if isImmersiveCanvas || isFillBackdrop { return Color.white }
        return theme.isLightSkin ? theme.primaryText : Color.white
    }

    private var fgSecondary: Color {
        if isImmersiveCanvas || isFillBackdrop { return Color.white.opacity(0.74) }
        return theme.isLightSkin ? theme.secondaryText : Color.white.opacity(0.65)
    }

    var body: some View {
        GeometryReader { screenGeo in
            let viewportWidth = max(280, screenGeo.size.width)
            let viewportHeight = max(320, screenGeo.size.height)
            let isLandscapeSplit = viewportWidth > viewportHeight && viewportWidth >= 600
            let ultraCompact = viewportHeight < 650
            let compactHeight = viewportHeight < 760
            let expandedLyricsStage = showingLyrics && controlsCollapsedInLyrics
            let inlineLyricVisible = !showingLyrics && !ultraCompact && currentInlineLyricText != nil
            let showFlowChips = !ultraCompact && !expandedLyricsStage && theme.effectivePlayerDeck != .appleMusic13

            let reservedChrome: CGFloat = {
                if expandedLyricsStage {
                    return ultraCompact ? 154 : 174
                }
                let topH: CGFloat = ultraCompact ? 50 : 56
                let inlineH: CGFloat = inlineLyricVisible ? 36 : 0
                let metaH: CGFloat = 48
                let scrubH: CGFloat = ultraCompact ? 56 : (compactHeight ? 62 : 68)
                let transportH: CGFloat = ultraCompact ? 62 : (compactHeight ? 70 : 78)
                let chipBarH: CGFloat = showFlowChips ? 42 : 0
                let dockH: CGFloat = ultraCompact ? 58 : (compactHeight ? 64 : 72)
                return topH + inlineH + metaH + scrubH + transportH + chipBarH + dockH + 18
            }()
            let stageMaxHeight = max(136, viewportHeight - reservedChrome)

            ZStack(alignment: .top) {
                if let track = player.currentTrack {
                    Group {
                        if isLandscapeSplit {
                            landscapeSplitPlayerLayout(
                                track: track,
                                viewportWidth: viewportWidth,
                                viewportHeight: viewportHeight
                            )
                        } else {
                            VStack(spacing: 0) {
                                grabberAndTopBar(track: track)
                                    .padding(.top, ultraCompact ? 4 : (compactHeight ? 6 : 10))
                                    .padding(.horizontal, 20)
                                    .contentShape(Rectangle())
                                    .gesture(verticalDismissDragGesture)

                                Spacer(minLength: ultraCompact ? 4 : 8)

                                if showingLyrics {
                                    syncedLyricsPanel(track: track, maxHeight: stageMaxHeight)
                                        .transition(.opacity)
                                } else {
                                    artworkStage(
                                        track: track,
                                        viewportWidth: viewportWidth,
                                        maxStageHeight: stageMaxHeight
                                    )
                                    .transition(.opacity)
                                }

                                Spacer(minLength: ultraCompact ? 4 : 8)

                                if inlineLyricVisible, let inlineLine = currentInlineLyricText {
                                    Button {
                                        haptics.playControlTap(style: .light)
                                        withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                            showingLyrics = true
                                        }
                                        scheduleLyricsControlsAutoHide()
                                    } label: {
                                        HStack(spacing: 6) {
                                            Image(systemName: "quote.bubble.fill")
                                                .font(.system(size: 11, weight: .semibold))
                                                .foregroundColor(theme.accent)
                                            Text(inlineLine)
                                                .font(theme.font(size: 12.5, weight: .semibold))
                                                .foregroundColor(fgPrimary.opacity(0.94))
                                                .lineLimit(1)
                                        }
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 5.5)
                                        .derbisGlassCapsule(
                                            forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
                                        )
                                        .padding(.horizontal, 22)
                                        .padding(.bottom, 6)
                                    }
                                    .buttonStyle(.plain)
                                }

                                titleAndLikeRow(track: track, compact: ultraCompact)
                                    .padding(.horizontal, 22)

                                if !expandedLyricsStage {
                                    scrubberSection
                                        .padding(.horizontal, 22)
                                        .padding(.top, ultraCompact ? 6 : (compactHeight ? 9 : 12))
                                        .transition(.opacity.combined(with: .move(edge: .bottom)))

                                    transportControlsRow(playSize: ultraCompact ? 54 : (compactHeight ? 58 : 64))
                                        .padding(.horizontal, 22)
                                        .padding(.top, ultraCompact ? 8 : (compactHeight ? 10 : 14))
                                        .transition(.opacity.combined(with: .move(edge: .bottom)))

                                    if showFlowChips {
                                        DerbisFlowStudioChipBar(
                                            fgPrimary: fgPrimary,
                                            fgSecondary: fgSecondary,
                                            forceGlass: isImmersiveCanvas || isFillBackdrop,
                                            onOpenStudioFX: { showingAudioStudioSheet = true },
                                            onOpenSimilar: { showingSimilarSheet = true }
                                        )
                                        .padding(.horizontal, 18)
                                        .padding(.top, compactHeight ? 8 : 10)
                                    }
                                } else {
                                    Button {
                                        haptics.playSelectionTick()
                                        revealLyricsControlsTemporarily()
                                    } label: {
                                        HStack(spacing: 8) {
                                            Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                                .font(.system(size: 12, weight: .bold))
                                            Text("Show Controls")
                                                .font(theme.font(size: 12, weight: .semibold))
                                        }
                                        .foregroundColor(fgPrimary.opacity(0.92))
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .derbisGlassCapsule(forceGlassOverBackdrop: true)
                                        .padding(.top, 8)
                                    }
                                    .buttonStyle(.plain)
                                }

                                bottomUtilityDock(track: track)
                                    .padding(.horizontal, 16)
                                    .padding(.top, ultraCompact ? 8 : (compactHeight ? 11 : 14))
                                    .padding(.bottom, ultraCompact ? 6 : (compactHeight ? 8 : 12))
                            }
                        }
                    }
                    .frame(width: viewportWidth, height: viewportHeight)
                    .offset(y: dismissDragOffset)
                    .scaleEffect(1.0 - min(0.05, dismissDragOffset / 2400.0))
                    .sheet(isPresented: $showingQueue) {
                        UpNextQueueSheet()
                            .environmentObject(player)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingPlaylistSheet) {
                        PlaylistPickerSheet(track: track)
                            .environmentObject(library)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingSpotifyCanvasSheet) {
                        SpotifyCanvasSheet()
                            .environmentObject(player)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingAudioStudioSheet) {
                        AudioEffectsSheet()
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingJamSheet) {
                        JamSheet()
                            .environmentObject(player)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingStudioConsole) {
                        DerbisPlayerConsoleSheet(
                            track: track,
                            onOpenPlaylistPicker: { showingPlaylistSheet = true },
                            onOpenAudioStudio: { showingAudioStudioSheet = true },
                            onOpenJam: { showingJamSheet = true },
                            onOpenArtist: { showingArtistSheet = true },
                            onOpenAlbum: { showingAlbumSheet = true },
                            onOpenSimilar: { showingSimilarSheet = true }
                        )
                        .environmentObject(player)
                        .environmentObject(library)
                        .environmentObject(theme)
                        .presentationDetents([.medium, .large])
                    }
                    .sheet(isPresented: $showingArtistSheet) {
                        ArtistDetailSheet(artistName: track.artist)
                            .environmentObject(player)
                            .environmentObject(library)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingAlbumSheet) {
                        AlbumDetailSheet(albumTitle: track.album, artistName: track.artist, seedTracks: [track])
                            .environmentObject(player)
                            .environmentObject(library)
                            .environmentObject(theme)
                    }
                    .sheet(isPresented: $showingSimilarSheet) {
                        SimilarSongsSheet(seedTrack: track)
                            .environmentObject(player)
                            .environmentObject(library)
                            .environmentObject(theme)
                    }
                    .sheet(item: $selectedGeniusMeaning) { entry in
                        GeniusLineMeaningSheet(
                            lineText: selectedGeniusLineText,
                            annotation: entry
                        )
                        .environmentObject(theme)
                        .presentationDetents([.medium, .large])
                    }
                    .onChange(of: theme.motionCanvasEnabled) { _ in
                        player.refreshMotionCanvas()
                    }
                    .onChange(of: theme.motionCanvasStyle) { _ in
                        player.refreshMotionCanvas()
                    }
                    .onChange(of: showingLyrics) { active in
                        if active {
                            scheduleLyricsControlsAutoHide()
                        } else {
                            autoHideTask?.cancel()
                            controlsCollapsedInLyrics = false
                        }
                    }
                    .onChange(of: theme.autoHideLyricsControls) { _ in
                        scheduleLyricsControlsAutoHide()
                    }
                }

                if let toast = haptics.lastGestureToast {
                    floatingStatusPill(icon: "airpodspro", text: toast)
                        .padding(.top, 52)
                        .transition(.move(edge: .top).combined(with: .opacity))
                } else if let feedback = gestureFeedbackBadge {
                    floatingStatusPill(icon: feedback.icon, text: feedback.label)
                        .padding(.top, 52)
                        .transition(.scale(scale: 0.9).combined(with: .opacity))
                }
            }
            .frame(width: viewportWidth, height: viewportHeight)
            .background {
                backdropLayer(width: viewportWidth, height: viewportHeight)
                    .ignoresSafeArea()
            }
        }
    }

    private var verticalDismissDragGesture: some Gesture {
        DragGesture(minimumDistance: 16)
            .onChanged { value in
                guard !isScrubbing else { return }
                let dy = value.translation.height
                let dx = value.translation.width
                guard dy > 0 && dy > abs(dx) * 1.15 else { return }
                dismissDragOffset = dy * 0.72
            }
            .onEnded { value in
                let dy = value.translation.height
                let predicted = value.predictedEndTranslation.height
                let dx = value.translation.width
                if !isScrubbing && dy > abs(dx) && (dy > 105 || predicted > 195) {
                    haptics.playControlTap(style: .light)
                    dismiss()
                } else {
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.84)) {
                        dismissDragOffset = 0
                    }
                }
            }
    }

    private func landscapeSplitPlayerLayout(
        track: Track,
        viewportWidth: CGFloat,
        viewportHeight: CGFloat
    ) -> some View {
        let leftPaneWidth = viewportWidth * 0.44
        let rightPaneWidth = viewportWidth - leftPaneWidth

        return VStack(spacing: 0) {
            grabberAndTopBar(track: track)
                .padding(.top, 6)
                .padding(.horizontal, 24)
                .contentShape(Rectangle())
                .gesture(verticalDismissDragGesture)

            HStack(spacing: 0) {
                VStack {
                    Spacer(minLength: 8)
                    artworkStage(
                        track: track,
                        viewportWidth: leftPaneWidth,
                        maxStageHeight: max(160, viewportHeight - 84)
                    )
                    Spacer(minLength: 8)
                }
                .frame(width: leftPaneWidth)

                VStack(spacing: 0) {
                    if showingLyrics {
                        syncedLyricsPanel(track: track, maxHeight: max(120, viewportHeight - 250))
                            .padding(.top, 6)
                    } else {
                        Spacer(minLength: 8)
                    }

                    titleAndLikeRow(track: track, compact: true)
                        .padding(.horizontal, 20)

                    scrubberSection
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                    transportControlsRow(playSize: 54)
                        .padding(.horizontal, 20)
                        .padding(.top, 10)

                    bottomUtilityDock(track: track)
                        .padding(.horizontal, 16)
                        .padding(.top, 10)
                        .padding(.bottom, 10)
                }
                .frame(width: rightPaneWidth)
            }
        }
    }

    private func floatingStatusPill(icon: String, text: String) -> some View {
        HStack(spacing: 7) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(theme.accent)
            Text(text)
                .font(theme.font(size: 12, weight: .bold))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .derbisGlassCapsule(interactive: false, forceGlassOverBackdrop: true)
        .allowsHitTesting(false)
    }

    private func triggerBadge(icon: String, label: String) {
        withAnimation(.spring(response: 0.26, dampingFraction: 0.82)) {
            gestureFeedbackBadge = (icon, label)
        }
        badgeClearTask?.cancel()
        badgeClearTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_050_000_000)
            if !Task.isCancelled {
                withAnimation(.easeOut(duration: 0.2)) {
                    gestureFeedbackBadge = nil
                }
            }
        }
    }

    private func scheduleLyricsControlsAutoHide() {
        autoHideTask?.cancel()
        guard showingLyrics && theme.autoHideLyricsControls else {
            withAnimation(.easeInOut(duration: 0.25)) {
                controlsCollapsedInLyrics = false
            }
            return
        }
        autoHideTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 4_500_000_000)
            if !Task.isCancelled && showingLyrics && theme.autoHideLyricsControls && !isScrubbing {
                withAnimation(.easeInOut(duration: 0.3)) {
                    controlsCollapsedInLyrics = true
                }
            }
        }
    }

    private func revealLyricsControlsTemporarily() {
        withAnimation(.easeInOut(duration: 0.25)) {
            controlsCollapsedInLyrics = false
        }
        scheduleLyricsControlsAutoHide()
    }

    @ViewBuilder
    private func backdropLayer(width: CGFloat, height: CGFloat) -> some View {
        let bleedWidth = width + 40
        let bleedHeight = height + 160

        ZStack {
            if theme.isLightSkin && !isImmersiveCanvas && !isFillBackdrop {
                PixelGraphPaperBackground()
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()
            } else {
                switch theme.backdropStyle {
                case .still:
                    LinearGradient(
                        colors: [
                            Color(hex: 0x15151D),
                            Color(hex: 0x09090E),
                            Color.black
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()

                case .fill:
                    ZStack(alignment: .top) {
                        Color.black

                        if let uiImage = player.currentArtworkImage {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFill()
                                .frame(width: width, height: height * 0.66)
                                .clipped()
                                .opacity(showingLyrics ? 0.26 : 0.94)
                                .blur(radius: showingLyrics ? 30 : 0)
                        } else {
                            LinearGradient(
                                colors: [player.ambientPrimary, Color.black],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        }

                        LinearGradient(
                            stops: [
                                .init(color: Color.black.opacity(0.42), location: 0.0),
                                .init(color: Color.black.opacity(showingLyrics ? 0.60 : 0.04), location: 0.25),
                                .init(color: Color.black.opacity(showingLyrics ? 0.78 : 0.32), location: 0.50),
                                .init(color: Color.black.opacity(0.92), location: 0.66),
                                .init(color: Color.black, location: 0.80)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()

                case .fluid:
                    FluidAuraBackdropView(
                        primary: player.ambientPrimary,
                        secondary: player.ambientSecondary,
                        accent: theme.accent,
                        isPlaying: player.isPlaying
                    )
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()

                case .visualiser:
                    DerbisAudioVisualiserBackdropView(
                        primary: player.ambientPrimary,
                        secondary: player.ambientSecondary,
                        accent: theme.accent,
                        isPlaying: player.isPlaying
                    )
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()

                case .ambient, .animated:
                    if theme.skin == .lateNight && !isImmersiveCanvas {
                        LinearGradient(
                            colors: [Color(hex: 0x16120B), Color.black, Color.black],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(width: bleedWidth, height: bleedHeight)
                        .clipped()
                    } else {
                        ZStack {
                            Color.black
                            LinearGradient(
                                colors: [
                                    player.ambientPrimary,
                                    player.ambientSecondary,
                                    Color(hex: 0x060609)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )

                            if let uiImage = player.currentArtworkImage {
                                Image(uiImage: uiImage)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: bleedWidth, height: bleedHeight)
                                    .clipped()
                                    .blur(radius: 64)
                                    .opacity(0.38)
                            }

                            Color.black.opacity(0.34)
                        }
                        .frame(width: bleedWidth, height: bleedHeight)
                        .clipped()
                    }
                }
            }

            if isImmersiveCanvas, let motion = activeMotionAsset {
                LoopingMotionVideoView(
                    url: motion.playbackURL,
                    isPlaying: player.isPlaying
                )
                .id(motion.playbackURL.absoluteString)
                .frame(width: width, height: bleedHeight)
                .clipped()
                .opacity(showingLyrics ? 0.34 : 1.0)

                LinearGradient(
                    stops: [
                        .init(color: Color.black.opacity(0.46), location: 0.0),
                        .init(color: Color.black.opacity(showingLyrics ? 0.62 : 0.06), location: 0.28),
                        .init(color: Color.black.opacity(showingLyrics ? 0.72 : 0.16), location: 0.56),
                        .init(color: Color.black.opacity(0.84), location: 0.78),
                        .init(color: Color.black.opacity(0.94), location: 1.0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(width: bleedWidth, height: bleedHeight)
                .allowsHitTesting(false)
            }
        }
        .frame(width: width, height: height)
        .clipped()
    }

    private func grabberAndTopBar(track: Track) -> some View {
        VStack(spacing: 5) {
            if !(isImmersiveCanvas || isFillBackdrop) {
                Capsule()
                    .fill(fgSecondary.opacity(0.45))
                    .frame(width: 36, height: 4)
                    .onTapGesture { dismiss() }
            }

            HStack {
                Button {
                    haptics.playControlTap(style: .light)
                    dismiss()
                } label: {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 14.5, weight: .bold))
                        .foregroundColor(fgPrimary)
                        .derbisGlassCircle(
                            size: 36,
                            forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
                        )
                }
                .buttonStyle(.plain)

                Spacer()

                VStack(spacing: 3) {
                    Menu {
                        Section("Player Deck") {
                            ForEach(DerbisPlayerDeck.allCases) { deck in
                                Button {
                                    haptics.playSelectionTick()
                                    withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) {
                                        theme.playerDeck = deck
                                    }
                                } label: {
                                    Label(deck.title, systemImage: theme.playerDeck == deck ? "checkmark" : deck.icon)
                                }
                            }
                        }
                    } label: {
                        if isImmersiveCanvas || isFillBackdrop {
                            // Reference: a single calm, sentence-case context label.
                            Text(track.source.rawValue.capitalized)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(fgPrimary)
                                .lineLimit(1)
                        } else {
                            HStack(spacing: 5) {
                                Image(systemName: theme.effectivePlayerDeck.icon)
                                    .font(.system(size: 9.5, weight: .bold))
                                    .foregroundColor(theme.accent)
                                Text("\(track.source.rawValue.uppercased()) · \(theme.effectivePlayerDeck.title.uppercased())")
                                    .font(theme.font(size: 9.5, weight: .bold))
                                    .tracking(0.9)
                                    .foregroundColor(fgSecondary)
                                    .lineLimit(1)
                            }
                        }
                    }

                    if let motion = activeMotionAsset, theme.backdropStyle == .animated {
                        Button {
                            haptics.playSelectionTick()
                            withAnimation(.easeInOut(duration: 0.25)) {
                                theme.motionCanvasStyle = (theme.motionCanvasStyle == .immersive) ? .framed : .immersive
                            }
                        } label: {
                            Text(motion.provider.rawValue.uppercased())
                                .font(theme.font(size: 9, weight: .bold))
                                .tracking(0.8)
                                .foregroundColor(.white.opacity(0.92))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Capsule().fill(Color.white.opacity(0.16)))
                        }
                        .buttonStyle(.plain)
                    } else if let timer = player.sleepTimerRemainingSec {
                        Text("Sleep \(TimeFormatting.clockString(seconds: Double(timer)))")
                            .font(theme.font(size: 10.5, weight: .semibold))
                            .foregroundColor(theme.accent)
                            .lineLimit(1)
                    } else if fx.hasAnyActiveModification {
                        Button {
                            showingAudioStudioSheet = true
                        } label: {
                            Text(abs(fx.playbackSpeed - 1.0) > 0.01 ? String(format: "FX • %.2gx", fx.playbackSpeed) : "MIX LAB ACTIVE")
                                .font(theme.font(size: 9, weight: .bold))
                                .tracking(0.7)
                                .foregroundColor(theme.accent)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Capsule().fill(Color.white.opacity(0.12)))
                        }
                        .buttonStyle(.plain)
                    }
                }

                Spacer()

                Button {
                    haptics.playControlTap(style: .light)
                    showingStudioConsole = true
                } label: {
                    Image(systemName: (isImmersiveCanvas || isFillBackdrop) ? "ellipsis" : "slider.horizontal.below.rectangle")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(fgPrimary)
                        .derbisGlassCircle(
                            size: 36,
                            forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func artworkStage(track: Track, viewportWidth: CGFloat, maxStageHeight: CGFloat) -> some View {
        let hasFooter = player.errorBanner != nil || (theme.skin == .lateNight && player.currentIndex + 1 < player.queue.count)
        let footerReserve: CGFloat = hasFooter ? 36 : 0
        let availableArtHeight = max(128, maxStageHeight - footerReserve - 4)
        let side = max(128, min(viewportWidth - 48, availableArtHeight))

        return VStack(spacing: 8) {
            ZStack {
                if isImmersiveCanvas || isFillBackdrop {
                    Color.clear
                        .frame(width: viewportWidth, height: availableArtHeight)
                } else {
                    DerbisDeckArtworkStage(
                        deck: theme.effectivePlayerDeck,
                        track: track,
                        side: side,
                        activeMotionAsset: activeMotionAsset,
                        isPlaying: player.isPlaying,
                        accent: theme.accent
                    )
                    .offset(x: artworkDragOffset)
                    .frame(width: viewportWidth, height: side)
                }

                if theme.playerGesturesEnabled {
                    HStack(spacing: 0) {
                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture(count: 2) {
                                haptics.playControlTap(style: .medium)
                                player.skipBy(seconds: -10)
                                triggerBadge(icon: "gobackward.10", label: "-10s")
                            }

                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture(count: 2) {
                                haptics.playSuccessFeedback()
                                library.toggleLike(track)
                                let likedNow = library.isLiked(track)
                                triggerBadge(
                                    icon: likedNow ? "heart.fill" : "heart.slash",
                                    label: likedNow ? "Liked" : "Removed Like"
                                )
                            }

                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture(count: 2) {
                                haptics.playControlTap(style: .medium)
                                player.skipBy(seconds: 10)
                                triggerBadge(icon: "goforward.10", label: "+10s")
                            }
                    }
                    .frame(width: viewportWidth, height: (isImmersiveCanvas || isFillBackdrop) ? availableArtHeight : side)
                    .simultaneousGesture(
                        DragGesture(minimumDistance: 20)
                            .onChanged { value in
                                let dx = value.translation.width
                                let dy = value.translation.height
                                if abs(dx) > abs(dy) {
                                    artworkDragOffset = dx * 0.32
                                } else if dy > 0 && dy > abs(dx) * 1.2 {
                                    dismissDragOffset = dy * 0.72
                                }
                            }
                            .onEnded { value in
                                let dx = value.translation.width
                                let dy = value.translation.height
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                    artworkDragOffset = 0
                                }
                                if dy > abs(dx) && (dy > 110 || value.predictedEndTranslation.height > 205) {
                                    haptics.playControlTap(style: .light)
                                    dismiss()
                                    return
                                }
                                if dismissDragOffset > 0 {
                                    withAnimation(.spring(response: 0.32, dampingFraction: 0.84)) {
                                        dismissDragOffset = 0
                                    }
                                }
                                guard abs(dx) > abs(dy) else { return }
                                if dx < -56 {
                                    haptics.playControlTap(style: .medium)
                                    player.skipNext()
                                } else if value.translation.width > 56 {
                                    haptics.playControlTap(style: .medium)
                                    player.skipPrevious()
                                }
                            }
                    )
                }

                // Hold-to-reveal droplet: blurred lyrics under a chrome bead of glass.
                if holdLensRevealed, let lensLocation = holdLensLocation {
                    let stageHeight = (isImmersiveCanvas || isFillBackdrop) ? availableArtHeight : side
                    let lensX = min(max(lensLocation.x, 118), viewportWidth - 118)
                    let lensY = min(max(lensLocation.y, 136), stageHeight - 136)

                    ArtworkLyricLens(lines: lensLines, accent: theme.accent)
                        .position(x: lensX, y: lensY)
                        .transition(.scale(scale: 0.8).combined(with: .opacity))
                        .allowsHitTesting(false)
                }
            }
            .simultaneousGesture(artworkHoldGesture)

            if let error = player.errorBanner {
                Text(error)
                    .font(theme.font(size: 12, weight: .medium))
                    .foregroundColor(theme.accent)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            } else if theme.skin == .lateNight,
                      player.currentIndex + 1 < player.queue.count {
                let nextTrack = player.queue[player.currentIndex + 1]
                HStack(spacing: 8) {
                    Text("UP NEXT")
                        .font(theme.font(size: 10, weight: .bold))
                        .foregroundColor(theme.accent)
                    Text("\(nextTrack.title) · \(nextTrack.artist)")
                        .font(theme.font(size: 12, weight: .medium))
                        .foregroundColor(fgSecondary)
                        .lineLimit(1)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(Color.white.opacity(0.08)))
                .padding(.horizontal, 24)
            }
        }
    }

    private func syncedLyricsPanel(track: Track, maxHeight: CGFloat) -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                TrackArtworkView(
                    url: track.displayArtworkURL,
                    size: 34,
                    cornerRadius: theme.isPixelSkin ? 0 : 8
                )

                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 6) {
                        Text(player.currentLyrics?.wordSynced == true ? "Word-Synced" : "Lyrics")
                            .font(theme.font(size: 12, weight: .bold))
                            .foregroundColor(fgPrimary)

                        if let prov = player.currentLyrics?.provider, !prov.isEmpty {
                            Text(prov.uppercased())
                                .font(.system(size: 8.5, weight: .bold))
                                .foregroundColor(theme.accent)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 1.5)
                                .background(Capsule().fill(Color.white.opacity(0.12)))
                        }
                    }

                    Text(player.currentLyrics?.synced == true ? "Tap any line to jump" : "Plain text")
                        .font(theme.font(size: 10.5))
                        .foregroundColor(fgSecondary)
                }

                Spacer(minLength: 4)

                if player.currentLyrics?.hasRomanization == true {
                    lyricQuickTogglePill(
                        label: "Aa",
                        active: theme.showLyricsRomanization
                    ) {
                        haptics.playSelectionTick()
                        theme.showLyricsRomanization.toggle()
                    }
                }

                if player.currentLyrics?.hasTranslation == true {
                    lyricQuickTogglePill(
                        label: "文A",
                        active: theme.showLyricsTranslation
                    ) {
                        haptics.playSelectionTick()
                        theme.showLyricsTranslation.toggle()
                    }
                }

                Button {
                    haptics.playControlTap(style: .light)
                    withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                        showingLyrics = false
                    }
                } label: {
                    Text("Artwork")
                        .font(theme.font(size: 11, weight: .bold))
                        .foregroundColor(fgPrimary)
                        .padding(.horizontal, 11)
                        .padding(.vertical, 5.5)
                        .derbisGlassCapsule(
                            forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 22)

            if player.isLoadingLyrics {
                Spacer()
                ProgressView()
                    .tint(fgPrimary)
                Text("Searching word-synced lyrics…")
                    .font(theme.font(size: 13))
                    .foregroundColor(fgSecondary)
                Spacer()
            } else if let payload = player.currentLyrics, !payload.lines.isEmpty {
                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 18) {
                            ForEach(Array(payload.lines.enumerated()), id: \.element.id) { idx, line in
                                let isActive = player.activeLyricIndex == line.id
                                let prevEnd = idx > 0
                                    ? payload.lines[idx - 1].endTimeSec
                                    : 0.0
                                let gapDuration = line.startTimeSec - prevEnd

                                VStack(spacing: 12) {
                                    if payload.synced && gapDuration >= 7.0 {
                                        NowPlayingInstrumentalGapSlot(
                                            prevEnd: prevEnd,
                                            lineStart: line.startTimeSec,
                                            gapDuration: gapDuration,
                                            accent: theme.accent
                                        )
                                    }

                                    let meaning = player.currentGeniusMeanings[line.id]
                                    VStack(alignment: line.isSecondaryVoice ? .trailing : .leading, spacing: 6) {
                                        Button {
                                            if payload.synced {
                                                haptics.playControlTap(style: .light)
                                                player.seek(to: line.startTimeSec)
                                            }
                                            if controlsCollapsedInLyrics {
                                                revealLyricsControlsTemporarily()
                                            }
                                        } label: {
                                            lyricLineContent(line: line, isActive: isActive)
                                        }
                                        .buttonStyle(.plain)
                                        .contextMenu {
                                            if let entry = meaning {
                                                Button {
                                                    selectedGeniusLineText = line.text
                                                    selectedGeniusMeaning = entry
                                                } label: {
                                                    Label("View Genius Meaning", systemImage: "quote.bubble.fill")
                                                }
                                            }
                                            Button {
                                                UIPasteboard.general.string = line.text
                                            } label: {
                                                Label("Copy Lyric Line", systemImage: "doc.on.doc")
                                            }
                                        }

                                        if let entry = meaning, theme.showGeniusMeaningMarkers {
                                            Button {
                                                haptics.playSelectionTick()
                                                selectedGeniusLineText = line.text
                                                selectedGeniusMeaning = entry
                                            } label: {
                                                HStack(spacing: 4) {
                                                    Image(systemName: entry.isVerifiedArtist ? "checkmark.seal.fill" : "quote.bubble.fill")
                                                        .font(.system(size: 9.5, weight: .bold))
                                                    Text(entry.isVerifiedArtist ? "Artist Meaning" : "Genius Meaning")
                                                        .font(.system(size: 10.5, weight: .bold, design: .rounded))
                                                }
                                                .foregroundColor(theme.accent)
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 3.5)
                                                .background(Capsule().fill(Color.white.opacity(0.12)))
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }
                                .id(line.id)
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 22)
                    }
                    .simultaneousGesture(
                        DragGesture(minimumDistance: 12)
                            .onChanged { _ in
                                if controlsCollapsedInLyrics {
                                    revealLyricsControlsTemporarily()
                                } else if theme.autoHideLyricsControls {
                                    scheduleLyricsControlsAutoHide()
                                }
                            }
                    )
                    .onAppear {
                        if let activeId = player.activeLyricIndex {
                            proxy.scrollTo(activeId, anchor: .center)
                        }
                    }
                    .onChange(of: player.activeLyricIndex) { activeId in
                        guard let activeId = activeId else { return }
                        withAnimation(.spring(response: 0.38, dampingFraction: 0.86)) {
                            proxy.scrollTo(activeId, anchor: .center)
                        }
                    }
                }

                DerbisSingLyricsFloatingBar()
            } else {
                Spacer()
                VStack(spacing: 10) {
                    Image(systemName: "quote.bubble")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(fgSecondary)
                    Text("No synced lyrics found")
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(fgPrimary)
                    Button("Retry lookup") {
                        player.loadLyrics(for: track, forceRefresh: true)
                    }
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.accent)
                }
                Spacer()
            }
        }
        .frame(maxHeight: maxHeight)
    }

    private func lyricQuickTogglePill(label: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 11, weight: .bold, design: .rounded))
                .foregroundColor(active ? .black : fgPrimary.opacity(0.85))
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(
                    Capsule()
                        .fill(active ? Color.white : Color.white.opacity(0.14))
                )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func lyricLineContent(line: LyricLine, isActive: Bool) -> some View {
        let align: HorizontalAlignment = line.isSecondaryVoice ? .trailing : .leading
        let textAlign: TextAlignment = line.isSecondaryVoice ? .trailing : .leading

        VStack(alignment: align, spacing: 5) {
            if isActive && line.words.count >= 2 {
                NowPlayingKaraokeActiveText(
                    line: line,
                    fgPrimary: fgPrimary,
                    font: theme.font(size: 23, weight: .bold),
                    textAlign: textAlign
                )
            } else {
                Text(line.text)
                    .font(theme.font(size: isActive ? 23 : 18.5, weight: .bold))
                    .foregroundColor(isActive ? fgPrimary : fgSecondary.opacity(0.42))
                    .multilineTextAlignment(textAlign)
            }

            if let bg = line.backingText, !bg.isEmpty {
                Text("(\(bg))")
                    .font(theme.font(size: isActive ? 14.5 : 13, weight: .semibold))
                    .italic()
                    .foregroundColor(isActive ? fgPrimary.opacity(0.72) : fgSecondary.opacity(0.32))
                    .multilineTextAlignment(textAlign)
            }

            if theme.showLyricsRomanization, let roman = line.pronunciation, !roman.isEmpty {
                Text(roman)
                    .font(theme.font(size: 13.5, weight: .medium))
                    .foregroundColor(isActive ? theme.accent.opacity(0.95) : fgSecondary.opacity(0.35))
                    .multilineTextAlignment(textAlign)
            }

            if theme.showLyricsTranslation, let trans = line.translation, !trans.isEmpty {
                Text(trans)
                    .font(theme.font(size: 13.5, weight: .medium))
                    .foregroundColor(isActive ? fgPrimary.opacity(0.78) : fgSecondary.opacity(0.35))
                    .multilineTextAlignment(textAlign)
            }
        }
        .frame(maxWidth: .infinity, alignment: line.isSecondaryVoice ? .trailing : .leading)
        .scaleEffect(isActive ? 1.0 : 0.97, anchor: line.isSecondaryVoice ? .trailing : .leading)
        .animation(.easeOut(duration: 0.2), value: isActive)
    }

    private func titleAndLikeRow(track: Track, compact: Bool = false) -> some View {
        HStack(alignment: .center, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(theme.font(size: compact ? 18 : 20, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .lineLimit(1)

                Button {
                    haptics.playSelectionTick()
                    showingArtistSheet = true
                } label: {
                    HStack(spacing: 4) {
                        Text(track.artist)
                            .font(theme.font(size: compact ? 13.5 : 14.5, weight: .semibold))
                            .foregroundColor(fgSecondary)
                            .lineLimit(1)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(theme.accent.opacity(0.85))
                    }
                }
                .buttonStyle(.plain)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if track.source != .localFile {
                let downloaded = library.isDownloaded(track)
                let downloading = library.activeDownloads[track.id] != nil
                Button {
                    if !downloaded && !downloading {
                        haptics.playControlTap(style: .light)
                        Task {
                            await library.downloadTrackForOffline(track, preferHighQuality: theme.highQualityAudio)
                        }
                    }
                } label: {
                    Group {
                        if downloading {
                            ProgressView()
                                .scaleEffect(0.72)
                                .tint(theme.accent)
                        } else {
                            Image(systemName: downloaded ? "arrow.down.circle.fill" : "arrow.down.circle")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(downloaded ? theme.accent : fgSecondary)
                        }
                    }
                    .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
            }

            Button {
                haptics.playControlTap(style: .medium)
                library.toggleLike(track)
            } label: {
                if isImmersiveCanvas || isFillBackdrop {
                    // Reference treatment: a filled red circle with a white check when the
                    // record is in your rotation, an open circle otherwise.
                    ZStack {
                        Circle()
                            .fill(library.isLiked(track) ? theme.accent : Color.clear)
                        Circle()
                            .strokeBorder(library.isLiked(track) ? Color.clear : Color.white.opacity(0.5), lineWidth: 1.6)
                        Image(systemName: library.isLiked(track) ? "checkmark" : "plus")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(width: 30, height: 30)
                    .frame(width: 38, height: 38)
                } else {
                    Image(systemName: library.isLiked(track) ? "heart.fill" : "heart")
                        .font(.system(size: 21, weight: .semibold))
                        .foregroundColor(library.isLiked(track) ? theme.accent : fgSecondary)
                        .frame(width: 38, height: 38)
                }
            }
            .buttonStyle(.plain)
        }
    }

    private var scrubberSection: some View {
        let activeDeck = theme.effectivePlayerDeck
        let rail: Color = (theme.isLightSkin && !isImmersiveCanvas && !isFillBackdrop)
            ? theme.border.opacity(0.26)
            : Color.white.opacity(0.20)
        return DerbisDeckScrubberView(
            deck: activeDeck,
            trackId: player.currentTrack?.id ?? "derbis",
            isScrubbing: $isScrubbing,
            scrubTime: $scrubTime,
            lastScrubNotch: $lastScrubNotch,
            accentColor: (activeDeck == .appleMusic13 || isImmersiveCanvas || isFillBackdrop) ? fgPrimary : theme.accent,
            railColor: rail,
            fgSecondary: fgSecondary,
            labelFont: theme.font(size: 11, weight: .semibold),
            onSeekCommit: { target in
                player.seek(to: target)
                scheduleLyricsControlsAutoHide()
            },
            centerAccessory: {
                codecBadgePill
            }
        )
    }

    private var codecBadgePill: some View {
        Button {
            haptics.playSelectionTick()
            showingAudioStudioSheet = true
        } label: {
            HStack(spacing: 5) {
                if player.isLoading {
                    ProgressView()
                        .scaleEffect(0.65)
                        .tint(fgPrimary)
                } else {
                    Image(systemName: fx.hasAnyActiveModification ? "slider.vertical.3" : "waveform")
                        .font(.system(size: 9, weight: .bold))
                }
                Text(player.activeCodecBadge)
                    .font(theme.font(size: 10, weight: .bold))
                    .lineLimit(1)
            }
            .foregroundColor(fx.hasAnyActiveModification ? .white : fgPrimary.opacity(0.92))
            .padding(.horizontal, 10)
            .padding(.vertical, 3.5)
            .derbisGlassCapsule(
                prominent: fx.hasAnyActiveModification,
                forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func transportControlsRow(playSize: CGFloat = 62) -> some View {
        if isImmersiveCanvas || isFillBackdrop {
            chromeTransportRow
        } else {
            legacyTransportRow(playSize: playSize)
        }
    }

    /// Full-bleed (reference) transport: plain glyphs — red shuffle with its dot,
    /// white skip glyphs, a big white pause — and the repeat at the far edge.
    private var chromeTransportRow: some View {
        return HStack(spacing: 0) {
            Button {
                haptics.playSelectionTick()
                player.isShuffleEnabled.toggle()
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: "shuffle")
                        .font(.system(size: 17, weight: .semibold))
                    Circle()
                        .fill(player.isShuffleEnabled ? theme.accent : Color.clear)
                        .frame(width: 4, height: 4)
                }
                .foregroundColor(player.isShuffleEnabled ? theme.accent : fgSecondary)
                .frame(width: 46, height: 46)
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipPrevious()
            } label: {
                Image(systemName: "backward.fill")
                    .font(.system(size: 23, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 54, height: 46)
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                haptics.playControlTap(style: .medium, scale: 1.0)
                player.togglePlayPause()
            } label: {
                ZStack {
                    if player.isLoading {
                        ProgressView()
                            .tint(fgPrimary)
                    } else {
                        Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 38, weight: .bold))
                            .foregroundColor(fgPrimary)
                            .offset(x: player.isPlaying ? 0 : 4)
                    }
                }
                .frame(width: 74, height: 62)
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipNext()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 23, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 54, height: 46)
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                haptics.playSelectionTick()
                player.repeatMode = player.repeatMode.next()
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: player.repeatMode.systemImage)
                        .font(.system(size: 17, weight: .semibold))
                    Circle()
                        .fill(player.repeatMode == .off ? Color.clear : theme.accent)
                        .frame(width: 4, height: 4)
                }
                .foregroundColor(player.repeatMode == .off ? fgSecondary : theme.accent)
                .frame(width: 46, height: 46)
            }
            .buttonStyle(.plain)
        }
    }

    private func legacyTransportRow(playSize: CGFloat = 62) -> some View {
        let deck = theme.effectivePlayerDeck
        let usePixelButtons = (theme.isPixelSkin || deck == .pixel) && !isImmersiveCanvas && !isFillBackdrop
        let useSquircleMorph = deck == .flow || deck == .sleeve || deck == .frost
        let playCorner: CGFloat = usePixelButtons ? 0 : (theme.isTorqueSkin ? 10 : (useSquircleMorph ? (player.isPlaying ? 18 : playSize * 0.5) : playSize * 0.5))

        return HStack {
            Button {
                haptics.playSelectionTick()
                player.isShuffleEnabled.toggle()
            } label: {
                Image(systemName: "shuffle")
                    .font(.system(size: 17.5, weight: .semibold))
                    .foregroundColor(player.isShuffleEnabled ? theme.accent : fgSecondary)
                    .frame(width: 40, height: 40)
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipPrevious()
            } label: {
                Image(systemName: "backward.fill")
                    .font(.system(size: 21, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 42)
                    .background(
                        Group {
                            if usePixelButtons {
                                Rectangle()
                                    .fill(theme.surface)
                                    .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                            } else if deck == .flow || deck == .frost {
                                Capsule()
                                    .fill(theme.isLightSkin && !isImmersiveCanvas && !isFillBackdrop ? theme.elevatedSurface : Color.white.opacity(0.10))
                            }
                        }
                    )
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium, scale: 1.0)
                player.togglePlayPause()
            } label: {
                ZStack {
                    if usePixelButtons {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.28))
                            .frame(width: playSize, height: playSize)
                            .offset(x: 3, y: 3)
                        Rectangle()
                            .fill(theme.accent)
                            .frame(width: playSize, height: playSize)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    } else if deck == .appleMusic13 {
                        Circle()
                            .fill(fgPrimary)
                            .frame(width: playSize, height: playSize)
                    } else {
                        RoundedRectangle(cornerRadius: playCorner, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [theme.accent, theme.secondaryAccent.opacity(0.88)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: playSize + (useSquircleMorph ? 8 : 0), height: playSize)
                            .shadow(color: theme.accent.opacity(0.36), radius: 14, x: 0, y: 6)
                            .animation(.spring(response: 0.34, dampingFraction: 0.75), value: player.isPlaying)
                    }

                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: max(21, playSize * 0.40), weight: .bold))
                        .foregroundColor(
                            usePixelButtons
                                ? .white
                                : (deck == .appleMusic13 ? (theme.isLightSkin ? .white : .black) : theme.onAccentText)
                        )
                        .offset(x: player.isPlaying ? 0 : 2)
                }
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipNext()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 21, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 42)
                    .background(
                        Group {
                            if usePixelButtons {
                                Rectangle()
                                    .fill(theme.surface)
                                    .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                            } else if deck == .flow || deck == .frost {
                                Capsule()
                                    .fill(theme.isLightSkin && !isImmersiveCanvas && !isFillBackdrop ? theme.elevatedSurface : Color.white.opacity(0.10))
                            }
                        }
                    )
            }

            Spacer()

            Button {
                haptics.playSelectionTick()
                player.repeatMode = player.repeatMode.next()
            } label: {
                Image(systemName: player.repeatMode.systemImage)
                    .font(.system(size: 17.5, weight: .semibold))
                    .foregroundColor(player.repeatMode == .off ? fgSecondary : theme.accent)
                    .frame(width: 40, height: 40)
            }
        }
    }

    @ViewBuilder
    private func bottomUtilityDock(track: Track) -> some View {
        if isImmersiveCanvas || isFillBackdrop {
            referenceUtilityDock(track: track)
        } else {
            legacyUtilityDock(track: track)
        }
    }

    /// Full-bleed reference dock: three bare glyphs, no capsule — lyrics on the left,
    /// the red headphones (tap through to AirPlay output) in the centre, list on the right.
    private func referenceUtilityDock(track: Track) -> some View {
        HStack(spacing: 0) {
            Button {
                haptics.playControlTap(style: .light)
                withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                    showingLyrics.toggle()
                }
                if showingLyrics && player.currentLyrics == nil && !player.isLoadingLyrics {
                    player.loadLyrics(for: track)
                }
            } label: {
                Image(systemName: showingLyrics ? "quote.bubble.fill" : "quote.bubble")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(showingLyrics ? theme.accent : fgPrimary)
                    .frame(width: 52, height: 44)
            }
            .buttonStyle(.plain)

            Spacer()

            ZStack {
                Image(systemName: "headphones")
                    .font(.system(size: 23, weight: .medium))
                    .foregroundColor(theme.accent)
                    .allowsHitTesting(false)
                AirPlayRoutePicker(tintColor: .clear)
                    .frame(width: 52, height: 44)
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .light)
                showingQueue = true
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 19, weight: .medium))
                    .foregroundColor(fgPrimary)
                    .frame(width: 52, height: 44)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 44)
        .frame(height: 44)
    }

    private func legacyUtilityDock(track: Track) -> some View {
        let airplayTint: UIColor = (theme.isLightSkin && !isImmersiveCanvas && !isFillBackdrop)
            ? UIColor(theme.primaryText)
            : UIColor.white

        return HStack(spacing: 0) {
            Button {
                haptics.playControlTap(style: .light)
                withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                    showingLyrics.toggle()
                }
                if showingLyrics && player.currentLyrics == nil && !player.isLoadingLyrics {
                    player.loadLyrics(for: track)
                }
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: showingLyrics ? "quote.bubble.fill" : "quote.bubble")
                        .font(.system(size: 15.5, weight: .semibold))
                    Text("Lyrics")
                        .font(theme.font(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                .foregroundColor(showingLyrics ? theme.accent : fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
            }
            .buttonStyle(.plain)

            Button {
                haptics.playControlTap(style: .light)
                showingAudioStudioSheet = true
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: "slider.vertical.3")
                        .font(.system(size: 15.5, weight: .semibold))
                    Text("Mix Lab")
                        .font(theme.font(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                .foregroundColor(fx.hasAnyActiveModification ? theme.accent : fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
            }
            .buttonStyle(.plain)

            VStack(spacing: 3) {
                AirPlayRoutePicker(tintColor: airplayTint)
                    .frame(width: 24, height: 19)
                Text("AirPlay")
                    .font(theme.font(size: 10, weight: .semibold))
                    .foregroundColor(fgSecondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 38)

            Button {
                haptics.playControlTap(style: .light)
                showingJamSheet = true
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: "person.2.wave.2.fill")
                        .font(.system(size: 15, weight: .semibold))
                    Text(jam.isActive ? "Jam · \(max(1, jam.participantCount))" : "Jam")
                        .font(theme.font(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                .foregroundColor(jam.isActive ? theme.accent : fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
            }
            .buttonStyle(.plain)

            Button {
                haptics.playControlTap(style: .light)
                showingQueue = true
            } label: {
                VStack(spacing: 3) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 15.5, weight: .semibold))
                    Text("Queue")
                        .font(theme.font(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                .foregroundColor(fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .derbisGlassCapsule(
            interactive: false,
            forceGlassOverBackdrop: isImmersiveCanvas || isFillBackdrop
        )
    }

    private var currentInlineLyricText: String? {
        guard let payload = player.currentLyrics,
              payload.synced,
              let active = player.activeLyricIndex,
              payload.lines.indices.contains(active) else {
            return nil
        }
        return payload.lines[active].text
    }
}

private struct InstrumentalBreathingDots: View {
    let progress: Double
    let accent: Color

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<3, id: \.self) { idx in
                let threshold = Double(idx + 1) / 3.5
                let lit = progress >= threshold
                Circle()
                    .fill(lit ? Color.white : Color.white.opacity(0.32))
                    .frame(width: 9, height: 9)
                    .scaleEffect(lit ? 1.18 : 0.92)
                    .animation(.easeInOut(duration: 0.35), value: lit)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct FluidAuraBackdropView: View {
    let primary: Color
    let secondary: Color
    let accent: Color
    let isPlaying: Bool

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0, paused: !isPlaying)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            Canvas { context, size in
                context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(Color(hex: 0x07070C)))

                let w = size.width
                let h = size.height
                let radius = max(w, h) * 0.72

                let p1 = CGPoint(
                    x: w * (0.5 + 0.28 * cos(t * 0.31)),
                    y: h * (0.32 + 0.18 * sin(t * 0.27))
                )
                let p2 = CGPoint(
                    x: w * (0.48 + 0.30 * sin(t * 0.23 + 1.4)),
                    y: h * (0.65 + 0.20 * cos(t * 0.29 + 0.8))
                )
                let p3 = CGPoint(
                    x: w * (0.52 + 0.24 * cos(t * 0.19 + 2.8)),
                    y: h * (0.46 + 0.24 * sin(t * 0.22 + 2.1))
                )

                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .radialGradient(
                        Gradient(colors: [primary.opacity(0.75), .clear]),
                        center: p1,
                        startRadius: 10,
                        endRadius: radius
                    )
                )
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .radialGradient(
                        Gradient(colors: [secondary.opacity(0.72), .clear]),
                        center: p2,
                        startRadius: 10,
                        endRadius: radius
                    )
                )
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .radialGradient(
                        Gradient(colors: [accent.opacity(0.32), .clear]),
                        center: p3,
                        startRadius: 10,
                        endRadius: radius * 0.82
                    )
                )
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .color(Color.black.opacity(0.28))
                )
            }
        }
    }
}

struct UpNextQueueSheet: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    @State private var showPlayedHistory: Bool = false

    private var playedEntries: [(index: Int, track: Track)] {
        guard !player.queue.isEmpty, player.currentIndex > 0 else { return [] }
        let upper = min(player.currentIndex, player.queue.count)
        return (0..<upper).map { ($0, player.queue[$0]) }
    }

    private var upcomingEntries: [(index: Int, track: Track)] {
        guard !player.queue.isEmpty else { return [] }
        let start = min(max(0, player.currentIndex), player.queue.count - 1)
        return (start..<player.queue.count).map { ($0, player.queue[$0]) }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                VStack(spacing: 0) {
                    endlessHeaderBar
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 6)

                    List {
                        if !playedEntries.isEmpty {
                            Section {
                                DisclosureGroup(
                                    isExpanded: $showPlayedHistory
                                ) {
                                    ForEach(playedEntries, id: \.index) { entry in
                                        queueRow(entry: entry)
                                    }
                                } label: {
                                    Text("Played earlier (\(playedEntries.count))")
                                        .font(theme.font(size: 12.5, weight: .bold))
                                        .foregroundColor(theme.secondaryText)
                                }
                            }
                            .listRowBackground(theme.surface)
                        }

                        Section {
                            ForEach(upcomingEntries, id: \.index) { entry in
                                queueRow(entry: entry)
                                    .onAppear {
                                        if player.endlessListeningEnabled,
                                           entry.index >= max(0, player.queue.count - 3) {
                                            Task {
                                                await player.loadMoreEndlessRecommendations()
                                            }
                                        }
                                    }
                            }
                            .onDelete { relativeOffsets in
                                let absolute = IndexSet(
                                    relativeOffsets.compactMap { rel in
                                        upcomingEntries.indices.contains(rel) ? upcomingEntries[rel].index : nil
                                    }
                                )
                                player.removeFromQueue(at: absolute)
                            }

                            if player.endlessListeningEnabled {
                                HStack(spacing: 10) {
                                    if player.isExpandingEndlessQueue {
                                        ProgressView()
                                            .tint(theme.accent)
                                        Text("Curating more from song radio…")
                                            .font(theme.font(size: 12.5, weight: .medium))
                                            .foregroundColor(theme.secondaryText)
                                    } else {
                                        Button {
                                            Task {
                                                await player.loadMoreEndlessRecommendations()
                                            }
                                        } label: {
                                            Label("Load more endless picks", systemImage: "infinity")
                                                .font(theme.font(size: 13, weight: .bold))
                                                .foregroundColor(theme.accent)
                                        }
                                    }
                                    Spacer()
                                }
                                .padding(.vertical, 6)
                                .listRowBackground(theme.surface)
                            }
                        } header: {
                            HStack {
                                Text(player.endlessListeningEnabled ? "NOW & ENDLESS UP NEXT" : "UP NEXT")
                                    .font(theme.font(size: 11, weight: .bold))
                                    .foregroundColor(theme.secondaryText)
                                Spacer()
                                if player.upcomingQueueCount() > 0 {
                                    Button("Clear upcoming") {
                                        player.clearUpcomingQueue()
                                    }
                                    .font(theme.font(size: 11, weight: .bold))
                                    .foregroundColor(theme.accent)
                                }
                            }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("Queue (\(upcomingEntries.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
        }
    }

    private var endlessHeaderBar: some View {
        HStack(spacing: 10) {
            Button {
                HapticsAndGesturesService.shared.playSelectionTick()
                let next = !player.endlessListeningEnabled
                player.setEndlessListeningEnabled(next)
                theme.endlessRadioEnabled = next
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "infinity")
                        .font(.system(size: 12.5, weight: .bold))
                    Text(player.endlessListeningEnabled ? "Endless Listening On" : "Endless Listening Off")
                        .font(theme.font(size: 12.5, weight: .bold))
                }
                .foregroundColor(player.endlessListeningEnabled ? theme.onAccentText : theme.primaryText)
                .padding(.horizontal, 13)
                .padding(.vertical, 8)
                .derbisGlassCapsule(prominent: player.endlessListeningEnabled)
            }
            .buttonStyle(.plain)

            Spacer()

            if let current = player.currentTrack {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                    player.startSongRadio(from: current)
                } label: {
                    Label("Reseed Radio", systemImage: "dot.radiowaves.left.and.right")
                        .font(theme.font(size: 12, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .derbisGlassCapsule()
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func queueRow(entry: (index: Int, track: Track)) -> some View {
        let isCurrent = entry.index == player.currentIndex
        let isAutoRadio = player.radioAutoTrackIds.contains(entry.track.id)

        return Button {
            player.play(track: entry.track, in: player.queue)
            dismiss()
        } label: {
            HStack(spacing: 12) {
                TrackArtworkView(url: entry.track.displayArtworkURL, size: 42)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(entry.track.title)
                            .font(theme.font(size: 14, weight: isCurrent ? .bold : .medium))
                            .foregroundColor(isCurrent ? theme.accent : theme.primaryText)
                            .lineLimit(1)

                        if isAutoRadio && !isCurrent {
                            Text("RADIO")
                                .font(.system(size: 8.5, weight: .black, design: .rounded))
                                .foregroundColor(theme.accent)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 1.5)
                                .background(Capsule().fill(theme.accent.opacity(0.15)))
                        }
                    }

                    Text(entry.track.artist)
                        .font(theme.font(size: 12))
                        .foregroundColor(theme.secondaryText)
                        .lineLimit(1)
                }
                Spacer()
                if isCurrent {
                    Image(systemName: "waveform")
                        .foregroundColor(theme.accent)
                }
            }
        }
        .listRowBackground(theme.surface)
    }
}

private struct NowPlayingInstrumentalGapSlot: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let prevEnd: Double
    let lineStart: Double
    let gapDuration: Double
    let accent: Color

    var body: some View {
        if clock.currentTime >= prevEnd && clock.currentTime < lineStart {
            InstrumentalBreathingDots(
                progress: min(1.0, max(0.0, (clock.currentTime - prevEnd) / max(0.1, gapDuration))),
                accent: accent
            )
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct NowPlayingKaraokeActiveText: View {
    @ObservedObject private var clock = PlaybackClock.shared
    let line: LyricLine
    let fgPrimary: Color
    let font: Font
    let textAlign: TextAlignment

    var body: some View {
        Text(buildAttributedText())
            .font(font)
            .multilineTextAlignment(textAlign)
    }

    private func buildAttributedText() -> AttributedString {
        var result = AttributedString()
        let now = clock.currentTime + 0.08

        for (idx, word) in line.words.enumerated() {
            if idx > 0 && !word.joinedToPrevious {
                result.append(AttributedString(" "))
            }
            var piece = AttributedString(word.text)
            if now >= word.endTimeSec {
                piece.foregroundColor = fgPrimary
            } else if now >= word.startTimeSec {
                let dur = max(0.05, word.endTimeSec - word.startTimeSec)
                let prog = min(1.0, max(0.0, (now - word.startTimeSec) / dur))
                piece.foregroundColor = fgPrimary.opacity(0.62 + 0.38 * prog)
            } else {
                piece.foregroundColor = fgPrimary.opacity(0.36)
            }
            result.append(piece)
        }
        return result
    }
}

private struct NowPlayingScrubberControl<CenterAccessory: View>: View {
    @ObservedObject private var clock = PlaybackClock.shared
    private let haptics = HapticsAndGesturesService.shared

    @Binding var isScrubbing: Bool
    @Binding var scrubTime: Double
    @Binding var lastScrubNotch: Int
    let tintColor: Color
    let fgSecondary: Color
    let labelFont: Font
    let onSeekCommit: (Double) -> Void
    @ViewBuilder let centerAccessory: () -> CenterAccessory

    var body: some View {
        let total = max(1.0, clock.duration)
        let displayed = isScrubbing ? scrubTime : min(clock.currentTime, total)

        VStack(spacing: 6) {
            Slider(
                value: Binding(
                    get: { displayed },
                    set: { newVal in
                        scrubTime = newVal
                        if isScrubbing {
                            haptics.playScrubberNotchIfNeeded(
                                progress: newVal / total,
                                lastNotch: &lastScrubNotch
                            )
                        }
                    }
                ),
                in: 0...total,
                onEditingChanged: { editing in
                    isScrubbing = editing
                    if editing {
                        scrubTime = displayed
                        lastScrubNotch = Int(((displayed / total) * 10.0).rounded(.down))
                        haptics.playControlTap(style: .soft, scale: 0.6)
                    } else {
                        lastScrubNotch = -1
                        haptics.playControlTap(style: .light, scale: 0.75)
                        onSeekCommit(scrubTime)
                    }
                }
            )
            .tint(tintColor)

            HStack {
                Text(TimeFormatting.clockString(seconds: displayed))
                    .font(labelFont)
                    .foregroundColor(fgSecondary)
                    .monospacedDigit()

                Spacer()

                centerAccessory()

                Spacer()

                Text("-\(TimeFormatting.clockString(seconds: max(0, total - displayed)))")
                    .font(labelFont)
                    .foregroundColor(fgSecondary)
                    .monospacedDigit()
            }
        }
    }
}

