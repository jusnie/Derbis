import SwiftUI
import UIKit
import CoreText

@MainActor
final class ThemeManager: ObservableObject {
    @Published var skin: DerbisSkin {
        didSet { UserDefaults.standard.set(skin.rawValue, forKey: "derbis.skin") }
    }

    @Published var fontChoice: DerbisFontChoice {
        didSet { UserDefaults.standard.set(fontChoice.rawValue, forKey: "derbis.fontChoice") }
    }

    @Published var highQualityAudio: Bool {
        didSet { UserDefaults.standard.set(highQualityAudio, forKey: "derbis.highQualityAudio") }
    }

    @Published var endlessRadioEnabled: Bool {
        didSet { UserDefaults.standard.set(endlessRadioEnabled, forKey: "derbis.endlessRadio") }
    }

    @Published var autoLoadLyrics: Bool {
        didSet { UserDefaults.standard.set(autoLoadLyrics, forKey: "derbis.autoLoadLyrics") }
    }

    @Published var motionCanvasEnabled: Bool {
        didSet { UserDefaults.standard.set(motionCanvasEnabled, forKey: "derbis.motionCanvasEnabled") }
    }

    @Published var motionCanvasStyle: MotionCanvasStyle {
        didSet { UserDefaults.standard.set(motionCanvasStyle.rawValue, forKey: "derbis.motionCanvasStyle") }
    }

    @Published var backdropStyle: PlayerBackdropStyle {
        didSet { UserDefaults.standard.set(backdropStyle.rawValue, forKey: "derbis.backdropStyle") }
    }

    @Published var showLyricsRomanization: Bool {
        didSet { UserDefaults.standard.set(showLyricsRomanization, forKey: "derbis.showLyricsRomanization") }
    }

    @Published var showLyricsTranslation: Bool {
        didSet { UserDefaults.standard.set(showLyricsTranslation, forKey: "derbis.showLyricsTranslation") }
    }

    @Published var autoHideLyricsControls: Bool {
        didSet { UserDefaults.standard.set(autoHideLyricsControls, forKey: "derbis.autoHideLyricsControls") }
    }

    @Published var lockScreenLiveLyrics: Bool {
        didSet { UserDefaults.standard.set(lockScreenLiveLyrics, forKey: "derbis.lockScreenLiveLyrics") }
    }

    @Published var lockScreenLyricArt: Bool {
        didSet { UserDefaults.standard.set(lockScreenLyricArt, forKey: "derbis.lockScreenLyricArt") }
    }

    @Published var playerGesturesEnabled: Bool {
        didSet { UserDefaults.standard.set(playerGesturesEnabled, forKey: "derbis.playerGesturesEnabled") }
    }

    @Published var liquidGlassEnabled: Bool {
        didSet { UserDefaults.standard.set(liquidGlassEnabled, forKey: "derbis.liquidGlassEnabled") }
    }

    @Published var ambientGlassBackdropEnabled: Bool {
        didSet { UserDefaults.standard.set(ambientGlassBackdropEnabled, forKey: "derbis.ambientGlassBackdropEnabled") }
    }

    @Published var coverFollowsAccent: Bool {
        didSet { UserDefaults.standard.set(coverFollowsAccent, forKey: "derbis.coverFollowsAccent") }
    }

    @Published var miniPlayerProgressThickness: Double {
        didSet { UserDefaults.standard.set(miniPlayerProgressThickness, forKey: "derbis.miniPlayerProgressThickness") }
    }

    @Published var tabBarMinimizesOnScroll: Bool {
        didSet { UserDefaults.standard.set(tabBarMinimizesOnScroll, forKey: "derbis.tabBarMinimizesOnScroll") }
    }

    @Published var dynamicCoverAccent: Color? = nil

    init() {
        let savedSkin = UserDefaults.standard.string(forKey: "derbis.skin") ?? DerbisSkin.liquidGlass.rawValue
        self.skin = DerbisSkin(rawValue: savedSkin) ?? .liquidGlass

        let savedFont = UserDefaults.standard.string(forKey: "derbis.fontChoice") ?? DerbisFontChoice.skinDefault.rawValue
        self.fontChoice = DerbisFontChoice(rawValue: savedFont) ?? .skinDefault

        if UserDefaults.standard.object(forKey: "derbis.highQualityAudio") == nil {
            self.highQualityAudio = true
        } else {
            self.highQualityAudio = UserDefaults.standard.bool(forKey: "derbis.highQualityAudio")
        }

        if UserDefaults.standard.object(forKey: "derbis.endlessRadio") == nil {
            self.endlessRadioEnabled = true
        } else {
            self.endlessRadioEnabled = UserDefaults.standard.bool(forKey: "derbis.endlessRadio")
        }

        if UserDefaults.standard.object(forKey: "derbis.autoLoadLyrics") == nil {
            self.autoLoadLyrics = true
        } else {
            self.autoLoadLyrics = UserDefaults.standard.bool(forKey: "derbis.autoLoadLyrics")
        }

        if UserDefaults.standard.object(forKey: "derbis.motionCanvasEnabled") == nil {
            self.motionCanvasEnabled = true
        } else {
            self.motionCanvasEnabled = UserDefaults.standard.bool(forKey: "derbis.motionCanvasEnabled")
        }

        let savedStyle = UserDefaults.standard.string(forKey: "derbis.motionCanvasStyle") ?? MotionCanvasStyle.immersive.rawValue
        self.motionCanvasStyle = MotionCanvasStyle(rawValue: savedStyle) ?? .immersive

        let savedBackdrop = UserDefaults.standard.string(forKey: "derbis.backdropStyle") ?? PlayerBackdropStyle.animated.rawValue
        self.backdropStyle = PlayerBackdropStyle(rawValue: savedBackdrop) ?? .animated

        if UserDefaults.standard.object(forKey: "derbis.showLyricsRomanization") == nil {
            self.showLyricsRomanization = true
        } else {
            self.showLyricsRomanization = UserDefaults.standard.bool(forKey: "derbis.showLyricsRomanization")
        }

        if UserDefaults.standard.object(forKey: "derbis.showLyricsTranslation") == nil {
            self.showLyricsTranslation = false
        } else {
            self.showLyricsTranslation = UserDefaults.standard.bool(forKey: "derbis.showLyricsTranslation")
        }

        if UserDefaults.standard.object(forKey: "derbis.autoHideLyricsControls") == nil {
            self.autoHideLyricsControls = false
        } else {
            self.autoHideLyricsControls = UserDefaults.standard.bool(forKey: "derbis.autoHideLyricsControls")
        }

        if UserDefaults.standard.object(forKey: "derbis.lockScreenLiveLyrics") == nil {
            self.lockScreenLiveLyrics = true
        } else {
            self.lockScreenLiveLyrics = UserDefaults.standard.bool(forKey: "derbis.lockScreenLiveLyrics")
        }

        if UserDefaults.standard.object(forKey: "derbis.lockScreenLyricArt") == nil {
            self.lockScreenLyricArt = false
        } else {
            self.lockScreenLyricArt = UserDefaults.standard.bool(forKey: "derbis.lockScreenLyricArt")
        }

        if UserDefaults.standard.object(forKey: "derbis.playerGesturesEnabled") == nil {
            self.playerGesturesEnabled = true
        } else {
            self.playerGesturesEnabled = UserDefaults.standard.bool(forKey: "derbis.playerGesturesEnabled")
        }

        if UserDefaults.standard.object(forKey: "derbis.liquidGlassEnabled") == nil {
            self.liquidGlassEnabled = true
        } else {
            self.liquidGlassEnabled = UserDefaults.standard.bool(forKey: "derbis.liquidGlassEnabled")
        }

        if UserDefaults.standard.object(forKey: "derbis.ambientGlassBackdropEnabled") == nil {
            self.ambientGlassBackdropEnabled = true
        } else {
            self.ambientGlassBackdropEnabled = UserDefaults.standard.bool(forKey: "derbis.ambientGlassBackdropEnabled")
        }

        if UserDefaults.standard.object(forKey: "derbis.coverFollowsAccent") == nil {
            self.coverFollowsAccent = false
        } else {
            self.coverFollowsAccent = UserDefaults.standard.bool(forKey: "derbis.coverFollowsAccent")
        }

        if UserDefaults.standard.object(forKey: "derbis.miniPlayerProgressThickness") == nil {
            self.miniPlayerProgressThickness = 2.0
        } else {
            let savedThickness = UserDefaults.standard.double(forKey: "derbis.miniPlayerProgressThickness")
            self.miniPlayerProgressThickness = min(5.0, max(0.0, savedThickness))
        }

        if UserDefaults.standard.object(forKey: "derbis.tabBarMinimizesOnScroll") == nil {
            self.tabBarMinimizesOnScroll = true
        } else {
            self.tabBarMinimizesOnScroll = UserDefaults.standard.bool(forKey: "derbis.tabBarMinimizesOnScroll")
        }
    }

    var usesLiquidGlass: Bool {
        !isPixelSkin && (liquidGlassEnabled || skin == .liquidGlass)
    }

    var usesPixelFont: Bool {
        switch fontChoice {
        case .pixelify:
            return true
        case .sfPro:
            return false
        case .skinDefault:
            return skin == .pixelUI
        }
    }

    var isPixelSkin: Bool {
        skin == .pixelUI
    }

    var colorScheme: ColorScheme {
        skin.isLight ? .light : .dark
    }

    var background: Color {
        switch skin {
        case .liquidGlass: return Color(hex: 0x000000)
        case .appleMusic13: return Color(hex: 0x000000)
        case .pixelUI: return Color(hex: 0xFFEDF1)
        case .derbisDefault: return Color(hex: 0x070709)
        case .lateNight: return Color(hex: 0x000000)
        }
    }

    var surface: Color {
        switch skin {
        case .liquidGlass: return Color.white.opacity(0.065)
        case .appleMusic13: return Color(hex: 0x1C1C1E)
        case .pixelUI: return Color(hex: 0xFFF8F6)
        case .derbisDefault: return Color(hex: 0x121218)
        case .lateNight: return Color(hex: 0x0E0E12)
        }
    }

    var elevatedSurface: Color {
        switch skin {
        case .liquidGlass: return Color.white.opacity(0.115)
        case .appleMusic13: return Color(hex: 0x2C2C2E)
        case .pixelUI: return Color(hex: 0xFFD9E2)
        case .derbisDefault: return Color(hex: 0x1B1B24)
        case .lateNight: return Color(hex: 0x17171E)
        }
    }

    var primaryText: Color {
        switch skin {
        case .pixelUI: return Color(hex: 0x5B2338)
        default: return Color.white
        }
    }

    var secondaryText: Color {
        switch skin {
        case .liquidGlass: return Color.white.opacity(0.65)
        case .appleMusic13: return Color.white.opacity(0.62)
        case .pixelUI: return Color(hex: 0x7E3B57)
        case .derbisDefault: return Color(hex: 0xA2A2B2)
        case .lateNight: return Color(hex: 0x9E9890)
        }
    }

    var accent: Color {
        if coverFollowsAccent, !isPixelSkin, let dynamic = dynamicCoverAccent {
            return dynamic
        }
        switch skin {
        case .liquidGlass: return Color(hex: 0xFC3C44)
        case .appleMusic13: return Color(hex: 0xFC3C44)
        case .pixelUI: return Color(hex: 0x9E1B44)
        case .derbisDefault: return Color(hex: 0xFA2D48)
        case .lateNight: return Color(hex: 0xF59E0B)
        }
    }

    var border: Color {
        switch skin {
        case .liquidGlass: return Color.white.opacity(0.15)
        case .appleMusic13: return Color.white.opacity(0.10)
        case .pixelUI: return Color(hex: 0xA84765)
        case .derbisDefault: return Color.white.opacity(0.08)
        case .lateNight: return Color(hex: 0xF59E0B).opacity(0.18)
        }
    }

    var cardRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .liquidGlass: return 18
        case .appleMusic13: return 14
        case .derbisDefault: return 16
        case .lateNight: return 12
        }
    }

    var artworkRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .liquidGlass: return 10
        case .appleMusic13: return 10
        case .derbisDefault: return 12
        case .lateNight: return 8
        }
    }

    var playerArtworkRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .liquidGlass: return 22
        case .appleMusic13: return 24
        case .derbisDefault: return 20
        case .lateNight: return 14
        }
    }

    func font(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        if usesPixelFont {
            let isBold: Bool = {
                switch weight {
                case .semibold, .bold, .heavy, .black:
                    return true
                default:
                    return false
                }
            }()
            let name = isBold ? "PixelifySans-Bold" : "PixelifySans-Regular"
            return Font.custom(name, size: size)
        }
        return Font.system(size: size, weight: weight, design: .default)
    }

    static func registerBundledFonts() {
        for file in ["pixelify_sans_regular", "pixelify_sans_bold"] {
            if let url = Bundle.main.url(forResource: file, withExtension: "ttf") {
                CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            }
        }
    }
}

extension Color {
    init(hex: UInt32, opacity: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: opacity)
    }
}

struct PixelGraphPaperBackground: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var player = AudioPlayerEngine.shared

    var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            if theme.isPixelSkin {
                Canvas { context, size in
                    let step: CGFloat = 20
                    var path = Path()
                    var x: CGFloat = 0
                    while x <= size.width {
                        path.move(to: CGPoint(x: x, y: 0))
                        path.addLine(to: CGPoint(x: x, y: size.height))
                        x += step
                    }
                    var y: CGFloat = 0
                    while y <= size.height {
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addLine(to: CGPoint(x: size.width, y: y))
                        y += step
                    }
                    context.stroke(path, with: .color(Color(hex: 0xA84765, opacity: 0.11)), lineWidth: 1)
                }
                .ignoresSafeArea()
                .allowsHitTesting(false)
            } else if theme.usesLiquidGlass && theme.ambientGlassBackdropEnabled {
                LinearGradient(
                    stops: [
                        .init(color: player.ambientPrimary.opacity(player.currentTrack != nil ? 0.56 : 0.24), location: 0.0),
                        .init(color: player.ambientSecondary.opacity(player.currentTrack != nil ? 0.34 : 0.14), location: 0.38),
                        .init(color: theme.background, location: 0.82)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                .allowsHitTesting(false)
            }
        }
    }
}

struct DerbisSurfaceCardModifier: ViewModifier {
    @EnvironmentObject private var theme: ThemeManager
    var elevated: Bool = false

    func body(content: Content) -> some View {
        let fill = elevated ? theme.elevatedSurface : theme.surface
        if theme.isPixelSkin {
            content
                .background(
                    ZStack {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.22))
                            .offset(x: 3, y: 3)
                        Rectangle()
                            .fill(fill)
                            .overlay(
                                Rectangle()
                                    .stroke(theme.border, lineWidth: 2)
                            )
                    }
                )
        } else if theme.skin == .liquidGlass {
            let shape = RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
            content
                .background(
                    shape
                        .fill(fill)
                        .overlay(
                            shape.strokeBorder(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(elevated ? 0.26 : 0.18),
                                        Color.white.opacity(0.04),
                                        Color.white.opacity(elevated ? 0.13 : 0.09)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 0.8
                            )
                        )
                )
        } else {
            content
                .background(
                    RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                        .fill(fill)
                        .overlay(
                            RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                                .stroke(theme.border, lineWidth: 1)
                        )
                )
        }
    }
}

extension View {
    func derbisCard(elevated: Bool = false) -> some View {
        modifier(DerbisSurfaceCardModifier(elevated: elevated))
    }
}

enum ArtworkPaletteExtractor {
    static func dominantColors(from image: UIImage) -> (primary: Color, secondary: Color) {
        guard let cgImage = image.cgImage else {
            return (Color(hex: 0x2A1A2E), Color(hex: 0x0C0C12))
        }
        let width = 16
        let height = 16
        var raw = [UInt8](repeating: 0, count: width * height * 4)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: &raw,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            return (Color(hex: 0x2A1A2E), Color(hex: 0x0C0C12))
        }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        var topR = 0.0, topG = 0.0, topB = 0.0, topCount = 0.0
        var botR = 0.0, botG = 0.0, botB = 0.0, botCount = 0.0

        for y in 0..<height {
            for x in 0..<width {
                let idx = (y * width + x) * 4
                let r = Double(raw[idx]) / 255.0
                let g = Double(raw[idx + 1]) / 255.0
                let b = Double(raw[idx + 2]) / 255.0
                let maxC = max(r, max(g, b))
                let minC = min(r, min(g, b))
                let satBoost = 1.0 + (maxC - minC) * 1.5
                if y < height / 2 {
                    topR += r * satBoost
                    topG += g * satBoost
                    topB += b * satBoost
                    topCount += satBoost
                } else {
                    botR += r * satBoost
                    botG += g * satBoost
                    botB += b * satBoost
                    botCount += satBoost
                }
            }
        }

        let r1 = min(0.68, (topR / max(1.0, topCount)) * 0.72)
        let g1 = min(0.68, (topG / max(1.0, topCount)) * 0.72)
        let b1 = min(0.68, (topB / max(1.0, topCount)) * 0.72)

        let r2 = min(0.45, (botR / max(1.0, botCount)) * 0.45)
        let g2 = min(0.45, (botG / max(1.0, botCount)) * 0.45)
        let b2 = min(0.45, (botB / max(1.0, botCount)) * 0.45)

        return (
            Color(.sRGB, red: r1, green: g1, blue: b1, opacity: 1.0),
            Color(.sRGB, red: r2, green: g2, blue: b2, opacity: 1.0)
        )
    }

    static func vibrantAccent(from image: UIImage) -> Color {
        guard let cgImage = image.cgImage else {
            return Color(hex: 0xFC3C44)
        }
        let side = 16
        var raw = [UInt8](repeating: 0, count: side * side * 4)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: &raw,
            width: side,
            height: side,
            bitsPerComponent: 8,
            bytesPerRow: side * 4,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            return Color(hex: 0xFC3C44)
        }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: side, height: side))

        var bestR = 0.98, bestG = 0.23, bestB = 0.27, bestScore = 0.0
        for idx in stride(from: 0, to: raw.count, by: 4) {
            let r = Double(raw[idx]) / 255.0
            let g = Double(raw[idx + 1]) / 255.0
            let b = Double(raw[idx + 2]) / 255.0
            let maxC = max(r, max(g, b))
            let minC = min(r, min(g, b))
            let chroma = maxC - minC
            let lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
            guard lum > 0.14 && lum < 0.88 else { continue }
            let score = chroma * 2.2 + (1.0 - abs(lum - 0.55)) * 0.4
            if score > bestScore {
                bestScore = score
                bestR = r
                bestG = g
                bestB = b
            }
        }

        let maxC = max(bestR, max(bestG, bestB))
        let boost = maxC < 0.65 ? (0.78 / max(0.2, maxC)) : 1.0
        return Color(
            .sRGB,
            red: min(1.0, bestR * boost),
            green: min(1.0, bestG * boost),
            blue: min(1.0, bestB * boost),
            opacity: 1.0
        )
    }
}
