import SwiftUI

struct DerbisDeckArtworkStage: View {
    let deck: DerbisPlayerDeck
    let track: Track
    let side: CGFloat
    let activeMotionAsset: MotionCanvasAsset?
    let isPlaying: Bool
    let accent: Color

    @EnvironmentObject private var theme: ThemeManager

    var body: some View {
        switch deck {
        case .flow:
            flowLeafStage
        case .vinyl:
            vinylTurntableStage
        case .sleeve:
            sleeveCardStage
        case .frost:
            frostFrameStage
        case .pixel:
            pixelBlockStage
        case .appleMusic13:
            classicStage
        }
    }

    private var flowLeafStage: some View {
        let leafRBig = min(52, side * 0.17)
        let leafRSmall = min(20, side * 0.065)
        let leaf = AsymmetricLeafShape(
            topLeading: leafRBig,
            bottomLeading: leafRSmall,
            bottomTrailing: leafRBig,
            topTrailing: leafRSmall
        )

        return ZStack {
            leaf
                .fill(
                    LinearGradient(
                        colors: [accent.opacity(0.28), theme.secondaryAccent.opacity(0.20)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: side + 8, height: side + 8)
                .blur(radius: isPlaying ? 12 : 5)

            ZStack {
                AsyncImage(url: track.displayArtworkURL) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    default:
                        ZStack {
                            theme.elevatedSurface
                            DerbisDiamondMark(color: accent, size: side * 0.24)
                        }
                    }
                }
                .frame(width: side, height: side)

                if theme.backdropStyle == .animated,
                   theme.motionCanvasStyle == .framed,
                   let motion = activeMotionAsset {
                    LoopingMotionVideoView(url: motion.playbackURL)
                        .id(motion.playbackURL.absoluteString)
                        .frame(width: side, height: side)
                }
            }
            .clipShape(leaf)
            .overlay(
                leaf.stroke(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.32),
                            accent.opacity(0.45),
                            theme.secondaryAccent.opacity(0.28)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.3
                )
            )
            .shadow(color: Color.black.opacity(0.46), radius: isPlaying ? 22 : 12, x: 0, y: 10)
            .scaleEffect(isPlaying ? 1.0 : 0.94)
            .animation(.spring(response: 0.42, dampingFraction: 0.78), value: isPlaying)
        }
    }

    private var vinylTurntableStage: some View {
        let platter = side
        let labelSize = platter * 0.44

        return TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: !isPlaying)) { timeline in
            let angle = isPlaying ? (timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 9.0) / 9.0) * 360.0 : 0.0

            ZStack(alignment: .topTrailing) {
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    Color(hex: 0x22232A),
                                    Color(hex: 0x0C0D12),
                                    Color(hex: 0x1A1B22),
                                    Color(hex: 0x08080B)
                                ],
                                center: .center,
                                startRadius: labelSize * 0.45,
                                endRadius: platter * 0.5
                            )
                        )
                        .frame(width: platter, height: platter)
                        .overlay(
                            ZStack {
                                ForEach([0.56, 0.66, 0.76, 0.86, 0.94], id: \.self) { ratio in
                                    Circle()
                                        .stroke(Color.white.opacity(0.065), lineWidth: 0.7)
                                        .frame(width: platter * ratio, height: platter * ratio)
                                }
                                Circle()
                                    .stroke(
                                        AngularGradient(
                                            colors: [
                                                Color.white.opacity(0.02),
                                                Color.white.opacity(0.18),
                                                Color.white.opacity(0.02),
                                                accent.opacity(0.16),
                                                Color.white.opacity(0.02)
                                            ],
                                            center: .center
                                        ),
                                        lineWidth: platter * 0.22
                                    )
                                    .frame(width: platter * 0.72, height: platter * 0.72)
                            }
                        )
                        .shadow(color: Color.black.opacity(0.55), radius: 20, x: 0, y: 12)

                    TrackArtworkView(
                        url: track.displayArtworkURL,
                        size: labelSize,
                        cornerRadius: labelSize * 0.5
                    )
                    .overlay(Circle().stroke(accent.opacity(0.6), lineWidth: 2))

                    Circle()
                        .fill(Color(hex: 0x050507))
                        .frame(width: 12, height: 12)
                        .overlay(Circle().stroke(Color.white.opacity(0.45), lineWidth: 1))
                }
                .rotationEffect(.degrees(angle))

                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [Color.white.opacity(0.85), Color(hex: 0x64748B)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 5, height: platter * 0.36)
                    .rotationEffect(.degrees(isPlaying ? 22 : 6), anchor: .top)
                    .offset(x: -10, y: 8)
                    .animation(.spring(response: 0.45, dampingFraction: 0.76), value: isPlaying)
            }
        }
    }

    private var sleeveCardStage: some View {
        let artSide = max(112, side - 26)
        return VStack(spacing: 8) {
            ZStack(alignment: .bottomLeading) {
                TrackArtworkView(
                    url: track.displayArtworkURL,
                    size: artSide,
                    cornerRadius: 16
                )
                HStack(spacing: 5) {
                    DerbisDiamondMark(color: accent, size: 11)
                    Text(track.codecBadge)
                        .font(.system(size: 9.5, weight: .black, design: .monospaced))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Capsule().fill(Color.black.opacity(0.65)))
                .padding(10)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(Color.white.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(Color.white.opacity(0.18), lineWidth: 1)
                )
        )
        .shadow(color: Color.black.opacity(0.42), radius: 18, x: 0, y: 10)
    }

    private var frostFrameStage: some View {
        ZStack {
            TrackArtworkView(
                url: track.displayArtworkURL,
                size: side,
                cornerRadius: 28
            )
            .overlay(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .strokeBorder(
                        LinearGradient(
                            colors: [Color.white.opacity(0.46), Color.white.opacity(0.08), accent.opacity(0.32)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.2
                    )
            )
            .shadow(color: accent.opacity(0.24), radius: 22, x: 0, y: 10)
        }
    }

    private var pixelBlockStage: some View {
        ZStack {
            Rectangle()
                .fill(Color(hex: 0x5B2338, opacity: 0.34))
                .frame(width: side, height: side)
                .offset(x: 6, y: 6)

            TrackArtworkView(
                url: track.displayArtworkURL,
                size: side,
                cornerRadius: 0
            )
            .overlay(
                Rectangle()
                    .stroke(theme.isPixelSkin ? theme.border : Color.white, lineWidth: 2.5)
            )
        }
    }

    private var classicStage: some View {
        TrackArtworkView(
            url: track.displayArtworkURL,
            size: side,
            cornerRadius: 20
        )
        .shadow(color: Color.black.opacity(0.45), radius: isPlaying ? 22 : 11, x: 0, y: isPlaying ? 12 : 6)
        .scaleEffect(isPlaying ? 1.0 : 0.91)
        .animation(.spring(response: 0.42, dampingFraction: 0.78), value: isPlaying)
    }
}

struct DerbisFlowStudioChipBar: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared

    let fgPrimary: Color
    let fgSecondary: Color
    let forceGlass: Bool
    let onOpenStudioFX: () -> Void
    let onOpenSimilar: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            studioChip(
                icon: "infinity",
                label: player.endlessListeningEnabled ? "Endless" : "Radio Off",
                active: player.endlessListeningEnabled
            ) {
                HapticsAndGesturesService.shared.playSelectionTick()
                let next = !player.endlessListeningEnabled
                player.setEndlessListeningEnabled(next)
                theme.endlessRadioEnabled = next
            }

            studioChip(
                icon: "speedometer",
                label: String(format: "%.2gx", fx.playbackSpeed),
                active: abs(fx.playbackSpeed - 1.0) > 0.01
            ) {
                HapticsAndGesturesService.shared.playSelectionTick()
                let presets: [Double] = [1.0, 1.10, 1.25, 0.90]
                if let idx = presets.firstIndex(where: { abs($0 - fx.playbackSpeed) < 0.03 }) {
                    fx.playbackSpeed = presets[(idx + 1) % presets.count]
                } else {
                    fx.playbackSpeed = 1.0
                }
            }

            studioChip(
                icon: fx.singEnabled ? "mic.slash.fill" : "mic.fill",
                label: fx.singEnabled ? "Sing \(Int((fx.singVocalLevel * 100).rounded()))%" : "Vocal Cut",
                active: fx.singEnabled
            ) {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                fx.singEnabled.toggle()
            }

            Menu {
                Section("Sleep Timer") {
                    ForEach(SleepTimerOption.allCases) { opt in
                        Button(opt.label) {
                            player.setSleepTimer(opt)
                        }
                    }
                }
                Section("More") {
                    Button {
                        onOpenSimilar()
                    } label: {
                        Label("Similar Songs (\(player.similarTracks.count))", systemImage: "sparkles")
                    }
                    Button {
                        onOpenStudioFX()
                    } label: {
                        Label("32-bit Mix Lab & EQ", systemImage: "slider.vertical.3")
                    }
                }
            } label: {
                let timerActive = player.sleepTimerRemainingSec != nil
                HStack(spacing: 4) {
                    Image(systemName: timerActive ? "moon.zzz.fill" : "sparkles")
                        .font(.system(size: 10.5, weight: .bold))
                    Text(
                        player.sleepTimerRemainingSec.map { "\(max(1, $0 / 60))m" }
                            ?? "Similar"
                    )
                    .font(theme.font(size: 11, weight: .bold))
                    .lineLimit(1)
                }
                .foregroundColor(timerActive ? theme.onAccentText : fgPrimary.opacity(0.92))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 7.5)
                .derbisGlassCapsule(prominent: timerActive, forceGlassOverBackdrop: forceGlass)
            }
        }
    }

    private func studioChip(
        icon: String,
        label: String,
        active: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 10.5, weight: .bold))
                Text(label)
                    .font(theme.font(size: 11, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundColor(active ? theme.onAccentText : fgPrimary.opacity(0.90))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 7.5)
            .derbisGlassCapsule(prominent: active, forceGlassOverBackdrop: forceGlass)
        }
        .buttonStyle(.plain)
    }
}

struct DerbisDeckScrubberView<CenterAccessory: View>: View {
    @ObservedObject private var clock = PlaybackClock.shared
    private let haptics = HapticsAndGesturesService.shared

    let deck: DerbisPlayerDeck
    let trackId: String
    @Binding var isScrubbing: Bool
    @Binding var scrubTime: Double
    @Binding var lastScrubNotch: Int
    let accentColor: Color
    let railColor: Color
    let fgSecondary: Color
    let labelFont: Font
    let onSeekCommit: (Double) -> Void
    @ViewBuilder let centerAccessory: () -> CenterAccessory

    private func pseudoBarHeight(index: Int, count: Int) -> CGFloat {
        var hash = trackId.utf8.reduce(2166136261 as UInt64) { ($0 ^ UInt64($1)) &* 16777619 }
        hash ^= UInt64(index &* 1315423911)
        let norm = Double(hash % 100) / 100.0
        let envelope = sin(Double(index) / Double(max(1, count - 1)) * .pi)
        return CGFloat(0.28 + 0.72 * (0.45 * norm + 0.55 * envelope))
    }

    var body: some View {
        let total = max(1.0, clock.duration)
        let displayed = isScrubbing ? scrubTime : min(clock.currentTime, total)
        let progress = min(1.0, max(0.0, displayed / total))

        VStack(spacing: 7) {
            if deck == .appleMusic13 {
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
                .tint(accentColor)
            } else {
                GeometryReader { geo in
                    let width = max(1, geo.size.width)
                    ZStack(alignment: .leading) {
                        if deck == .pixel {
                            let segCount = 24
                            HStack(spacing: 3) {
                                ForEach(0..<segCount, id: \.self) { idx in
                                    let lit = Double(idx + 1) / Double(segCount) <= progress + 0.02
                                    Rectangle()
                                        .fill(lit ? accentColor : railColor)
                                        .frame(maxWidth: .infinity, maxHeight: isScrubbing ? 14 : 11)
                                }
                            }
                        } else if deck == .flow || deck == .sleeve {
                            let barCount = 38
                            HStack(alignment: .center, spacing: 3) {
                                ForEach(0..<barCount, id: \.self) { idx in
                                    let ratio = Double(idx) / Double(max(1, barCount - 1))
                                    let lit = ratio <= progress
                                    let h = pseudoBarHeight(index: idx, count: barCount) * (isScrubbing ? 24 : 20)
                                    Capsule()
                                        .fill(lit ? accentColor : railColor)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: max(4, h))
                                }
                            }
                        } else {
                            Capsule()
                                .fill(railColor)
                                .frame(height: isScrubbing ? 10 : 7)
                            Capsule()
                                .fill(accentColor)
                                .frame(width: width * progress, height: isScrubbing ? 10 : 7)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                let pct = min(1.0, max(0.0, Double(value.location.x / width)))
                                if !isScrubbing {
                                    isScrubbing = true
                                    lastScrubNotch = Int((pct * 10.0).rounded(.down))
                                    haptics.playControlTap(style: .soft, scale: 0.6)
                                }
                                scrubTime = pct * total
                                haptics.playScrubberNotchIfNeeded(progress: pct, lastNotch: &lastScrubNotch)
                            }
                            .onEnded { value in
                                let pct = min(1.0, max(0.0, Double(value.location.x / width)))
                                let target = pct * total
                                scrubTime = target
                                isScrubbing = false
                                lastScrubNotch = -1
                                haptics.playControlTap(style: .light, scale: 0.75)
                                onSeekCommit(target)
                            }
                    )
                }
                .frame(height: 26)
                .animation(.easeOut(duration: 0.16), value: isScrubbing)
            }

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

struct SimilarSongsSheet: View {
    let seedTrack: Track

    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 12) {
                            TrackArtworkView(url: seedTrack.displayArtworkURL, size: 54, cornerRadius: theme.artworkRadius)
                            VStack(alignment: .leading, spacing: 3) {
                                Text("SEED TRACK")
                                    .font(theme.font(size: 10, weight: .bold))
                                    .tracking(1.1)
                                    .foregroundColor(theme.accent)
                                Text(seedTrack.title)
                                    .font(theme.font(size: 15, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                    .lineLimit(1)
                                Text(seedTrack.artist)
                                    .font(theme.font(size: 12.5))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Button {
                                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                player.startSongRadio(from: seedTrack)
                                dismiss()
                            } label: {
                                Label("Start Radio", systemImage: "infinity")
                                    .font(theme.font(size: 12.5, weight: .bold))
                                    .foregroundColor(theme.onAccentText)
                                    .padding(.horizontal, 13)
                                    .padding(.vertical, 8)
                                    .derbisGlassCapsule(prominent: true)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(14)
                        .derbisCard(elevated: true)

                        if player.isLoadingSimilarTracks && player.similarTracks.isEmpty {
                            HStack {
                                Spacer()
                                ProgressView().tint(theme.accent).padding(.vertical, 28)
                                Spacer()
                            }
                        } else if player.similarTracks.isEmpty {
                            Text("Play a song with an active connection to surface similar cuts.")
                                .font(theme.font(size: 13))
                                .foregroundColor(theme.secondaryText)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(24)
                                .derbisCard()
                        } else {
                            VStack(spacing: 2) {
                                ForEach(player.similarTracks) { item in
                                    TrackRowView(track: item, contextQueue: [seedTrack] + player.similarTracks)
                                }
                            }
                            .padding(.vertical, 6)
                            .derbisCard()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Similar Songs")
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
}
