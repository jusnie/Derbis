import SwiftUI

/// The press-and-hold lens for the player stage — the reference player's WebGL water
/// droplet, ported natively. The bead magnifies the scene it covers (a snapshot of the
/// deck's own backdrop, captured the moment the hold is recognized), wobbles its own
/// silhouette, and squashes along the finger's travel direction with a real spring.
/// The frosted lyric window rides inside, blurred — a hint of the song rather than a
/// reading surface — and the rim catches the top-left like a bead of water standing
/// off the cover.
///
/// Interaction contract (owned by the caller): pressing the cover for a beat (or
/// holding without moving) wakes the lens; dragging it slides the same blurred window
/// under the finger; releasing puts it back to sleep.
///
/// [snapshot] is a rendering of the deck's backdrop in bleed coordinates (the live
/// backdrop overhangs the viewport by 20pt each side and 80pt top/bottom). If the
/// snapshot is missing the bead falls back to an accent-tinted ambience so it still
/// reads over any backdrop style. [sampleOrigin] is the point in that snapshot the
/// droplet's centre magnifies; the caller derives it from the stage's position inside
/// the viewport, so the bead always shows what is actually under the finger.
struct ArtworkLyricLens: View {
    let lines: [LyricLine]
    var accent: Color = Color(red: 0.988, green: 0.235, blue: 0.267)
    var snapshot: UIImage? = nil
    var sampleOrigin: CGPoint = CGPoint(x: 215, y: 420)
    var bleedSize: CGSize = CGSize(width: 430, height: 1024)

    @ObservedObject private var clock = PlaybackClock.shared

    private let lensSize = CGSize(width: 236, height: 272)
    /// Padding around the droplet so the wobbling silhouette is never clipped by the
    /// view's own frame.
    private let pad: CGFloat = 30
    private let magnification: CGFloat = 1.14

    @State private var physics = DropletPhysics()

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

    private var dropletRect: CGRect {
        CGRect(x: pad, y: pad, width: lensSize.width, height: lensSize.height)
    }

    var body: some View {
        TimelineView(.animation) { timeline in
            let now = timeline.date.timeIntervalSinceReferenceDate
            let _ = physics.tick(at: now)

            let wobble = LensWobbleShape(phase: now, amplitude: physics.amplitude)
            let droplet = dropletRect

            ZStack {
                // 1. The refracted scene: the deck's backdrop magnified about the
                //    finger, inside a silhouette that slowly wobbles.
                Canvas { context, size in
                    var context = context
                    let silhouette = wobble.path(in: droplet)
                    context.clip(to: silhouette)
                    context.fill(silhouette, with: .color(Color(red: 0.05, green: 0.045, blue: 0.04)))

                    if let snapshot {
                        context.concatenate(
                            CGAffineTransform(translationX: droplet.midX, y: droplet.midY)
                                .scaledBy(x: 1 / magnification, y: 1 / magnification)
                                .translatedBy(x: -sampleOrigin.x, y: -sampleOrigin.y)
                        )
                        context.draw(
                            Image(uiImage: snapshot),
                            in: CGRect(origin: .zero, size: bleedSize)
                        )
                        // Restore the identity transform (the inverse of the above) so the
                        // scrim below lands exactly on the droplet's rect.
                        context.concatenate(
                            CGAffineTransform(translationX: sampleOrigin.x, y: sampleOrigin.y)
                                .scaledBy(x: magnification, y: magnification)
                                .translatedBy(x: -droplet.midX, y: -droplet.midY)
                        )
                    } else {
                        context.fill(
                            silhouette,
                            with: .radialGradient(
                                Gradient(colors: [accent.opacity(0.42), .black.opacity(0.92)]),
                                center: CGPoint(x: droplet.midX, y: droplet.midY - 30),
                                startRadius: 10,
                                endRadius: droplet.width * 0.95
                            )
                        )
                    }

                    // Legibility scrim under the words, darkest at the bead's poles.
                    context.fill(
                        silhouette,
                        with: .linearGradient(
                            Gradient(stops: [
                                .init(color: .black.opacity(0.32), location: 0.0),
                                .init(color: .clear, location: 0.30),
                                .init(color: .clear, location: 0.70),
                                .init(color: .black.opacity(0.32), location: 1.0)
                            ]),
                            startPoint: CGPoint(x: droplet.midX, y: droplet.minY),
                            endPoint: CGPoint(x: droplet.midX, y: droplet.maxY)
                        )
                    )
                }

                // 2. The frosted words under the water — large, soft, barely legible.
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

                // 3. The rim: a top-lit edge with the accent warming the foot of the
                //    bead, stroked along the same wobbling silhouette the shader cuts.
                Canvas { context, _ in
                    let silhouette = wobble.path(in: dropletRect)
                    context.stroke(
                        silhouette,
                        with: .linearGradient(
                            Gradient(stops: [
                                .init(color: .white.opacity(0.55), location: 0.0),
                                .init(color: .white.opacity(0.06), location: 0.42),
                                .init(color: accent.opacity(0.34), location: 0.78),
                                .init(color: .white.opacity(0.28), location: 1.0)
                            ]),
                            startPoint: CGPoint(x: dropletRect.midX, y: dropletRect.minY),
                            endPoint: CGPoint(x: dropletRect.midX, y: dropletRect.maxY)
                        ),
                        lineWidth: 1.35
                    )
                }

                // 4. The specular catchlight — the water droplet's kick from the upper-left.
                Ellipse()
                    .fill(
                        RadialGradient(
                            colors: [.white.opacity(0.34), .clear],
                            center: .init(x: 0.5, y: 0.5),
                            startRadius: 1,
                            endRadius: 46
                        )
                    )
                    .frame(width: 92, height: 56)
                    .offset(x: -34, y: -80)
                    .allowsHitTesting(false)
            }
            .frame(width: lensSize.width + pad * 2, height: lensSize.height + pad * 2)
            .scaleEffect(x: physics.scaleX, y: physics.scaleY)
        }
        .shadow(color: .black.opacity(0.48), radius: 22, y: 12)
        .onChange(of: sampleOrigin) { newValue in
            physics.addPosition(newValue, at: Date().timeIntervalSinceReferenceDate)
        }
        .accessibilityHidden(true)
    }
}

/// The wobbling silhouette of the droplet: the ellipse's perimeter modulated by
/// three harmonics, exactly the reference's `r = 1 + a·(0.045·sin(3θ+t·1.1) +
/// 0.035·sin(2θ−t·0.8+1.3) + 0.02·sin(5θ+t·1.7))`. Rebuilt per animation frame;
/// cheap — 73 points.
private struct LensWobbleShape: Shape {
    var phase: Double
    var amplitude: Double

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let cx = rect.midX, cy = rect.midY
        let rx = rect.width / 2, ry = rect.height / 2
        let steps = 72
        for i in 0...steps {
            let th = Double(i) / Double(steps) * 2 * .pi
            let r = 1 + amplitude * (
                0.045 * sin(3 * th + phase * 1.1)
                + 0.035 * sin(2 * th - phase * 0.8 + 1.3)
                + 0.02 * sin(5 * th + phase * 1.7)
            )
            let p = CGPoint(x: cx + CGFloat(cos(th) * r) * rx, y: cy + CGFloat(sin(th) * r) * ry)
            if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
        }
        path.closeSubpath()
        return path
    }
}

/// Spring state for the droplet, driven by finger motion: [speed] low-passes the drag
/// velocity, the squash spring kicks on every fast movement and rings back to neutral,
/// and the axis-aligned scales stretch the bead along its travel direction — the same
/// squash-and-stretch the reference's physics loop applies to its lens.
private final class DropletPhysics {
    private(set) var speed: CGFloat = 0
    private(set) var amplitude: Double = 0.75
    private(set) var scaleX: CGFloat = 1
    private(set) var scaleY: CGFloat = 1

    private var dirX: CGFloat = 0
    private var dirY: CGFloat = 1
    private var squash: CGFloat = 0
    private var squashVelocity: CGFloat = 0
    private var lastSample: CGPoint?
    private var lastStamp: TimeInterval?
    private var lastTick: TimeInterval?

    func addPosition(_ sample: CGPoint, at t: TimeInterval) {
        defer { lastSample = sample; lastStamp = t }
        guard let lp = lastSample, let lt = lastStamp else { return }
        let dt = max(0.008, t - lt)
        let dx = sample.x - lp.x, dy = sample.y - lp.y
        let spd = hypot(dx, dy) / dt
        speed = speed * 0.62 + spd * 0.38
        if spd > 30 {
            var nx = dirX * 0.7 + dx / spd * 0.3
            var ny = dirY * 0.7 + dy / spd * 0.3
            let n = hypot(nx, ny)
            if n > 0.001 { nx /= n; ny /= n }
            dirX = nx; dirY = ny
        }
        squashVelocity += min(spd / 750, 1.15) * 2.1
    }

    func tick(at t: TimeInterval) {
        let dt = lastTick.map { min(0.05, max(0.008, t - $0)) } ?? 1.0 / 60.0
        lastTick = t
        speed *= CGFloat(pow(0.86, dt * 60))
        squashVelocity += (-squash * 160 - squashVelocity * 11.5) * CGFloat(dt)
        squash = min(1.4, max(-1.4, squash + squashVelocity * CGFloat(dt)))
        amplitude = 0.75 + Double(min(speed * 0.0006, 0.6))
        let amt = min(speed * 0.00016, 0.22)
        scaleX = 1 + amt * dirX - amt * dirY * 0.5 + squash * 0.5
        scaleY = 1 + amt * dirY - amt * dirX * 0.5 - squash * 0.5
    }
}
