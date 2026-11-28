/*
 * The Liquid Glass page background from the glass-skin mock, fed by ThemeManager.
 *
 * Four modes share one stack: a black canvas, a mode layer and a dimming veil. Glow paints a
 * static radial field in the accent pair; Animated drifts four blobs through the frame (skipped
 * when background motion is off); Blur paints the synced now-playing artwork, scaled and heavily
 * blurred, plus an accent tint; Custom loads the user-picked image from the app's Documents
 * directory and does the same. Every mode honours intensity (how much of the colour field shows),
 * blur (Blur and Custom), brightness (the veil) and the accent override (hue/lightness or
 * "follow the artwork").
 *
 * Mirrors Android's GlassBackgroundEngine; keep the two in step when changing behaviour.
 */
import SwiftUI

struct GlassBackgroundView: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var player = AudioPlayerEngine.shared
    @State private var customImage: UIImage? = nil

    var body: some View {
        let accents = glassAccentPair()
        ZStack {
            Color.black
            modeLayer(accents)
            if veil > 0.01 {
                Color.black.opacity(veil)
            }
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .task(id: theme.glassBackgroundImagePath) {
            let path = theme.glassBackgroundImagePath
            guard !path.isEmpty else {
                customImage = nil
                return
            }
            customImage = UIImage(contentsOfFile: path)
        }
    }

    // MARK: - Shared math (kept identical to the Android engine)

    private var veil: Double {
        let brightness = min(100, max(15, theme.glassBackgroundBrightness))
        return Double(100 - brightness) / 100.0 * 0.85
    }

    private var intensity: Double {
        Double(min(100, max(20, theme.glassBackgroundIntensity))) / 100.0
    }

    private func glassAccentPair() -> (primary: Color, secondary: Color) {
        let hue = theme.glassAccentHue
        if hue >= 0 {
            let lightness = min(100, max(0, theme.glassAccentLightness)) / 100.0
            let primary = Color(hue: Double(hue % 360) / 360.0, saturation: 0.72, brightness: lightness)
            let secondaryHue = Double((hue + 28) % 360) / 360.0
            let secondary = Color(hue: secondaryHue, saturation: 0.65,
                                  brightness: max(0.12, lightness * 0.45))
            return (primary, secondary)
        }
        return (player.ambientPrimary, player.ambientSecondary)
    }

    // MARK: - Mode layers

    @ViewBuilder
    private func modeLayer(_ accents: (primary: Color, secondary: Color)) -> some View {
        switch theme.glassBackgroundMode {
        case .glow:
            glowLayer(accents)
        case .animated:
            animatedLayer(accents)
        case .artworkBlur:
            artworkBlurLayer(accents)
        case .customImage:
            customImageLayer(accents)
        }
    }

    private func glowLayer(_ accents: (primary: Color, secondary: Color)) -> some View {
        GeometryReader { proxy in
            let maxSide = max(proxy.size.width, proxy.size.height)
            ZStack {
                RadialGradient(
                    colors: [accents.primary.opacity(0.95 * intensity), accents.primary.opacity(0.27 * intensity), .clear],
                    center: UnitPoint(x: 0.12, y: 0.14),
                    startRadius: 0,
                    endRadius: maxSide * 0.78
                )
                RadialGradient(
                    colors: [accents.secondary.opacity(0.9 * intensity), accents.secondary.opacity(0.25 * intensity), .clear],
                    center: UnitPoint(x: 0.96, y: 0.68),
                    startRadius: 0,
                    endRadius: maxSide * 0.66
                )
                RadialGradient(
                    colors: [accents.primary.opacity(0.75 * intensity), accents.primary.opacity(0.21 * intensity), .clear],
                    center: UnitPoint(x: 0.42, y: 1.02),
                    startRadius: 0,
                    endRadius: maxSide * 0.55
                )
            }
        }
    }

    private func animatedLayer(_ accents: (primary: Color, secondary: Color)) -> some View {
        if theme.glassBackgroundMotion {
            return AnyView(
                TimelineView(.animation) { timeline in
                    blobCanvas(accents, time: timeline.date.timeIntervalSinceReferenceDate)
                }
            )
        }
        // Motion off: hold the frame at the start position, like the frozen first keyframe.
        return AnyView(blobCanvas(accents, time: 0))
    }

    private func blobCanvas(_ accents: (primary: Color, secondary: Color), time: Double) -> some View {
        let t1 = (1 - cos(2 * Double.pi * time / 11.0)) / 2
        let t2 = (1 - cos(2 * Double.pi * time / 14.0)) / 2
        let t3 = (1 - cos(2 * Double.pi * time / 9.0)) / 2
        let inten = intensity
        return Canvas { context, size in
            let w = Double(size.width)
            let h = Double(size.height)
            let span = max(w, h)
            func point(_ x: Double, _ y: Double) -> CGPoint { CGPoint(x: x, y: y) }
            func radius(_ fraction: Double) -> CGFloat { CGFloat(span * fraction) }
            drawBlob(context, center: point(w * (0.08 + 0.42 * t1), h * (0.10 + 0.42 * t1 * t1)),
                     radius: radius(0.72), color: accents.primary, alpha: 0.92 * inten)
            drawBlob(context, center: point(w * (0.92 - 0.40 * t2), h * (0.88 - 0.36 * t2)),
                     radius: radius(0.68), color: accents.secondary, alpha: 0.90 * inten)
            drawBlob(context, center: point(w * (0.18 + 0.36 * t3), h * (0.46 + 0.18 * t3)),
                     radius: radius(0.52), color: accents.primary, alpha: 0.72 * inten)
            drawBlob(context, center: point(w * (0.78 - 0.28 * t1), h * (0.14 + 0.30 * t2)),
                     radius: radius(0.46), color: accents.secondary, alpha: 0.60 * inten)
        }
    }

    private func drawBlob(_ context: GraphicsContext, center: CGPoint, radius: CGFloat, color: Color, alpha: Double) {
        let shading = GraphicsContext.Shading.radialGradient(
            Gradient(colors: [color.opacity(alpha), color.opacity(alpha * 0.28), .clear]),
            center: center,
            startRadius: 0,
            endRadius: radius
        )
        context.fill(
            Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)),
            with: shading
        )
    }

    private func artworkBlurLayer(_ accents: (primary: Color, secondary: Color)) -> some View {
        ZStack {
            if let artwork = theme.glassArtwork {
                Image(uiImage: artwork)
                    .resizable()
                    .scaledToFill()
                    .blur(radius: blurRadius)
                    .saturation(1.15)
                    .opacity(0.35 + 0.55 * intensity)
            } else {
                // No artwork playing: the procedural gradient field the mock draws on canvas.
                LinearGradient(
                    colors: [accents.primary, Color(hex: 0x101820), accents.secondary],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .opacity(0.9 * intensity)
            }
            GeometryReader { proxy in
                let maxSide = max(proxy.size.width, proxy.size.height)
                RadialGradient(
                    colors: [accents.primary.opacity(0.28 * intensity), .clear],
                    center: UnitPoint(x: 0.2, y: 0.12),
                    startRadius: 0,
                    endRadius: maxSide * 0.7
                )
            }
        }
        .clipped()
    }

    private func customImageLayer(_ accents: (primary: Color, secondary: Color)) -> some View {
        ZStack {
            if let customImage {
                Image(uiImage: customImage)
                    .resizable()
                    .scaledToFill()
                    .blur(radius: blurRadius)
                    .opacity(0.45 + 0.45 * intensity)
            } else {
                glowLayer(accents)
            }
        }
        .clipped()
    }

    private var blurRadius: CGFloat {
        CGFloat(min(80, max(0, theme.glassBackgroundBlur)))
    }
}
