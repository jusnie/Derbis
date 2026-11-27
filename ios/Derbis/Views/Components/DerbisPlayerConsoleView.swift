import SwiftUI
import UIKit

struct DerbisAudioVisualiserBackdropView: View {
    let primary: Color
    let secondary: Color
    let accent: Color
    let isPlaying: Bool

    var body: some View {
        ZStack {
            Color(hex: 0x030303)

            TimelineView(.animation(minimumInterval: 1.0 / 60.0, paused: !isPlaying)) { timeline in
                let t = timeline.date.timeIntervalSinceReferenceDate
                let levels = AudioEffectsEngine.readVisualizerLevels()
                let fallbackPulse = isPlaying ? Float(0.32 + 0.22 * abs(sin(t * 2.3))) : 0.08
                let low = max(levels.low, fallbackPulse)
                let mid = max(levels.mid, fallbackPulse * 0.85)
                let high = max(levels.high, fallbackPulse * 0.7)

                Canvas { context, size in
                    let center = CGPoint(x: size.width * 0.5, y: size.height * 0.44)
                    let auraRadius = max(size.width, size.height) * CGFloat(0.42 + Double(low) * 0.24)

                    context.fill(
                        Path(ellipseIn: CGRect(
                            x: center.x - auraRadius,
                            y: center.y - auraRadius,
                            width: auraRadius * 2,
                            height: auraRadius * 2
                        )),
                        with: .radialGradient(
                            Gradient(colors: [
                                accent.opacity(0.38 + Double(low) * 0.22),
                                primary.opacity(0.34),
                                secondary.opacity(0.14),
                                .clear
                            ]),
                            center: center,
                            startRadius: 8,
                            endRadius: auraRadius
                        )
                    )

                    let waveLayers: [(amp: Double, freq: Double, speed: Double, yFrac: CGFloat, color: Color)] = [
                        (Double(24 + low * 68), 1.6, 1.9, 0.52, primary.opacity(0.52)),
                        (Double(20 + mid * 56), 2.3, -2.4, 0.58, accent.opacity(0.46)),
                        (Double(16 + high * 44), 3.1, 3.1, 0.64, secondary.opacity(0.48))
                    ]

                    for layer in waveLayers {
                        var wave = Path()
                        let baseY = size.height * layer.yFrac
                        let steps = 48
                        wave.move(to: CGPoint(x: 0, y: size.height))
                        for step in 0...steps {
                            let frac = Double(step) / Double(steps)
                            let x = CGFloat(frac) * size.width
                            let angle = frac * Double.pi * 2.0 * layer.freq + t * layer.speed
                            let mod = sin(angle) * 0.7 + cos(angle * 0.5 - t) * 0.3
                            let y = baseY + CGFloat(mod * layer.amp)
                            wave.addLine(to: CGPoint(x: x, y: y))
                        }
                        wave.addLine(to: CGPoint(x: size.width, y: size.height))
                        wave.closeSubpath()

                        context.fill(
                            wave,
                            with: .linearGradient(
                                Gradient(colors: [layer.color, Color(hex: 0x030303, opacity: 0.94)]),
                                startPoint: CGPoint(x: 0, y: baseY - 60),
                                endPoint: CGPoint(x: 0, y: size.height)
                            )
                        )
                    }
                }
            }

            Color.black.opacity(0.22)
        }
        .onAppear {
            AudioEffectsEngine.setVisualizerActive(true)
        }
        .onDisappear {
            AudioEffectsEngine.setVisualizerActive(false)
        }
    }
}

struct GeniusLineMeaningSheet: View {
    let lineText: String
    let annotation: GeniusAnnotationEntry
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 8) {
                            Image(systemName: annotation.isVerifiedArtist ? "checkmark.seal.fill" : "quote.bubble.fill")
                                .foregroundColor(theme.accent)
                            Text(annotation.badgeLabel.uppercased())
                                .font(.system(size: 10.5, weight: .black, design: .rounded))
                                .tracking(0.8)
                                .foregroundColor(theme.accent)
                            Spacer()
                            if annotation.votesTotal > 0 {
                                Label("\(annotation.votesTotal)", systemImage: "hand.thumbsup.fill")
                                    .font(theme.font(size: 11.5, weight: .bold))
                                    .foregroundColor(theme.secondaryText)
                            }
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            Text("LYRIC")
                                .font(.system(size: 9.5, weight: .bold, design: .rounded))
                                .foregroundColor(theme.secondaryText)
                            Text("\"\(annotation.fragment.isEmpty ? lineText : annotation.fragment)\"")
                                .font(theme.font(size: 17, weight: .bold))
                                .foregroundColor(theme.primaryText)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .derbisGlassPanel(cornerRadius: 18, tint: theme.accent.opacity(0.22), interactive: false)

                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text("By \(annotation.authorName)")
                                    .font(theme.font(size: 13, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Spacer()
                                Text("Genius")
                                    .font(theme.font(size: 11.5, weight: .bold))
                                    .foregroundColor(theme.secondaryText)
                            }

                            Divider()

                            Text(annotation.explanation)
                                .font(theme.font(size: 15))
                                .lineSpacing(4)
                                .foregroundColor(theme.primaryText)
                                .textSelection(.enabled)
                        }
                        .padding(16)
                        .derbisCard(elevated: true)
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Line Meaning")
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

struct DerbisSingLyricsFloatingBar: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared

    @State private var singSliderExpanded: Bool = false

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            Menu {
                Toggle("Pronunciation (Romanization)", isOn: $theme.showLyricsRomanization)

                Toggle(
                    "Translate Lyrics",
                    isOn: Binding(
                        get: { theme.showLyricsTranslation },
                        set: { enabled in
                            theme.showLyricsTranslation = enabled
                            if enabled {
                                player.ensureLyricsTranslated(to: theme.lyricsTranslationLanguage)
                            }
                        }
                    )
                )

                Menu("Translation Language (\(theme.lyricsTranslationLanguage.title))") {
                    ForEach(LyricsTranslationLanguage.allCases) { lang in
                        Button {
                            theme.lyricsTranslationLanguage = lang
                            theme.showLyricsTranslation = true
                            player.ensureLyricsTranslated(to: lang)
                        } label: {
                            HStack {
                                Text(lang.title)
                                if theme.lyricsTranslationLanguage == lang {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                }

                Divider()

                Toggle("Genius Meaning Markers", isOn: $theme.showGeniusMeaningMarkers)
            } label: {
                HStack(spacing: 6) {
                    if player.isTranslatingLyrics {
                        ProgressView()
                            .scaleEffect(0.7)
                            .tint(.white)
                    } else {
                        Image(systemName: "character.bubble.fill")
                            .font(.system(size: 13.5, weight: .bold))
                    }
                    Text(theme.showLyricsTranslation ? theme.lyricsTranslationLanguage.rawValue.uppercased() : "Translate")
                        .font(theme.font(size: 12, weight: .bold))
                }
                .foregroundColor(theme.showLyricsTranslation ? theme.onAccentText : .white.opacity(0.9))
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .derbisGlassCapsule(prominent: theme.showLyricsTranslation, forceGlassOverBackdrop: true)
            }

            Spacer(minLength: 6)

            if singSliderExpanded {
                HStack(spacing: 10) {
                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        fx.singEnabled.toggle()
                    } label: {
                        Image(systemName: fx.singEnabled ? "mic.fill" : "mic.slash.fill")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(fx.singEnabled ? theme.accent : .white.opacity(0.75))
                    }
                    .buttonStyle(.plain)

                    Slider(
                        value: Binding(
                            get: { fx.singVocalLevel },
                            set: { val in
                                if !fx.singEnabled { fx.singEnabled = true }
                                fx.singVocalLevel = val
                            }
                        ),
                        in: 0.0...1.0,
                        step: 0.05
                    )
                    .tint(theme.accent)
                    .frame(minWidth: 68, maxWidth: 100)

                    Text("\(Int((fx.singVocalLevel * 100).rounded()))%")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(.white)
                        .frame(width: 34, alignment: .trailing)

                    Button {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.84)) {
                            singSliderExpanded = false
                        }
                    } label: {
                        Image(systemName: "chevron.down.circle.fill")
                            .font(.system(size: 15))
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .derbisGlassCapsule(forceGlassOverBackdrop: true)
                .transition(.scale(scale: 0.9, anchor: .trailing).combined(with: .opacity))
            } else {
                Button {
                    HapticsAndGesturesService.shared.playControlTap(style: .light)
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                        if !fx.singEnabled {
                            fx.singEnabled = true
                        }
                        singSliderExpanded = true
                    }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: fx.singEnabled ? "mic.fill" : "mic")
                            .font(.system(size: 14, weight: .bold))
                        if fx.singEnabled {
                            Text("\(Int((fx.singVocalLevel * 100).rounded()))%")
                                .font(.system(size: 11.5, weight: .bold, design: .monospaced))
                        } else {
                            Text("Sing")
                                .font(theme.font(size: 12, weight: .bold))
                        }
                    }
                    .foregroundColor(fx.singEnabled ? theme.onAccentText : .white)
                    .padding(.horizontal, 13)
                    .padding(.vertical, 9)
                    .derbisGlassCapsule(prominent: fx.singEnabled, forceGlassOverBackdrop: true)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 6)
    }
}

struct DerbisPlayerConsoleSheet: View {
    let track: Track
    let onOpenPlaylistPicker: () -> Void
    let onOpenAudioStudio: () -> Void
    let onOpenJam: () -> Void
    let onOpenArtist: () -> Void
    let onOpenAlbum: () -> Void
    let onOpenSimilar: () -> Void

    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @Environment(\.dismiss) private var dismiss

    private var shareURLString: String {
        if let vid = track.videoId {
            return "https://music.youtube.com/watch?v=\(vid)"
        }
        return "\(track.title) — \(track.artist)"
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 12) {
                            TrackArtworkView(url: track.displayArtworkURL, size: 48, cornerRadius: theme.artworkRadius)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(track.title)
                                    .font(theme.font(size: 15.5, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                    .lineLimit(1)
                                Text(track.artist)
                                    .font(theme.font(size: 13))
                                    .foregroundColor(theme.secondaryText)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Button {
                                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                                player.startSongRadio(from: track)
                                dismiss()
                            } label: {
                                Label("Radio", systemImage: "infinity")
                                    .font(theme.font(size: 12, weight: .bold))
                                    .foregroundColor(theme.onAccentText)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 7)
                                    .derbisGlassCapsule(prominent: true)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.bottom, 2)

                        topFourActionTiles

                        playerDeckCard

                        speedAndPitchQuickCard

                        backdropStyleCard

                        lyricsAndAutoMixCard

                        moreActionsCard
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Studio Console")
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

    private var topFourActionTiles: some View {
        HStack(spacing: 8) {
            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                dismiss()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    onOpenArtist()
                }
            } label: {
                actionTileLabel(icon: "person.wave.2.fill", title: "Artist")
            }
            .buttonStyle(.plain)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                dismiss()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    onOpenSimilar()
                }
            } label: {
                actionTileLabel(icon: "sparkles", title: "Similar")
            }
            .buttonStyle(.plain)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                dismiss()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    onOpenPlaylistPicker()
                }
            } label: {
                actionTileLabel(icon: "text.badge.plus", title: "Playlist")
            }
            .buttonStyle(.plain)

            ShareLink(item: shareURLString) {
                actionTileLabel(icon: "square.and.arrow.up", title: "Share")
            }
        }
    }

    private func actionTileLabel(icon: String, title: String) -> some View {
        VStack(spacing: 7) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(theme.accent)
            Text(title)
                .font(theme.font(size: 11, weight: .bold))
                .foregroundColor(theme.primaryText)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 13)
        .derbisCard(elevated: true)
    }

    private var playerDeckCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Player Deck", systemImage: "square.stack.3d.up.fill")
                    .font(theme.font(size: 14, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text(theme.effectivePlayerDeck.title)
                    .font(theme.font(size: 12, weight: .bold))
                    .foregroundColor(theme.accent)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(DerbisPlayerDeck.allCases) { deck in
                        let active = theme.playerDeck == deck
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) {
                                theme.playerDeck = deck
                            }
                        } label: {
                            HStack(spacing: 5) {
                                Image(systemName: deck.icon)
                                    .font(.system(size: 11.5, weight: .bold))
                                Text(deck.title)
                                    .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                            }
                            .foregroundColor(active ? theme.onAccentText : theme.primaryText)
                            .padding(.horizontal, 13)
                            .padding(.vertical, 8)
                            .derbisGlassCapsule(prominent: active)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(14)
        .derbisCard()
    }

    private var speedAndPitchQuickCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Speed & Pitch", systemImage: "speedometer")
                    .font(theme.font(size: 14, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text(String(format: "%.2fx", fx.playbackSpeed))
                    .font(.system(size: 13, weight: .bold, design: .monospaced))
                    .foregroundColor(theme.accent)
            }

            Slider(value: $fx.playbackSpeed, in: 0.5...2.0, step: 0.05)
                .tint(theme.accent)

            Toggle("Pitch follows speed (Vinyl varispeed)", isOn: $fx.pitchFollowsSpeed)
                .font(theme.font(size: 13, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)
        }
        .padding(14)
        .derbisCard()
    }

    private var backdropStyleCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Backdrop Atmosphere")
                .font(theme.font(size: 14, weight: .bold))
                .foregroundColor(theme.primaryText)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(PlayerBackdropStyle.allCases) { style in
                        let active = theme.backdropStyle == style
                        Button {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            theme.backdropStyle = style
                        } label: {
                            Text(style.rawValue)
                                .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                                .foregroundColor(active ? theme.onAccentText : theme.primaryText)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .derbisGlassCapsule(prominent: active)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(14)
        .derbisCard()
    }

    private var lyricsAndAutoMixCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Toggle(
                "Endless Listening (Song Radio)",
                isOn: Binding(
                    get: { player.endlessListeningEnabled },
                    set: {
                        player.setEndlessListeningEnabled($0)
                        theme.endlessRadioEnabled = $0
                    }
                )
            )
            .font(theme.font(size: 13.5, weight: .semibold))
            .foregroundColor(theme.primaryText)
            .tint(theme.accent)

            Divider()

            HStack {
                Label("Lyrics Source", systemImage: "quote.bubble")
                    .font(theme.font(size: 14, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Menu(player.currentLyrics?.provider ?? "Auto") {
                    Button("Auto Priority Chain") {
                        player.loadLyrics(for: track, forceRefresh: true, preferredSource: nil)
                    }
                    ForEach(LyricsSourceKind.allCases) { src in
                        Button(src.title) {
                            player.loadLyrics(for: track, forceRefresh: true, preferredSource: src)
                        }
                    }
                }
                .font(theme.font(size: 12.5, weight: .bold))
                .foregroundColor(theme.accent)
            }

            Divider()

            Toggle("Lock Screen Live Lyrics", isOn: Binding(
                get: { theme.lockScreenLiveLyrics },
                set: {
                    theme.lockScreenLiveLyrics = $0
                    player.refreshNowPlayingLockScreenPreferences()
                }
            ))
            .font(theme.font(size: 13.5, weight: .medium))
            .foregroundColor(theme.primaryText)
            .tint(theme.accent)

            Divider()

            Toggle("AutoMix Crossfade (\(Int(theme.autoMixSeconds))s)", isOn: $theme.autoMixEnabled)
                .font(theme.font(size: 13.5, weight: .medium))
                .foregroundColor(theme.primaryText)
                .tint(theme.accent)

            if theme.autoMixEnabled {
                Button {
                    let ok = player.previewAutoMixTransition()
                    if ok {
                        HapticsAndGesturesService.shared.playSuccessFeedback()
                        dismiss()
                    }
                } label: {
                    Label("Preview Next Transition (Jump to final 15s)", systemImage: "waveform.path")
                        .font(theme.font(size: 12.5, weight: .bold))
                        .foregroundColor(theme.accent)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(14)
        .derbisCard()
    }

    private var moreActionsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !track.album.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Button {
                    dismiss()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                        onOpenAlbum()
                    }
                } label: {
                    HStack {
                        Label("Go to Album · \(track.album)", systemImage: "opticaldisc")
                            .font(theme.font(size: 14, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                            .lineLimit(1)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(theme.secondaryText)
                    }
                }
                .buttonStyle(.plain)

                Divider()
            }

            Button {
                dismiss()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    onOpenAudioStudio()
                }
            } label: {
                HStack {
                    Label("32-bit Mix Lab, AutoEQ & Reverb", systemImage: "slider.vertical.3")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .buttonStyle(.plain)

            Divider()

            Button {
                dismiss()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    onOpenJam()
                }
            } label: {
                HStack {
                    Label("Listen Together (Derbis Jam)", systemImage: "person.2.wave.2.fill")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .buttonStyle(.plain)

            Divider()

            HStack {
                Label("Sleep Timer", systemImage: "moon.zzz.fill")
                    .font(theme.font(size: 14, weight: .semibold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Menu(
                    player.sleepTimerRemainingSec.map { TimeFormatting.clockString(seconds: Double($0)) } ?? "Off"
                ) {
                    ForEach(SleepTimerOption.allCases) { opt in
                        Button(opt.label) {
                            player.setSleepTimer(opt)
                        }
                    }
                }
                .font(theme.font(size: 13, weight: .bold))
                .foregroundColor(theme.accent)
            }

            Divider()

            let blocked = library.isArtistBlocked(track.artist)
            Button(role: blocked ? nil : .destructive) {
                let willBlock = !blocked
                library.toggleBlockedArtist(track.artist)
                if willBlock && player.queue.count > 1 {
                    player.skipNext()
                    dismiss()
                }
            } label: {
                Label(
                    blocked ? "Unblock \(track.artist)" : "Don't Play \(track.artist)",
                    systemImage: blocked ? "person.crop.circle.badge.checkmark" : "hand.raised.slash"
                )
                .font(theme.font(size: 14, weight: .semibold))
                .foregroundColor(blocked ? theme.accent : .red)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .derbisCard()
    }
}
