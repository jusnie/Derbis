import SwiftUI

/// The press-and-hold lens for the player stage, in the iOS Liquid Glass language:
/// a tall bead of chrome glass over the artwork, with the current lyric window
/// rendered large, frosted and out of focus underneath — the same way the tab
/// bar droplet magnifies its active glyph, but for the song's words.
///
/// Interaction contract (owned by the caller): pressing the cover for a beat (or
/// holding without moving) wakes the lens; dragging it slides the same blurred
/// window under the finger; releasing puts it back to sleep.
struct ArtworkLyricLens: View {
    let lines: [LyricLine]
    var accent: Color = Color(red: 0.988, green: 0.235, blue: 0.267)

    @ObservedObject private var clock = PlaybackClock.shared

    private let lensSize = CGSize(width: 236, height: 272)

    private var focusSeconds: Double { clock.currentTime }

    private var window: [LyricLine] {
        let clean = lines.filter { !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        guard !clean.isEmpty else { return [] }
        let focusIndex: Int = {
            if let active = clean.lastIndex(where: { $0.startTimeSec <= focusSeconds }) {
                return active
            }
            return clean.count / 2
        }()
        let half = 2
        let lower = max(0, min(focusIndex - half, clean.count - (half * 2 + 1)))
        return Array(clean[lower..<min(clean.count, lower + half * 2 + 1)])
    }

    private var focusLineId: Int? {
        let clean = lines.filter { !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        return clean.last(where: { $0.startTimeSec <= focusSeconds })?.id
    }

    var body: some View {
        ZStack {
            // The frosted words under the glass: large, soft and barely legible —
            // a hint of the song rather than a reading surface.
            VStack(spacing: 13) {
                ForEach(window) { line in
                    Text(line.text)
                        .font(.system(size: 25, weight: .heavy))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.72)
                        .foregroundColor(.white.opacity(line.id == focusLineId ? 0.96 : 0.72))
                }
            }
            .padding(.horizontal, 18)
            .frame(width: lensSize.width, height: lensSize.height)
            .blur(radius: 7.4)
            .mask(Ellipse().inset(by: 6))

            // The glass itself, matching the tab bar droplet's material.
            DerbisOpticalMaterial(
                shape: AnyShape(Ellipse()),
                intensity: .chrome,
                prominent: true,
                tint: accent
            )
            .clipShape(Ellipse())

            // Top-light sheen so the bead reads as standing off the cover.
            Ellipse()
                .stroke(
                    LinearGradient(
                        stops: [
                            .init(color: .white.opacity(0.55), location: 0.0),
                            .init(color: .white.opacity(0.06), location: 0.42),
                            .init(color: accent.opacity(0.34), location: 0.78),
                            .init(color: .white.opacity(0.28), location: 1.0)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 1.35
                )
                .blur(radius: 0.4)

            Ellipse()
                .fill(
                    RadialGradient(
                        colors: [.white.opacity(0.16), .clear],
                        center: .init(x: 0.36, y: 0.24),
                        startRadius: 2,
                        endRadius: lensSize.width * 0.62
                    )
                )
        }
        .frame(width: lensSize.width, height: lensSize.height)
        .shadow(color: .white.opacity(0.18), radius: 9, y: 0)
        .shadow(color: .black.opacity(0.48), radius: 22, y: 12)
        .accessibilityHidden(true)
    }
}
