import SwiftUI
import UIKit

struct NowPlayingView: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    @State private var showingLyrics: Bool = false
    @State private var showingQueue: Bool = false
    @State private var showingPlaylistSheet: Bool = false
    @State private var showingSpotifyCanvasSheet: Bool = false
    @State private var isScrubbing: Bool = false
    @State private var scrubTime: Double = 0

    private var activeMotionAsset: MotionCanvasAsset? {
        guard theme.motionCanvasEnabled,
              let asset = player.currentMotionCanvas,
              asset.trackId == player.currentTrack?.id else {
            return nil
        }
        return asset
    }

    private var isImmersiveCanvas: Bool {
        activeMotionAsset != nil && theme.motionCanvasStyle == .immersive
    }

    private var fgPrimary: Color {
        if isImmersiveCanvas { return Color.white }
        return theme.isPixelSkin ? theme.primaryText : Color.white
    }

    private var fgSecondary: Color {
        if isImmersiveCanvas { return Color.white.opacity(0.72) }
        return theme.isPixelSkin ? theme.secondaryText : Color.white.opacity(0.65)
    }

    var body: some View {
        GeometryReader { screenGeo in
            let viewportWidth = max(300, screenGeo.size.width)
            let viewportHeight = max(520, screenGeo.size.height)
            let compactHeight = viewportHeight < 700
            let stageMaxHeight = max(210, min(330, viewportHeight * (showingLyrics ? 0.44 : 0.37)))

            ZStack {
                if let track = player.currentTrack {
                    VStack(spacing: 0) {
                        grabberAndTopBar(track: track)
                            .padding(.top, compactHeight ? 8 : 12)
                            .padding(.horizontal, 20)

                        Spacer(minLength: compactHeight ? 8 : 12)

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

                        Spacer(minLength: compactHeight ? 8 : 12)

                        if !showingLyrics, let inlineLine = currentInlineLyricText {
                            Button {
                                withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                    showingLyrics = true
                                }
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
                                            (theme.isPixelSkin && !isImmersiveCanvas)
                                                ? theme.elevatedSurface
                                                : Color.white.opacity(0.11)
                                        )
                                )
                                .padding(.horizontal, 24)
                                .padding(.bottom, 8)
                            }
                            .buttonStyle(.plain)
                        }

                        titleAndLikeRow(track: track)
                            .padding(.horizontal, 24)

                        scrubberSection
                            .padding(.horizontal, 24)
                            .padding(.top, compactHeight ? 10 : 14)

                        transportControlsRow
                            .padding(.horizontal, 24)
                            .padding(.top, compactHeight ? 14 : 20)

                        bottomUtilityDock(track: track)
                            .padding(.horizontal, 20)
                            .padding(.top, compactHeight ? 14 : 20)
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
                    .onChange(of: theme.motionCanvasEnabled) { _ in
                        player.refreshMotionCanvas()
                    }
                    .onChange(of: theme.motionCanvasStyle) { _ in
                        player.refreshMotionCanvas()
                    }
                }
            }
            .frame(width: viewportWidth, height: viewportHeight)
            .background {
                backdropLayer(width: viewportWidth, height: viewportHeight)
                    .ignoresSafeArea()
            }
        }
    }

    @ViewBuilder
    private func backdropLayer(width: CGFloat, height: CGFloat) -> some View {
        let bleedWidth = width + 40
        let bleedHeight = height + 160

        ZStack {
            if theme.isPixelSkin && !isImmersiveCanvas {
                PixelGraphPaperBackground()
                    .frame(width: bleedWidth, height: bleedHeight)
                    .clipped()
            } else if theme.skin == .lateNight && !isImmersiveCanvas {
                LinearGradient(
                    colors: [
                        Color(hex: 0x16120B),
                        Color.black,
                        Color.black
                    ],
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

                    if let motion = activeMotionAsset {
                        Button {
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
                    }
                }

                Spacer()

                Menu {
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

                    Menu("Playback speed") {
                        ForEach([Float(0.75), Float(1.0), Float(1.15), Float(1.25), Float(1.5)], id: \.self) { rate in
                            Button {
                                player.setRate(rate)
                            } label: {
                                HStack {
                                    Text(String(format: "%.2gx", rate))
                                    if abs(player.playbackRate - rate) < 0.01 {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
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
            if isImmersiveCanvas {
                Spacer(minLength: 140)
                    .frame(maxHeight: maxStageHeight)
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

                        if theme.motionCanvasStyle == .framed, let motion = activeMotionAsset {
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
                    .animation(.spring(response: 0.42, dampingFraction: 0.78), value: player.isPlaying)
                }
                .frame(width: viewportWidth, height: side)
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
            HStack(spacing: 10) {
                TrackArtworkView(
                    url: track.displayArtworkURL,
                    size: 36,
                    cornerRadius: theme.isPixelSkin ? 0 : 8
                )

                VStack(alignment: .leading, spacing: 1) {
                    Text("Synced lyrics")
                        .font(theme.font(size: 12, weight: .bold))
                        .foregroundColor(fgPrimary)
                    Text(player.currentLyrics?.synced == true ? "Tap any line to jump" : "Plain text")
                        .font(theme.font(size: 11))
                        .foregroundColor(fgSecondary)
                }

                Spacer()

                Button {
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
                                    (theme.isPixelSkin && !isImmersiveCanvas)
                                        ? theme.elevatedSurface
                                        : Color.white.opacity(0.14)
                                )
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)

            if player.isLoadingLyrics {
                Spacer()
                ProgressView()
                    .tint(fgPrimary)
                Text("Loading synced lyrics")
                    .font(theme.font(size: 13))
                    .foregroundColor(fgSecondary)
                Spacer()
            } else if let payload = player.currentLyrics, !payload.lines.isEmpty {
                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 16) {
                            ForEach(payload.lines) { line in
                                let isActive = player.activeLyricIndex == line.id
                                Button {
                                    if payload.synced {
                                        player.seek(to: line.startTimeSec)
                                    }
                                } label: {
                                    Text(line.text)
                                        .font(theme.font(size: isActive ? 22 : 18, weight: .bold))
                                        .foregroundColor(isActive ? fgPrimary : fgSecondary.opacity(0.46))
                                        .multilineTextAlignment(.leading)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .scaleEffect(isActive ? 1.0 : 0.97, anchor: .leading)
                                        .animation(.easeOut(duration: 0.22), value: isActive)
                                }
                                .buttonStyle(.plain)
                                .id(line.id)
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 20)
                    }
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
                        player.loadLyrics(for: track)
                    }
                    .font(theme.font(size: 13, weight: .bold))
                    .foregroundColor(theme.accent)
                }
                Spacer()
            }
        }
        .frame(maxHeight: maxHeight)
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
                    }
                ),
                in: 0...total,
                onEditingChanged: { editing in
                    isScrubbing = editing
                    if !editing {
                        player.seek(to: scrubTime)
                    }
                }
            )
            .tint((theme.isPixelSkin && !isImmersiveCanvas) ? theme.accent : fgPrimary)

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
        HStack(spacing: 5) {
            if player.isLoading {
                ProgressView()
                    .scaleEffect(0.65)
                    .tint(fgPrimary)
            } else {
                Image(systemName: "waveform")
                    .font(.system(size: 9, weight: .bold))
            }
            Text(player.activeCodecBadge)
                .font(theme.font(size: 10, weight: .bold))
                .lineLimit(1)
        }
        .foregroundColor(fgPrimary.opacity(0.9))
        .padding(.horizontal, 9)
        .padding(.vertical, 3.5)
        .background(
            Group {
                if theme.isPixelSkin && !isImmersiveCanvas {
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

    private var transportControlsRow: some View {
        HStack {
            Button {
                player.isShuffleEnabled.toggle()
            } label: {
                Image(systemName: "shuffle")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(player.isShuffleEnabled ? theme.accent : fgSecondary)
                    .frame(width: 40, height: 40)
            }

            Spacer()

            Button {
                player.skipPrevious()
            } label: {
                Image(systemName: "backward.fill")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 48)
            }

            Spacer()

            Button {
                player.togglePlayPause()
            } label: {
                ZStack {
                    if theme.isPixelSkin && !isImmersiveCanvas {
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
                        .foregroundColor((theme.isPixelSkin && !isImmersiveCanvas) ? .white : .black)
                        .offset(x: player.isPlaying ? 0 : 2)
                }
            }

            Spacer()

            Button {
                player.skipNext()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 48, height: 48)
            }

            Spacer()

            Button {
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
        let airplayTint: UIColor = (theme.isPixelSkin && !isImmersiveCanvas)
            ? UIColor(red: 0.36, green: 0.14, blue: 0.22, alpha: 1.0)
            : UIColor.white

        return HStack(spacing: 0) {
            Button {
                withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                    showingLyrics.toggle()
                }
                if showingLyrics && player.currentLyrics == nil && !player.isLoadingLyrics {
                    player.loadLyrics(for: track)
                }
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: showingLyrics ? "quote.bubble.fill" : "quote.bubble")
                        .font(.system(size: 17, weight: .semibold))
                    Text("Lyrics")
                        .font(theme.font(size: 10, weight: .semibold))
                }
                .foregroundColor(showingLyrics ? theme.accent : fgSecondary)
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
                                .font(.system(size: 17, weight: .semibold))
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
                showingQueue = true
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 17, weight: .semibold))
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
