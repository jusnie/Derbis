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
    @State private var isScrubbing: Bool = false
    @State private var scrubTime: Double = 0

    private var fgPrimary: Color {
        theme.isPixelSkin ? theme.primaryText : Color.white
    }

    private var fgSecondary: Color {
        theme.isPixelSkin ? theme.secondaryText : Color.white.opacity(0.65)
    }

    var body: some View {
        ZStack {
            backdropLayer

            if let track = player.currentTrack {
                VStack(spacing: 0) {
                    grabberAndTopBar(track: track)
                        .padding(.top, 12)
                        .padding(.horizontal, 24)

                    Spacer(minLength: 12)

                    if showingLyrics {
                        syncedLyricsPanel(track: track)
                            .transition(.opacity)
                    } else {
                        artworkStage(track: track)
                            .transition(.opacity)
                    }

                    Spacer(minLength: 14)

                    if !showingLyrics, let inlineLine = currentInlineLyricText {
                        Button {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                                showingLyrics = true
                            }
                        } label: {
                            Text(inlineLine)
                                .font(theme.font(size: 14, weight: .semibold))
                                .foregroundColor(fgPrimary.opacity(0.92))
                                .lineLimit(1)
                                .padding(.horizontal, 28)
                                .padding(.bottom, 10)
                        }
                        .buttonStyle(.plain)
                    }

                    titleAndLikeRow(track: track)
                        .padding(.horizontal, 28)

                    scrubberSection
                        .padding(.horizontal, 28)
                        .padding(.top, 14)

                    transportControlsRow
                        .padding(.horizontal, 28)
                        .padding(.top, 22)

                    bottomUtilityDock(track: track)
                        .padding(.horizontal, 32)
                        .padding(.top, 24)
                        .padding(.bottom, 22)
                }
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
            }
        }
    }

    @ViewBuilder
    private var backdropLayer: some View {
        if theme.isPixelSkin {
            PixelGraphPaperBackground()
        } else if theme.skin == .lateNight {
            LinearGradient(
                colors: [
                    Color(hex: 0x16120B),
                    Color.black,
                    Color.black
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        } else {
            ZStack {
                Color.black.ignoresSafeArea()
                LinearGradient(
                    colors: [
                        player.ambientPrimary,
                        player.ambientSecondary,
                        Color(hex: 0x060609)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                if let uiImage = player.currentArtworkImage {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .blur(radius: 68)
                        .opacity(0.38)
                        .ignoresSafeArea()
                }

                Color.black.opacity(0.34).ignoresSafeArea()
            }
        }
    }

    private func grabberAndTopBar(track: Track) -> some View {
        VStack(spacing: 10) {
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
                        .frame(width: 34, height: 34)
                }

                Spacer()

                VStack(spacing: 2) {
                    Text(track.source.rawValue.uppercased())
                        .font(theme.font(size: 10, weight: .bold))
                        .tracking(1.1)
                        .foregroundColor(fgSecondary)
                    if let timer = player.sleepTimerRemainingSec {
                        Text("Sleep \(TimeFormatting.clockString(seconds: Double(timer)))")
                            .font(theme.font(size: 11, weight: .semibold))
                            .foregroundColor(theme.accent)
                    }
                }

                Spacer()

                Menu {
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
                        .frame(width: 34, height: 34)
                }
            }
        }
    }

    private func artworkStage(track: Track) -> some View {
        VStack(spacing: 14) {
            GeometryReader { geo in
                let side = min(geo.size.width - 56, geo.size.height)
                HStack {
                    Spacer()
                    ZStack {
                        if theme.isPixelSkin {
                            Rectangle()
                                .fill(Color(hex: 0x5B2338, opacity: 0.28))
                                .frame(width: side, height: side)
                                .offset(x: 5, y: 5)
                        }
                        TrackArtworkView(
                            url: track.displayArtworkURL,
                            size: max(180, side),
                            cornerRadius: theme.playerArtworkRadius
                        )
                        .shadow(
                            color: theme.isPixelSkin ? .clear : Color.black.opacity(0.45),
                            radius: player.isPlaying ? 26 : 12,
                            x: 0,
                            y: player.isPlaying ? 16 : 8
                        )
                        .scaleEffect(theme.isPixelSkin ? 1.0 : (player.isPlaying ? 1.0 : 0.88))
                        .animation(.spring(response: 0.42, dampingFraction: 0.78), value: player.isPlaying)
                    }
                    Spacer()
                }
            }
            .frame(maxHeight: 330)

            if let error = player.errorBanner {
                Text(error)
                    .font(theme.font(size: 12, weight: .medium))
                    .foregroundColor(theme.accent)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)
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
            }
        }
    }

    private func syncedLyricsPanel(track: Track) -> some View {
        VStack(spacing: 10) {
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
                        VStack(alignment: .leading, spacing: 18) {
                            ForEach(payload.lines) { line in
                                let isActive = player.activeLyricIndex == line.id
                                Button {
                                    if payload.synced {
                                        player.seek(to: line.startTimeSec)
                                    }
                                } label: {
                                    Text(line.text)
                                        .font(theme.font(size: isActive ? 24 : 19, weight: .bold))
                                        .foregroundColor(isActive ? fgPrimary : fgSecondary.opacity(0.45))
                                        .multilineTextAlignment(.leading)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .scaleEffect(isActive ? 1.0 : 0.97, anchor: .leading)
                                        .animation(.easeOut(duration: 0.22), value: isActive)
                                }
                                .buttonStyle(.plain)
                                .id(line.id)
                            }
                        }
                        .padding(.horizontal, 28)
                        .padding(.vertical, 30)
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
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundColor(fgSecondary)
                    Text("No synced lyrics found")
                        .font(theme.font(size: 16, weight: .bold))
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
        .frame(maxHeight: 350)
    }

    private func titleAndLikeRow(track: Track) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(track.title)
                    .font(theme.font(size: 21, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .lineLimit(1)

                Text(track.artist)
                    .font(theme.font(size: 16, weight: .medium))
                    .foregroundColor(fgSecondary)
                    .lineLimit(1)
            }

            Spacer()

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
            .tint(theme.isPixelSkin ? theme.accent : fgPrimary)

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
                if theme.isPixelSkin {
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
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 52, height: 52)
            }

            Spacer()

            Button {
                player.togglePlayPause()
            } label: {
                ZStack {
                    if theme.isPixelSkin {
                        Rectangle()
                            .fill(theme.accent)
                            .frame(width: 68, height: 68)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    } else {
                        Circle()
                            .fill(fgPrimary)
                            .frame(width: 68, height: 68)
                    }

                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(theme.isPixelSkin ? .white : .black)
                        .offset(x: player.isPlaying ? 0 : 2)
                }
            }

            Spacer()

            Button {
                player.skipNext()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(fgPrimary)
                    .frame(width: 52, height: 52)
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
        HStack {
            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                    showingLyrics.toggle()
                }
                if showingLyrics && player.currentLyrics == nil && !player.isLoadingLyrics {
                    player.loadLyrics(for: track)
                }
            } label: {
                Image(systemName: showingLyrics ? "quote.bubble.fill" : "quote.bubble")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(showingLyrics ? theme.accent : fgSecondary)
                    .frame(width: 44, height: 36)
            }

            Spacer()

            AirPlayRoutePicker(tintColor: theme.isPixelSkin ? UIColor(red: 0.36, green: 0.14, blue: 0.22, alpha: 1.0) : UIColor.white)
                .frame(width: 44, height: 36)

            Spacer()

            if track.source != .localFile {
                Button {
                    if !library.isDownloaded(track) {
                        Task {
                            await library.downloadTrackForOffline(track, preferHighQuality: theme.highQualityAudio)
                        }
                    }
                } label: {
                    Image(systemName: library.isDownloaded(track) ? "checkmark.circle.fill" : "arrow.down.circle")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundColor(library.isDownloaded(track) ? theme.accent : fgSecondary)
                        .frame(width: 44, height: 36)
                }

                Spacer()
            }

            Button {
                showingQueue = true
            } label: {
                Image(systemName: "list.bullet")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(fgSecondary)
                    .frame(width: 44, height: 36)
            }
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
