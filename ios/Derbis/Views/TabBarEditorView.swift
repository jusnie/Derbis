import SwiftUI

/// Tab bar customization, full feature: icons-only vs. icons+names, enable/disable up to five
/// tabs (at least one), drag to reorder, rename, pick glyphs, add/delete custom tabs, and
/// reset with confirmation. Mirrors the Android TabBarEditorOverlay.
struct TabBarEditorView: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var notice: String?
    @State private var noticeTask: Task<Void, Never>?
    @State private var confirmReset = false
    @State private var editTarget: TabBarTabItem?
    @State private var addingTab = false
    @State private var previewSelection: String?

    private var config: TabBarConfig { theme.tabBarConfig }

    var body: some View {
        NavigationStack {
            List {
                previewSection
                appearanceSection
                tabsSection
                if let notice {
                    Section {
                        Text(notice)
                            .font(theme.font(size: 12.5, weight: .semibold))
                            .foregroundColor(theme.accent)
                            .listRowBackground(theme.accent.opacity(0.08))
                    }
                }
                addAndResetSection
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(DerbisPanelBackground())
            .navigationTitle("Tab Bar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
            .toolbarBackground(.visible, for: .navigationBar)
            .alert("Reset Tab Bar?", isPresented: $confirmReset) {
                Button("Reset", role: .destructive) {
                    withAnimation(reduceMotion ? nil : .spring(response: 0.35, dampingFraction: 0.8)) {
                        theme.tabBarConfig = .default()
                    }
                    HapticsAndGesturesService.shared.playControlTap(style: .medium)
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Your custom tabs, order, icons and labels will be replaced with the default tab bar.")
            }
            .sheet(item: $editTarget) { item in
                TabEditSheetView(initial: item) { action, label, icon in
                    let updated = applyOverrides(config: theme.tabBarConfig, item: item, action: action, label: label, icon: icon)
                    theme.tabBarConfig = updated
                } onDelete: {
                    theme.tabBarConfig = theme.tabBarConfig.withRemoved(id: item.id)
                }
            }
            .sheet(isPresented: $addingTab) {
                TabEditSheetView(initial: nil) { action, label, icon in
                    theme.tabBarConfig = theme.tabBarConfig.withCustomTab(action: action, label: label, icon: icon)
                } onDelete: { }
            }
        }
    }

    // MARK: - Sections

    private var previewSection: some View {
        Section {
            previewCard
                .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
        }
    }

    private var appearanceSection: some View {
        Section("Appearance") {
            modeCards
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
        }
    }

    private var tabsSection: some View {
        Section {
            ForEach(config.items) { item in
                tabRow(item)
            }
            .onMove(perform: move)
        } header: {
            VStack(alignment: .leading, spacing: 2) {
                Text("Tabs")
                Text("Up to \(TabBarConfig.maxEnabled) tabs stay in the bar. Drag to reorder.")
                    .font(theme.font(size: 11.5))
                    .foregroundColor(theme.secondaryText)
                    .textCase(nil)
            }
        }
        .environment(\.editMode, .constant(.active))
    }

    private var addAndResetSection: some View {
        Section {
            Button {
                HapticsAndGesturesService.shared.playSelectionTick()
                addingTab = true
            } label: {
                HStack(spacing: 14) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(theme.accent)
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Add Tab")
                            .font(theme.font(size: 15, weight: .semibold))
                            .foregroundColor(theme.primaryText)
                        Text("Bind a tab to a shortcut or pick a destination")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                    }
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button(role: .destructive) {
                confirmReset = true
            } label: {
                HStack(spacing: 14) {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.red)
                    Text("Reset to Defaults")
                        .font(theme.font(size: 15, weight: .semibold))
                        .foregroundColor(.red)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Preview

    private var previewCard: some View {
        let entries = resolvedPreviewEntries()
        let iconsOnly = config.mode == .iconsOnly
        return VStack {
            HStack(spacing: iconsOnly ? 18 : 4) {
                ForEach(entries, id: \.item.id) { entry in
                    let active = entry.item.id == previewSelection
                    Button {
                        withAnimation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.75)) {
                            previewSelection = entry.item.id
                        }
                        HapticsAndGesturesService.shared.playSelectionTick()
                    } label: {
                        VStack(spacing: 3) {
                            Image(systemName: entry.symbol)
                                .font(.system(size: iconsOnly ? 20 : 17, weight: active ? .bold : .medium))
                            if !iconsOnly {
                                Text(entry.label)
                                    .font(.system(size: 9.5, weight: active ? .bold : .medium))
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.75)
                            }
                        }
                        .foregroundColor(active ? .white : Color.white.opacity(0.66))
                        .padding(.horizontal, iconsOnly ? 12 : 12)
                        .padding(.vertical, 9)
                        .background(
                            Capsule()
                                .fill(active ? theme.accent.opacity(0.85) : Color.white.opacity(0.001))
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(entry.label)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [Color(red: 0.17, green: 0.17, blue: 0.19), Color(red: 0.09, green: 0.09, blue: 0.11)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .overlay(Capsule().stroke(Color.white.opacity(0.09), lineWidth: 1))
            )
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 10)
        .padding(.vertical, 20)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(theme.isLightSkin ? Color.white.opacity(0.55) : Color(red: 0.13, green: 0.13, blue: 0.15))
        )
        .onAppear {
            if previewSelection == nil {
                previewSelection = entries.first?.item.id
            }
        }
    }

    // MARK: - Appearance cards

    private var modeCards: some View {
        HStack(spacing: 12) {
            modeCard(
                title: "Icons only",
                mode: .iconsOnly,
                icons: ["sparkles.rectangle.stack.fill", "magnifyingglass", "square.stack.3d.up.fill"],
                labels: nil
            )
            modeCard(
                title: "Icons & names",
                mode: .iconsAndNames,
                icons: ["sparkles.rectangle.stack.fill", "magnifyingglass", "square.stack.3d.up.fill"],
                labels: ["Home", "Explore", "Library"]
            )
        }
    }

    private func modeCard(title: String, mode: TabBarLabelMode, icons: [String], labels: [String]?) -> some View {
        let selected = config.mode == mode
        return Button {
            withAnimation(reduceMotion ? nil : .spring(response: 0.32, dampingFraction: 0.78)) {
                theme.tabBarConfig = config.withMode(mode)
            }
            HapticsAndGesturesService.shared.playSelectionTick()
        } label: {
            VStack(spacing: 10) {
                HStack(spacing: 16) {
                    ForEach(Array(icons.enumerated()), id: \.offset) { index, symbol in
                        VStack(spacing: 4) {
                            Image(systemName: symbol)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(index == 0 ? theme.accent : Color.white.opacity(0.66))
                            if let labels {
                                Text(labels[index])
                                    .font(.system(size: 7.5, weight: .semibold))
                                    .foregroundColor(index == 0 ? theme.accent : Color.white.opacity(0.66))
                            }
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 11)
                .background(Capsule().fill(Color(red: 0.09, green: 0.09, blue: 0.11)))

                Text(title)
                    .font(theme.font(size: 12.5, weight: .bold))
                    .foregroundColor(theme.primaryText)
            }
            .frame(maxWidth: .infinity)
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(selected ? theme.accent.opacity(0.14) : (theme.isLightSkin ? Color.white.opacity(0.55) : Color(red: 0.17, green: 0.17, blue: 0.19)))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(selected ? theme.accent : theme.border.opacity(0.6), lineWidth: selected ? 2 : 1)
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    // MARK: - Rows

    private func tabRow(_ item: TabBarTabItem) -> some View {
        let resolved = resolvedInfo(item)
        return HStack(spacing: 12) {
            Button {
                toggle(item)
            } label: {
                Image(systemName: item.enabled ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 24))
                    .foregroundColor(item.enabled ? theme.accent : theme.secondaryText.opacity(0.55))
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("\(item.enabled ? "Hide" : "Show") \(resolved.title)")

            Button {
                editTarget = item
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: resolved.symbol)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(item.enabled ? theme.accent : theme.secondaryText)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(item.enabled ? theme.accent.opacity(0.16) : theme.secondaryText.opacity(0.1)))
                    VStack(alignment: .leading, spacing: 1) {
                        Text(resolved.title)
                            .font(theme.font(size: 14.5, weight: .semibold))
                            .foregroundColor(item.enabled ? theme.primaryText : theme.secondaryText)
                            .lineLimit(1)
                        if item.isCustom {
                            Text(DerbisDockCatalog.defaultLabel(for: item.action))
                                .font(theme.font(size: 11.5))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                        }
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 2)
    }

    // MARK: - Actions

    private func resolvedInfo(_ item: TabBarTabItem) -> (title: String, symbol: String) {
        let custom = item.label?.trimmingCharacters(in: .whitespaces)
        let title = (custom?.isEmpty == false) ? custom! : DerbisDockCatalog.defaultLabel(for: item.action)
        let iconId = item.icon ?? DerbisDockCatalog.defaultIcon(for: item.action)
        return (title, DerbisDockIcons.symbol(for: iconId))
    }

    private func resolvedPreviewEntries() -> [(item: TabBarTabItem, label: String, symbol: String)] {
        config.enabledItems().map { item in
            let info = resolvedInfo(item)
            return (item, info.title, info.symbol)
        }
    }

    private func toggle(_ item: TabBarTabItem) {
        if item.enabled && config.enabledCount() <= TabBarConfig.minEnabled {
            showNotice("At least one tab has to stay in the bar")
            return
        }
        if !item.enabled && config.enabledCount() >= TabBarConfig.maxEnabled {
            showNotice("Up to \(TabBarConfig.maxEnabled) tabs can stay in the bar")
            return
        }
        HapticsAndGesturesService.shared.playSelectionTick()
        theme.tabBarConfig = config.withToggled(item.id)
    }

    private func showNotice(_ text: String) {
        noticeTask?.cancel()
        withAnimation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.8)) {
            notice = text
        }
        noticeTask = Task {
            try? await Task.sleep(nanoseconds: 2_400_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                withAnimation(.easeOut(duration: 0.25)) {
                    notice = nil
                }
            }
        }
    }

    private func move(from source: IndexSet, to destination: Int) {
        var items = config.items
        items.move(fromOffsets: source, toOffset: destination)
        theme.tabBarConfig = TabBarConfig(mode: config.mode, items: items)
        HapticsAndGesturesService.shared.playSelectionTick()
    }

    private func applyOverrides(config: TabBarConfig, item: TabBarTabItem, action: String, label: String?, icon: String?) -> TabBarConfig {
        var items = config.items
        if let index = items.firstIndex(where: { $0.id == item.id }) {
            if item.isCustom && items[index].action != action {
                items[index].action = action
            }
            items[index] = items[index].normalized()
        }
        let base = TabBarConfig(mode: config.mode, items: items)
        return base.withOverride(id: item.id, label: label, icon: icon)
    }
}

/// Edit sheet for a single tab: destination, custom name, glyph, and (for custom tabs) delete.
private struct TabEditSheetView: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    let initial: TabBarTabItem?
    let onSave: (_ action: String, _ label: String?, _ icon: String?) -> Void
    let onDelete: () -> Void

    @State private var action: String = DerbisDockCatalog.playlists
    @State private var name: String = ""
    @State private var iconOverride: String?

    private var isEditing: Bool { initial != nil }
    private var defaultAction: String { initial?.action ?? DerbisDockCatalog.playlists }
    private var effectiveIcon: String { iconOverride ?? DerbisDockCatalog.defaultIcon(for: action) }
    private var nameIsCustom: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
    }

    init(initial: TabBarTabItem?, onSave: @escaping (String, String?, String?) -> Void, onDelete: @escaping () -> Void) {
        self.initial = initial
        self.onSave = onSave
        self.onDelete = onDelete
        _action = State(initialValue: initial?.action ?? DerbisDockCatalog.playlists)
        _name = State(initialValue: initial?.label ?? "")
        _iconOverride = State(initialValue: initial?.icon)
    }

    var body: some View {
        NavigationStack {
            Form {
                destinationSection
                nameSection
                iconSection
            }
            .navigationTitle(isEditing ? (name.isEmpty ? DerbisDockCatalog.defaultLabel(for: defaultAction) : name) : "Add Tab")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? "Save" : "Add") {
                        let trimmed = name.trimmingCharacters(in: .whitespaces)
                        onSave(action, trimmed.isEmpty ? nil : String(trimmed.prefix(40)), iconOverride)
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
                if isEditing, initial?.isCustom == true {
                    ToolbarItem(placement: .bottomBar) {
                        Button(role: .destructive) {
                            onDelete()
                            dismiss()
                        } label: {
                            Label("Delete Tab", systemImage: "trash")
                        }
                    }
                }
            }
        }
        .presentationDetents([.large])
    }

    private var destinationSection: some View {
        Section("Opens") {
            FlowLayout(spacing: 8, rowSpacing: 8) {
                ForEach(DerbisDockCatalog.customDestinations, id: \.self) { destination in
                    let selected = action == destination
                    Button {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                            action = destination
                        }
                        HapticsAndGesturesService.shared.playSelectionTick()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: DerbisDockIcons.symbol(for: DerbisDockCatalog.defaultIcon(for: destination)))
                                .font(.system(size: 13, weight: .semibold))
                            Text(DerbisDockCatalog.defaultLabel(for: destination))
                                .font(theme.font(size: 12.5, weight: .semibold))
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .foregroundColor(selected ? theme.accent : theme.primaryText)
                        .background(
                            Capsule().fill(selected ? theme.accent.opacity(0.18) : (theme.isLightSkin ? Color.black.opacity(0.05) : Color.white.opacity(0.07)))
                        )
                        .overlay(
                            Capsule().stroke(selected ? theme.accent.opacity(0.7) : Color.clear, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 4)
        }
    }

    private var nameSection: some View {
        Section("Name") {
            TextField(DerbisDockCatalog.defaultLabel(for: action), text: $name)
                .textInputAutocapitalization(.words)
                .onChange(of: name) { _, newValue in
                    if newValue.count > 40 {
                        name = String(newValue.prefix(40))
                    }
                }
            if nameIsCustom {
                Button("Use Default Name") {
                    name = ""
                    HapticsAndGesturesService.shared.playSelectionTick()
                }
                .font(.system(size: 13, weight: .semibold))
            }
        }
    }

    private var iconSection: some View {
        Section("Icon") {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 46), spacing: 10)], spacing: 10) {
                ForEach(DerbisDockIcons.picker, id: \.self) { iconID in
                    let picked = effectiveIcon == iconID
                    Button {
                        let defaultForAction = DerbisDockCatalog.defaultIcon(for: action)
                        iconOverride = (iconID == defaultForAction) ? nil : iconID
                        HapticsAndGesturesService.shared.playSelectionTick()
                    } label: {
                        Image(systemName: DerbisDockIcons.symbol(for: iconID))
                            .font(.system(size: 19, weight: .semibold))
                            .foregroundColor(picked ? .white : theme.primaryText)
                            .frame(width: 46, height: 46)
                            .background(
                                Circle().fill(picked ? theme.accent : (theme.isLightSkin ? Color.black.opacity(0.06) : Color.white.opacity(0.08)))
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(iconID)
                    .accessibilityAddTraits(picked ? .isSelected : [])
                }
            }
            .padding(.vertical, 4)
            if iconOverride != nil {
                Button("Use Default Icon") {
                    iconOverride = nil
                    HapticsAndGesturesService.shared.playSelectionTick()
                }
                .font(.system(size: 13, weight: .semibold))
            }
        }
    }
}

/// Simple flow layout (chips wrap to the next row when they run out of width).
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    var rowSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(proposal: proposal, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let arrangement = arrange(proposal: proposal, subviews: subviews)
        for (index, frame) in arrangement.frames.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                                  proposal: ProposedViewSize(width: frame.width, height: frame.height))
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (frames: [CGRect], size: CGSize) {
        let maxWidth = proposal.width ?? .infinity
        var frames: [CGRect] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalWidth: CGFloat = 0
        for subview in subviews {
            let ideal = subview.sizeThatFits(.unspecified)
            if x > 0, x + ideal.width > maxWidth {
                x = 0
                y += rowHeight + rowSpacing
                rowHeight = 0
            }
            frames.append(CGRect(x: x, y: y, width: ideal.width, height: ideal.height))
            x += ideal.width + spacing
            rowHeight = max(rowHeight, ideal.height)
            totalWidth = max(totalWidth, x - spacing)
        }
        return (frames, CGSize(width: min(maxWidth, totalWidth), height: y + rowHeight))
    }
}

/// App panel background matching SettingsView's cards page.
private struct DerbisPanelBackground: View {
    @EnvironmentObject private var theme: ThemeManager

    var body: some View {
        if theme.isLightSkin {
            Color(red: 0.96, green: 0.96, blue: 0.97).ignoresSafeArea()
        } else {
            Color.black.ignoresSafeArea()
        }
    }
}
