import SwiftUI
import UIKit

struct AudioEffectsSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var fx = AudioEffectsEngine.shared
    @ObservedObject private var haptics = HapticsAndGesturesService.shared
    @Environment(\.dismiss) private var dismiss

    private enum StudioTab: String, CaseIterable, Identifiable {
        case equalizer = "EQ & AutoEQ"
        case spatial = "Sing & FX"
        case haptics = "Haptics & AirPods"

        var id: String { rawValue }
    }

    @State private var selectedTab: StudioTab = .equalizer
    @State private var autoEQBanner: String?

    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: 0x0B0B10).ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        studioStatusHeader

                        Picker("Studio Section", selection: $selectedTab) {
                            ForEach(StudioTab.allCases) { tab in
                                Text(tab.rawValue).tag(tab)
                            }
                        }
                        .pickerStyle(.segmented)
                        .onChange(of: selectedTab) { _ in
                            haptics.playSelectionTick()
                        }

                        switch selectedTab {
                        case .equalizer:
                            equalizerSection
                        case .spatial:
                            spatialAndSpeedSection
                        case .haptics:
                            hapticsAndAirPodsSection
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 10)
                    .padding(.bottom, 34)
                }
            }
            .navigationTitle("Audio Studio")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    if fx.hasAnyActiveModification {
                        Button("Reset") {
                            haptics.playControlTap(style: .medium)
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) {
                                fx.resetAllEffects()
                            }
                        }
                        .foregroundStyle(theme.accent)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        haptics.playControlTap(style: .light)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private var studioStatusHeader: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(fx.dspTapActiveOnCurrentStream ? Color.green : theme.accent)
                .frame(width: 8, height: 8)

            VStack(alignment: .leading, spacing: 2) {
                Text(fx.dspTapActiveOnCurrentStream ? "Realtime 32-Bit Float DSP Active" : "Speed & Vinyl Engine Ready")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white)

                Text(
                    fx.dspTapActiveOnCurrentStream
                        ? "10-band biquad EQ, adaptive bass, and spatial stage engaged"
                        : "EQ & spatial tap engage automatically on direct/downloaded streams"
                )
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(.white.opacity(0.55))
            }

            Spacer(minLength: 0)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color.white.opacity(0.06))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Color.white.opacity(0.09), lineWidth: 0.7)
                )
        )
    }

    private var equalizerSection: some View {
        VStack(spacing: 16) {
            frequencyResponseCard

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Toggle(isOn: $fx.eqEnabled) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("10-Band Parametric Graphic EQ")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white)
                            Text("Preset: \(fx.selectedPreset.rawValue)")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(.white.opacity(0.55))
                        }
                    }
                    .tint(theme.accent)
                    .onChange(of: fx.eqEnabled) { _ in
                        haptics.playSelectionTick()
                    }
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(EqualizerPreset.allCases) { preset in
                            let selected = fx.selectedPreset == preset
                            Button {
                                haptics.playSelectionTick()
                                withAnimation(.easeOut(duration: 0.2)) {
                                    fx.applyPreset(preset)
                                }
                            } label: {
                                Text(preset.rawValue)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(selected ? .black : .white.opacity(0.85))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 7)
                                    .background(
                                        Capsule()
                                            .fill(selected ? Color.white : Color.white.opacity(0.09))
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(spacing: 12) {
                HStack {
                    Text("BANDS (-12 dB TO +12 dB)")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(0.8)
                        .foregroundStyle(.white.opacity(0.5))
                    Spacer()
                    Button("Flatten") {
                        haptics.playControlTap(style: .light)
                        withAnimation(.easeOut(duration: 0.2)) {
                            fx.applyPreset(.flat)
                        }
                    }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(theme.accent)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        ForEach(0..<10, id: \.self) { idx in
                            VerticalEQBandColumn(
                                label: EqualizerPreset.bandLabels[idx],
                                gainDB: fx.bandGainsDB.indices.contains(idx) ? fx.bandGainsDB[idx] : 0,
                                accent: theme.accent
                            ) { newGain in
                                fx.updateBand(index: idx, gainDB: newGain)
                            }
                        }
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 4)
                }

                Divider().background(Color.white.opacity(0.1))

                VStack(spacing: 6) {
                    HStack {
                        Text("Preamp Headroom")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(.white)
                        Spacer()
                        Text(String(format: "%+.1f dB", fx.preampDB))
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundStyle(.white.opacity(0.75))
                    }
                    Slider(value: $fx.preampDB, in: -6.0...6.0, step: 0.5)
                        .tint(theme.accent)
                }
            }
            .padding(14)
            .background(studioCardBackground)

            autoEQHeadphoneCard
        }
    }

    private var autoEQHeadphoneCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("AutoEQ Headphone Profiles")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                    Text(
                        fx.activeAutoEQProfileName.isEmpty
                            ? "Harman target correction curves or paste GraphicEQ: text"
                            : "Active: \(fx.activeAutoEQProfileName)"
                    )
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(fx.activeAutoEQProfileName.isEmpty ? .white.opacity(0.55) : theme.accent)
                }
                Spacer()
                Button {
                    if let pasted = UIPasteboard.general.string,
                       fx.applyAutoEQGraphicEQString(pasted) {
                        haptics.playSuccessFeedback()
                        autoEQBanner = "Applied GraphicEQ curve from clipboard!"
                    } else {
                        autoEQBanner = "Clipboard didn't contain a GraphicEQ: curve."
                    }
                } label: {
                    Label("Paste", systemImage: "doc.on.clipboard")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(theme.accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(Color.white.opacity(0.09)))
                }
                .buttonStyle(.plain)
            }

            if let msg = autoEQBanner {
                Text(msg)
                    .font(.system(size: 11.5, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(AudioEffectsEngine.builtInAutoEQProfiles) { profile in
                        let full = "\(profile.brand) \(profile.name)"
                        let active = fx.activeAutoEQProfileName == full
                        Button {
                            haptics.playSelectionTick()
                            fx.applyAutoEQProfile(profile)
                            autoEQBanner = nil
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(profile.brand.uppercased())
                                    .font(.system(size: 9.5, weight: .black, design: .rounded))
                                    .foregroundStyle(active ? .black.opacity(0.65) : .white.opacity(0.5))
                                Text(profile.name)
                                    .font(.system(size: 12.5, weight: .bold))
                                    .foregroundStyle(active ? .black : .white)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(active ? Color.white : Color.white.opacity(0.08))
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(14)
        .background(studioCardBackground)
    }

    private var frequencyResponseCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("LIVE FREQUENCY RESPONSE")
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.9)
                    .foregroundStyle(.white.opacity(0.5))
                Spacer()
                Text("20 Hz – 20 kHz")
                    .font(.system(size: 10, weight: .semibold, design: .monospaced))
                    .foregroundStyle(.white.opacity(0.45))
            }

            Canvas { context, size in
                let midY = size.height * 0.5
                var zeroLine = Path()
                zeroLine.move(to: CGPoint(x: 0, y: midY))
                zeroLine.addLine(to: CGPoint(x: size.width, y: midY))
                context.stroke(zeroLine, with: .color(.white.opacity(0.14)), lineWidth: 1)

                let steps = 96
                let minLog = log10(20.0)
                let maxLog = log10(20_000.0)

                var curve = Path()
                var fillPath = Path()

                for i in 0...steps {
                    let t = Double(i) / Double(steps)
                    let freq = pow(10.0, minLog + t * (maxLog - minLog))
                    let db = fx.estimatedMagnitudeDB(at: freq)
                    let x = CGFloat(t) * size.width
                    let normalized = CGFloat(db / 15.0)
                    let y = midY - normalized * (size.height * 0.42)

                    if i == 0 {
                        curve.move(to: CGPoint(x: x, y: y))
                        fillPath.move(to: CGPoint(x: x, y: midY))
                        fillPath.addLine(to: CGPoint(x: x, y: y))
                    } else {
                        curve.addLine(to: CGPoint(x: x, y: y))
                        fillPath.addLine(to: CGPoint(x: x, y: y))
                    }
                }

                fillPath.addLine(to: CGPoint(x: size.width, y: midY))
                fillPath.closeSubpath()

                context.fill(
                    fillPath,
                    with: .linearGradient(
                        Gradient(colors: [theme.accent.opacity(0.32), theme.accent.opacity(0.02)]),
                        startPoint: CGPoint(x: 0, y: 0),
                        endPoint: CGPoint(x: 0, y: size.height)
                    )
                )
                context.stroke(curve, with: .color(theme.accent), lineWidth: 2.2)
            }
            .frame(height: 108)
        }
        .padding(14)
        .background(studioCardBackground)
    }

    private var spatialAndSpeedSection: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Playback Speed & Vinyl Pitch")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(fx.pitchFollowsSpeed ? "Vinyl varispeed: pitch shifts naturally with tempo" : "Time-domain: tempo shifts while musical key stays locked")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                    Spacer()
                    Text(String(format: "%.2fx", fx.playbackSpeed))
                        .font(.system(size: 15, weight: .bold, design: .monospaced))
                        .foregroundStyle(theme.accent)
                }

                Slider(value: $fx.playbackSpeed, in: 0.5...2.0, step: 0.05)
                    .tint(theme.accent)

                HStack(spacing: 8) {
                    ForEach([0.75, 0.85, 1.0, 1.15, 1.25, 1.5], id: \.self) { presetSpeed in
                        let isCurrent = abs(fx.playbackSpeed - presetSpeed) < 0.01
                        Button {
                            haptics.playSelectionTick()
                            fx.playbackSpeed = presetSpeed
                        } label: {
                            Text(presetSpeed == 1.0 ? "1.0x" : String(format: "%.2gx", presetSpeed))
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(isCurrent ? .black : .white.opacity(0.85))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 7)
                                .background(
                                    Capsule()
                                        .fill(isCurrent ? Color.white : Color.white.opacity(0.09))
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }

                Toggle(isOn: $fx.pitchFollowsSpeed) {
                    Label("Vinyl Pitch Mode (Slowed / Nightcore)", systemImage: "opticaldisc.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                }
                .tint(theme.accent)
                .onChange(of: fx.pitchFollowsSpeed) { _ in
                    haptics.playSelectionTick()
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(alignment: .leading, spacing: 16) {
                effectSliderRow(
                    title: "Adaptive Bass Boost",
                    subtitle: "100 Hz low-shelf with knee detector so sub-bass never clips",
                    icon: "speaker.wave.3.fill",
                    value: $fx.adaptiveBassBoost
                )

                Divider().background(Color.white.opacity(0.08))

                effectSliderRow(
                    title: "Stereo Widener",
                    subtitle: "Expands side stage above 150 Hz while keeping bass centered",
                    icon: "dot.radiowaves.left.and.right",
                    value: $fx.stereoWidener
                )

                Divider().background(Color.white.opacity(0.08))

                effectSliderRow(
                    title: "Headphone Crossfeed",
                    subtitle: "700 Hz natural speaker blend for relaxed long listening",
                    icon: "headphones",
                    value: $fx.crossfeedAmount
                )

                Divider().background(Color.white.opacity(0.08))

                Toggle(isOn: $fx.warmTubeSaturation) {
                    VStack(alignment: .leading, spacing: 2) {
                        Label("Warm Analog Soft-Clipper", systemImage: "waveform.path.ecg")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(.white)
                        Text("Subtle even/odd harmonic warmth and peak protection")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                }
                .tint(theme.accent)
                .onChange(of: fx.warmTubeSaturation) { _ in
                    haptics.playSelectionTick()
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(alignment: .leading, spacing: 12) {
                Toggle(isOn: $fx.singEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Label("Derbis Sing (Karaoke Vocal Cut)", systemImage: "mic.fill")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.white)
                        Text("Attenuates center vocals while keeping <165 Hz bass & stereo instruments")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                }
                .tint(theme.accent)
                .onChange(of: fx.singEnabled) { _ in
                    haptics.playSelectionTick()
                }

                if fx.singEnabled {
                    VStack(spacing: 6) {
                        HStack {
                            Text("Vocal Volume")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white)
                            Spacer()
                            Text("\(Int((fx.singVocalLevel * 100).rounded()))%")
                                .font(.system(size: 12, weight: .bold, design: .monospaced))
                                .foregroundStyle(theme.accent)
                        }
                        Slider(value: $fx.singVocalLevel, in: 0.0...1.0, step: 0.05)
                            .tint(theme.accent)
                    }
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Label("Spatial Room & Hall Reverb", systemImage: "dot.radiowaves.up.forward")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.white)
                        Text("Schroeder-Moorer stereo acoustic environment")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                    Spacer()
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(ReverbRoomPreset.allCases) { room in
                            let active = fx.reverbPreset == room
                            Button {
                                haptics.playSelectionTick()
                                fx.reverbPreset = room
                            } label: {
                                Text(room.rawValue)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(active ? .black : .white.opacity(0.85))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 7)
                                    .background(
                                        Capsule()
                                            .fill(active ? Color.white : Color.white.opacity(0.09))
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                if fx.reverbPreset != .off {
                    VStack(spacing: 6) {
                        HStack {
                            Text("Room Wet Mix")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white)
                            Spacer()
                            Text("\(Int((fx.reverbWetMix / 0.65 * 100).rounded()))%")
                                .font(.system(size: 12, weight: .bold, design: .monospaced))
                                .foregroundStyle(theme.accent)
                        }
                        Slider(value: $fx.reverbWetMix, in: 0.05...0.65, step: 0.05)
                            .tint(theme.accent)
                    }
                }
            }
            .padding(14)
            .background(studioCardBackground)
        }
    }

    private func effectSliderRow(
        title: String,
        subtitle: String,
        icon: String,
        value: Binding<Double>
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Label(title, systemImage: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                Spacer()
                Text("\(Int((value.wrappedValue * 100).rounded()))%")
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .foregroundStyle(value.wrappedValue > 0.01 ? theme.accent : .white.opacity(0.55))
            }
            Text(subtitle)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(.white.opacity(0.52))
            Slider(value: value, in: 0.0...1.0, step: 0.05)
                .tint(theme.accent)
        }
    }

    private var hapticsAndAirPodsSection: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Beat-Synced Music Haptics")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.white)
                        Text("Real-time <110 Hz kick & >2 kHz snare Taptic Engine pulses while in foreground")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                    Spacer()
                }

                Picker("Music Haptics", selection: $haptics.musicHapticsMode) {
                    ForEach(MusicHapticsMode.allCases) { mode in
                        Text(mode.rawValue).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: haptics.musicHapticsMode) { _ in
                    haptics.playControlTap(style: .medium)
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(alignment: .leading, spacing: 12) {
                Toggle(isOn: $haptics.controlHapticsEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Tactile Control Haptics")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(.white)
                        Text("Crisp feedback on transport controls, double-tap zones, and toggles")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                }
                .tint(theme.accent)

                if haptics.controlHapticsEnabled {
                    HStack {
                        Text("Tap Strength")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(.white.opacity(0.8))
                        Spacer()
                        Text("\(Int((haptics.controlHapticsStrength * 100).rounded()))%")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundStyle(theme.accent)
                    }
                    Slider(value: $haptics.controlHapticsStrength, in: 0.1...1.0, step: 0.05)
                        .tint(theme.accent)

                    Toggle("10% Timeline Scrubber Notches", isOn: $haptics.scrubberNotchesEnabled)
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundStyle(.white)
                        .tint(theme.accent)
                }
            }
            .padding(14)
            .background(studioCardBackground)

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Toggle(isOn: $haptics.headGesturesEnabled) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("AirPods Head Gestures")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white)
                            Text(
                                haptics.headTrackingSupported
                                    ? (haptics.headphonesConnected ? "Compatible AirPods connected" : "Connect AirPods Pro / Max / 3+ to use hands-free gestures")
                                    : "Headphone motion hardware unavailable on this device"
                            )
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(haptics.headphonesConnected ? Color.green : .white.opacity(0.55))
                        }
                    }
                    .tint(theme.accent)
                    .disabled(!haptics.headTrackingSupported)
                }

                if haptics.headGesturesEnabled {
                    Divider().background(Color.white.opacity(0.08))

                    HStack {
                        Label("Double Nod", systemImage: "arrow.up.and.down.circle.fill")
                            .font(.system(size: 13.5, weight: .medium))
                            .foregroundStyle(.white)
                        Spacer()
                        Picker("Double Nod", selection: $haptics.headDoubleNodAction) {
                            ForEach(HeadNodAction.allCases) { act in
                                Text(act.rawValue).tag(act)
                            }
                        }
                        .tint(theme.accent)
                    }

                    HStack {
                        Label("Head Shake", systemImage: "arrow.left.and.right.circle.fill")
                            .font(.system(size: 13.5, weight: .medium))
                            .foregroundStyle(.white)
                        Spacer()
                        Picker("Head Shake", selection: $haptics.headShakeAction) {
                            ForEach(HeadNodAction.allCases) { act in
                                Text(act.rawValue).tag(act)
                            }
                        }
                        .tint(theme.accent)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("Gesture Sensitivity")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(.white.opacity(0.8))
                            Spacer()
                            Text(String(format: "%.1fx", haptics.headSensitivity))
                                .font(.system(size: 12, weight: .bold, design: .monospaced))
                                .foregroundStyle(theme.accent)
                        }
                        Slider(value: $haptics.headSensitivity, in: 0.6...1.5, step: 0.1)
                            .tint(theme.accent)
                    }
                }
            }
            .padding(14)
            .background(studioCardBackground)
        }
    }

    private var studioCardBackground: some View {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
            .fill(Color.white.opacity(0.055))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.white.opacity(0.08), lineWidth: 0.6)
            )
    }
}

private struct VerticalEQBandColumn: View {
    let label: String
    let gainDB: Double
    let accent: Color
    let onChange: (Double) -> Void

    var body: some View {
        VStack(spacing: 8) {
            Text(String(format: "%+.0f", gainDB))
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundStyle(abs(gainDB) > 0.2 ? accent : .white.opacity(0.5))

            GeometryReader { geo in
                let height = max(1, geo.size.height)
                let normalized = (gainDB + 12.0) / 24.0
                let thumbY = (1.0 - normalized) * height
                let midY = height * 0.5

                ZStack(alignment: .top) {
                    Capsule()
                        .fill(Color.white.opacity(0.11))
                        .frame(width: 6)
                        .frame(maxHeight: .infinity)

                    Rectangle()
                        .fill(Color.white.opacity(0.28))
                        .frame(width: 14, height: 1)
                        .position(x: geo.size.width * 0.5, y: midY)

                    let barTop = min(midY, thumbY)
                    let barHeight = max(2, abs(thumbY - midY))
                    Capsule()
                        .fill(accent)
                        .frame(width: 6, height: barHeight)
                        .offset(y: barTop)

                    Circle()
                        .fill(Color.white)
                        .frame(width: 16, height: 16)
                        .shadow(color: .black.opacity(0.35), radius: 3, y: 1)
                        .position(x: geo.size.width * 0.5, y: min(height - 8, max(8, thumbY)))
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { drag in
                            let clampedY = min(height, max(0, drag.location.y))
                            let ratio = 1.0 - (clampedY / height)
                            let rawDB = (ratio * 24.0) - 12.0
                            let stepped = (rawDB * 2.0).rounded() / 2.0
                            if abs(stepped - gainDB) >= 0.45 {
                                HapticsAndGesturesService.shared.playSelectionTick()
                                onChange(stepped)
                            }
                        }
                )
            }
            .frame(width: 30, height: 142)

            Text(label)
                .font(.system(size: 10, weight: .semibold, design: .rounded))
                .foregroundStyle(.white.opacity(0.68))
        }
    }
}
