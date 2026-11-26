import SwiftUI
import UIKit

struct TaurusChromeBlurView: UIViewRepresentable {
    var style: UIBlurEffect.Style = .systemChromeMaterialDark

    func makeUIView(context: Context) -> UIVisualEffectView {
        let view = UIVisualEffectView(effect: UIBlurEffect(style: style))
        view.overrideUserInterfaceStyle = .dark
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        uiView.effect = UIBlurEffect(style: style)
    }
}

struct DerbisGlassContainer<Content: View>: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var spacing: CGFloat = 16
    let content: Content

    init(spacing: CGFloat = 16, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    var body: some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *), theme.usesLiquidGlass, !reduceTransparency {
            GlassEffectContainer(spacing: spacing) {
                content
            }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

struct DerbisGlassCapsuleModifier: ViewModifier {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var prominent: Bool = false
    var tint: Color? = nil
    var interactive: Bool = true
    var forceGlassOverBackdrop: Bool = false

    func body(content: Content) -> some View {
        let usePixel = theme.isPixelSkin && !forceGlassOverBackdrop
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel

        if usePixel {
            content
                .background(
                    Rectangle()
                        .fill(prominent ? theme.accent : theme.surface)
                        .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                )
                .contentShape(Rectangle())
        } else if !useGlass {
            content
                .background(
                    Capsule()
                        .fill(prominent ? (tint ?? theme.accent) : theme.surface)
                        .overlay(Capsule().stroke(theme.border, lineWidth: 1))
                )
                .contentShape(Capsule())
        } else if reduceTransparency {
            content
                .background(
                    Capsule()
                        .fill(prominent ? (tint ?? theme.accent) : Color.white.opacity(0.16))
                        .overlay(Capsule().stroke(Color.white.opacity(0.24), lineWidth: 0.8))
                )
                .contentShape(Capsule())
        } else {
            systemOrSpecularCapsule(content: content)
        }
    }

    @ViewBuilder
    private func systemOrSpecularCapsule(content: Content) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            let base: SwiftUI.Glass = {
                var g: SwiftUI.Glass = .regular
                if prominent {
                    g = g.tint(tint ?? theme.accent)
                } else if let tint = tint {
                    g = g.tint(tint)
                }
                if interactive {
                    g = g.interactive()
                }
                return g
            }()
            content
                .glassEffect(base, in: Capsule())
                .contentShape(Capsule())
        } else {
            fallbackSpecularCapsule(content: content)
        }
        #else
        fallbackSpecularCapsule(content: content)
        #endif
    }

    private func fallbackSpecularCapsule(content: Content) -> some View {
        content
            .background {
                ZStack {
                    TaurusChromeBlurView(style: .systemThinMaterialDark)
                        .clipShape(Capsule())

                    if prominent {
                        Capsule()
                            .fill((tint ?? theme.accent).opacity(0.76))
                    } else if let tint = tint {
                        Capsule()
                            .fill(tint.opacity(0.24))
                    } else {
                        Capsule()
                            .fill(Color.white.opacity(0.06))
                    }

                    Capsule()
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(prominent ? 0.45 : 0.34),
                                    Color.white.opacity(0.07),
                                    Color.white.opacity(0.18)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.75
                        )
                }
                .shadow(color: Color.black.opacity(0.28), radius: 8, x: 0, y: 4)
            }
            .contentShape(Capsule())
    }
}

struct DerbisGlassCircleModifier: ViewModifier {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    let size: CGFloat
    var prominent: Bool = false
    var tint: Color? = nil
    var interactive: Bool = true
    var forceGlassOverBackdrop: Bool = false

    func body(content: Content) -> some View {
        let usePixel = theme.isPixelSkin && !forceGlassOverBackdrop
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel

        if usePixel {
            content
                .frame(width: size, height: size)
                .background(
                    Rectangle()
                        .fill(prominent ? theme.accent : theme.elevatedSurface)
                        .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                )
                .contentShape(Rectangle())
        } else if !useGlass {
            content
                .frame(width: size, height: size)
                .background(
                    Circle()
                        .fill(prominent ? (tint ?? theme.accent) : Color.white.opacity(0.12))
                        .overlay(Circle().stroke(theme.border, lineWidth: 0.8))
                )
                .contentShape(Circle())
        } else if reduceTransparency {
            content
                .frame(width: size, height: size)
                .background(
                    Circle()
                        .fill(prominent ? (tint ?? theme.accent) : Color.white.opacity(0.16))
                        .overlay(Circle().stroke(Color.white.opacity(0.24), lineWidth: 0.8))
                )
                .contentShape(Circle())
        } else {
            systemOrSpecularCircle(content: content)
        }
    }

    @ViewBuilder
    private func systemOrSpecularCircle(content: Content) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            let base: SwiftUI.Glass = {
                var g: SwiftUI.Glass = .regular
                if prominent {
                    g = g.tint(tint ?? theme.accent)
                } else if let tint = tint {
                    g = g.tint(tint)
                }
                if interactive {
                    g = g.interactive()
                }
                return g
            }()
            content
                .frame(width: size, height: size)
                .glassEffect(base, in: Circle())
                .contentShape(Circle())
        } else {
            fallbackSpecularCircle(content: content)
        }
        #else
        fallbackSpecularCircle(content: content)
        #endif
    }

    private func fallbackSpecularCircle(content: Content) -> some View {
        content
            .frame(width: size, height: size)
            .background {
                ZStack {
                    TaurusChromeBlurView(style: .systemThinMaterialDark)
                        .clipShape(Circle())

                    if prominent {
                        Circle()
                            .fill((tint ?? theme.accent).opacity(0.78))
                    } else if let tint = tint {
                        Circle()
                            .fill(tint.opacity(0.22))
                    } else {
                        Circle()
                            .fill(Color.white.opacity(0.08))
                    }

                    Circle()
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.38),
                                    Color.white.opacity(0.06),
                                    Color.white.opacity(0.16)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.75
                        )
                }
                .shadow(color: Color.black.opacity(0.32), radius: 8, x: 0, y: 4)
            }
            .contentShape(Circle())
    }
}

struct DerbisGlassPanelModifier: ViewModifier {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var cornerRadius: CGFloat = 24
    var tint: Color? = nil
    var interactive: Bool = false
    var forceGlassOverBackdrop: Bool = false

    func body(content: Content) -> some View {
        let usePixel = theme.isPixelSkin && !forceGlassOverBackdrop
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel

        if usePixel {
            content
                .background(
                    ZStack {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.22))
                            .offset(x: 3, y: 3)
                        Rectangle()
                            .fill(theme.elevatedSurface)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    }
                )
        } else if !useGlass {
            content
                .background(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(theme.elevatedSurface)
                        .overlay(
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .stroke(theme.border, lineWidth: 1)
                        )
                )
        } else if reduceTransparency {
            content
                .background(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(Color(hex: 0x1A1A22))
                        .overlay(
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .stroke(Color.white.opacity(0.22), lineWidth: 0.8)
                        )
                )
        } else {
            systemOrSpecularPanel(content: content)
        }
    }

    @ViewBuilder
    private func systemOrSpecularPanel(content: Content) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            let base: SwiftUI.Glass = {
                var g: SwiftUI.Glass = .regular
                if let tint = tint {
                    g = g.tint(tint)
                }
                if interactive {
                    g = g.interactive()
                }
                return g
            }()
            content
                .glassEffect(base, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .contentShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        } else {
            fallbackSpecularPanel(content: content)
        }
        #else
        fallbackSpecularPanel(content: content)
        #endif
    }

    private func fallbackSpecularPanel(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        return content
            .background {
                ZStack {
                    TaurusChromeBlurView(style: .systemChromeMaterialDark)
                        .clipShape(shape)

                    if let tint = tint {
                        shape.fill(tint.opacity(0.18))
                    } else {
                        shape.fill(Color.white.opacity(0.05))
                    }

                    shape
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.36),
                                    Color.white.opacity(0.06),
                                    Color.white.opacity(0.16)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.8
                        )
                }
                .shadow(color: Color.black.opacity(0.42), radius: 16, x: 0, y: 8)
            }
            .contentShape(shape)
    }
}

extension View {
    func derbisGlassCapsule(
        prominent: Bool = false,
        tint: Color? = nil,
        interactive: Bool = true,
        forceGlassOverBackdrop: Bool = false
    ) -> some View {
        modifier(
            DerbisGlassCapsuleModifier(
                prominent: prominent,
                tint: tint,
                interactive: interactive,
                forceGlassOverBackdrop: forceGlassOverBackdrop
            )
        )
    }

    func derbisGlassCircle(
        size: CGFloat,
        prominent: Bool = false,
        tint: Color? = nil,
        interactive: Bool = true,
        forceGlassOverBackdrop: Bool = false
    ) -> some View {
        modifier(
            DerbisGlassCircleModifier(
                size: size,
                prominent: prominent,
                tint: tint,
                interactive: interactive,
                forceGlassOverBackdrop: forceGlassOverBackdrop
            )
        )
    }

    func derbisGlassPanel(
        cornerRadius: CGFloat = 24,
        tint: Color? = nil,
        interactive: Bool = false,
        forceGlassOverBackdrop: Bool = false
    ) -> some View {
        modifier(
            DerbisGlassPanelModifier(
                cornerRadius: cornerRadius,
                tint: tint,
                interactive: interactive,
                forceGlassOverBackdrop: forceGlassOverBackdrop
            )
        )
    }
}
