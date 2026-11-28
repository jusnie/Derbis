import SwiftUI
import UIKit

enum ReceiptCardTheme: String, CaseIterable, Identifiable {
    case receipt = "Receipt"
    case linearStory = "Linear Story"
    case pixelStamp = "Pixel Stamp"

    var id: String { rawValue }
}

struct ListeningReceiptSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var statsStore = ListeningStatsStore.shared
    @ObservedObject private var library = LibraryStore.shared
    @Environment(\.dismiss) private var dismiss

    @State private var period: StatsPeriod = .month
    @State private var cardStyle: ReceiptCardTheme = .receipt
    @State private var sharePayload: ReceiptActivityPayload?

    private var summary: PeriodStatsSummary {
        statsStore.summary(for: period, limit: 10)
    }

    private var effectiveRows: [(rank: Int, title: String, artist: String, meta: String)] {
        if !summary.topTracks.isEmpty {
            return summary.topTracks.prefix(8).map { item in
                let mins = max(1, Int(item.playedMs / 60_000))
                return (item.rank, item.title, item.subtitle, "\(item.plays)× · \(mins)m")
            }
        }
        let fallback = Array((library.orbitTracks + library.likedTracks).prefix(8))
        return fallback.enumerated().map { idx, t in
            (idx + 1, t.title, t.artist, t.formattedDuration)
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        HStack(spacing: 8) {
                            ForEach(ReceiptCardTheme.allCases) { style in
                                let active = cardStyle == style
                                Button {
                                    HapticsAndGesturesService.shared.playSelectionTick()
                                    cardStyle = style
                                } label: {
                                    Text(style.rawValue)
                                        .font(theme.font(size: 12.5, weight: active ? .bold : .semibold))
                                        .foregroundColor(active ? theme.onAccentText : theme.primaryText)
                                        .padding(.horizontal, 13)
                                        .padding(.vertical, 8)
                                        .derbisGlassCapsule(prominent: active)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(StatsPeriod.allCases) { p in
                                    let active = period == p
                                    Button {
                                        HapticsAndGesturesService.shared.playSelectionTick()
                                        period = p
                                    } label: {
                                        Text(p.rawValue)
                                            .font(theme.font(size: 12, weight: active ? .bold : .medium))
                                            .foregroundColor(active ? theme.accent : theme.secondaryText)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .derbisGlassCapsule()
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }

                        DerbisReceiptRenderableCard(
                            style: cardStyle,
                            periodTitle: period.rawValue,
                            totalPlays: max(summary.totalPlays, library.totalPlaysCount),
                            totalDurationText: summary.totalPlayedMs > 0 ? summary.formattedDuration : "\(library.totalMinutesListened)m",
                            topArtist: summary.topArtists.first?.title ?? effectiveRows.first?.artist ?? "Derbis Studio",
                            rows: effectiveRows
                        )
                        .frame(maxWidth: 344)

                        Button {
                            HapticsAndGesturesService.shared.playControlTap(style: .medium)
                            exportAndShareCard()
                        } label: {
                            Label("Share Receipt Card", systemImage: "square.and.arrow.up")
                                .font(theme.font(size: 14, weight: .bold))
                                .foregroundColor(theme.onAccentText)
                                .frame(maxWidth: 344)
                                .padding(.vertical, 13)
                                .derbisGlassCapsule(prominent: true)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Listening Receipt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .sheet(item: $sharePayload) { item in
                ReceiptActivitySheet(image: item.image)
            }
        }
    }

    @MainActor
    private func exportAndShareCard() {
        let viewToRender = DerbisReceiptRenderableCard(
            style: cardStyle,
            periodTitle: period.rawValue,
            totalPlays: max(summary.totalPlays, library.totalPlaysCount),
            totalDurationText: summary.totalPlayedMs > 0 ? summary.formattedDuration : "\(library.totalMinutesListened)m",
            topArtist: summary.topArtists.first?.title ?? effectiveRows.first?.artist ?? "Derbis Studio",
            rows: effectiveRows
        )
        .frame(width: 344)
        .padding(16)
        .background(cardStyle == .linearStory ? Color(hex: 0x030303) : (cardStyle == .pixelStamp ? Color(hex: 0xFFEDF1) : Color(hex: 0xF5F2EB)))

        let renderer = ImageRenderer(content: viewToRender)
        renderer.scale = 3.0
        if let rendered = renderer.uiImage {
            sharePayload = ReceiptActivityPayload(image: rendered)
        }
    }
}

private struct ReceiptActivityPayload: Identifiable {
    let id = UUID()
    let image: UIImage
}

private struct ReceiptActivitySheet: UIViewControllerRepresentable {
    let image: UIImage

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [image], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

struct DerbisReceiptRenderableCard: View {
    let style: ReceiptCardTheme
    let periodTitle: String
    let totalPlays: Int
    let totalDurationText: String
    let topArtist: String
    let rows: [(rank: Int, title: String, artist: String, meta: String)]

    private var isDark: Bool {
        style == .linearStory
    }

    private var ink: Color {
        switch style {
        case .receipt: return Color(hex: 0x181615)
        case .linearStory: return Color(hex: 0xF5F5F7)
        case .pixelStamp: return Color(hex: 0x5B2338)
        }
    }

    private var muted: Color {
        switch style {
        case .receipt: return Color(hex: 0x6B645C)
        case .linearStory: return Color(hex: 0x94A3B8)
        case .pixelStamp: return Color(hex: 0x7E3B57)
        }
    }

    private var accent: Color {
        switch style {
        case .receipt: return Color(hex: 0x181615)
        case .linearStory: return Color(hex: 0x2997FF)
        case .pixelStamp: return Color(hex: 0x9E1B44)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                DerbisDiamondMark(color: accent, size: 20)
                Text("DERBIS STUDIO")
                    .font(.system(size: 13, weight: .black, design: style == .receipt ? .monospaced : .default))
                    .tracking(1.4)
                    .foregroundColor(ink)
                Spacer()
                Text(periodTitle.uppercased())
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(accent)
            }

            dashedDivider

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("TOTAL TIME")
                        .font(.system(size: 9.5, weight: .bold, design: .monospaced))
                        .foregroundColor(muted)
                    Text(totalDurationText)
                        .font(.system(size: 18, weight: .black, design: .monospaced))
                        .foregroundColor(ink)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("TOP ARTIST")
                        .font(.system(size: 9.5, weight: .bold, design: .monospaced))
                        .foregroundColor(muted)
                    Text(topArtist)
                        .font(.system(size: 14, weight: .bold, design: style == .receipt ? .monospaced : .default))
                        .foregroundColor(accent)
                        .lineLimit(1)
                }
            }

            dashedDivider

            if rows.isEmpty {
                Text("Spin a few songs to print your Derbis receipt.")
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
                    .foregroundColor(muted)
                    .padding(.vertical, 14)
            } else {
                VStack(spacing: 8) {
                    ForEach(rows, id: \.rank) { row in
                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                            Text(String(format: "%02d", row.rank))
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(accent)
                                .frame(width: 20, alignment: .leading)

                            VStack(alignment: .leading, spacing: 1) {
                                Text(row.title)
                                    .font(.system(size: 12.5, weight: .bold, design: style == .receipt ? .monospaced : .default))
                                    .foregroundColor(ink)
                                    .lineLimit(1)
                                Text(row.artist)
                                    .font(.system(size: 10.5, weight: .medium, design: style == .receipt ? .monospaced : .default))
                                    .foregroundColor(muted)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 4)
                            Text(row.meta)
                                .font(.system(size: 10.5, weight: .bold, design: .monospaced))
                                .foregroundColor(ink)
                        }
                    }
                }
            }

            dashedDivider

            HStack {
                Text("PLAYS: \(totalPlays)")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(muted)
                Spacer()
                Text("ENDLESS AUDIO · 32-BIT")
                    .font(.system(size: 9.5, weight: .bold, design: .monospaced))
                    .foregroundColor(muted)
            }

            HStack(spacing: 2) {
                ForEach(0..<34, id: \.self) { idx in
                    Rectangle()
                        .fill(ink.opacity(idx.isMultiple(of: 3) ? 0.88 : 0.42))
                        .frame(width: idx.isMultiple(of: 4) ? 4 : 2, height: 22)
                }
            }
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(.top, 2)
        }
        .padding(18)
        .background(cardBackground)
    }

    private var dashedDivider: some View {
        Rectangle()
            .fill(ink.opacity(0.18))
            .frame(height: 1)
    }

    @ViewBuilder
    private var cardBackground: some View {
        switch style {
        case .receipt:
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(hex: 0xFAF7F2))
                .shadow(color: Color.black.opacity(0.16), radius: 12, x: 0, y: 6)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color(hex: 0x181615, opacity: 0.18), lineWidth: 1)
                )
        case .linearStory:
            let shape = RoundedRectangle(cornerRadius: 22, style: .continuous)
            ZStack {
                shape.fill(Color(hex: 0x0A0A0E))
                shape.fill(
                    LinearGradient(
                        colors: [
                            Color(hex: 0x2997FF, opacity: 0.24),
                            Color(hex: 0x818CF8, opacity: 0.14),
                            Color(hex: 0xEC4899, opacity: 0.12)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                shape.strokeBorder(
                    LinearGradient(
                        colors: [Color(hex: 0x2997FF, opacity: 0.55), Color(hex: 0x818CF8, opacity: 0.35)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.2
                )
            }
        case .pixelStamp:
            ZStack {
                Rectangle()
                    .fill(Color(hex: 0x5B2338, opacity: 0.28))
                    .offset(x: 4, y: 4)
                Rectangle()
                    .fill(Color(hex: 0xFFF8F6))
                    .overlay(Rectangle().stroke(Color(hex: 0x5B2338), lineWidth: 2))
            }
        }
    }
}
