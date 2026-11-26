import CoreImage.CIFilterBuiltins
import SwiftUI
import UIKit

struct JamSheet: View {
    @EnvironmentObject private var player: AudioPlayerEngine
    @EnvironmentObject private var theme: ThemeManager
    @ObservedObject private var jam = JamService.shared
    @Environment(\.dismiss) private var dismiss

    @State private var selectedMode: JamTransportMode = .lan
    @State private var selectedPermission: JamGuestPermission = .addSongs
    @State private var requireApprovalOnCreate: Bool = true
    @State private var joinInput: String = ""
    @State private var showingQRCode: Bool = false
    @State private var showingRelayConfig: Bool = false
    @State private var copiedFeedback: Bool = false

    private var inviteShareMessage: String {
        let trackLine: String
        if let current = player.currentTrack {
            trackLine = "🎶 \(current.title) — \(current.artist)\n"
        } else {
            trackLine = ""
        }
        return "🎧 Listen Together on Derbis Jam!\n\(trackLine)Session Code: \(jam.code)\nJoin: \(jam.shareURL)"
    }

    private var trackContributionsByParticipant: [String: Int] {
        guard let queue = jam.session?.queue else { return [:] }
        var counts: [String: Int] = [:]
        for item in queue where !item.addedBy.isEmpty {
            counts[item.addedBy, default: 0] += 1
        }
        return counts
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        headerBanner

                        if let err = jam.failure {
                            failureBanner(err)
                        }

                        if !jam.isActive {
                            identityCard

                            if !jam.nearbyRooms.isEmpty {
                                nearbyRoomsCard
                            }

                            createJamCard
                            joinJamCard
                        } else {
                            activeJamHeroCard

                            if jam.connection == .awaitingApproval {
                                awaitingApprovalCard
                            }

                            if jam.isHost {
                                if !jam.pendingRequests.isEmpty {
                                    pendingRequestsCard
                                }
                                hostControlsCard
                            }

                            participantsCard

                            if let session = jam.session, !session.queue.isEmpty {
                                sharedQueueCard(session: session)
                            }

                            leaveOrEndButton
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 44)
                }
            }
            .navigationTitle("Derbis Jam")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(theme.font(size: 15, weight: .bold))
                        .foregroundColor(theme.accent)
                }
            }
            .onAppear {
                if jam.isRemoteRelayAvailable {
                    selectedMode = .server
                }
                if let incoming = jam.pendingIncomingInviteCode, !incoming.isEmpty {
                    joinInput = incoming
                    jam.pendingIncomingInviteCode = nil
                }
                jam.startNearbyDiscovery()
            }
            .onDisappear {
                jam.stopNearbyDiscovery()
            }
        }
    }

    private var headerBanner: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(theme.accent.opacity(0.20))
                    .frame(width: 52, height: 52)
                Image(systemName: "person.2.wave.2.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(theme.accent)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text("LISTEN TOGETHER")
                        .font(.system(size: 10, weight: .black, design: .rounded))
                        .tracking(1.0)
                        .foregroundColor(theme.accent)
                    if jam.isActive {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 6, height: 6)
                    }
                }

                Text(jam.isActive ? "Live Jam Session" : "Derbis Jam")
                    .font(theme.font(size: 22, weight: .bold))
                    .foregroundColor(theme.primaryText)

                Text("Real-time synchronized playback & shared queue across iOS and Android.")
                    .font(theme.font(size: 12.5))
                    .foregroundColor(theme.secondaryText)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .derbisGlassPanel(cornerRadius: 22, tint: theme.accent.opacity(0.24), interactive: false)
    }

    private func failureBanner(_ error: JamFailure) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
                .font(.system(size: 15, weight: .bold))

            Text(error.userMessage)
                .font(theme.font(size: 13, weight: .medium))
                .foregroundColor(theme.primaryText)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                jam.failure = nil
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(theme.secondaryText)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .derbisCard(elevated: true)
    }

    private var identityCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Your Jam display name")
                .font(theme.font(size: 14, weight: .bold))
                .foregroundColor(theme.primaryText)

            HStack(spacing: 10) {
                Image(systemName: "person.crop.circle.fill")
                    .foregroundColor(theme.accent)
                    .font(.system(size: 18))

                TextField("Listener name", text: $jam.displayName)
                    .font(theme.font(size: 15, weight: .medium))
                    .foregroundColor(theme.primaryText)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(theme.elevatedSurface)
            )
        }
        .padding(15)
        .derbisCard()
    }

    private var nearbyRoomsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 7) {
                Image(systemName: "dot.radiowaves.left.and.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(theme.accent)
                Text("Nearby Jams on Wi-Fi")
                    .font(theme.font(size: 15, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text("\(jam.nearbyRooms.count) live")
                    .font(theme.font(size: 11, weight: .bold))
                    .foregroundColor(theme.accent)
            }

            ForEach(jam.nearbyRooms) { room in
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(theme.accent.opacity(0.18))
                            .frame(width: 40, height: 40)
                        Image(systemName: "waveform")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(theme.accent)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(room.hostName)'s Jam")
                            .font(theme.font(size: 14.5, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .lineLimit(1)
                        Text(room.currentTrackTitle.isEmpty ? "Tap to listen together" : "Playing: \(room.currentTrackTitle)")
                            .font(theme.font(size: 12))
                            .foregroundColor(theme.secondaryText)
                            .lineLimit(1)
                    }

                    Spacer()

                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        Task { await jam.joinJam(rawCode: room.code) }
                    } label: {
                        Text("Join")
                            .font(theme.font(size: 13, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 7)
                            .derbisGlassCapsule(prominent: true)
                    }
                    .buttonStyle(.plain)
                }
                .padding(10)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(theme.elevatedSurface.opacity(0.65))
                )
            }
        }
        .padding(15)
        .derbisGlassPanel(cornerRadius: 20, tint: theme.accent.opacity(0.18), interactive: false)
    }

    private var createJamCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Start Listen Together")
                    .font(theme.font(size: 16, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        showingRelayConfig.toggle()
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "network")
                            .font(.system(size: 11, weight: .semibold))
                        Text("Relay")
                            .font(theme.font(size: 11.5, weight: .semibold))
                    }
                    .foregroundColor(theme.accent)
                }
                .buttonStyle(.plain)
            }

            if showingRelayConfig {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Custom Derbis Jam Relay (optional wss://…/jam)")
                        .font(theme.font(size: 11.5, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                    TextField("wss://your-relay.example.com/jam", text: $jam.customRelayURL)
                        .font(.system(size: 13, design: .monospaced))
                        .foregroundColor(theme.primaryText)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(10)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(theme.elevatedSurface)
                        )
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Connection mode")
                    .font(theme.font(size: 12.5, weight: .semibold))
                    .foregroundColor(theme.secondaryText)

                HStack(spacing: 8) {
                    ForEach(JamTransportMode.allCases) { mode in
                        let disabled = (mode == .server && !jam.isRemoteRelayAvailable)
                        let active = selectedMode == mode && !disabled
                        Button {
                            guard !disabled else {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    showingRelayConfig = true
                                }
                                return
                            }
                            HapticsAndGesturesService.shared.playSelectionTick()
                            selectedMode = mode
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: mode == .lan ? "wifi" : "globe")
                                    .font(.system(size: 12, weight: .bold))
                                Text(mode.shortBadge)
                                    .font(theme.font(size: 12.5, weight: .bold))
                                    .lineLimit(1)
                            }
                            .foregroundColor(active ? .white : (disabled ? theme.secondaryText.opacity(0.5) : theme.primaryText))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 9)
                            .derbisGlassCapsule(prominent: active)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Guest permissions")
                    .font(theme.font(size: 12.5, weight: .semibold))
                    .foregroundColor(theme.secondaryText)

                ForEach(JamGuestPermission.allCases) { perm in
                    let chosen = selectedPermission == perm
                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        selectedPermission = perm
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: chosen ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(chosen ? theme.accent : theme.secondaryText)
                                .font(.system(size: 16, weight: .semibold))

                            VStack(alignment: .leading, spacing: 2) {
                                Text(perm.title)
                                    .font(theme.font(size: 14, weight: .bold))
                                    .foregroundColor(theme.primaryText)
                                Text(perm.subtitle)
                                    .font(theme.font(size: 11.5))
                                    .foregroundColor(theme.secondaryText)
                            }
                            Spacer()
                        }
                        .padding(10)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(chosen ? theme.accent.opacity(0.12) : theme.elevatedSurface.opacity(0.55))
                        )
                    }
                    .buttonStyle(.plain)
                }
            }

            Toggle(isOn: $requireApprovalOnCreate) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Require host approval")
                        .font(theme.font(size: 14, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Text("Approve each listener before they join the room")
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                }
            }
            .tint(theme.accent)

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                let effectiveMode: JamTransportMode = (selectedMode == .server && !jam.isRemoteRelayAvailable) ? .lan : selectedMode
                Task {
                    await jam.createJam(
                        mode: effectiveMode,
                        guestPermission: selectedPermission,
                        approvalRequired: requireApprovalOnCreate
                    )
                }
            } label: {
                HStack(spacing: 8) {
                    if jam.connection == .connecting && jam.isHost {
                        ProgressView().tint(.white)
                    } else {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 16, weight: .bold))
                    }
                    Text("Start Listen Together")
                        .font(theme.font(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(theme.accent)
                )
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .derbisCard(elevated: true)
    }

    private var joinJamCard: some View {
        let extracted = JamSessionCode.extractCandidateLetters(from: joinInput)
        let isValid = extracted.count == JamSessionCode.encodedLength && JamSessionCode.parse(extracted) != nil

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Join a Friend's Jam")
                    .font(theme.font(size: 16, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Button {
                    if let pasted = UIPasteboard.general.string, !pasted.isEmpty {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        joinInput = JamSessionCode.extractCandidateLetters(from: pasted)
                    }
                } label: {
                    Label("Paste", systemImage: "doc.on.clipboard")
                        .font(theme.font(size: 12.5, weight: .bold))
                        .foregroundColor(theme.accent)
                }
                .buttonStyle(.plain)
            }

            TextField("Paste 38-letter Jam code or invite link", text: $joinInput)
                .font(.system(size: 13.5, weight: .semibold, design: .monospaced))
                .foregroundColor(theme.primaryText)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(theme.elevatedSurface)
                )

            Button {
                HapticsAndGesturesService.shared.playControlTap(style: .medium)
                Task {
                    await jam.joinJam(rawCode: joinInput)
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "person.2.fill")
                        .font(.system(size: 14, weight: .bold))
                    Text("Join Listen Together")
                        .font(theme.font(size: 14.5, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 15, style: .continuous)
                        .fill(isValid ? theme.accent : theme.secondaryText.opacity(0.35))
                )
            }
            .disabled(!isValid)
            .buttonStyle(.plain)

            Text("Works across Derbis iOS & Derbis Android. Paste a session code or derbis-web.vercel.app/jam/… link.")
                .font(theme.font(size: 11.5))
                .foregroundColor(theme.secondaryText)
        }
        .padding(16)
        .derbisCard()
    }

    private var activeJamHeroCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Text(jam.isHost ? "HOSTING JAM" : "LISTENING TOGETHER")
                    .font(.system(size: 10.5, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(theme.accent))

                if let mode = jam.transportMode {
                    Text(mode.shortBadge)
                        .font(theme.font(size: 11, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .derbisGlassCapsule()
                }

                Spacer()

                Text(statusBadgeText)
                    .font(theme.font(size: 11.5, weight: .bold))
                    .foregroundColor(jam.connection == .connected ? .green : .orange)
            }

            if let track = player.currentTrack {
                HStack(spacing: 12) {
                    TrackArtworkView(url: track.displayArtworkURL, size: 52, cornerRadius: 12)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(track.title)
                            .font(theme.font(size: 15, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .lineLimit(1)
                        Text(track.artist)
                            .font(theme.font(size: 12.5))
                            .foregroundColor(theme.secondaryText)
                            .lineLimit(1)
                    }
                    Spacer()
                    Image(systemName: player.isPlaying ? "waveform" : "pause.fill")
                        .foregroundColor(theme.accent)
                        .font(.system(size: 16, weight: .bold))
                }
                .padding(10)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color.white.opacity(0.06))
                )
            }

            if !jam.code.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("SESSION CODE")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundColor(theme.secondaryText)

                    Text(jam.code)
                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                        .foregroundColor(theme.primaryText)
                        .lineLimit(2)
                        .textSelection(.enabled)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(theme.border, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                )

                HStack(spacing: 8) {
                    Button {
                        UIPasteboard.general.string = jam.code
                        HapticsAndGesturesService.shared.playSuccessFeedback()
                        copiedFeedback = true
                        Task {
                            try? await Task.sleep(nanoseconds: 1_500_000_000)
                            copiedFeedback = false
                        }
                    } label: {
                        Label(copiedFeedback ? "Copied!" : "Copy Code", systemImage: copiedFeedback ? "checkmark" : "doc.on.doc")
                            .font(theme.font(size: 12.5, weight: .bold))
                            .foregroundColor(theme.primaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .derbisGlassCapsule()
                    }
                    .buttonStyle(.plain)

                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        withAnimation(.spring(response: 0.32, dampingFraction: 0.84)) {
                            showingQRCode.toggle()
                        }
                    } label: {
                        Label("QR Code", systemImage: "qrcode")
                            .font(theme.font(size: 12.5, weight: .bold))
                            .foregroundColor(showingQRCode ? .white : theme.primaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .derbisGlassCapsule(prominent: showingQRCode)
                    }
                    .buttonStyle(.plain)

                    ShareLink(item: inviteShareMessage) {
                        Label("Invite", systemImage: "square.and.arrow.up")
                            .font(theme.font(size: 12.5, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .derbisGlassCapsule(prominent: true)
                    }
                }

                if showingQRCode, let qrImg = Self.makeQRCodeImage(from: jam.shareURL) {
                    VStack(spacing: 8) {
                        Image(uiImage: qrImg)
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 176, height: 176)
                            .padding(14)
                            .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Color.white))

                        Text("Scan with iPhone or Android camera to join")
                            .font(theme.font(size: 11.5, weight: .medium))
                            .foregroundColor(theme.secondaryText)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 6)
                }
            }
        }
        .padding(16)
        .derbisGlassPanel(cornerRadius: 24, tint: player.ambientPrimary.opacity(0.32), interactive: false)
    }

    private var awaitingApprovalCard: some View {
        HStack(spacing: 12) {
            ProgressView().tint(theme.accent)
            VStack(alignment: .leading, spacing: 2) {
                Text("Waiting for host approval…")
                    .font(theme.font(size: 14, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Text("Playback will sync automatically as soon as the host admits you.")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }
        }
        .padding(15)
        .derbisCard(elevated: true)
    }

    private var pendingRequestsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Join requests (\(jam.pendingRequests.count))")
                .font(theme.font(size: 15, weight: .bold))
                .foregroundColor(theme.primaryText)

            ForEach(jam.pendingRequests) { req in
                HStack(spacing: 10) {
                    Image(systemName: "person.badge.clock.fill")
                        .foregroundColor(theme.accent)
                    Text(req.name)
                        .font(theme.font(size: 14.5, weight: .semibold))
                        .foregroundColor(theme.primaryText)
                    Spacer()

                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .light)
                        jam.rejectParticipant(req.participantId)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(theme.secondaryText)
                            .frame(width: 32, height: 32)
                            .background(Circle().fill(theme.elevatedSurface))
                    }
                    .buttonStyle(.plain)

                    Button {
                        HapticsAndGesturesService.shared.playControlTap(style: .medium)
                        jam.approveParticipant(req.participantId)
                    } label: {
                        Label("Approve", systemImage: "checkmark")
                            .font(theme.font(size: 12.5, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(Capsule().fill(theme.accent))
                    }
                    .buttonStyle(.plain)
                }
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(theme.elevatedSurface))
            }
        }
        .padding(15)
        .derbisCard(elevated: true)
    }

    private var hostControlsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Host controls")
                .font(theme.font(size: 15, weight: .bold))
                .foregroundColor(theme.primaryText)

            HStack(spacing: 6) {
                ForEach(JamGuestPermission.allCases) { perm in
                    let active = jam.permission == perm
                    Button {
                        HapticsAndGesturesService.shared.playSelectionTick()
                        jam.setGuestPermission(perm)
                    } label: {
                        Text(perm.title)
                            .font(theme.font(size: 12, weight: active ? .bold : .medium))
                            .foregroundColor(active ? .white : theme.primaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .derbisGlassCapsule(prominent: active)
                    }
                    .buttonStyle(.plain)
                }
            }

            Toggle(
                "Require approval for new guests",
                isOn: Binding(
                    get: { jam.isApprovalRequired },
                    set: { jam.setApprovalRequired($0) }
                )
            )
            .font(theme.font(size: 13.5, weight: .medium))
            .foregroundColor(theme.primaryText)
            .tint(theme.accent)

            Toggle(
                "Lock room",
                isOn: Binding(
                    get: { jam.isLocked },
                    set: { jam.setSessionLocked($0) }
                )
            )
            .font(theme.font(size: 13.5, weight: .medium))
            .foregroundColor(theme.primaryText)
            .tint(theme.accent)

            if jam.bannedCount > 0 {
                HStack {
                    Text("Blocked guests: \(jam.bannedCount)")
                        .font(theme.font(size: 12.5))
                        .foregroundColor(theme.secondaryText)
                    Spacer()
                    Button("Clear blocks") {
                        jam.clearBans()
                    }
                    .font(theme.font(size: 12.5, weight: .bold))
                    .foregroundColor(theme.accent)
                }
            }
        }
        .padding(15)
        .derbisCard()
    }

    private var participantsCard: some View {
        let list = jam.session?.participants ?? []
        let contribs = trackContributionsByParticipant

        return VStack(alignment: .leading, spacing: 10) {
            Text("Listeners (\(max(1, list.count)))")
                .font(theme.font(size: 15, weight: .bold))
                .foregroundColor(theme.primaryText)

            ForEach(list) { p in
                let addedCount = contribs[p.id] ?? 0
                HStack(spacing: 10) {
                    ZStack {
                        Circle()
                            .fill(p.isHost ? theme.accent.opacity(0.22) : theme.elevatedSurface)
                            .frame(width: 36, height: 36)
                        Image(systemName: p.isHost ? "crown.fill" : "headphones")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(p.isHost ? theme.accent : theme.secondaryText)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            Text(p.name)
                                .font(theme.font(size: 14.5, weight: .bold))
                                .foregroundColor(theme.primaryText)
                            if p.id == jam.selfParticipantId {
                                Text("YOU")
                                    .font(.system(size: 9.5, weight: .black, design: .rounded))
                                    .foregroundColor(theme.accent)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Capsule().fill(theme.accent.opacity(0.16)))
                            }
                        }
                        Text(
                            p.isHost
                                ? (addedCount > 0 ? "Host · \(addedCount) in queue" : "Host")
                                : (addedCount > 0 ? "Added \(addedCount) songs" : "Listening in sync")
                        )
                        .font(theme.font(size: 11.5))
                        .foregroundColor(theme.secondaryText)
                    }

                    Spacer()

                    if jam.isHost && !p.isHost {
                        Menu {
                            Button(role: .destructive) {
                                jam.removeParticipant(p.id, ban: false)
                            } label: {
                                Label("Remove from Jam", systemImage: "person.fill.xmark")
                            }
                            Button(role: .destructive) {
                                jam.removeParticipant(p.id, ban: true)
                            } label: {
                                Label("Block from Room", systemImage: "hand.raised.slash")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                                .font(.system(size: 18))
                                .foregroundColor(theme.secondaryText)
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding(15)
        .derbisCard()
    }

    private func sharedQueueCard(session: JamSessionState) -> some View {
        let nameById = Dictionary(uniqueKeysWithValues: session.participants.map { ($0.id, $0.name) })

        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Shared Jam Queue")
                    .font(theme.font(size: 15, weight: .bold))
                    .foregroundColor(theme.primaryText)
                Spacer()
                Text("\(session.queue.count) tracks")
                    .font(theme.font(size: 12))
                    .foregroundColor(theme.secondaryText)
            }

            LazyVStack(spacing: 4) {
                ForEach(Array(session.queue.enumerated()), id: \.element.id) { idx, item in
                    let isPlayingIdx = idx == session.currentIndex
                    let adderName = nameById[item.addedBy]
                    Button {
                        guard jam.canControlPlayback else { return }
                        HapticsAndGesturesService.shared.playSelectionTick()
                        jam.dispatchUserAction(.selectIndex(idx))
                    } label: {
                        HStack(spacing: 10) {
                            TrackArtworkView(url: URL(string: item.thumbnailUrl), size: 42, cornerRadius: 8)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .font(theme.font(size: 14, weight: .semibold))
                                    .foregroundColor(isPlayingIdx ? theme.accent : theme.primaryText)
                                    .lineLimit(1)
                                Text(
                                    adderName != nil
                                        ? "\(item.artist) · Added by \(adderName!)"
                                        : item.artist
                                )
                                .font(theme.font(size: 11.5))
                                .foregroundColor(theme.secondaryText)
                                .lineLimit(1)
                            }
                            Spacer()
                            if isPlayingIdx {
                                Image(systemName: "waveform")
                                    .foregroundColor(theme.accent)
                                    .font(.system(size: 13, weight: .bold))
                            }
                        }
                        .padding(.vertical, 4)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(15)
        .derbisCard()
    }

    private var leaveOrEndButton: some View {
        Button(role: .destructive) {
            HapticsAndGesturesService.shared.playControlTap(style: .medium)
            jam.leaveOrEndJam()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "xmark.circle.fill")
                Text(jam.isHost ? "End Listen Together" : "Leave Jam")
                    .font(theme.font(size: 14.5, weight: .bold))
            }
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            .derbisCard(elevated: true)
        }
        .buttonStyle(.plain)
    }

    private var statusBadgeText: String {
        switch jam.connection {
        case .connected: return "Connected"
        case .connecting: return "Connecting…"
        case .awaitingApproval: return "Pending Approval"
        case .disconnected: return "Reconnecting…"
        case .idle: return "Idle"
        }
    }

    private static func makeQRCodeImage(from string: String) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 8, y: 8))
        guard let cgImg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgImg)
    }
}
