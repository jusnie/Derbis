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

struct TaurusTabBarItemSpec: Equatable, Identifiable {
    let id: Int
    let title: String
    let symbol: String
}

struct TaurusLiquidGlassTabBar: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    let items: [TaurusTabBarItemSpec]
    @Binding var selectedId: Int

    var body: some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *), !reduceTransparency {
            TaurusSystemTabBarRepresentable(
                items: items,
                selectedId: selectedId,
                accentColor: UIColor(theme.accent),
                onSelect: { newId in
                    if selectedId != newId {
                        selectedId = newId
                    }
                }
            )
            .frame(height: 62)
            .transaction { $0.animation = nil }
        } else {
            TaurusFallbackLensTabBar(items: items, selectedId: $selectedId)
                .frame(height: 62)
        }
        #else
        TaurusFallbackLensTabBar(items: items, selectedId: $selectedId)
            .frame(height: 62)
        #endif
    }
}

private struct TaurusSystemTabBarRepresentable: UIViewRepresentable {
    let items: [TaurusTabBarItemSpec]
    let selectedId: Int
    let accentColor: UIColor
    let onSelect: (Int) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelect: onSelect)
    }

    func makeUIView(context: Context) -> TaurusTabBarHostView {
        let host = TaurusTabBarHostView()
        host.bar.delegate = context.coordinator
        host.configure(items: items, selectedId: selectedId, accentColor: accentColor)
        return host
    }

    func updateUIView(_ uiView: TaurusTabBarHostView, context: Context) {
        context.coordinator.onSelect = onSelect
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

private final class TaurusTabBarHostView: UIView {
    let bar = UITabBar()
    private var currentSpecs: [TaurusTabBarItemSpec] = []
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
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(items: [TaurusTabBarItemSpec], selectedId: Int, accentColor: UIColor) {
        if bar.tintColor != accentColor {
            bar.tintColor = accentColor
        }
        if bar.unselectedItemTintColor == nil {
            bar.unselectedItemTintColor = UIColor.white.withAlphaComponent(0.68)
        }

        if currentSpecs != items {
            currentSpecs = items
            let built = items.map { spec -> UITabBarItem in
                let config = UIImage.SymbolConfiguration(pointSize: 17, weight: .semibold)
                let img = UIImage(systemName: spec.symbol, withConfiguration: config)
                return UITabBarItem(title: spec.title, image: img, tag: spec.id)
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
        guard bounds.width > 0 else { return }
        let platterHeight: CGFloat = 62
        let fitted = bar.sizeThatFits(CGSize(width: bounds.width, height: platterHeight))
        let gutter = max(0, fitted.height - platterHeight)
        let targetFrame = CGRect(
            x: -gutter,
            y: 0,
            width: bounds.width + 2 * gutter,
            height: fitted.height
        )
        if bar.frame != targetFrame {
            bar.frame = targetFrame
        }
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        bounds.insetBy(dx: 0, dy: -24).contains(point)
    }
}

private struct TaurusFallbackLensTabBar: View {
    @EnvironmentObject private var theme: ThemeManager

    let items: [TaurusTabBarItemSpec]
    @Binding var selectedId: Int

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
                    TaurusChromeBlurView(style: .systemChromeMaterialDark)
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
                        .fill(Color.white.opacity(0.12))
                        .overlay(
                            Capsule()
                                .strokeBorder(Color.white.opacity(0.24), lineWidth: 0.65)
                        )
                        .frame(width: max(54, slotWidth - 10), height: 50)
                        .position(x: restingCenterX, y: geo.size.height / 2)
                        .animation(.spring(response: 0.34, dampingFraction: 0.78), value: selectedId)
                }

                HStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { _, spec in
                        let selected = spec.id == selectedId
                        let magnified = isHoldingLens && selected
                        VStack(spacing: 3) {
                            Image(systemName: spec.symbol)
                                .font(.system(size: 18, weight: selected ? .bold : .medium))
                            Text(spec.title)
                                .font(theme.font(size: 10.5, weight: selected ? .bold : .medium))
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
                        TaurusChromeBlurView(style: .systemUltraThinMaterialDark)
                            .clipShape(Capsule())

                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(0.24),
                                        theme.accent.opacity(0.14),
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
                                    .font(.system(size: 19, weight: .bold))
                                Text(activeSpec.title)
                                    .font(theme.font(size: 11, weight: .bold))
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

