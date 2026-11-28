import SwiftUI
import UIKit
import CoreText

@MainActor
final class ThemeManager: ObservableObject {
    @Published var skin: DerbisSkin {
        didSet { UserDefaults.standard.set(skin.rawValue, forKey: "derbis.skin") }
    }

    @Published var playerDeck: DerbisPlayerDeck {
        didSet { UserDefaults.standard.set(playerDeck.rawValue, forKey: "derbis.playerDeck") }
    }

    @Published var fontChoice: DerbisFontChoice {
        didSet { UserDefaults.standard.set(fontChoice.rawValue, forKey: "derbis.fontChoice") }
    }

    @Published var highQualityAudio: Bool {
        didSet { UserDefaults.standard.set(highQualityAudio, forKey: "derbis.highQualityAudio") }
    }

    @Published var endlessRadioEnabled: Bool {
        didSet {
            UserDefaults.standard.set(endlessRadioEnabled, forKey: "derbis.endlessRadio")
            AudioPlayerEngine.shared.syncEndlessListeningFromPreferences(endlessRadioEnabled)
        }
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

    /// Optical budget for the glass material: Auto / Always on / Off.
    @Published var glassEffects: DerbisGlassEffects {
        didSet { UserDefaults.standard.set(glassEffects.rawValue, forKey: "derbis.glassEffects") }
    }

    /// Thinnest the frozen backdrop may get before type stops being readable.
    @Published var glassTransparency: Double {
        didSet { UserDefaults.standard.set(glassTransparency, forKey: "derbis.glassTransparency") }
    }

    /// How hard the lens bends the backdrop along the rim.
    @Published var glassRefraction: Double {
        didSet { UserDefaults.standard.set(glassRefraction, forKey: "derbis.glassRefraction") }
    }

    /// Liquid Glass page background: which of the four modes paints the canvas.
    @Published var glassBackgroundMode: DerbisBackgroundMode {
        didSet { UserDefaults.standard.set(glassBackgroundMode.rawValue, forKey: "derbis.glassBackgroundMode") }
    }

    /// Strength of the mode's colour field, 20...100.
    @Published var glassBackgroundIntensity: Double {
        didSet {
            let clamped = min(100, max(20, glassBackgroundIntensity))
            if clamped != glassBackgroundIntensity { glassBackgroundIntensity = clamped }
            UserDefaults.standard.set(glassBackgroundIntensity, forKey: "derbis.glassBackgroundIntensity")
        }
    }

    /// Blur radius, in points, applied by the Blur and Custom modes.
    @Published var glassBackgroundBlur: Double {
        didSet {
            let clamped = min(80, max(0, glassBackgroundBlur))
            if clamped != glassBackgroundBlur { glassBackgroundBlur = clamped }
            UserDefaults.standard.set(glassBackgroundBlur, forKey: "derbis.glassBackgroundBlur")
        }
    }

    /// How much of the black veil is lifted, 15...100 (100 = fully clear).
    @Published var glassBackgroundBrightness: Double {
        didSet {
            let clamped = min(100, max(15, glassBackgroundBrightness))
            if clamped != glassBackgroundBrightness { glassBackgroundBrightness = clamped }
            UserDefaults.standard.set(glassBackgroundBrightness, forKey: "derbis.glassBackgroundBrightness")
        }
    }

    /// Drift motion for Animated mode (and the slow pan of Blur/Custom).
    @Published var glassBackgroundMotion: Bool {
        didSet { UserDefaults.standard.set(glassBackgroundMotion, forKey: "derbis.glassBackgroundMotion") }
    }

    /// Absolute path of the user-picked custom background image, or empty for none.
    @Published var glassBackgroundImagePath: String {
        didSet { UserDefaults.standard.set(glassBackgroundImagePath, forKey: "derbis.glassBackgroundImagePath") }
    }

    /// Accent override hue, 0...360, or -1 to follow the artwork/theme accent.
    @Published var glassAccentHue: Int {
        didSet {
            let clamped = min(360, max(-1, glassAccentHue))
            if clamped != glassAccentHue { glassAccentHue = clamped }
            UserDefaults.standard.set(glassAccentHue, forKey: "derbis.glassAccentHue")
        }
    }

    /// Accent lightness used when glassAccentHue is set, 0...100.
    @Published var glassAccentLightness: Double {
        didSet {
            let clamped = min(100, max(0, glassAccentLightness))
            if clamped != glassAccentLightness { glassAccentLightness = clamped }
            UserDefaults.standard.set(glassAccentLightness, forKey: "derbis.glassAccentLightness")
        }
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

    @Published var hideTabLabels: Bool {
        didSet { UserDefaults.standard.set(hideTabLabels, forKey: "derbis.hideTabLabels") }
    }

    /// Persisted tab bar layout: which tabs appear, in what order, icons-only vs. names,
    /// plus any custom tabs the user added. See Models/TabBarConfig.swift.
    @Published var tabBarConfig: TabBarConfig {
        didSet {
            UserDefaults.standard.set(tabBarConfig.encode(), forKey: "derbis.tabBarConfig")
        }
    }

    @Published var lyricsTranslationLanguage: LyricsTranslationLanguage {
        didSet { UserDefaults.standard.set(lyricsTranslationLanguage.rawValue, forKey: "derbis.lyricsTranslationLanguage") }
    }

    @Published var geniusMeaningsFilter: GeniusMeaningFilter {
        didSet { UserDefaults.standard.set(geniusMeaningsFilter.rawValue, forKey: "derbis.geniusMeaningsFilter") }
    }

    @Published var showGeniusMeaningMarkers: Bool {
        didSet { UserDefaults.standard.set(showGeniusMeaningMarkers, forKey: "derbis.showGeniusMeaningMarkers") }
    }

    @Published var lyricsSourcePriority: [LyricsSourceKind] {
        didSet {
            if let data = try? JSONEncoder().encode(lyricsSourcePriority) {
                UserDefaults.standard.set(data, forKey: "derbis.lyricsSourcePriority")
            }
        }
    }

    @Published var autoMixEnabled: Bool {
        didSet { UserDefaults.standard.set(autoMixEnabled, forKey: "derbis.autoMixEnabled") }
    }

    @Published var autoMixSeconds: Double {
        didSet {
            let clamped = min(12.0, max(1.0, autoMixSeconds.rounded()))
            if abs(clamped - autoMixSeconds) > 0.01 {
                autoMixSeconds = clamped
                return
            }
            UserDefaults.standard.set(autoMixSeconds, forKey: "derbis.autoMixSeconds")
        }
    }

    @Published var autoMixStyle: AutoMixStyle {
        didSet { UserDefaults.standard.set(autoMixStyle.rawValue, forKey: "derbis.autoMixStyle") }
    }

    @Published var customFontPostScriptName: String {
        didSet { UserDefaults.standard.set(customFontPostScriptName, forKey: "derbis.customFontPostScriptName") }
    }

    @Published var customFontDisplayName: String {
        didSet { UserDefaults.standard.set(customFontDisplayName, forKey: "derbis.customFontDisplayName") }
    }

    @Published var dynamicCoverAccent: Color? = nil

    /// Artwork the glass blooms with. Resolved once per track by the root view rather than by
    /// each of the sixty-odd glass surfaces observing the player.
    @Published var glassArtwork: UIImage? = nil

    /// Live cover palette, mirrored from the player by the root view.
    @Published var ambientBloomOverride: (primary: Color, secondary: Color)? = nil

    init() {
        let defaults = UserDefaults.standard
        let migratedV2 = defaults.bool(forKey: "derbis.migratedLinearGlassV2")

        if !migratedV2 {
            self.skin = .derbisDefault
            self.playerDeck = .flow
            self.liquidGlassEnabled = false
            defaults.set(DerbisSkin.derbisDefault.rawValue, forKey: "derbis.skin")
            defaults.set(DerbisPlayerDeck.flow.rawValue, forKey: "derbis.playerDeck")
            defaults.set(false, forKey: "derbis.liquidGlassEnabled")
            defaults.set(true, forKey: "derbis.migratedLinearGlassV2")
        } else {
            let savedSkin = defaults.string(forKey: "derbis.skin") ?? DerbisSkin.derbisDefault.rawValue
            self.skin = DerbisSkin(rawValue: savedSkin) ?? .derbisDefault

            let savedDeck = defaults.string(forKey: "derbis.playerDeck") ?? DerbisPlayerDeck.flow.rawValue
            self.playerDeck = DerbisPlayerDeck(rawValue: savedDeck) ?? .flow

            if defaults.object(forKey: "derbis.liquidGlassEnabled") == nil {
                self.liquidGlassEnabled = false
            } else {
                self.liquidGlassEnabled = defaults.bool(forKey: "derbis.liquidGlassEnabled")
            }
        }

        let savedFont = defaults.string(forKey: "derbis.fontChoice") ?? DerbisFontChoice.skinDefault.rawValue
        self.fontChoice = DerbisFontChoice(rawValue: savedFont) ?? .skinDefault

        if defaults.object(forKey: "derbis.highQualityAudio") == nil {
            self.highQualityAudio = true
        } else {
            self.highQualityAudio = defaults.bool(forKey: "derbis.highQualityAudio")
        }

        if defaults.object(forKey: "derbis.endlessRadio") == nil {
            self.endlessRadioEnabled = true
        } else {
            self.endlessRadioEnabled = defaults.bool(forKey: "derbis.endlessRadio")
        }

        if defaults.object(forKey: "derbis.autoLoadLyrics") == nil {
            self.autoLoadLyrics = true
        } else {
            self.autoLoadLyrics = defaults.bool(forKey: "derbis.autoLoadLyrics")
        }

        if defaults.object(forKey: "derbis.motionCanvasEnabled") == nil {
            self.motionCanvasEnabled = true
        } else {
            self.motionCanvasEnabled = defaults.bool(forKey: "derbis.motionCanvasEnabled")
        }

        let savedStyle = defaults.string(forKey: "derbis.motionCanvasStyle") ?? MotionCanvasStyle.framed.rawValue
        self.motionCanvasStyle = MotionCanvasStyle(rawValue: savedStyle) ?? .framed

        let savedBackdrop = defaults.string(forKey: "derbis.backdropStyle") ?? PlayerBackdropStyle.fluid.rawValue
        self.backdropStyle = PlayerBackdropStyle(rawValue: savedBackdrop) ?? .fluid

        if defaults.object(forKey: "derbis.showLyricsRomanization") == nil {
            self.showLyricsRomanization = true
        } else {
            self.showLyricsRomanization = defaults.bool(forKey: "derbis.showLyricsRomanization")
        }

        if defaults.object(forKey: "derbis.showLyricsTranslation") == nil {
            self.showLyricsTranslation = false
        } else {
            self.showLyricsTranslation = defaults.bool(forKey: "derbis.showLyricsTranslation")
        }

        if defaults.object(forKey: "derbis.autoHideLyricsControls") == nil {
            self.autoHideLyricsControls = false
        } else {
            self.autoHideLyricsControls = defaults.bool(forKey: "derbis.autoHideLyricsControls")
        }

        if defaults.object(forKey: "derbis.lockScreenLiveLyrics") == nil {
            self.lockScreenLiveLyrics = true
        } else {
            self.lockScreenLiveLyrics = defaults.bool(forKey: "derbis.lockScreenLiveLyrics")
        }

        if defaults.object(forKey: "derbis.lockScreenLyricArt") == nil {
            self.lockScreenLyricArt = false
        } else {
            self.lockScreenLyricArt = defaults.bool(forKey: "derbis.lockScreenLyricArt")
        }

        if defaults.object(forKey: "derbis.playerGesturesEnabled") == nil {
            self.playerGesturesEnabled = true
        } else {
            self.playerGesturesEnabled = defaults.bool(forKey: "derbis.playerGesturesEnabled")
        }

        if defaults.object(forKey: "derbis.ambientGlassBackdropEnabled") == nil {
            self.ambientGlassBackdropEnabled = true
        } else {
            self.ambientGlassBackdropEnabled = defaults.bool(forKey: "derbis.ambientGlassBackdropEnabled")
        }

        let savedGlassEffects = defaults.string(forKey: "derbis.glassEffects") ?? DerbisGlassEffects.automatic.rawValue
        self.glassEffects = DerbisGlassEffects(rawValue: savedGlassEffects) ?? .automatic

        if defaults.object(forKey: "derbis.glassTransparency") == nil {
            self.glassTransparency = 50
        } else {
            self.glassTransparency = defaults.double(forKey: "derbis.glassTransparency")
        }

        if defaults.object(forKey: "derbis.glassRefraction") == nil {
            self.glassRefraction = 55
        } else {
            self.glassRefraction = defaults.double(forKey: "derbis.glassRefraction")
        }

        if let savedBackground = defaults.string(forKey: "derbis.glassBackgroundMode"),
           let decodedBackground = DerbisBackgroundMode(rawValue: savedBackground) {
            self.glassBackgroundMode = decodedBackground
        } else {
            self.glassBackgroundMode = .glow
        }

        if defaults.object(forKey: "derbis.glassBackgroundIntensity") == nil {
            self.glassBackgroundIntensity = 80
        } else {
            self.glassBackgroundIntensity = min(100, max(20, defaults.double(forKey: "derbis.glassBackgroundIntensity")))
        }

        if defaults.object(forKey: "derbis.glassBackgroundBlur") == nil {
            self.glassBackgroundBlur = 40
        } else {
            self.glassBackgroundBlur = min(80, max(0, defaults.double(forKey: "derbis.glassBackgroundBlur")))
        }

        if defaults.object(forKey: "derbis.glassBackgroundBrightness") == nil {
            self.glassBackgroundBrightness = 72
        } else {
            self.glassBackgroundBrightness = min(100, max(15, defaults.double(forKey: "derbis.glassBackgroundBrightness")))
        }

        if defaults.object(forKey: "derbis.glassBackgroundMotion") == nil {
            self.glassBackgroundMotion = true
        } else {
            self.glassBackgroundMotion = defaults.bool(forKey: "derbis.glassBackgroundMotion")
        }

        self.glassBackgroundImagePath = defaults.string(forKey: "derbis.glassBackgroundImagePath") ?? ""

        if defaults.object(forKey: "derbis.glassAccentHue") == nil {
            self.glassAccentHue = -1
        } else {
            self.glassAccentHue = min(360, max(-1, defaults.integer(forKey: "derbis.glassAccentHue")))
        }

        if defaults.object(forKey: "derbis.glassAccentLightness") == nil {
            self.glassAccentLightness = 55
        } else {
            self.glassAccentLightness = min(100, max(0, defaults.double(forKey: "derbis.glassAccentLightness")))
        }

        if defaults.object(forKey: "derbis.coverFollowsAccent") == nil {
            self.coverFollowsAccent = false
        } else {
            self.coverFollowsAccent = defaults.bool(forKey: "derbis.coverFollowsAccent")
        }

        if defaults.object(forKey: "derbis.miniPlayerProgressThickness") == nil {
            self.miniPlayerProgressThickness = 2.5
        } else {
            let savedThickness = defaults.double(forKey: "derbis.miniPlayerProgressThickness")
            self.miniPlayerProgressThickness = min(5.0, max(0.0, savedThickness))
        }

        if defaults.object(forKey: "derbis.tabBarMinimizesOnScroll") == nil {
            self.tabBarMinimizesOnScroll = true
        } else {
            self.tabBarMinimizesOnScroll = defaults.bool(forKey: "derbis.tabBarMinimizesOnScroll")
        }

        self.hideTabLabels = defaults.bool(forKey: "derbis.hideTabLabels")

        self.tabBarConfig = TabBarConfig.decode(from: defaults.string(forKey: "derbis.tabBarConfig")).normalized()

        let savedTransLang = defaults.string(forKey: "derbis.lyricsTranslationLanguage") ?? LyricsTranslationLanguage.english.rawValue
        self.lyricsTranslationLanguage = LyricsTranslationLanguage(rawValue: savedTransLang) ?? .english

        let savedGeniusFilter = defaults.string(forKey: "derbis.geniusMeaningsFilter") ?? GeniusMeaningFilter.everyone.rawValue
        self.geniusMeaningsFilter = GeniusMeaningFilter(rawValue: savedGeniusFilter) ?? .everyone

        if defaults.object(forKey: "derbis.showGeniusMeaningMarkers") == nil {
            self.showGeniusMeaningMarkers = true
        } else {
            self.showGeniusMeaningMarkers = defaults.bool(forKey: "derbis.showGeniusMeaningMarkers")
        }

        if let srcData = defaults.data(forKey: "derbis.lyricsSourcePriority"),
           let decodedSrc = try? JSONDecoder().decode([LyricsSourceKind].self, from: srcData),
           !decodedSrc.isEmpty {
            var unique: [LyricsSourceKind] = []
            for item in decodedSrc where !unique.contains(item) { unique.append(item) }
            for missing in LyricsSourceKind.allCases where !unique.contains(missing) { unique.append(missing) }
            self.lyricsSourcePriority = unique
        } else {
            self.lyricsSourcePriority = [.appleTTML, .netEaseYRC, .lrclib]
        }

        self.autoMixEnabled = defaults.bool(forKey: "derbis.autoMixEnabled")

        if defaults.object(forKey: "derbis.autoMixSeconds") == nil {
            self.autoMixSeconds = 6.0
        } else {
            let savedMix = defaults.double(forKey: "derbis.autoMixSeconds")
            self.autoMixSeconds = min(12.0, max(1.0, savedMix.rounded()))
        }

        let savedMixStyle = defaults.string(forKey: "derbis.autoMixStyle") ?? AutoMixStyle.smartBlend.rawValue
        self.autoMixStyle = AutoMixStyle(rawValue: savedMixStyle) ?? .smartBlend

        self.customFontPostScriptName = defaults.string(forKey: "derbis.customFontPostScriptName") ?? ""
        self.customFontDisplayName = defaults.string(forKey: "derbis.customFontDisplayName") ?? ""
    }

    var effectivePlayerDeck: DerbisPlayerDeck {
        if skin == .pixelUI && playerDeck == .flow {
            return .pixel
        }
        if skin == .appleMusic13 && playerDeck == .flow {
            return .appleMusic13
        }
        return playerDeck
    }

    /// Bloom colours for the glass field. Published by the root view from the player's
    /// ambient extraction, so the material never has to observe playback itself.
    var activeAmbientColors: (primary: Color, secondary: Color) {
        if let ambient = ambientBloomOverride {
            return ambient
        }
        return (accent, secondaryAccent)
    }

    var usesLiquidGlass: Bool {
        !skin.isLight && !isPixelSkin && (skin == .liquidGlass || liquidGlassEnabled)
    }

    /// Whether a surface is allowed to draw the optical material at all. Off is the user
    /// asking for the cheap, flat version explicitly, so it outranks everything else.
    var opticalGlassAllowed: Bool {
        usesLiquidGlass && glassEffects != .off
    }

    /// `true` when even the frost should give way to an opaque, bordered panel.
    var glassPrefersOpaque: Bool {
        glassTransparency <= 16
    }

    /// 0...1 strength of the frosted film. Low numbers are the legibility choice on the
    /// transparency slider, not merely a darker blur.
    var glassFilmAlpha: Double {
        let t = min(100, max(0, glassTransparency)) / 100
        return 0.30 + (1 - t) * 0.52
    }

    var glassRefractionScale: Double {
        min(100, max(0, glassRefraction)) / 100
    }

    var usesPixelFont: Bool {
        switch fontChoice {
        case .pixelify:
            return true
        case .sfPro, .sfRounded, .customFile:
            return false
        case .skinDefault:
            return skin == .pixelUI
        }
    }

    var isPixelSkin: Bool {
        skin == .pixelUI
    }

    var isTorqueSkin: Bool {
        skin == .torque
    }

    var isLightSkin: Bool {
        skin.isLight
    }

    var colorScheme: ColorScheme {
        skin.isLight ? .light : .dark
    }

    var background: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x030303)
        case .pixelUI: return Color(hex: 0xFFEDF1)
        case .torque: return Color(hex: 0x060709)
        case .ivory: return Color(hex: 0xFAF7F2)
        case .lateNight: return Color(hex: 0x000000)
        case .liquidGlass: return Color(hex: 0x020306)
        case .appleMusic13: return Color(hex: 0x000000)
        }
    }

    var inkSurface: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x0A0A0C)
        case .pixelUI: return Color(hex: 0xFFF8F6)
        case .torque: return Color(hex: 0x0B0E14)
        case .ivory: return Color(hex: 0xF4EFE6)
        case .lateNight: return Color(hex: 0x08080B)
        case .liquidGlass: return Color(hex: 0x080A10)
        case .appleMusic13: return Color(hex: 0x111113)
        }
    }

    var surface: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x16161A)
        case .pixelUI: return Color(hex: 0xFFF8F6)
        case .torque: return Color(hex: 0x11141B)
        case .ivory: return Color(hex: 0xFFFFFF)
        case .lateNight: return Color(hex: 0x0E0E12)
        case .liquidGlass: return Color.white.opacity(0.065)
        case .appleMusic13: return Color(hex: 0x1C1C1E)
        }
    }

    var elevatedSurface: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x222228)
        case .pixelUI: return Color(hex: 0xFFD9E2)
        case .torque: return Color(hex: 0x1A1F2A)
        case .ivory: return Color(hex: 0xF1ECE3)
        case .lateNight: return Color(hex: 0x17171E)
        case .liquidGlass: return Color.white.opacity(0.115)
        case .appleMusic13: return Color(hex: 0x2C2C2E)
        }
    }

    var primaryText: Color {
        switch skin {
        case .pixelUI: return Color(hex: 0x5B2338)
        case .ivory: return Color(hex: 0x181615)
        case .derbisDefault: return Color(hex: 0xF5F5F7)
        case .torque: return Color(hex: 0xF1F5F9)
        default: return Color.white
        }
    }

    var secondaryText: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x8E8E98)
        case .pixelUI: return Color(hex: 0x7E3B57)
        case .torque: return Color(hex: 0x94A3B8)
        case .ivory: return Color(hex: 0x6B645C)
        case .lateNight: return Color(hex: 0x9E9890)
        case .liquidGlass: return Color.white.opacity(0.66)
        case .appleMusic13: return Color.white.opacity(0.62)
        }
    }

    var accent: Color {
        if coverFollowsAccent, !isPixelSkin, !isLightSkin, let dynamic = dynamicCoverAccent {
            return dynamic
        }
        switch skin {
        case .derbisDefault: return Color(hex: 0x2997FF)
        case .pixelUI: return Color(hex: 0x9E1B44)
        case .torque: return Color(hex: 0x22D3EE)
        case .ivory: return Color(hex: 0xC2410C)
        case .lateNight: return Color(hex: 0xF59E0B)
        case .liquidGlass: return Color(hex: 0x38BDF8)
        case .appleMusic13: return Color(hex: 0xFC3C44)
        }
    }

    var secondaryAccent: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0x818CF8)
        case .pixelUI: return Color(hex: 0xC93B6B)
        case .torque: return Color(hex: 0xF97316)
        case .ivory: return Color(hex: 0x9A3412)
        case .lateNight: return Color(hex: 0xFB923C)
        case .liquidGlass: return Color(hex: 0x818CF8)
        case .appleMusic13: return Color(hex: 0xFA2D48)
        }
    }

    var tertiaryAccent: Color {
        switch skin {
        case .derbisDefault: return Color(hex: 0xEC4899)
        case .pixelUI: return Color(hex: 0x5B2338)
        case .torque: return Color(hex: 0x38BDF8)
        case .ivory: return Color(hex: 0xD97706)
        case .lateNight: return Color(hex: 0xFBBF24)
        case .liquidGlass: return Color(hex: 0xF472B6)
        case .appleMusic13: return Color(hex: 0xFF6482)
        }
    }

    var onAccentText: Color {
        switch skin {
        case .torque, .lateNight:
            return Color(hex: 0x060709)
        default:
            return Color.white
        }
    }

    var border: Color {
        switch skin {
        case .derbisDefault: return Color.white.opacity(0.09)
        case .pixelUI: return Color(hex: 0x5B2338)
        case .torque: return Color(hex: 0x22D3EE, opacity: 0.22)
        case .ivory: return Color(hex: 0x181615, opacity: 0.11)
        case .lateNight: return Color(hex: 0xF59E0B).opacity(0.18)
        case .liquidGlass: return Color.white.opacity(0.15)
        case .appleMusic13: return Color.white.opacity(0.10)
        }
    }

    var cardRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .torque: return 6
        case .derbisDefault: return 18
        case .ivory: return 16
        case .lateNight: return 14
        case .liquidGlass: return 20
        case .appleMusic13: return 14
        }
    }

    var artworkRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .torque: return 4
        case .derbisDefault: return 12
        case .ivory: return 10
        case .lateNight: return 8
        case .liquidGlass: return 12
        case .appleMusic13: return 10
        }
    }

    var playerArtworkRadius: CGFloat {
        switch skin {
        case .pixelUI: return 0
        case .torque: return 10
        case .derbisDefault: return 28
        case .ivory: return 22
        case .lateNight: return 16
        case .liquidGlass: return 24
        case .appleMusic13: return 20
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
        if fontChoice == .customFile, !customFontPostScriptName.isEmpty {
            return Font.custom(customFontPostScriptName, size: size)
        }
        if fontChoice == .sfRounded {
            return Font.system(size: size, weight: weight, design: .rounded)
        }
        if fontChoice == .sfSerif {
            return Font.system(size: size, weight: weight, design: .serif)
        }
        if fontChoice == .sfMono {
            return Font.system(size: size, weight: weight, design: .monospaced)
        }
        if let bundled = fontChoice.postScriptName(for: weight) {
            return Font.custom(bundled, size: size)
        }
        if skin == .torque && weight == .bold {
            return Font.system(size: size, weight: .heavy, design: .default)
        }
        return Font.system(size: size, weight: weight, design: .default)
    }

    func importCustomFontFile(from url: URL) -> Bool {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }

        guard let data = try? Data(contentsOf: url), !data.isEmpty,
              let provider = CGDataProvider(data: data as CFData),
              let cgFont = CGFont(provider),
              let postScript = cgFont.postScriptName as String?,
              !postScript.isEmpty else {
            return false
        }

        let fm = FileManager.default
        guard let appSupport = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return false
        }
        let dir = appSupport.appendingPathComponent("Derbis/ImportedFonts", isDirectory: true)
        try? fm.createDirectory(at: dir, withIntermediateDirectories: true)

        let ext = url.pathExtension.isEmpty ? "ttf" : url.pathExtension.lowercased()
        let dst = dir.appendingPathComponent("custom_font.\(ext)")
        try? fm.removeItem(at: dst)
        guard (try? data.write(to: dst, options: .atomic)) != nil else { return false }

        CTFontManagerRegisterFontsForURL(dst as CFURL, .process, nil)
        customFontPostScriptName = postScript
        customFontDisplayName = (cgFont.fullName as String?) ?? url.deletingPathExtension().lastPathComponent
        fontChoice = .customFile
        return true
    }

    static func registerBundledFonts() {
        // Pixelify Sans + the open OFL families shared with the Android build. Every file here
        // is also listed in Info.plist UIAppFonts; manual registration covers the case where the
        // app loads before the system registers them.
        let bundled = [
            "pixelify_sans_regular", "pixelify_sans_bold",
            "Outfit-Regular", "Outfit-Medium", "Outfit-SemiBold", "Outfit-Bold", "Outfit-ExtraBold",
            "Inter-Regular", "Inter-Medium", "Inter-SemiBold", "Inter-Bold", "Inter-ExtraBold",
            "Manrope-Regular", "Manrope-Medium", "Manrope-SemiBold", "Manrope-Bold", "Manrope-ExtraBold",
            "Poppins-Regular", "Poppins-Medium", "Poppins-SemiBold", "Poppins-Bold", "Poppins-ExtraBold", "Poppins-Black",
            "Montserrat-Regular", "Montserrat-Medium", "Montserrat-SemiBold", "Montserrat-Bold", "Montserrat-ExtraBold",
            "Nunito-Regular", "Nunito-Medium", "Nunito-SemiBold", "Nunito-Bold", "Nunito-ExtraBold",
            "Roboto-Regular", "Roboto-Medium", "Roboto-SemiBold", "Roboto-Bold", "Roboto-ExtraBold"
        ]
        for file in bundled {
            if let url = Bundle.main.url(forResource: file, withExtension: "ttf") {
                CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            }
        }
        if let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            let dir = appSupport.appendingPathComponent("Derbis/ImportedFonts", isDirectory: true)
            if let files = try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil) {
                for fontURL in files {
                    CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
                }
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

struct AsymmetricLeafShape: Shape {
    var topLeading: CGFloat
    var bottomLeading: CGFloat
    var bottomTrailing: CGFloat
    var topTrailing: CGFloat

    func path(in rect: CGRect) -> Path {
        let maxR = min(rect.width, rect.height) * 0.5
        let tl = min(maxR, max(0, topLeading))
        let bl = min(maxR, max(0, bottomLeading))
        let br = min(maxR, max(0, bottomTrailing))
        let tr = min(maxR, max(0, topTrailing))

        var p = Path()
        p.move(to: CGPoint(x: rect.minX + tl, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX - tr, y: rect.minY))
        p.addArc(center: CGPoint(x: rect.maxX - tr, y: rect.minY + tr), radius: tr, startAngle: .degrees(-90), endAngle: .degrees(0), clockwise: false)
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - br))
        p.addArc(center: CGPoint(x: rect.maxX - br, y: rect.maxY - br), radius: br, startAngle: .degrees(0), endAngle: .degrees(90), clockwise: false)
        p.addLine(to: CGPoint(x: rect.minX + bl, y: rect.maxY))
        p.addArc(center: CGPoint(x: rect.minX + bl, y: rect.maxY - bl), radius: bl, startAngle: .degrees(90), endAngle: .degrees(180), clockwise: false)
        p.addLine(to: CGPoint(x: rect.minX, y: rect.minY + tl))
        p.addArc(center: CGPoint(x: rect.minX + tl, y: rect.minY + tl), radius: tl, startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false)
        p.closeSubpath()
        return p
    }
}

struct PixelSwitchToggleStyle: ToggleStyle {
    let accent: Color
    let ink: Color
    let surface: Color

    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.label
            Spacer(minLength: 10)
            Button {
                HapticsAndGesturesService.shared.playSelectionTick()
                withAnimation(.easeOut(duration: 0.14)) {
                    configuration.isOn.toggle()
                }
            } label: {
                ZStack(alignment: configuration.isOn ? .trailing : .leading) {
                    Rectangle()
                        .fill(ink.opacity(0.24))
                        .frame(width: 44, height: 24)
                        .offset(x: 2, y: 2)

                    Rectangle()
                        .fill(configuration.isOn ? accent : surface)
                        .frame(width: 44, height: 24)
                        .overlay(Rectangle().stroke(ink, lineWidth: 2))

                    Rectangle()
                        .fill(configuration.isOn ? Color.white : ink)
                        .frame(width: 14, height: 14)
                        .padding(.horizontal, 4)
                }
            }
            .buttonStyle(.plain)
        }
    }
}

struct DerbisToggleTintModifier: ViewModifier {
    @EnvironmentObject private var theme: ThemeManager

    @ViewBuilder
    func body(content: Content) -> some View {
        if theme.isPixelSkin {
            content.toggleStyle(
                PixelSwitchToggleStyle(
                    accent: theme.accent,
                    ink: theme.primaryText,
                    surface: theme.surface
                )
            )
        } else {
            content.tint(theme.accent)
        }
    }
}

extension View {
    func derbisToggleTint() -> some View {
        modifier(DerbisToggleTintModifier())
    }
}

struct PixelGraphPaperBackground: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var player = AudioPlayerEngine.shared

    var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            switch theme.skin {
            case .pixelUI:
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
                    context.stroke(path, with: .color(Color(hex: 0x5B2338, opacity: 0.11)), lineWidth: 1)
                }
                .ignoresSafeArea()
                .allowsHitTesting(false)

            case .derbisDefault:
                ZStack {
                    RadialGradient(
                        colors: [
                            Color(hex: 0x2997FF, opacity: 0.13),
                            Color(hex: 0x818CF8, opacity: 0.07),
                            .clear
                        ],
                        center: .topLeading,
                        startRadius: 12,
                        endRadius: 360
                    )
                    RadialGradient(
                        colors: [
                            (player.currentTrack != nil ? player.ambientPrimary.opacity(0.22) : Color(hex: 0xEC4899, opacity: 0.06)),
                            .clear
                        ],
                        center: .topTrailing,
                        startRadius: 20,
                        endRadius: 320
                    )
                }
                .ignoresSafeArea()
                .allowsHitTesting(false)

            case .torque:
                ZStack {
                    LinearGradient(
                        colors: [
                            Color(hex: 0x22D3EE, opacity: 0.10),
                            Color(hex: 0xF97316, opacity: 0.04),
                            .clear
                        ],
                        startPoint: .topLeading,
                        endPoint: .center
                    )
                    Canvas { context, size in
                        var path = Path()
                        let strideStep: CGFloat = 28
                        var offset: CGFloat = -size.height
                        while offset < size.width + size.height {
                            path.move(to: CGPoint(x: offset, y: 0))
                            path.addLine(to: CGPoint(x: offset - size.height * 0.35, y: size.height))
                            offset += strideStep
                        }
                        context.stroke(path, with: .color(Color.white.opacity(0.025)), lineWidth: 1)
                    }
                }
                .ignoresSafeArea()
                .allowsHitTesting(false)

            case .ivory:
                LinearGradient(
                    colors: [
                        Color(hex: 0xC2410C, opacity: 0.05),
                        Color(hex: 0xFAF7F2),
                        Color(hex: 0xF1ECE3, opacity: 0.65)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                .allowsHitTesting(false)

            case .liquidGlass:
                if theme.ambientGlassBackdropEnabled {
                    GlassBackgroundView()
                }

            case .lateNight, .appleMusic13:
                EmptyView()
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
                            .fill(Color(hex: 0x5B2338, opacity: 0.24))
                            .offset(x: 3, y: 3)
                        Rectangle()
                            .fill(fill)
                            .overlay(
                                Rectangle()
                                    .stroke(theme.border, lineWidth: 2)
                            )
                    }
                )
        } else if theme.skin == .derbisDefault {
            let shape = RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
            content
                .background(
                    shape
                        .fill(fill)
                        .overlay(
                            shape.strokeBorder(
                                LinearGradient(
                                    colors: [
                                        Color(hex: 0x2997FF, opacity: elevated ? 0.26 : 0.14),
                                        Color.white.opacity(0.06),
                                        Color(hex: 0x818CF8, opacity: elevated ? 0.18 : 0.08)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 0.9
                            )
                        )
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
        } else if theme.skin == .ivory {
            let shape = RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
            content
                .background(
                    shape
                        .fill(fill)
                        .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
                        .overlay(shape.stroke(theme.border, lineWidth: 1))
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
            return (Color(hex: 0x16243E), Color(hex: 0x0A0C14))
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
            return (Color(hex: 0x16243E), Color(hex: 0x0A0C14))
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
            return Color(hex: 0x2997FF)
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
            return Color(hex: 0x2997FF)
        }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: side, height: side))

        var bestR = 0.16, bestG = 0.59, bestB = 1.0, bestScore = 0.0
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
