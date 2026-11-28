import SwiftUI

// MARK: - Per-glyph keyframe motion (ported from the "Tab bar preview" mock)
//
// The mock ships one hand-tuned keyframe animation per glyph (a-heart, a-star, ...),
// fired when a tab becomes active, replayed when the active tab is tapped again, and
// played when an icon is picked in the editor. iOS runs the same piecewise table as
// Android (`DerbisTabIconMotion.kt`) by interpolating a 0...1 progress value against
// the mock's keyframes; the per-icon easing is applied to the progress itself.
// Reduced-motion collapses everything to identity.

struct GlyphPose: Equatable {
    var scaleX: CGFloat = 1
    var scaleY: CGFloat = 1
    var rotation: Double = 0
    var dx: CGFloat = 0
    var dy: CGFloat = 0
}

private struct GlyphStop {
    let p: Double          // 0...1 position in the animation
    let sx: Double
    let sy: Double
    var rotation: Double = 0
    var dx: CGFloat = 0
    var dy: CGFloat = 0

    static func uniform(_ p: Double, _ scale: Double = 1, rotation: Double = 0, dx: CGFloat = 0, dy: CGFloat = 0) -> GlyphStop {
        GlyphStop(p: p, sx: scale, sy: scale, rotation: rotation, dx: dx, dy: dy)
    }
}

private struct GlyphSpec {
    let duration: Double
    let c1: CGPoint       // cubic-bezier control points (timingCurve)
    let c2: CGPoint
    let stops: [GlyphStop]
}

private let ease = CGPoint(x: 0.4, y: 0.0)     // FastOutSlowInEasing stand-in for plain `ease`
private let easePop = CGPoint(x: 0.3, y: 1.5)
private let easeStar = CGPoint(x: 0.3, y: 1.2)
private let easePlus = CGPoint(x: 0.3, y: 1.3)
private let easeC2 = CGPoint(x: 0.5, y: 1.0)

private let glyphSpecs: [String: GlyphSpec] = [
    "pop": GlyphSpec(duration: 0.5, c1: easePop, c2: easeC2, stops: [
        .uniform(0), .uniform(0.30, 0.7), .uniform(0.65, 1.22), .uniform(1)
    ]),
    "heart": GlyphSpec(duration: 0.65, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), .uniform(0.15, 1.35), .uniform(0.30, 0.95),
        .uniform(0.45, 1.25), .uniform(0.70, 1.0), .uniform(1)
    ]),
    "star": GlyphSpec(duration: 0.6, c1: easeStar, c2: easeC2, stops: [
        .uniform(0), GlyphStop(p: 0.5, sx: 1.3, sy: 1.3, rotation: 180), GlyphStop(p: 1, sx: 1, sy: 1, rotation: 360)
    ]),
    "clock": GlyphSpec(duration: 0.7, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 1, sx: 1, sy: 1, rotation: 360)
    ]),
    "bolt": GlyphSpec(duration: 0.55, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 0.20, sx: 1.3, sy: 1.3, rotation: -12),
        GlyphStop(p: 0.40, sx: 0.9, sy: 0.9, rotation: 10),
        GlyphStop(p: 0.60, sx: 1.2, sy: 1.2, rotation: -6), .uniform(1)
    ]),
    "download": GlyphSpec(duration: 0.55, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), .uniform(0.30, 1, dy: 6), .uniform(0.55, 1, dy: -3), .uniform(1)
    ]),
    "search": GlyphSpec(duration: 0.6, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 0.25, sx: 1.1, sy: 1.1, rotation: -22),
        GlyphStop(p: 0.55, sx: 1.1, sy: 1.1, rotation: 14),
        GlyphStop(p: 0.80, sx: 1, sy: 1, rotation: -6), .uniform(1)
    ]),
    "home": GlyphSpec(duration: 0.55, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 0.30, sx: 1.08, sy: 1.08, dy: -6),
        GlyphStop(p: 0.55, sx: 0.92, sy: 1.06), .uniform(1)
    ]),
    "note": GlyphSpec(duration: 0.6, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 0.25, sx: 1.1, sy: 1.1, rotation: -18),
        GlyphStop(p: 0.60, sx: 1.1, sy: 1.1, rotation: 14), .uniform(1)
    ]),
    "plus": GlyphSpec(duration: 0.55, c1: easePlus, c2: easeC2, stops: [
        .uniform(0), GlyphStop(p: 0.5, sx: 1.3, sy: 1.3, rotation: 50), GlyphStop(p: 1, sx: 1, sy: 1, rotation: 90)
    ]),
    "grid": GlyphSpec(duration: 0.55, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), GlyphStop(p: 0.35, sx: 0.78, sy: 0.78, rotation: -8),
        GlyphStop(p: 0.70, sx: 1.15, sy: 1.15, rotation: 6), .uniform(1)
    ]),
    "list": GlyphSpec(duration: 0.5, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), .uniform(0.30, 1, dx: 5), .uniform(0.60, 1, dx: -3), .uniform(1)
    ]),
    "playlist": GlyphSpec(duration: 0.5, c1: ease, c2: CGPoint(x: 0.2, y: 1.0), stops: [
        .uniform(0), .uniform(0.30, 1, dx: 5), .uniform(0.60, 1, dx: -3), .uniform(1)
    ])
]

/// The mock's per-glyph `i-*` animation id for an icon-picker id. Unknown glyphs pop.
func derbisGlyphMotionID(for iconID: String?) -> String {
    switch iconID {
    case DerbisDockIcons.home: return "home"
    case DerbisDockIcons.list: return "list"
    case DerbisDockIcons.search: return "search"
    case DerbisDockIcons.playlist: return "playlist"
    case DerbisDockIcons.plus: return "plus"
    case DerbisDockIcons.heart: return "heart"
    case DerbisDockIcons.note: return "note"
    case DerbisDockIcons.star: return "star"
    case DerbisDockIcons.download: return "download"
    case DerbisDockIcons.clock: return "clock"
    case DerbisDockIcons.bolt: return "bolt"
    case DerbisDockIcons.grid: return "grid"
    default: return "pop"
    }
}

func derbisGlyphPose(iconID: String?, at progress: CGFloat) -> GlyphPose {
    let spec = glyphSpecs[derbisGlyphMotionID(for: iconID)] ?? glyphSpecs["pop"]!
    let stops = spec.stops
    let p = min(max(Double(progress), 0), 1)
    if p <= stops[0].p { return pose(from: stops[0]) }
    if p >= stops[stops.count - 1].p { return pose(from: stops[stops.count - 1]) }
    for index in 1..<stops.count {
        let upper = stops[index]
        if p <= upper.p {
            let lower = stops[index - 1]
            let span = upper.p - lower.p
            let local = span <= 0 ? 1 : (p - lower.p) / span
            return GlyphPose(
                scaleX: CGFloat(lower.sx + (upper.sx - lower.sx) * local),
                scaleY: CGFloat(lower.sy + (upper.sy - lower.sy) * local),
                rotation: lower.rotation + (upper.rotation - lower.rotation) * local,
                dx: lower.dx + (upper.dx - lower.dx) * CGFloat(local),
                dy: lower.dy + (upper.dy - lower.dy) * CGFloat(local)
            )
        }
    }
    return GlyphPose()
}

private func pose(from stop: GlyphStop) -> GlyphPose {
    GlyphPose(scaleX: CGFloat(stop.sx), scaleY: CGFloat(stop.sy), rotation: stop.rotation, dx: stop.dx, dy: stop.dy)
}

/// Applies the mock's per-glyph keyframes. Increment `trigger` to (re)play — the caller
/// decides when to fire (selection change, same-tap replay, icon-picker selection,
/// toggle-on), mirroring how the mock's `svg.play` class + reflow restart works.
private struct DerbisGlyphMotionModifier: ViewModifier {
    let iconID: String?
    let trigger: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var progress: CGFloat = 1

    func body(content: Content) -> some View {
        let spec = glyphSpecs[derbisGlyphMotionID(for: iconID)] ?? glyphSpecs["pop"]!
        let pose = derbisGlyphPose(iconID: iconID, at: reduceMotion ? 1 : progress)
        content
            .scaleEffect(x: pose.scaleX, y: pose.scaleY)
            .rotationEffect(.degrees(pose.rotation))
            .offset(x: pose.dx, y: pose.dy)
            .onChange(of: trigger) { _, _ in
                guard !reduceMotion else { return }
                progress = 0
                DispatchQueue.main.async {
                    withAnimation(.timingCurve(spec.c1.x, spec.c1.y, spec.c2.x, spec.c2.y, duration: spec.duration)) {
                        progress = 1
                    }
                }
            }
    }
}

extension View {
    func derbisGlyphMotion(iconID: String?, trigger: Int) -> some View {
        modifier(DerbisGlyphMotionModifier(iconID: iconID, trigger: trigger))
    }

    /// The mock's `.pop` for newly inserted pill buttons (0% scale .6, opacity 0).
    /// Plays once when the view first appears; skipped under reduced motion.
    func derbisAppearPop() -> some View {
        modifier(DerbisAppearPopModifier())
    }

    /// The mock's `.shake` (±5 px horizontal wobble, .3s) for limit violations —
    /// five-tab cap and the one-tab floor.
    func derbisShake(trigger: Int) -> some View {
        modifier(DerbisShakeModifier(trigger: trigger))
    }
}

private struct DerbisAppearPopModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown: CGFloat = 1

    func body(content: Content) -> some View {
        content
            .scaleEffect(0.6 + 0.4 * (reduceMotion ? 1 : shown))
            .opacity(reduceMotion ? 1 : shown)
            .onAppear {
                guard !reduceMotion else { shown = 1; return }
                shown = 0
                withAnimation(.timingCurve(0.3, 1.0, 0.5, 1.0, duration: 0.35)) { shown = 1 }
            }
    }
}

private struct DerbisShakeModifier: ViewModifier {
    let trigger: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var progress: CGFloat = 1

    private func offset(at t: CGFloat) -> CGFloat {
        // 25% → -5, 75% → +5 (mock keyframes), in points.
        let lo: CGFloat = -5, hi: CGFloat = 5
        if t <= 0 { return 0 }
        if t < 0.25 { return lo * (t / 0.25) }
        if t < 0.75 { return lo + (hi - lo) * ((t - 0.25) / 0.5) }
        if t < 1 { return hi * (1 - (t - 0.75) / 0.25) }
        return 0
    }

    func body(content: Content) -> some View {
        content
            .offset(x: reduceMotion ? 0 : offset(at: progress))
            .onChange(of: trigger) { _, _ in
                guard !reduceMotion else { return }
                progress = 0
                withAnimation(.linear(duration: 0.3)) { progress = 1 }
            }
    }
}

// MARK: - Check/radio draw (mock .check.draw / .radio.draw)

/// A circle whose tick draws itself (trim 0→1, .38s easeOut, 60 ms delay) with the
/// mock's .bump scale envelope (.78 → 1 via cubic-bezier(.3, 1.6, .5, 1)).
struct DerbisCheckCircle: View {
    let enabled: Bool
    var size: CGFloat = 24
    var lineWidth: CGFloat = 2.2
    var tint: Color
    var track: Color = .white.opacity(0.35)
    /// false = radio look (outline + tick in `tint`, never filled)
    var filled: Bool = true

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var draw: CGFloat = 1
    @State private var bump: CGFloat = 1
    @State private var seeded = false

    var body: some View {
        ZStack {
            Circle()
                .strokeBorder(enabled ? tint : track, lineWidth: lineWidth * 0.72)
                .background(Circle().fill(enabled && filled ? tint : Color.clear))
            CheckTick()
                .trim(from: 0, to: enabled ? draw : 0)
                .stroke(filled ? .white : tint, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round, lineJoin: .round))
                .padding(size * 0.22)
        }
        .frame(width: size, height: size)
        .scaleEffect(bump)
        .onAppear {
            seeded = true
            draw = enabled ? 1 : 0
            bump = 1
        }
        .onChange(of: enabled) { _, newValue in
            guard seeded else { return }
            if newValue {
                if reduceMotion {
                    draw = 1
                    bump = 1
                    return
                }
                draw = 0
                bump = 0.78
                withAnimation(.timingCurve(0.3, 1.6, 0.5, 1, duration: 0.42)) { bump = 1 }
                withAnimation(.easeOut(duration: 0.38).delay(0.06)) { draw = 1 }
            } else {
                draw = 0
            }
        }
    }
}

private struct CheckTick: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.width * 0.08, y: rect.height * 0.55))
        path.addLine(to: CGPoint(x: rect.width * 0.40, y: rect.height * 0.85))
        path.addLine(to: CGPoint(x: rect.width * 0.92, y: rect.height * 0.15))
        return path
    }
}
