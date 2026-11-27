import SwiftUI
import UIKit

struct DerbisChromeBlurView: UIViewRepresentable {
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
        let useTorque = theme.isTorqueSkin && !forceGlassOverBackdrop
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel && (!theme.isLightSkin || forceGlassOverBackdrop)

        if usePixel {
            content
                .background(
                    ZStack {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.22))
                            .offset(x: 2, y: 2)
                        Rectangle()
                            .fill(prominent ? (tint ?? theme.accent) : theme.surface)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    }
                )
                .contentShape(Rectangle())
        } else if useTorque {
            let shape = RoundedRectangle(cornerRadius: 6, style: .continuous)
            content
                .background(
                    shape
                        .fill(prominent ? (tint ?? theme.accent) : theme.elevatedSurface)
                        .overlay(shape.stroke(prominent ? Color.white.opacity(0.25) : theme.border, lineWidth: 1))
                )
                .contentShape(shape)
        } else if !useGlass {
            content
                .background(
                    Capsule()
                        .fill(prominent ? (tint ?? theme.accent) : theme.elevatedSurface)
                        .overlay(
                            Capsule().stroke(
                                prominent ? Color.white.opacity(0.22) : theme.border,
                                lineWidth: 1
                            )
                        )
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
                    DerbisChromeBlurView(style: .systemThinMaterialDark)
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
        let useTorque = theme.isTorqueSkin && !forceGlassOverBackdrop
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel && (!theme.isLightSkin || forceGlassOverBackdrop)

        if usePixel {
            content
                .frame(width: size, height: size)
                .background(
                    ZStack {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.22))
                            .offset(x: 2, y: 2)
                        Rectangle()
                            .fill(prominent ? (tint ?? theme.accent) : theme.elevatedSurface)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    }
                )
                .contentShape(Rectangle())
        } else if useTorque {
            let shape = RoundedRectangle(cornerRadius: 6, style: .continuous)
            content
                .frame(width: size, height: size)
                .background(
                    shape
                        .fill(prominent ? (tint ?? theme.accent) : theme.elevatedSurface)
                        .overlay(shape.stroke(theme.border, lineWidth: 1))
                )
                .contentShape(shape)
        } else if !useGlass {
            content
                .frame(width: size, height: size)
                .background(
                    Circle()
                        .fill(prominent ? (tint ?? theme.accent) : theme.elevatedSurface)
                        .overlay(Circle().stroke(theme.border, lineWidth: 1))
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
                    DerbisChromeBlurView(style: .systemThinMaterialDark)
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
        let useGlass = (theme.usesLiquidGlass || forceGlassOverBackdrop) && !usePixel && (!theme.isLightSkin || forceGlassOverBackdrop)
        let resolvedRadius = theme.isTorqueSkin && !forceGlassOverBackdrop ? 8 : cornerRadius

        if usePixel {
            content
                .background(
                    ZStack {
                        Rectangle()
                            .fill(Color(hex: 0x5B2338, opacity: 0.24))
                            .offset(x: 3, y: 3)
                        Rectangle()
                            .fill(tint?.opacity(0.22) ?? theme.elevatedSurface)
                            .background(theme.surface)
                            .overlay(Rectangle().stroke(theme.border, lineWidth: 2))
                    }
                )
        } else if !useGlass {
            let shape = RoundedRectangle(cornerRadius: resolvedRadius, style: .continuous)
            content
                .background(
                    ZStack {
                        shape.fill(theme.elevatedSurface)
                        if let tint = tint {
                            shape.fill(
                                LinearGradient(
                                    colors: [tint.opacity(theme.isLightSkin ? 0.86 : 0.34), tint.opacity(theme.isLightSkin ? 0.68 : 0.12)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        }
                        shape.stroke(theme.border, lineWidth: 1)
                    }
                )
        } else if reduceTransparency {
            content
                .background(
                    RoundedRectangle(cornerRadius: resolvedRadius, style: .continuous)
                        .fill(Color(hex: 0x1A1A22))
                        .overlay(
                            RoundedRectangle(cornerRadius: resolvedRadius, style: .continuous)
                                .stroke(Color.white.opacity(0.22), lineWidth: 0.8)
                        )
                )
        } else {
            systemOrSpecularPanel(content: content, radius: resolvedRadius)
        }
    }

    @ViewBuilder
    private func systemOrSpecularPanel(content: Content, radius: CGFloat) -> some View {
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
                .glassEffect(base, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
                .contentShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        } else {
            fallbackSpecularPanel(content: content, radius: radius)
        }
        #else
        fallbackSpecularPanel(content: content, radius: radius)
        #endif
    }

    private func fallbackSpecularPanel(content: Content, radius: CGFloat) -> some View {
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        return content
            .background {
                ZStack {
                    DerbisChromeBlurView(style: .systemChromeMaterialDark)
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

struct DerbisTabBarItemSpec: Equatable, Identifiable {
    let id: Int
    let title: String
    let symbol: String
}

struct DerbisLiquidGlassTabBar: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    let items: [DerbisTabBarItemSpec]
    @Binding var selectedId: Int
    var onLongPressFirstItem: (() -> Void)? = nil

    var body: some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *), theme.skin == .appleMusic13, !reduceTransparency {
            DerbisSystemTabBarRepresentable(
                items: items,
                selectedId: selectedId,
                accentColor: UIColor(theme.accent),
                onSelect: { newId in
                    if selectedId != newId {
                        selectedId = newId
                    }
                },
                onLongPressFirstItem: onLongPressFirstItem
            )
            .frame(height: 54)
            .transaction { $0.animation = nil }
        } else {
            DerbisLensTabBar(
                items: items,
                selectedId: $selectedId,
                onLongPressFirstItem: onLongPressFirstItem
            )
            .frame(height: 54)
        }
        #else
        DerbisLensTabBar(
            items: items,
            selectedId: $selectedId,
            onLongPressFirstItem: onLongPressFirstItem
        )
        .frame(height: 54)
        #endif
    }
}

private struct DerbisSystemTabBarRepresentable: UIViewRepresentable {
    let items: [DerbisTabBarItemSpec]
    let selectedId: Int
    let accentColor: UIColor
    let onSelect: (Int) -> Void
    var onLongPressFirstItem: (() -> Void)?

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelect: onSelect)
    }

    func makeUIView(context: Context) -> DerbisTabBarHostView {
        let host = DerbisTabBarHostView()
        host.bar.delegate = context.coordinator
        host.onLongPressFirstItem = onLongPressFirstItem
        host.configure(items: items, selectedId: selectedId, accentColor: accentColor)
        return host
    }

    func updateUIView(_ uiView: DerbisTabBarHostView, context: Context) {
        context.coordinator.onSelect = onSelect
        uiView.onLongPressFirstItem = onLongPressFirstItem
        uiView.configure(items: items, selectedId: selectedId, accentColor: accentColor)
    }

    final class Coordinator: NSObject, UITabBarDelegate {
        var onSelect: (Int) -> Void

        init(onSelect: @escaping (Int) -> Void) {
            self.onSelect = onSelect
        }

        func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
            onSelect(item.tag)
        }
    }
}

private final class DerbisTabBarHostView: UIView {
    let bar = UITabBar()
    var onLongPressFirstItem: (() -> Void)?
    private var currentSpecs: [DerbisTabBarItemSpec] = []
    private var barItems: [UITabBarItem] = []

    override var safeAreaInsets: UIEdgeInsets {
        .zero
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        clipsToBounds = false

        bar.overrideUserInterfaceStyle = .dark
        bar.itemPositioning = .fill
        bar.itemSpacing = 0
        bar.clipsToBounds = false
        addSubview(bar)

        let hold = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        hold.minimumPressDuration = 0.48
        hold.cancelsTouchesInView = false
        addGestureRecognizer(hold)
    }

    @objc private func handleLongPress(_ gr: UILongPressGestureRecognizer) {
        guard gr.state == .began, !barItems.isEmpty else { return }
        let loc = gr.location(in: self)
        let firstSlotMaxX = bounds.width / CGFloat(barItems.count)
        if loc.x >= 0 && loc.x <= firstSlotMaxX {
            onLongPressFirstItem?()
        }
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(items: [DerbisTabBarItemSpec], selectedId: Int, accentColor: UIColor) {
        if bar.tintColor != accentColor {
            bar.tintColor = accentColor
        }
        if bar.unselectedItemTintColor == nil {
            bar.unselectedItemTintColor = UIColor.white.withAlphaComponent(0.68)
        }

        if currentSpecs != items {
            currentSpecs = items
            let built = items.map { spec -> UITabBarItem in
                let config = UIImage.SymbolConfiguration(pointSize: spec.title.isEmpty ? 19 : 17, weight: .semibold)
                let img = UIImage(systemName: spec.symbol, withConfiguration: config)
                let barItem = UITabBarItem(title: spec.title.isEmpty ? nil : spec.title, image: img, tag: spec.id)
                if spec.title.isEmpty {
                    barItem.imageInsets = UIEdgeInsets(top: 5, left: 0, bottom: -5, right: 0)
                }
                return barItem
            }
            barItems = built
            bar.setItems(built, animated: false)
            setNeedsLayout()
        }

        if let target = barItems.first(where: { $0.tag == selectedId }),
           bar.selectedItem !== target {
            bar.selectedItem = target
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        bar.frame = bounds
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        bounds.insetBy(dx: 0, dy: -12).contains(point)
    }
}

private struct DerbisBottomOverlayInsetKey: EnvironmentKey {
    static let defaultValue: CGFloat = 84
}

extension EnvironmentValues {
    var derbisBottomOverlayInset: CGFloat {
        get { self[DerbisBottomOverlayInsetKey.self] }
        set { self[DerbisBottomOverlayInsetKey.self] = newValue }
    }
}

private struct DerbisBottomClearanceModifier: ViewModifier {
    @Environment(\.derbisBottomOverlayInset) private var inset

    func body(content: Content) -> some View {
        content.padding(.bottom, inset)
    }
}

extension View {
    func derbisBottomBarClearance() -> some View {
        modifier(DerbisBottomClearanceModifier())
    }
}

private struct DerbisLensTabBar: View {
    @EnvironmentObject private var theme: ThemeManager

    let items: [DerbisTabBarItemSpec]
    @Binding var selectedId: Int
    var onLongPressFirstItem: (() -> Void)? = nil

    @State private var isHoldingLens: Bool = false
    @State private var lensX: CGFloat = 0
    @State private var holdActivationTask: Task<Void, Never>?

    var body: some View {
        GeometryReader { geo in
            let totalWidth = max(1, geo.size.width)
            let count = max(1, items.count)
            let slotWidth = totalWidth / CGFloat(count)
            let activeIndex = items.firstIndex(where: { $0.id == selectedId }) ?? 0
            let restingCenterX = (CGFloat(activeIndex) + 0.5) * slotWidth
            let clampedLensX = min(totalWidth - slotWidth * 0.48, max(slotWidth * 0.48, lensX))
            let dropletWidth = min(slotWidth + 14, max(76, slotWidth * 1.08))

            ZStack {
                ZStack {
                    DerbisChromeBlurView(style: .systemChromeMaterialDark)
                        .clipShape(Capsule())
                    Capsule()
                        .fill(Color.white.opacity(0.05))
                    Capsule()
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.36),
                                    Color.white.opacity(0.06),
                                    Color.white.opacity(0.18)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.8
                        )
                }
                .shadow(color: Color.black.opacity(0.42), radius: 16, x: 0, y: 8)

                if !isHoldingLens {
                    Capsule()
                        .fill(theme.accent.opacity(0.18))
                        .overlay(
                            Capsule()
                                .strokeBorder(theme.accent.opacity(0.42), lineWidth: 0.85)
                        )
                        .frame(width: max(54, slotWidth - 10), height: 48)
                        .position(x: restingCenterX, y: geo.size.height / 2)
                        .animation(.spring(response: 0.34, dampingFraction: 0.78), value: selectedId)
                }

                HStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { _, spec in
                        let selected = spec.id == selectedId
                        let magnified = isHoldingLens && selected
                        VStack(spacing: 3) {
                            Image(systemName: spec.symbol)
                                .font(.system(size: spec.title.isEmpty ? 20 : 18, weight: selected ? .bold : .medium))
                            if !spec.title.isEmpty {
                                Text(spec.title)
                                    .font(theme.font(size: 10.5, weight: selected ? .bold : .medium))
                            }
                        }
                        .foregroundColor(selected ? theme.accent : Color.white.opacity(0.68))
                        .scaleEffect(magnified ? 1.16 : 1.0)
                        .offset(y: magnified ? -2 : 0)
                        .animation(.interactiveSpring(response: 0.24, dampingFraction: 0.72), value: magnified)
                        .frame(width: slotWidth, height: geo.size.height)
                    }
                }

                if isHoldingLens {
                    ZStack {
                        DerbisChromeBlurView(style: .systemUltraThinMaterialDark)
                            .clipShape(Capsule())

                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(0.24),
                                        theme.accent.opacity(0.18),
                                        Color.white.opacity(0.08)
                                    ],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )

                        Capsule()
                            .strokeBorder(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(0.78),
                                        Color.white.opacity(0.16),
                                        Color.white.opacity(0.46)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.15
                            )

                        if activeIndex < items.count {
                            let activeSpec = items[activeIndex]
                            VStack(spacing: 3) {
                                Image(systemName: activeSpec.symbol)
                                    .font(.system(size: activeSpec.title.isEmpty ? 21 : 19, weight: .bold))
                                if !activeSpec.title.isEmpty {
                                    Text(activeSpec.title)
                                        .font(theme.font(size: 11, weight: .bold))
                                }
                            }
                            .foregroundColor(.white)
                        }
                    }
                    .frame(width: dropletWidth, height: 58)
                    .scaleEffect(1.12)
                    .shadow(color: Color.white.opacity(0.18), radius: 10, x: 0, y: 0)
                    .shadow(color: Color.black.opacity(0.5), radius: 16, x: 0, y: 8)
                    .position(x: clampedLensX, y: geo.size.height / 2 - 4)
                    .transition(.scale(scale: 0.88).combined(with: .opacity))
                    .allowsHitTesting(false)
                }
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        lensX = value.location.x
                        let idx = min(count - 1, max(0, Int(value.location.x / slotWidth)))
                        let candidateId = items[idx].id
                        if candidateId != selectedId {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedId = candidateId
                        }

                        if abs(value.translation.width) > 5 {
                            holdActivationTask?.cancel()
                            if !isHoldingLens {
                                HapticsAndGesturesService.shared.playControlTap(style: .light)
                                withAnimation(.interactiveSpring(response: 0.26, dampingFraction: 0.72)) {
                                    isHoldingLens = true
                                }
                            }
                        } else if !isHoldingLens && holdActivationTask == nil {
                            holdActivationTask = Task { @MainActor in
                                try? await Task.sleep(nanoseconds: 130_000_000)
                                guard !Task.isCancelled else { return }
                                HapticsAndGesturesService.shared.playControlTap(style: .light)
                                withAnimation(.interactiveSpring(response: 0.28, dampingFraction: 0.70)) {
                                    isHoldingLens = true
                                }
                            }
                        }
                    }
                    .onEnded { value in
                        holdActivationTask?.cancel()
                        holdActivationTask = nil
                        let idx = min(count - 1, max(0, Int(value.location.x / slotWidth)))
                        let candidateId = items[idx].id
                        if candidateId != selectedId {
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedId = candidateId
                        } else if !isHoldingLens {
                            HapticsAndGesturesService.shared.playSelectionTick()
                        }
                        withAnimation(.spring(response: 0.34, dampingFraction: 0.76)) {
                            isHoldingLens = false
                        }
                    }
            )
        }
    }
}

extension Notification.Name {
    static let derbisScrollTabBarMinimizeChanged = Notification.Name("derbis.scrollTabBarMinimizeChanged")
}

private struct DerbisScrollMinimizeModifier: ViewModifier {
    @State private var lastDispatchedMinimized: Bool?

    func body(content: Content) -> some View {
        content.simultaneousGesture(
            DragGesture(minimumDistance: 22)
                .onChanged { value in
                    let dy = value.translation.height
                    let dx = value.translation.width
                    guard abs(dy) > abs(dx) * 1.35 else { return }
                    if dy < -34, lastDispatchedMinimized != true {
                        lastDispatchedMinimized = true
                        NotificationCenter.default.post(
                            name: .derbisScrollTabBarMinimizeChanged,
                            object: nil,
                            userInfo: ["minimized": true]
                        )
                    } else if dy > 28, lastDispatchedMinimized != false {
                        lastDispatchedMinimized = false
                        NotificationCenter.default.post(
                            name: .derbisScrollTabBarMinimizeChanged,
                            object: nil,
                            userInfo: ["minimized": false]
                        )
                    }
                }
                .onEnded { _ in
                    lastDispatchedMinimized = nil
                }
        )
    }
}

extension View {
    func derbisAutoMinimizeTabBarOnScroll() -> some View {
        modifier(DerbisScrollMinimizeModifier())
    }
}
