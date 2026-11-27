import SwiftUI
import UIKit

// The optical material behind every Liquid Glass surface.
//
// The previous generation of this file stacked a translucent white film on top of a
// UIBlurEffect. The blur sampled the page, the page was a near-black gradient, so every
// panel resolved to the same flat smoke colour no matter what was playing. That is the
// difference the Android build solved by recording the page into a backdrop layer: glass
// only reads as glass when it has something with structure behind it to bend.
//
// UIKit on iOS 16 has no public backdrop sampler, so this builds the structure instead of
// sampling it. Each surface renders its own small "field" — the active skin's atmosphere
// plus a heavily blurred bloom of the current artwork and the accent ramps — and then puts
// a real `.ultraThinMaterial` over that. SwiftUI materials do blur what is composited
// beneath them inside the same window, so the frost, the vibrancy and the colour bleed are
// all genuinely computed, not faked with a flat tint. The rim, the lens band and the
// dispersion are drawn on top, which is the part UIBlurEffect never had.
//
// Tiers gate cost, never legibility: every tier still renders a frosted, rimmed surface.

enum DerbisGlassTier: Int, Comparable, Sendable {
    /// Lens band, chromatic dispersion and a living specular highlight. Flagship devices.
    case full = 2
    /// Specular rim and artwork bloom, no dispersion.
    case balanced = 1
    /// Atmosphere only. Low Power Mode, Reduce Motion or a crowded run loop.
    case efficient = 0

    static func < (lhs: DerbisGlassTier, rhs: DerbisGlassTier) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

/// How much of the field a surface is allowed to draw.
enum DerbisGlassIntensity: Sendable {
    /// Docks, tab bars, mini players, sheets. Full bloom plus lens.
    case chrome
    /// In-page cards. Bloom at reduced strength, no lens band.
    case card
    /// Chips, capsules and small circular controls.
    case control
}

// MARK: - Resolution

enum DerbisGlassResolver {
    static func tier(
        reduceTransparency: Bool,
        reduceMotion: Bool,
        lowPowerMode: Bool
    ) -> DerbisGlassTier {
        if reduceTransparency { return .efficient }
        if ProcessInfo.processInfo.isLowPowerModeEnabled || lowPowerMode { return .efficient }
        if reduceMotion { return .balanced }
        return .full
    }
}

// MARK: - Palette

/// Everything a glass surface needs to look like it is sitting on the app, resolved once
/// per surface rather than per gradient stop.
struct DerbisGlassPalette {
    let isLight: Bool
    let base: Color
    let bloomPrimary: Color
    let bloomSecondary: Color
    let accent: Color
    let secondaryAccent: Color
    let artwork: UIImage?

    static func resolve(theme: ThemeManager) -> DerbisGlassPalette {
        let isLight = theme.isLightSkin
        let ambient = theme.activeAmbientColors
        return DerbisGlassPalette(
            isLight: isLight,
            base: isLight ? Color.white : Color(hex: 0x080A10),
            bloomPrimary: ambient.primary,
            bloomSecondary: ambient.secondary,
            accent: theme.accent,
            secondaryAccent: theme.secondaryAccent,
            artwork: theme.glassArtwork
        )
    }

}

// MARK: - Field

/// The structured substrate a glass surface frosts. Drawn underneath the material, slightly
/// oversized so the blur has colour to pull in from beyond the shape's edge.
struct DerbisGlassField: View {
    let palette: DerbisGlassPalette
    let intensity: DerbisGlassIntensity
    var bloomStrength: Double = 1.0

    private var overscan: CGFloat {
        intensity == .control ? 10 : 22
    }

    private var showsArtwork: Bool {
        guard let artwork = palette.artwork else { return false }
        return intensity != .control && artwork.size.width > 0
    }

    var body: some View {
        ZStack {
            palette.base

            RadialGradient(
                colors: [
                    palette.bloomPrimary.opacity(0.62 * bloomStrength),
                    palette.bloomPrimary.opacity(0.10 * bloomStrength),
                    .clear
                ],
                center: .topLeading,
                startRadius: 2,
                endRadius: 320
            )

            RadialGradient(
                colors: [
                    palette.bloomSecondary.opacity(0.48 * bloomStrength),
                    .clear
                ],
                center: .bottomTrailing,
                startRadius: 6,
                endRadius: 300
            )

            if intensity == .chrome {
                RadialGradient(
                    colors: [
                        palette.secondaryAccent.opacity(0.30 * bloomStrength),
                        .clear
                    ],
                    center: .bottomLeading,
                    startRadius: 4,
                    endRadius: 240
                )
            }

            if showsArtwork, let artwork = palette.artwork {
                Image(uiImage: artwork)
                    .resizable()
                    .scaledToFill()
                    .blur(radius: intensity == .chrome ? 34 : 26)
                    .saturation(1.75)
                    .opacity(intensity == .chrome ? 0.58 : 0.34)
                    .blendMode(.plusLighter)
                    .allowsHitTesting(false)
            }
        }
        .padding(-overscan)
        .drawingGroup(opaque: false, colorMode: .extendedLinear)
    }
}

// MARK: - Specular rim

/// The lit edge. A single gradient stroke reads as paint; glass needs the highlight to run
/// along one side, break at the opposite corner and pick up a faint bounce from the accent.
struct DerbisSpecularRim: View {
    let shape: AnyShape
    let palette: DerbisGlassPalette
    let prominent: Bool
    var drift: Bool = false

    @State private var phase: Double = 0

    private var topAlpha: Double { prominent ? 0.62 : 0.44 }
    private var bottomAlpha: Double { prominent ? 0.22 : 0.14 }

    var body: some View {
        ZStack {
            shape
                .stroke(
                    LinearGradient(
                        stops: [
                            .init(color: .white.opacity(topAlpha), location: 0.0),
                            .init(color: .white.opacity(0.06), location: 0.42),
                            .init(color: palette.accent.opacity(drift ? 0.26 : 0.18), location: 0.72),
                            .init(color: .white.opacity(bottomAlpha), location: 1.0)
                        ],
                        startPoint: UnitPoint(x: 0.06 + 0.10 * phase, y: 0),
                        endPoint: UnitPoint(x: 0.94 - 0.10 * phase, y: 1)
                    ),
                    lineWidth: prominent ? 1.1 : 0.85
                )

            if palette.isLight {
                shape
                    .stroke(Color.black.opacity(0.10), lineWidth: 0.5)
                    .offset(y: 0.6)
                    .blendMode(.multiply)
            }
        }
        .allowsHitTesting(false)
        .onAppear {
            guard drift else { return }
            withAnimation(.easeInOut(duration: 7.5).repeatForever(autoreverses: true)) {
                phase = 1
            }
        }
    }
}

// MARK: - Lens band

/// The refraction band. A real optical lens bends the backdrop hardest along its rim and
/// splits it into colour fringes while doing it; that split is the single most recognisable
/// cue of Apple's material and the thing a translucent film can never imitate.
struct DerbisLensBand: View {
    let shape: AnyShape
    let palette: DerbisGlassPalette
    let bandWidth: CGFloat

    var body: some View {
        ZStack {
            shape
                .stroke(
                    LinearGradient(
                        colors: [
                            palette.accent.opacity(0.30),
                            Color.white.opacity(0.14),
                            palette.secondaryAccent.opacity(0.24)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: bandWidth
                )
                .offset(x: 0.7)

            shape
                .stroke(
                    LinearGradient(
                        colors: [
                            palette.secondaryAccent.opacity(0.22),
                            Color.white.opacity(0.10),
                            palette.accent.opacity(0.26)
                        ],
                        startPoint: .bottomTrailing,
                        endPoint: .topLeading
                    ),
                    lineWidth: bandWidth
                )
                .offset(x: -0.7)
        }
        .blendMode(.plusLighter)
        .allowsHitTesting(false)
    }
}

// MARK: - Sheen

/// A short vertical sheen across the upper third, the way a pane catches a ceiling light.
struct DerbisGlassSheen: View {
    let shape: AnyShape
    let prominent: Bool

    var body: some View {
        shape
            .fill(
                LinearGradient(
                    stops: [
                        .init(color: .white.opacity(prominent ? 0.16 : 0.10), location: 0.0),
                        .init(color: .white.opacity(0.03), location: 0.26),
                        .init(color: .clear, location: 0.52)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .allowsHitTesting(false)
    }
}

// MARK: - The material

/// The complete glass surface: a structured field, a genuinely blurred material over it,
/// the lit rim, the lens band and a soft drop shadow.
///
/// This is a `View` rather than a bare `.background` so the tint, the bloom and the rim can
/// each react to the current track without the caller re-resolving anything.
struct DerbisOpticalMaterial: View {
    let shape: AnyShape
    let intensity: DerbisGlassIntensity
    var prominent: Bool = false
    var tint: Color? = nil

    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ObservedObject private var power = DerbisPowerMonitor.shared

    private var tier: DerbisGlassTier {
        if !theme.opticalGlassAllowed { return .efficient }
        switch theme.glassEffects {
        case .off:
            return .efficient
        case .alwaysOn:
            return reduceTransparency ? .efficient : .full
        case .automatic:
            return DerbisGlassResolver.tier(
                reduceTransparency: reduceTransparency,
                reduceMotion: reduceMotion,
                lowPowerMode: power.isLowPowerMode
            )
        }
    }

    private var palette: DerbisGlassPalette {
        DerbisGlassPalette.resolve(theme: theme)
    }

    private var isOpaque: Bool {
        reduceTransparency || theme.glassPrefersOpaque
    }

    /// The frozen film sits between the field and the content. On the light skins it has to
    /// be far denser or the white type keeps the espresso ink from clearing contrast.
    private var filmColor: Color {
        if let tint = tint, prominent {
            return tint.opacity(palette.isLight ? 0.90 : 0.74)
        }
        if let tint = tint {
            return tint.opacity(palette.isLight ? 0.70 : 0.30)
        }
        let alpha = theme.glassFilmAlpha
        return palette.isLight ? Color.white.opacity(min(0.96, alpha + 0.34)) : Color.white.opacity(alpha * 0.34)
    }

    private var material: Material {
        if prominent {
            return palette.isLight ? .thickMaterial : .regularMaterial
        }
        switch intensity {
        case .chrome:
            return palette.isLight ? .regularMaterial : .thinMaterial
        case .card:
            return palette.isLight ? .thinMaterial : .ultraThinMaterial
        case .control:
            return palette.isLight ? .thinMaterial : .ultraThinMaterial
        }
    }

    private var showsLens: Bool {
        guard tier >= .balanced else { return false }
        guard intensity == .chrome || intensity == .control else { return false }
        return !isOpaque && theme.glassRefractionScale > 0.05
    }

    private var showsDispersion: Bool {
        showsLens && tier == .full && theme.glassEffects != .off
    }

    private var bandWidth: CGFloat {
        let base: CGFloat = intensity == .control ? 3.0 : 5.0
        return base * CGFloat(0.45 + theme.glassRefractionScale * 0.85)
    }

    var body: some View {
        ZStack {
            if isOpaque {
                shape.fill(
                    palette.isLight
                        ? Color(hex: 0xF4EFE6)
                        : Color(hex: 0x12141B)
                )
                shape.stroke(Color.white.opacity(palette.isLight ? 0.0 : 0.18), lineWidth: 0.8)
            } else {
                DerbisGlassField(
                    palette: palette,
                    intensity: intensity,
                    bloomStrength: prominent ? 1.0 : (intensity == .chrome ? 0.92 : 0.62)
                )
                .clipShape(shape)

                material
                    .clipShape(shape)

                shape.fill(filmColor)

                if showsLens {
                    DerbisLensBand(
                        shape: shape,
                        palette: palette,
                        bandWidth: showsDispersion ? bandWidth : bandWidth * 0.55
                    )
                }

                DerbisGlassSheen(shape: shape, prominent: prominent || intensity == .chrome)

                DerbisSpecularRim(
                    shape: shape,
                    palette: palette,
                    prominent: prominent,
                    drift: tier == .full && !reduceMotion && intensity == .chrome
                )
            }
        }
        .shadow(
            color: Color.black.opacity(palette.isLight ? 0.10 : (intensity == .chrome ? 0.46 : 0.30)),
            radius: intensity == .chrome ? 16 : 8,
            x: 0,
            y: intensity == .chrome ? 8 : 4
        )
        .animation(.easeInOut(duration: 0.35), value: palette.bloomPrimary)
        .allowsHitTesting(false)
    }
}

// MARK: - Power state

/// Playback is the one thing that notices Low Power Mode on its own, so the material reads it
/// from here instead of polling `ProcessInfo` on every one of the sixty-odd glass surfaces.
final class DerbisPowerMonitor: ObservableObject {
    static let shared = DerbisPowerMonitor()

    @Published private(set) var isLowPowerMode: Bool

    private var observer: NSObjectProtocol?

    private init() {
        isLowPowerMode = ProcessInfo.processInfo.isLowPowerModeEnabled
        observer = NotificationCenter.default.addObserver(
            forName: .NSProcessInfoPowerStateDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            // The notification arrives on the main queue; hop only to make the snapshot
            // read on the same turn the rest of the UI sees it.
            DispatchQueue.main.async {
                self?.isLowPowerMode = ProcessInfo.processInfo.isLowPowerModeEnabled
            }
        }
    }
}
