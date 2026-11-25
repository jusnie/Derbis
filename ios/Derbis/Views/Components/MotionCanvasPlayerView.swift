import SwiftUI
import AVFoundation
import UIKit
import WebKit

struct LoopingMotionVideoView: UIViewRepresentable {
    let url: URL
    let isPlaying: Bool

    @Environment(\.scenePhase) private var scenePhase

    private var effectivePlaying: Bool {
        isPlaying && scenePhase == .active
    }

    func makeUIView(context: Context) -> CanvasPlayerContainerView {
        let view = CanvasPlayerContainerView()
        view.configure(url: url, shouldPlay: effectivePlaying)
        return view
    }

    func updateUIView(_ uiView: CanvasPlayerContainerView, context: Context) {
        uiView.configure(url: url, shouldPlay: effectivePlaying)
    }

    static func dismantleUIView(_ uiView: CanvasPlayerContainerView, coordinator: ()) {
        uiView.tearDown()
    }
}

final class CanvasPlayerContainerView: UIView {
    override static var layerClass: AnyClass {
        AVPlayerLayer.self
    }

    private var playerLayer: AVPlayerLayer {
        layer as! AVPlayerLayer
    }

    private var queuePlayer: AVQueuePlayer?
    private var playerLooper: AVPlayerLooper?
    private var readyObservation: NSKeyValueObservation?
    private var loadedURL: URL?

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        isUserInteractionEnabled = false
        clipsToBounds = true
        alpha = 0
        playerLayer.videoGravity = .resizeAspectFill
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(url: URL, shouldPlay: Bool) {
        if loadedURL != url {
            tearDown()
            loadedURL = url
            alpha = 0

            let asset = AVURLAsset(url: url)
            let templateItem = AVPlayerItem(asset: asset)
            templateItem.preferredForwardBufferDuration = 4

            let qPlayer = AVQueuePlayer()
            qPlayer.isMuted = true
            qPlayer.volume = 0
            qPlayer.preventsDisplaySleepDuringVideoPlayback = false
            qPlayer.automaticallyWaitsToMinimizeStalling = true
            qPlayer.audiovisualBackgroundPlaybackPolicy = .pauses

            let looper = AVPlayerLooper(player: qPlayer, templateItem: templateItem)
            self.queuePlayer = qPlayer
            self.playerLooper = looper
            self.playerLayer.player = qPlayer

            readyObservation = playerLayer.observe(\.isReadyForDisplay, options: [.initial, .new]) { [weak self] layer, _ in
                guard layer.isReadyForDisplay else { return }
                DispatchQueue.main.async { [weak self] in
                    guard let self = self else { return }
                    UIView.animate(withDuration: 0.42, delay: 0, options: [.curveEaseOut, .beginFromCurrentState]) {
                        self.alpha = 1
                    }
                }
            }
        }

        if shouldPlay {
            queuePlayer?.play()
        } else {
            queuePlayer?.pause()
        }
    }

    func tearDown() {
        readyObservation?.invalidate()
        readyObservation = nil
        playerLooper?.disableLooping()
        playerLooper = nil
        queuePlayer?.pause()
        queuePlayer?.removeAllItems()
        queuePlayer = nil
        playerLayer.player = nil
        loadedURL = nil
    }

    override func willMove(toWindow newWindow: UIWindow?) {
        super.willMove(toWindow: newWindow)
        if newWindow == nil {
            queuePlayer?.pause()
        }
    }

    deinit {
        readyObservation?.invalidate()
    }
}

struct SpotifyCanvasSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @EnvironmentObject private var player: AudioPlayerEngine
    @ObservedObject private var sessionStore = SpotifySessionStore.shared
    @Environment(\.dismiss) private var dismiss

    @State private var cookieInput: String = ""
    @State private var showingWebSignIn: Bool = false
    @State private var isProbing: Bool = false
    @State private var probeStatus: String?

    var body: some View {
        NavigationStack {
            ZStack {
                PixelGraphPaperBackground()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        statusCard

                        signInSection

                        manualCookieSection

                        diagnosticsSection
                    }
                    .padding(18)
                }
            }
            .navigationTitle("Spotify Canvas")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundColor(theme.accent)
                }
            }
            .sheet(isPresented: $showingWebSignIn) {
                SpotifyLoginWebSheet { capturedCookie in
                    if sessionStore.saveCookie(capturedCookie) {
                        probeStatus = "Signed in — Spotify Canvas unlocked"
                        player.refreshMotionCanvas()
                    }
                }
                .environmentObject(theme)
            }
        }
    }

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Circle()
                    .fill(sessionStore.hasSession ? Color(hex: 0x1ED760) : theme.accent)
                    .frame(width: 10, height: 10)

                Text(sessionStore.hasSession ? "Spotify session active" : "Public Canvas & Apple Motion mode")
                    .font(theme.font(size: 15, weight: .bold))
                    .foregroundColor(theme.primaryText)

                Spacer()

                if sessionStore.hasSession && !sessionStore.maskedPreview.isEmpty {
                    Text(sessionStore.maskedPreview)
                        .font(theme.font(size: 11, weight: .semibold))
                        .foregroundColor(theme.secondaryText)
                }
            }

            Text(
                sessionStore.hasSession
                    ? "Resolves official 9:16 Spotify Canvas loops via Pathfinder and spclient, with Apple Music animated covers as fallback."
                    : "Derbis already plays Apple Music animated covers and public Canvas mirrors without an account. Sign in with Spotify to unlock the full official 9:16 Canvas catalog."
            )
            .font(theme.font(size: 12))
            .foregroundColor(theme.secondaryText)

            if let msg = probeStatus ?? sessionStore.statusMessage {
                Text(msg)
                    .font(theme.font(size: 12, weight: .semibold))
                    .foregroundColor(theme.accent)
                    .padding(.top, 2)
            }
        }
        .padding(16)
        .derbisCard(elevated: true)
    }

    private var signInSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Quick connect")
                .font(theme.font(size: 12, weight: .bold))
                .foregroundColor(theme.secondaryText)

            Button {
                showingWebSignIn = true
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "person.badge.key.fill")
                        .font(.system(size: 15, weight: .bold))
                    Text(sessionStore.hasSession ? "Refresh Spotify web login" : "Sign in to Spotify in-app")
                        .font(theme.font(size: 14, weight: .bold))
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                .background(
                    RoundedRectangle(cornerRadius: theme.cardRadius, style: .continuous)
                        .fill(theme.accent)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var manualCookieSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Or paste sp_dc cookie")
                .font(theme.font(size: 12, weight: .bold))
                .foregroundColor(theme.secondaryText)

            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    SecureField("Paste sp_dc value from open.spotify.com", text: $cookieInput)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(theme.font(size: 13))
                        .foregroundColor(theme.primaryText)

                    Button {
                        if let clip = UIPasteboard.general.string {
                            cookieInput = clip
                        }
                    } label: {
                        Text("Paste")
                            .font(theme.font(size: 12, weight: .bold))
                            .foregroundColor(theme.accent)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .fill(theme.elevatedSurface)
                            )
                    }
                    .buttonStyle(.plain)
                }
                .padding(11)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(theme.elevatedSurface.opacity(0.65))
                )

                Button {
                    if sessionStore.saveCookie(cookieInput) {
                        cookieInput = ""
                        probeStatus = "Saved sp_dc to Keychain"
                        player.refreshMotionCanvas()
                    } else {
                        probeStatus = "That string does not look like a valid sp_dc cookie"
                    }
                } label: {
                    Text("Save sp_dc cookie")
                        .font(theme.font(size: 13, weight: .bold))
                        .foregroundColor(theme.primaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(theme.elevatedSurface)
                        )
                }
                .buttonStyle(.plain)
                .disabled(cookieInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(14)
            .derbisCard()
        }
    }

    private var diagnosticsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Diagnostics & cache")
                .font(theme.font(size: 12, weight: .bold))
                .foregroundColor(theme.secondaryText)

            VStack(spacing: 10) {
                Button {
                    isProbing = true
                    probeStatus = "Testing Canvas lookup…"
                    Task {
                        let res = await MotionCanvasService.shared.testConnectionProbe()
                        probeStatus = res
                        isProbing = false
                    }
                } label: {
                    HStack {
                        if isProbing {
                            ProgressView().scaleEffect(0.85)
                        } else {
                            Image(systemName: "waveform.and.magnifyingglass")
                        }
                        Text("Test Canvas connection")
                            .font(theme.font(size: 14, weight: .semibold))
                        Spacer()
                    }
                    .foregroundColor(theme.primaryText)
                    .padding(14)
                    .derbisCard()
                }
                .buttonStyle(.plain)
                .disabled(isProbing)

                Button {
                    Task {
                        await MotionCanvasService.shared.clearCachedVideos()
                        probeStatus = "Cleared cached Canvas MP4 loops"
                        player.refreshMotionCanvas()
                    }
                } label: {
                    HStack {
                        Image(systemName: "arrow.triangle.2.circlepath")
                        Text("Clear cached Canvas videos")
                            .font(theme.font(size: 14, weight: .semibold))
                        Spacer()
                    }
                    .foregroundColor(theme.primaryText)
                    .padding(14)
                    .derbisCard()
                }
                .buttonStyle(.plain)

                if sessionStore.hasSession {
                    Button(role: .destructive) {
                        sessionStore.disconnect()
                        probeStatus = "Disconnected Spotify session"
                        player.refreshMotionCanvas()
                    } label: {
                        HStack {
                            Image(systemName: "trash")
                            Text("Disconnect Spotify session")
                                .font(theme.font(size: 14, weight: .semibold))
                            Spacer()
                        }
                        .foregroundColor(.red)
                        .padding(14)
                        .derbisCard()
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct SpotifyLoginWebSheet: View {
    @EnvironmentObject private var theme: ThemeManager
    @Environment(\.dismiss) private var dismiss

    var onCapturedCookie: (String) -> Void

    var body: some View {
        NavigationStack {
            SpotifyLoginWebViewRepresentable { cookie in
                onCapturedCookie(cookie)
                dismiss()
            }
            .ignoresSafeArea(edges: .bottom)
            .navigationTitle("Sign in to Spotify")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(theme.accent)
                }
            }
        }
    }
}

private struct SpotifyLoginWebViewRepresentable: UIViewRepresentable {
    var onCookieFound: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onCookieFound: onCookieFound)
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.customUserAgent =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1"
        context.coordinator.startPolling(webView: webView)

        if let loginURL = URL(string: "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F") {
            webView.load(URLRequest(url: loginURL))
        }
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        coordinator.stopPolling()
    }

    @MainActor
    final class Coordinator: NSObject, WKNavigationDelegate {
        private let onCookieFound: (String) -> Void
        private var pollTask: Task<Void, Never>?
        private var didCapture: Bool = false

        init(onCookieFound: @escaping (String) -> Void) {
            self.onCookieFound = onCookieFound
        }

        func startPolling(webView: WKWebView) {
            pollTask?.cancel()
            pollTask = Task { @MainActor [weak self, weak webView] in
                while !Task.isCancelled {
                    try? await Task.sleep(nanoseconds: 850_000_000)
                    guard !Task.isCancelled, let self = self, !self.didCapture, let webView = webView else {
                        return
                    }
                    await self.inspectCookies(in: webView)
                }
            }
        }

        func stopPolling() {
            pollTask?.cancel()
            pollTask = nil
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            Task { @MainActor [weak self, weak webView] in
                guard let self = self, let webView = webView, !self.didCapture else { return }
                await self.inspectCookies(in: webView)
            }
        }

        private func inspectCookies(in webView: WKWebView) async {
            let cookies = await webView.configuration.websiteDataStore.httpCookieStore.allCookies()
            for cookie in cookies where cookie.name == "sp_dc" && cookie.domain.contains("spotify.com") {
                if let clean = SpotifySessionStore.extractCleanCookie(from: cookie.value) {
                    didCapture = true
                    stopPolling()
                    onCookieFound(clean)
                    return
                }
            }
        }
    }
}
