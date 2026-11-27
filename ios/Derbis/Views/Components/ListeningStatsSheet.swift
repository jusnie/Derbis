import SwiftUI
import UniformTypeIdentifiers

struct ListeningStatsSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var statsStore = ListeningStatsStore.shared
    @Environment(\.dismiss) private var dismiss

    @State private var selectedPeriod: StatsPeriod = .month
    @State private var selectedCategory: Int = 0
    @State private var showingExportImporter: Bool = false
    @State private var showingReceiptSheet: Bool = false
    @State private var showingEraseAlert: Bool = false
    @State private var importFeedback: String?

    var body: some View {
        let currentSummary = statsStore.summary(for: selectedPeriod, limit: 25)

        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        periodPickerStrip

                        heroSummaryCard(summary: currentSummary)

                        if let msg = importFeedback {
                            Text(msg)
                                .font(theme.font(size: 13, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .fill(theme.accent)
                                )
                        }

                        categoryTabs
                        rankedListCard(summary: currentSummary)
                        actionsCard
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle("Listening Stats")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .fileImporter(
                isPresented: $showingExportImporter,
                allowedContentTypes: [.json],
                allowsMultipleSelection: true
            ) { result in
                if case .success(let urls) = result, !urls.isEmpty {
                    Task {
                        let outcome = await statsStore.importSpotifyExportFiles(urls: urls)
                        if outcome.added > 0 || outcome.skipped > 0 {
                            importFeedback = "Imported \(outcome.added) plays (\(outcome.skipped) duplicates skipped)."
                        } else {
                            importFeedback = "No valid StreamingHistory JSON entries found."
                        }
                    }
                }
            }
            .alert("Erase listening history?", isPresented: $showingEraseAlert) {
                Button("Erase", role: .destructive) {
                    statsStore.eraseHistory()
                    importFeedback = "On-device listening log cleared."
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Every recorded and imported play in Listening Stats will be removed from this iPhone.")
            }
            .sheet(isPresented: $showingReceiptSheet) {
                ListeningReceiptSheet()
                    .environmentObject(theme)
            }
        }
    }

    private var periodPickerStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(StatsPeriod.allCases) { period in
                    let active = selectedPeriod == period
                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        selectedPeriod = period
                    } label: {
                        Text(period.rawValue)
                            .font(theme.font(size: 13, weight: active ? .bold : .medium))
                            .foregroundColor(active ? .white : theme.primaryText)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .derbisGlassCapsule(prominent: active)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func heroSummaryCard(summary: PeriodStatsSummary) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("LISTENING TIME")
                        .font(.system(size: 10, weight: .black, design: .rounded))
                        .foregroundColor(theme.accent)
                    Text(summary.formattedDuration)
                        .font(theme.font(size: 28, weight: .bold))
                        .foregroundColor(theme.primaryText)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text("TOTAL PLAYS")
                        .font(.system(size: 10, weight: .black, design: .rounded))
                        .foregroundColor(theme.secondaryText)
                    Text("\(summary.totalPlays)")
                        .font(theme.font(size: 26, weight: .bold))
                        .foregroundColor(theme.primaryText)
                }
            }

            Divider()

            Toggle(isOn: $statsStore.recordPlaysEnabled) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Record plays on this iPhone")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Text("Counts once a song plays 30s (or half of a shorter track)")
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .tint(theme.accent)
        }
        .padding(16)
        .derbisGlassPanel(cornerRadius: 22, tint: theme.accent.opacity(0.24), interactive: false)
    }

    private var categoryTabs: some View {
        let titles = ["Top Tracks", "Top Artists", "Top Albums"]
        return HStack(spacing: 8) {
            ForEach(0..<titles.count, id: \.self) { idx in
                let active = selectedCategory == idx
                Button {
                    HapticsAndGesturesService.shared.playSelectionTick()
                    selectedCategory = idx
                } label: {
                    Text(titles[idx])
                        .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                        .foregroundColor(active ? .white : theme.primaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .derbisGlassCapsule(prominent: active)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func rankedListCard(summary: PeriodStatsSummary) -> some View {
        let items: [RankedStatItem] = {
            switch selectedCategory {
            case 0: return summary.topTracks
            case 1: return summary.topArtists
            default: return summary.topAlbums
            }
        }()

        return VStack(alignment: .leading, spacing: 10) {
            if items.isEmpty {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Image(systemName: "chart.bar.xaxis")
                            .font(.system(size: 28))
                            .foregroundColor(theme.secondaryText)
                        Text("No plays recorded for \(selectedPeriod.rawValue.lowercased()) yet")
                            .font(theme.font(size: 13.5, weight: .medium))
                            .foregroundColor(theme.secondaryText)
                    }
                    .padding(.vertical, 28)
                    Spacer()
                }
            } else {
                ForEach(items) { entry in
                    HStack(spacing: 12) {
                        Text("\(entry.rank)")
                            .font(.system(size: 13, weight: .black, design: .rounded))
                            .foregroundColor(entry.rank <= 3 ? theme.accent : theme.secondaryText)
                            .frame(width: 24, alignment: .leading)

                        if !entry.thumbnailURL.isEmpty, let url = URL(string: entry.thumbnailURL) {
                            TrackArtworkView(
                                url: url,
                                size: 42,
                                cornerRadius: selectedCategory == 1 ? 21 : 8
                            )
                        } else {
                            ZStack {
                                RoundedRectangle(cornerRadius: selectedCategory == 1 ? 21 : 8, style: .continuous)
                                    .fill(theme.elevatedSurface)
                                    .frame(width: 42, height: 42)
                                Image(systemName: selectedCategory == 1 ? "person.fill" : "music.note")
                                    .foregroundColor(theme.secondaryText)
                                    .font(.system(size: 15, weight: .semibold))
                            }
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.title)
                                .font(theme.font(size: 14, weight: .bold))
                                .foregroundColor(theme.primaryText)
                                .lineLimit(1)
                            Text(entry.subtitle)
                                .font(theme.font(size: 11.5))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                        }

                        Spacer()

                        Text(entry.plays == 1 ? "1 play" : "\(entry.plays) plays")
                            .font(theme.font(size: 12, weight: .bold))
                            .foregroundColor(theme.accent)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .padding(15)
        .derbisCard()
    }

    private var actionsCard: some View {
        VStack(spacing: 12) {
            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                showingReceiptSheet = true
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "doc.plaintext.fill")
                        .foregroundColor(theme.accent)
                    Text("Print & Share Listening Receipt Card")
                        .font(theme.font(size: 14, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 12.5, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .buttonStyle(.plain)

            Divider()

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .light)
                showingExportImporter = true
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "square.and.arrow.down")
                        .foregroundColor(theme.accent)
                    Text(statsStore.isImporting ? "Importing…" : "Import Spotify StreamingHistory JSON")
                        .font(theme.font(size: 14, weight: .bold))
                        .foregroundColor(theme.primaryText)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .disabled(statsStore.isImporting)
            .buttonStyle(.plain)

            Divider()

            Button(role: .destructive) {
                showingEraseAlert = true
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "trash")
                    Text("Erase Listening History")
                        .font(theme.font(size: 14, weight: .bold))
                    Spacer()
                }
                .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
        .padding(15)
        .derbisCard()
    }
}
