import SwiftUI
import UIKit

struct NowPlayingView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @ObservedObject private var haptics = HapticsAndGesturesService.shared
    @Environment(\.dismiss) private var dismiss

    @State private var showingLyrics: Bool = false
    @State private var showingQueue: Bool = false
    @State private var showingPlaylistSheet: Bool = false
    @State private var showingSpotifyCanvasSheet: Bool = false
    @State private var showingAudioStudioSheet: Bool = false

    @State private var isScrubbing: Bool = false
    @State private var scrubTime: Double = 0
    @State private var lastScrubNotch: Int = -1

    @State private var controlsCollapsedInLyrics: Bool = false
    @State private var autoHideTask: Task<Void, Never>?

    @State private var gestureFeedbackBadge: (icon: String, label: String)?
    @State private var badgeClearTask: Task<Void, Never>?
    @State private var artworkDragOffset: CGFloat = 0

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
        theme.backdropStyle == .fill && !(theme.isPixelSkin)
    }

    private var fgPrimary: Color {
        if isImmersiveCanvas || isFillBackdrop { return Color.white }
        return theme.isPixelSkin ? theme.primaryText : Color.white
    }

    private var fgSecondary: Color {
        if isImmersiveCanvas || isFillBackdrop { return Color.white.opacity(0.74) }
        return theme.isPixelSkin ? theme.secondaryText : Color.white.opacity(0.65)
    }

    var body: some View {
        GeometryReader { screenGeo in
            let viewportWidth = max(300, screenGeo.size.width)
            let viewportHeight = max(520, screenGeo.size.height)
            let compactHeight = viewportHeight < 700
            let expandedLyricsStage = showingLyrics && controlsCollapsedInLyrics
            let stageMaxHeight: CGFloat = {
                if expandedLyricsStage {
                    return max(320, viewportHeight * 0.68)
                }
                if showingLyrics {
                    return max(230, min(360, viewportHeight * 0.46))
                }
                return max(205, min(330, viewportHeight * 0.37))
            }()

            ZStack(alignment: .top) {
                if let track = player.currentTrack {
                    VStack(spacing: 0) {
                        grabberAndTopBar(track: track)
                            .padding(.top, compactHeight ? 8 : 12)
                            .padding(.horizontal, 20)

                        Spacer(minLength: compactHeight ? 6 : 10)

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

                        Spacer(minLength: compactHeight ? 6 : 10)

                        if !showingLyrics, let inlineLine = currentInlineLyricText {
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
                                        .font(theme.font(size: 13, weight: .semibold))
                                        .foregroundColor(fgPrimary.opacity(0.94))
                                        .lineLimit(1)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(
                                    Capsule()
                                        .fill(
                                            (theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop)
                                                ? theme.elevatedSurface
                                                : Color.white.opacity(0.12)
                                        )
                                )
                                .padding(.horizontal, 24)
                                .padding(.bottom, 8)
                            }
                            .buttonStyle(.plain)
                        }

                        titleAndLikeRow(track: track)
                            .padding(.horizontal, 24)

                        if !expandedLyricsStage {
                            scrubberSection
                                .padding(.horizontal, 24)
                                .padding(.top, compactHeight ? 10 : 14)
                                .transition(.opacity.combined(with: .move(edge: .bottom)))

                            transportControlsRow
                                .padding(.horizontal, 24)
                                .padding(.top, compactHeight ? 14 : 20)
                                .transition(.opacity.combined(with: .move(edge: .bottom)))
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
                                .foregroundColor(fgPrimary.opacity(0.9))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Capsule().fill(Color.white.opacity(0.14)))
                                .padding(.top, 10)
                            }
                            .buttonStyle(.plain)
                        }

                        bottomUtilityDock(track: track)
                            .padding(.horizontal, 16)
                            .padding(.top, compactHeight ? 12 : 18)
                            .padding(.bottom, compactHeight ? 12 : 18)
                    }
                    .frame(width: viewportWidth, height: viewportHeight)
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
                        .padding(.top, 56)
                        .transition(.move(edge: .top).combined(with: .opacity))
                } else if let feedback = gestureFeedbackBadge {
                    floatingStatusPill(icon: feedback.icon, text: feedback.label)
                        .padding(.top, 56)
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
        .background(
            Capsule()
                .fill(Color.black.opacity(0.76))
                .overlay(Capsule().stroke(Color.white.opacity(0.14), lineWidth: 0.7))
        )
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
            if theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop {
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
        VStack(spacing: 8) {
            Capsule()
                .fill(fgSecondary.opacity(0.45))
                .frame(width: 38, height: 5)
                .onTapGesture { dismiss() }

            HStack {
                Button {
                    haptics.playControlTap(style: .light)
                    dismiss()
                } label: {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(fgPrimary)
                        .frame(width: 36, height: 36)
                }

                Spacer()

                VStack(spacing: 3) {
                    Text(track.source.rawValue.uppercased())
                        .font(theme.font(size: 10, weight: .bold))
                        .tracking(1.1)
                        .foregroundColor(fgSecondary)

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
                            .font(theme.font(size: 11, weight: .semibold))
                            .foregroundColor(theme.accent)
                    } else if fx.hasAnyActiveModification {
                        Button {
                            showingAudioStudioSheet = true
                        } label: {
                            Text(abs(fx.playbackSpeed - 1.0) > 0.01 ? String(format: "FX • %.2gx", fx.playbackSpeed) : "AUDIO FX ACTIVE")
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

                Menu {
                    Button {
                        showingAudioStudioSheet = true
                    } label: {
                        Label("Audio Studio (EQ, Speed & FX)…", systemImage: "slider.vertical.3")
                    }

                    Button {
                        withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                            showingLyrics.toggle()
                        }
                        if showingLyrics && player.currentLyrics == nil && !player.isLoadingLyrics {
                            player.loadLyrics(for: track)
                        }
                    } label: {
                        Label(showingLyrics ? "Show artwork" : "Synced lyrics", systemImage: "quote.bubble")
                    }

                    Menu("Player Backdrop") {
                        ForEach(PlayerBackdropStyle.allCases) { style in
                            Button {
                                theme.backdropStyle = style
                            } label: {
                                HStack {
                                    Text("\(style.rawValue) — \(style.subtitle)")
                                    if theme.backdropStyle == style {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    }

                    Menu("Lyrics & Lock Screen") {
                        Button {
                            theme.showLyricsRomanization.toggle()
                        } label: {
                            Label(
                                "Pronunciation (Romanization)",
                                systemImage: theme.showLyricsRomanization ? "checkmark.circle.fill" : "circle"
                            )
                        }

                        Button {
                            theme.showLyricsTranslation.toggle()
                        } label: {
                            Label(
                                "Translated Lyrics",
                                systemImage: theme.showLyricsTranslation ? "checkmark.circle.fill" : "circle"
                            )
                        }

                        Button {
                            theme.autoHideLyricsControls.toggle()
                        } label: {
                            Label(
                                "Auto-Hide Controls in Lyrics",
                                systemImage: theme.autoHideLyricsControls ? "checkmark.circle.fill" : "circle"
                            )
                        }

                        Divider()

                        Button {
                            theme.lockScreenLiveLyrics.toggle()
                            player.refreshNowPlayingLockScreenPreferences()
                        } label: {
                            Label(
                                "Lock Screen Live Lyrics",
                                systemImage: theme.lockScreenLiveLyrics ? "checkmark.circle.fill" : "circle"
                            )
                        }

                        Button {
                            theme.lockScreenLyricArt.toggle()
                            player.refreshNowPlayingLockScreenPreferences()
                        } label: {
                            Label(
                                "Lock Screen Lyric Card Art",
                                systemImage: theme.lockScreenLyricArt ? "checkmark.circle.fill" : "circle"
                            )
                        }
                    }

                    Button {
                        showingPlaylistSheet = true
                    } label: {
                        Label("Add to playlist", systemImage: "music.note.list")
                    }

                    if track.source != .localFile {
                        if library.isDownloaded(track) {
                            Button(role: .destructive) {
                                library.removeOfflineDownload(track)
                            } label: {
                                Label("Remove offline copy", systemImage: "trash")
                            }
                        } else {
                            Button {
                                Task {
                                    await library.downloadTrackForOffline(track, preferHighQuality: theme.highQualityAudio)
                                }
                            } label: {
                                Label("Download offline", systemImage: "arrow.down.circle")
                            }
                        }
                    }

                    Menu("Motion Canvas") {
                        Button {
                            theme.motionCanvasEnabled.toggle()
                        } label: {
                            Label(
                                theme.motionCanvasEnabled ? "Disable Motion Canvas" : "Enable Motion Canvas",
                                systemImage: theme.motionCanvasEnabled ? "sparkles.rectangle.stack.fill" : "sparkles.rectangle.stack"
                            )
                        }

                        ForEach(MotionCanvasStyle.allCases) { styleOption in
                            Button {
                                theme.motionCanvasStyle = styleOption
                            } label: {
                                HStack {
                                    Text(styleOption.title)
                                    if theme.motionCanvasStyle == styleOption {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }

                        Button {
                            showingSpotifyCanvasSheet = true
                        } label: {
                            Label("Spotify Canvas account…", systemImage: "person.badge.key")
                        }
                    }

                    Menu("Sleep timer") {
                        ForEach(SleepTimerOption.allCases) { opt in
                            Button {
                                player.setSleepTimer(opt)
                            } label: {
                                Text(opt.label)
                            }
                        }
                    }

                    Divider()

                    Button(role: library.isArtistBlocked(track.artist) ? .none : .destructive) {
                        let willBlock = !library.isArtistBlocked(track.artist)
                        library.toggleBlockedArtist(track.artist)
                        haptics.playControlTap(style: .medium)
                        if willBlock && player.queue.count > 1 {
                            player.skipNext()
                        }
                    } label: {
                        Label(
                            library.isArtistBlocked(track.artist) ? "Unblock \(track.artist)" : "Don't Play \(track.artist)",
                            systemImage: library.isArtistBlocked(track.artist) ? "person.crop.circle.badge.checkmark" : "hand.raised.slash"
                        )
                    }
                } label: {
                    Image(systemName: "ellipsis.circle.fill")
                        .font(.system(size: 22))
                        .foregroundColor(fgPrimary.opacity(0.85))
                        .frame(width: 36, height: 36)
                }
            }
        }
    }

    private func artworkStage(track: Track, viewportWidth: CGFloat, maxStageHeight: CGFloat) -> some View {
        let side = max(176, min(viewportWidth - 56, maxStageHeight - 8))

        return VStack(spacing: 12) {
            ZStack {
                if isImmersiveCanvas || isFillBackdrop {
                    Color.clear
                        .frame(width: viewportWidth, height: maxStageHeight)
                } else {
                    ZStack {
                        if theme.isPixelSkin {
                            Rectangle()
                                .fill(Color(hex: 0x5B2338, opacity: 0.28))
                                .frame(width: side, height: side)
                                .offset(x: 5, y: 5)
                        }

                        ZStack {
                            TrackArtworkView(
                                url: track.displayArtworkURL,
                                size: side,
                                cornerRadius: theme.playerArtworkRadius
                            )

                            if theme.backdropStyle == .animated,
                               theme.motionCanvasStyle == .framed,
                               let motion = activeMotionAsset {
                                LoopingMotionVideoView(
                                    url: motion.playbackURL,
                                    isPlaying: player.isPlaying
                                )
                                .id(motion.playbackURL.absoluteString)
                                .frame(width: side, height: side)
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius: theme.playerArtworkRadius,
                                        style: .continuous
                                    )
                                )
                                .overlay(
                                    RoundedRectangle(
                                        cornerRadius: theme.playerArtworkRadius,
                                        style: .continuous
                                    )
                                    .stroke(theme.isPixelSkin ? theme.border : Color.white.opacity(0.08), lineWidth: theme.isPixelSkin ? 2 : 0.6)
                                )
                            }
                        }
                        .shadow(
                            color: theme.isPixelSkin ? .clear : Color.black.opacity(0.45),
                            radius: player.isPlaying ? 24 : 12,
                            x: 0,
                            y: player.isPlaying ? 14 : 7
                        )
                        .scaleEffect(theme.isPixelSkin ? 1.0 : (player.isPlaying ? 1.0 : 0.89))
                        .offset(x: artworkDragOffset)
                        .animation(.spring(response: 0.42, dampingFraction: 0.78), value: player.isPlaying)
                    }
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
                    .frame(width: viewportWidth, height: (isImmersiveCanvas || isFillBackdrop) ? maxStageHeight : side)
                    .simultaneousGesture(
                        DragGesture(minimumDistance: 24)
                            .onChanged { value in
                                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                                artworkDragOffset = value.translation.width * 0.32
                            }
                            .onEnded { value in
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                    artworkDragOffset = 0
                                }
                                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                                if value.translation.width < -56 {
                                    haptics.playControlTap(style: .medium)
                                    player.skipNext()
                                } else if value.translation.width > 56 {
                                    haptics.playControlTap(style: .medium)
                                    player.skipPrevious()
                                }
                            }
                    )
                }
            }

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
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(
                            Capsule()
                                .fill(
                                    (theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop)
                                        ? theme.elevatedSurface
                                        : Color.white.opacity(0.14)
                                )
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
                                    ? (payload.lines[idx - 1].endTimeSec ?? (payload.lines[idx - 1].startTimeSec + 3.0))
                                    : 0.0
                                let gapDuration = line.startTimeSec - prevEnd
                                let inInstrumentalGap = payload.synced
                                    && gapDuration >= 7.0
                                    && player.currentTime >= prevEnd
                                    && player.currentTime < line.startTimeSec

                                VStack(spacing: 12) {
                                    if inInstrumentalGap {
                                        InstrumentalBreathingDots(
                                            progress: min(1.0, max(0.0, (player.currentTime - prevEnd) / max(0.1, gapDuration))),
                                            accent: theme.accent
                                        )
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    }

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
                Text(buildKaraokeAttributedText(for: line))
                    .font(theme.font(size: 23, weight: .bold))
                    .multilineTextAlignment(textAlign)
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

    private func buildKaraokeAttributedText(for line: LyricLine) -> AttributedString {
        var result = AttributedString()
        let now = player.currentTime + 0.08

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

    private func titleAndLikeRow(track: Track) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(track.title)
                    .font(theme.font(size: 20, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .lineLimit(1)

                Text(track.artist)
                    .font(theme.font(size: 15, weight: .medium))
                    .foregroundColor(fgSecondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                haptics.playControlTap(style: .medium)
                library.toggleLike(track)
            } label: {
                Image(systemName: library.isLiked(track) ? "heart.fill" : "heart")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(library.isLiked(track) ? theme.accent : fgSecondary)
                    .frame(width: 40, height: 40)
            }
        }
    }

    private var scrubberSection: some View {
        let total = max(1.0, player.duration)
        let displayed = isScrubbing ? scrubTime : min(player.currentTime, total)

        return VStack(spacing: 6) {
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
                        lastScrubNotch = Int(((displayed / total) * 10.0).rounded(.down))
                        haptics.playControlTap(style: .soft, scale: 0.6)
                    } else {
                        lastScrubNotch = -1
                        haptics.playControlTap(style: .light, scale: 0.75)
                        player.seek(to: scrubTime)
                        scheduleLyricsControlsAutoHide()
                    }
                }
            )
            .tint((theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop) ? theme.accent : fgPrimary)

            HStack {
                Text(TimeFormatting.clockString(seconds: displayed))
                    .font(theme.font(size: 11, weight: .semibold))
                    .foregroundColor(fgSecondary)
                    .monospacedDigit()

                Spacer()

                codecBadgePill

                Spacer()

                Text("-\(TimeFormatting.clockString(seconds: max(0, total - displayed)))")
                    .font(theme.font(size: 11, weight: .semibold))
                    .foregroundColor(fgSecondary)
                    .monospacedDigit()
            }
        }
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
            .foregroundColor(fx.hasAnyActiveModification ? theme.accent : fgPrimary.opacity(0.9))
            .padding(.horizontal, 9)
            .padding(.vertical, 3.5)
            .background(
                Group {
                    if theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop {
                        Rectangle()
                            .fill(theme.elevatedSurface)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 1.5))
                    } else {
                        Capsule()
                            .fill(Color.white.opacity(0.14))
                    }
                }
            )
        }
        .buttonStyle(.plain)
    }

    private var transportControlsRow: some View {
        HStack {
            Button {
                haptics.playSelectionTick()
                player.isShuffleEnabled.toggle()
            } label: {
                Image(systemName: "shuffle")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(player.isShuffleEnabled ? theme.accent : fgSecondary)
                    .frame(width: 40, height: 40)
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipPrevious()
            } label: {
                Image(systemName: "backward.fill")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 48)
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium, scale: 1.0)
                player.togglePlayPause()
            } label: {
                ZStack {
                    if theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop {
                        Rectangle()
                            .fill(theme.accent)
                            .frame(width: 64, height: 64)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    } else {
                        Circle()
                            .fill(fgPrimary)
                            .frame(width: 64, height: 64)
                    }

                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor((theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop) ? .white : .black)
                        .offset(x: player.isPlaying ? 0 : 2)
                }
            }

            Spacer()

            Button {
                haptics.playControlTap(style: .medium)
                player.skipNext()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 48)
            }

            Spacer()

            Button {
                haptics.playSelectionTick()
                player.repeatMode = player.repeatMode.next()
            } label: {
                Image(systemName: player.repeatMode.systemImage)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(player.repeatMode == .off ? fgSecondary : theme.accent)
                    .frame(width: 40, height: 40)
            }
        }
    }

    private func bottomUtilityDock(track: Track) -> some View {
        let airplayTint: UIColor = (theme.isPixelSkin && !isImmersiveCanvas && !isFillBackdrop)
            ? UIColor(red: 0.36, green: 0.14, blue: 0.22, alpha: 1.0)
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
                VStack(spacing: 4) {
                    Image(systemName: showingLyrics ? "quote.bubble.fill" : "quote.bubble")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Lyrics")
                        .font(theme.font(size: 10, weight: .semibold))
                }
                .foregroundColor(showingLyrics ? theme.accent : fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
            }
            .buttonStyle(.plain)

            Button {
                haptics.playControlTap(style: .light)
                showingAudioStudioSheet = true
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: "slider.vertical.3")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Audio FX")
                        .font(theme.font(size: 10, weight: .semibold))
                }
                .foregroundColor(fx.hasAnyActiveModification ? theme.accent : fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
            }
            .buttonStyle(.plain)

            VStack(spacing: 4) {
                AirPlayRoutePicker(tintColor: airplayTint)
                    .frame(width: 24, height: 20)
                Text("AirPlay")
                    .font(theme.font(size: 10, weight: .semibold))
                    .foregroundColor(fgSecondary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 40)

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
                    VStack(spacing: 4) {
                        if downloading {
                            ProgressView()
                                .scaleEffect(0.7)
                                .tint(theme.accent)
                                .frame(height: 18)
                        } else {
                            Image(systemName: downloaded ? "checkmark.circle.fill" : "arrow.down.circle")
                                .font(.system(size: 16, weight: .semibold))
                        }
                        Text(downloaded ? "Saved" : "Offline")
                            .font(theme.font(size: 10, weight: .semibold))
                    }
                    .foregroundColor(downloaded ? theme.accent : fgSecondary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                }
                .buttonStyle(.plain)
            }

            Button {
                haptics.playControlTap(style: .light)
                showingQueue = true
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Queue")
                        .font(theme.font(size: 10, weight: .semibold))
                }
                .foregroundColor(fgSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
            }
            .buttonStyle(.plain)
        }
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

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                List {
                    ForEach(Array(player.queue.enumerated()), id: \.element.id) { idx, item in
                        Button {
                            player.play(track: item, in: player.queue)
                            dismiss()
                        } label: {
                            HStack(spacing: 12) {
                                TrackArtworkView(url: item.displayArtworkURL, size: 42)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.title)
                                        .font(theme.font(size: 14, weight: idx == player.currentIndex ? .bold : .medium))
                                        .foregroundColor(idx == player.currentIndex ? theme.accent : theme.primaryText)
                                        .lineLimit(1)
                                    Text(item.artist)
                                        .font(theme.font(size: 12))
                                        .foregroundColor(theme.secondaryText)
                                        .lineLimit(1)
                                }
                                Spacer()
                                if idx == player.currentIndex {
                                    Image(systemName: "waveform")
                                        .foregroundColor(theme.accent)
                                }
                            }
                        }
                        .listRowBackground(theme.surface)
                    }
                    .onDelete { offsets in
                        player.removeFromQueue(at: offsets)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Queue (\(player.queue.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
