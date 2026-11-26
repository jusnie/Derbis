import Foundation
import UIKit
import CoreHaptics
import CoreMotion
import Combine

@MainActor
final class HapticsAndGesturesService: ObservableObject {
    static let shared = HapticsAndGesturesService()

    @Published var controlHapticsEnabled: Bool {
        didSet { UserDefaults.standard.set(controlHapticsEnabled, forKey: "derbis.controlHapticsEnabled") }
    }

    @Published var controlHapticsStrength: Double {
        didSet { UserDefaults.standard.set(controlHapticsStrength, forKey: "derbis.controlHapticsStrength") }
    }

    @Published var scrubberNotchesEnabled: Bool {
        didSet { UserDefaults.standard.set(scrubberNotchesEnabled, forKey: "derbis.scrubberNotchesEnabled") }
    }

    @Published var musicHapticsMode: MusicHapticsMode {
        didSet {
            UserDefaults.standard.set(musicHapticsMode.rawValue, forKey: "derbis.musicHapticsMode")
            reconfigureMusicHapticsState()
        }
    }

    @Published var headGesturesEnabled: Bool {
        didSet {
            UserDefaults.standard.set(headGesturesEnabled, forKey: "derbis.headGesturesEnabled")
            reconfigureHeadTracking()
        }
    }

    @Published var headDoubleNodAction: HeadNodAction {
        didSet { UserDefaults.standard.set(headDoubleNodAction.rawValue, forKey: "derbis.headDoubleNodAction") }
    }

    @Published var headShakeAction: HeadNodAction {
        didSet { UserDefaults.standard.set(headShakeAction.rawValue, forKey: "derbis.headShakeAction") }
    }

    @Published var headSensitivity: Double {
        didSet {
            UserDefaults.standard.set(headSensitivity, forKey: "derbis.headSensitivity")
            headBridge?.updateSensitivity(headSensitivity)
        }
    }

    @Published private(set) var headphonesConnected: Bool = false
    @Published private(set) var headTrackingSupported: Bool = false
    @Published private(set) var lastGestureToast: String?

    var onHeadGestureAction: ((HeadNodAction) -> Void)?

    private let capabilityProbe = CMHeadphoneMotionManager()

    private var hapticEngine: CHHapticEngine?
    private var hapticEngineRunning: Bool = false
    private var beatPollTimer: Timer?
    private var appIsForeground: Bool = true
    private var playbackActive: Bool = false
    private var headBridge: HeadphoneMotionBridge?
    private var toastClearTask: Task<Void, Never>?

    private init() {
        if UserDefaults.standard.object(forKey: "derbis.controlHapticsEnabled") == nil {
            self.controlHapticsEnabled = true
        } else {
            self.controlHapticsEnabled = UserDefaults.standard.bool(forKey: "derbis.controlHapticsEnabled")
        }

        let savedStrength = UserDefaults.standard.object(forKey: "derbis.controlHapticsStrength") as? Double ?? 0.65
        self.controlHapticsStrength = min(1.0, max(0.1, savedStrength))

        if UserDefaults.standard.object(forKey: "derbis.scrubberNotchesEnabled") == nil {
            self.scrubberNotchesEnabled = true
        } else {
            self.scrubberNotchesEnabled = UserDefaults.standard.bool(forKey: "derbis.scrubberNotchesEnabled")
        }

        let savedMode = UserDefaults.standard.string(forKey: "derbis.musicHapticsMode") ?? MusicHapticsMode.off.rawValue
        self.musicHapticsMode = MusicHapticsMode(rawValue: savedMode) ?? .off

        self.headGesturesEnabled = UserDefaults.standard.bool(forKey: "derbis.headGesturesEnabled")

        let savedNod = UserDefaults.standard.string(forKey: "derbis.headDoubleNodAction") ?? HeadNodAction.playPause.rawValue
        self.headDoubleNodAction = HeadNodAction(rawValue: savedNod) ?? .playPause

        let savedShake = UserDefaults.standard.string(forKey: "derbis.headShakeAction") ?? HeadNodAction.nextTrack.rawValue
        self.headShakeAction = HeadNodAction(rawValue: savedShake) ?? .nextTrack

        let savedSens = UserDefaults.standard.object(forKey: "derbis.headSensitivity") as? Double ?? 1.0
        self.headSensitivity = min(1.5, max(0.6, savedSens))
        self.headTrackingSupported = capabilityProbe.isDeviceMotionAvailable

        setupLifecycleObservers()
        reconfigureMusicHapticsState()
        if headGesturesEnabled {
            reconfigureHeadTracking()
        }
    }

    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let softImpact = UIImpactFeedbackGenerator(style: .soft)
    private let rigidImpact = UIImpactFeedbackGenerator(style: .rigid)
    private let heavyImpact = UIImpactFeedbackGenerator(style: .heavy)
    private let selectionGenerator = UISelectionFeedbackGenerator()
    private let notificationGenerator = UINotificationFeedbackGenerator()

    func setPlaybackActive(_ active: Bool) {
        guard playbackActive != active else { return }
        playbackActive = active
        reconfigureMusicHapticsState()
    }

    func playControlTap(style: UIImpactFeedbackGenerator.FeedbackStyle = .light, scale: Double = 1.0) {
        guard controlHapticsEnabled else { return }
        let generator: UIImpactFeedbackGenerator
        switch style {
        case .light: generator = lightImpact
        case .medium: generator = mediumImpact
        case .soft: generator = softImpact
        case .rigid: generator = rigidImpact
        default: generator = heavyImpact
        }
        let intensity = CGFloat(min(1.0, max(0.08, controlHapticsStrength * scale)))
        generator.impactOccurred(intensity: intensity)
        generator.prepare()
    }

    func playSelectionTick() {
        guard controlHapticsEnabled else { return }
        selectionGenerator.selectionChanged()
        selectionGenerator.prepare()
    }

    func playSuccessFeedback() {
        guard controlHapticsEnabled else { return }
        notificationGenerator.notificationOccurred(.success)
        notificationGenerator.prepare()
    }

    func playScrubberNotchIfNeeded(progress: Double, lastNotch: inout Int) {
        guard controlHapticsEnabled && scrubberNotchesEnabled else { return }
        let clamped = min(1.0, max(0.0, progress))
        let notch = Int((clamped * 10.0).rounded(.down))
        guard notch != lastNotch else { return }
        defer { lastNotch = notch }
        guard lastNotch >= 0 else { return }

        if notch == 0 || notch >= 10 {
            playControlTap(style: .rigid, scale: 0.9)
        } else {
            playControlTap(style: .soft, scale: 0.55)
        }
    }

    private func setupLifecycleObservers() {
        NotificationCenter.default.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                self?.appIsForeground = false
                self?.reconfigureMusicHapticsState()
            }
        }

        NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                self?.appIsForeground = true
                self?.reconfigureMusicHapticsState()
            }
        }
    }

    private func reconfigureMusicHapticsState() {
        let hardwareSupports = CHHapticEngine.capabilitiesForHardware().supportsHaptics
        let shouldRun = hardwareSupports && appIsForeground && playbackActive && musicHapticsMode != .off
        AudioEffectsEngine.setMusicAnalyzerActive(shouldRun)

        if shouldRun {
            ensureHapticEngineStarted()
            if beatPollTimer == nil {
                beatPollTimer = Timer.scheduledTimer(withTimeInterval: 0.022, repeats: true) { [weak self] _ in
                    Task { @MainActor in
                        self?.pollBeatTransients()
                    }
                }
            }
        } else {
            beatPollTimer?.invalidate()
            beatPollTimer = nil
            stopHapticEngine()
        }
    }

    private func ensureHapticEngineStarted() {
        if hapticEngine == nil {
            guard let engine = try? CHHapticEngine() else { return }
            engine.playsHapticsOnly = true
            engine.isAutoShutdownEnabled = true
            engine.stoppedHandler = { [weak self] _ in
                Task { @MainActor in
                    self?.hapticEngineRunning = false
                }
            }
            engine.resetHandler = { [weak self] in
                Task { @MainActor in
                    guard let self else { return }
                    try? self.hapticEngine?.start()
                    self.hapticEngineRunning = true
                }
            }
            hapticEngine = engine
        }
        if !hapticEngineRunning {
            do {
                try hapticEngine?.start()
                hapticEngineRunning = true
            } catch {
                hapticEngineRunning = false
            }
        }
    }

    private func stopHapticEngine() {
        guard hapticEngineRunning else { return }
        hapticEngine?.stop(completionHandler: nil)
        hapticEngineRunning = false
    }

    private func pollBeatTransients() {
        guard hapticEngineRunning, let engine = hapticEngine else { return }
        let hits = AudioEffectsEngine.consumeDetectedBeatHits()
        guard hits.kick > 0.01 || hits.snap > 0.01 else { return }

        let modeGain: Float
        let allowSnap: Bool
        switch musicHapticsMode {
        case .off:
            return
        case .subtle:
            modeGain = 0.45
            allowSnap = true
        case .balanced:
            modeGain = 0.72
            allowSnap = true
        case .dynamic:
            modeGain = 1.0
            allowSnap = true
        case .bassOnly:
            modeGain = 0.85
            allowSnap = false
        }

        var events: [CHHapticEvent] = []
        if hits.kick > 0.01 {
            let intensity = min(1.0, max(0.18, hits.kick * modeGain))
            let sharpness: Float = 0.18
            events.append(
                CHHapticEvent(
                    eventType: .hapticTransient,
                    parameters: [
                        CHHapticEventParameter(parameterID: .hapticIntensity, value: intensity),
                        CHHapticEventParameter(parameterID: .hapticSharpness, value: sharpness)
                    ],
                    relativeTime: 0
                )
            )
        }

        if allowSnap && hits.snap > 0.01 {
            let intensity = min(0.92, max(0.14, hits.snap * modeGain * 0.78))
            let sharpness: Float = 0.78
            events.append(
                CHHapticEvent(
                    eventType: .hapticTransient,
                    parameters: [
                        CHHapticEventParameter(parameterID: .hapticIntensity, value: intensity),
                        CHHapticEventParameter(parameterID: .hapticSharpness, value: sharpness)
                    ],
                    relativeTime: hits.kick > 0.01 ? 0.012 : 0
                )
            )
        }

        guard !events.isEmpty,
              let pattern = try? CHHapticPattern(events: events, parameters: []),
              let player = try? engine.makePlayer(with: pattern) else {
            return
        }
        try? player.start(atTime: CHHapticTimeImmediate)
    }

    private func reconfigureHeadTracking() {
        headTrackingSupported = capabilityProbe.isDeviceMotionAvailable
        guard headGesturesEnabled && headTrackingSupported else {
            headBridge?.stop()
            headBridge = nil
            headphonesConnected = false
            return
        }

        if headBridge == nil {
            let bridge = HeadphoneMotionBridge(
                sensitivity: headSensitivity,
                onConnectionChanged: { [weak self] connected in
                    Task { @MainActor in
                        self?.headphonesConnected = connected
                    }
                },
                onRecognizedGesture: { [weak self] isDoubleNod in
                    Task { @MainActor in
                        self?.handleHeadGesture(isDoubleNod: isDoubleNod)
                    }
                }
            )
            headBridge = bridge
        }
        headBridge?.updateSensitivity(headSensitivity)
        headBridge?.start()
    }

    private func handleHeadGesture(isDoubleNod: Bool) {
        let action = isDoubleNod ? headDoubleNodAction : headShakeAction
        guard action != .none else { return }

        playControlTap(style: .medium, scale: 0.95)
        let gestureName = isDoubleNod ? "Double Nod" : "Head Shake"
        lastGestureToast = "\(gestureName): \(action.rawValue)"
        toastClearTask?.cancel()
        toastClearTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 2_200_000_000)
            if !Task.isCancelled {
                self.lastGestureToast = nil
            }
        }
        onHeadGestureAction?(action)
    }
}

private final class HeadphoneMotionBridge: NSObject, CMHeadphoneMotionManagerDelegate {
    private struct AxisBurst {
        var swings: Int = 0
        var direction: Int = 0
        var start: TimeInterval = 0
        var end: TimeInterval = 0
        var quietSince: TimeInterval = 0
    }

    private let manager = CMHeadphoneMotionManager()
    private let motionQueue: OperationQueue = {
        let q = OperationQueue()
        q.name = "com.derbis.headmotion"
        q.maxConcurrentOperationCount = 1
        q.qualityOfService = .userInteractive
        return q
    }()

    private let onConnectionChanged: @Sendable (Bool) -> Void
    private let onRecognizedGesture: @Sendable (Bool) -> Void

    private var sensitivity: Double
    private var pitchBurst = AxisBurst()
    private var yawBurst = AxisBurst()
    private var cooldownUntil: TimeInterval = 0

    private let maxSwingGap: TimeInterval = 0.30
    private let quietWindow: TimeInterval = 0.35
    private let nodMaxDuration: TimeInterval = 1.60
    private let shakeMaxDuration: TimeInterval = 1.20
    private let postGestureCooldown: TimeInterval = 1.00

    init(
        sensitivity: Double,
        onConnectionChanged: @escaping @Sendable (Bool) -> Void,
        onRecognizedGesture: @escaping @Sendable (Bool) -> Void
    ) {
        self.sensitivity = sensitivity
        self.onConnectionChanged = onConnectionChanged
        self.onRecognizedGesture = onRecognizedGesture
        super.init()
        manager.delegate = self
    }

    func updateSensitivity(_ newValue: Double) {
        let clamped = min(1.5, max(0.6, newValue))
        motionQueue.addOperation { [weak self] in
            self?.sensitivity = clamped
        }
    }

    func start() {
        guard manager.isDeviceMotionAvailable else { return }
        guard !manager.isDeviceMotionActive else { return }
        onConnectionChanged(manager.isConnectionStatusActive)
        manager.startDeviceMotionUpdates(to: motionQueue) { [weak self] motion, _ in
            guard let self, let motion else { return }
            self.processSample(motion)
        }
    }

    func stop() {
        if manager.isDeviceMotionActive {
            manager.stopDeviceMotionUpdates()
        }
    }

    func headphoneMotionManagerDidConnect(_ manager: CMHeadphoneMotionManager) {
        onConnectionChanged(true)
    }

    func headphoneMotionManagerDidDisconnect(_ manager: CMHeadphoneMotionManager) {
        motionQueue.addOperation { [weak self] in
            self?.pitchBurst = AxisBurst()
            self?.yawBurst = AxisBurst()
        }
        onConnectionChanged(false)
    }

    private func processSample(_ motion: CMDeviceMotion) {
        let timestamp = motion.timestamp
        guard timestamp >= cooldownUntil else { return }

        let scale = max(0.6, min(1.5, sensitivity))
        let nodThreshold = 1.10 / scale
        let shakeThreshold = 1.40 / scale

        updateBurst(&pitchBurst, rate: motion.rotationRate.x, threshold: nodThreshold, timestamp: timestamp)
        updateBurst(&yawBurst, rate: motion.rotationRate.z, threshold: shakeThreshold, timestamp: timestamp)

        if evaluateDoubleNod(at: timestamp) {
            pitchBurst = AxisBurst()
            yawBurst = AxisBurst()
            cooldownUntil = timestamp + postGestureCooldown
            onRecognizedGesture(true)
            return
        }

        if evaluateHeadShake(at: timestamp) {
            pitchBurst = AxisBurst()
            yawBurst = AxisBurst()
            cooldownUntil = timestamp + postGestureCooldown
            onRecognizedGesture(false)
        }
    }

    private func updateBurst(_ burst: inout AxisBurst, rate: Double, threshold: Double, timestamp: TimeInterval) {
        let sign: Int
        if rate > threshold {
            sign = 1
        } else if rate < -threshold {
            sign = -1
        } else {
            sign = 0
        }

        if sign == 0 {
            if burst.swings > 0 && burst.quietSince == 0 {
                burst.quietSince = timestamp
            }
            if burst.quietSince > 0 && (timestamp - burst.quietSince) > (quietWindow * 2.4) {
                burst = AxisBurst()
            }
            return
        }

        burst.quietSince = 0
        if burst.swings == 0 || (timestamp - burst.end) > maxSwingGap {
            burst.swings = 1
            burst.direction = sign
            burst.start = timestamp
            burst.end = timestamp
            return
        }

        burst.end = timestamp
        if sign != burst.direction {
            burst.direction = sign
            burst.swings += 1
        }
    }

    private func evaluateDoubleNod(at timestamp: TimeInterval) -> Bool {
        guard pitchBurst.swings >= 4 && pitchBurst.swings <= 5 else { return false }
        guard yawBurst.swings <= 1 else { return false }
        guard (pitchBurst.end - pitchBurst.start) <= nodMaxDuration else { return false }
        guard pitchBurst.quietSince > 0 && (timestamp - pitchBurst.quietSince) >= quietWindow else { return false }
        return true
    }

    private func evaluateHeadShake(at timestamp: TimeInterval) -> Bool {
        guard yawBurst.swings >= 3 && yawBurst.swings <= 6 else { return false }
        guard pitchBurst.swings <= 1 else { return false }
        guard (yawBurst.end - yawBurst.start) <= shakeMaxDuration else { return false }
        guard yawBurst.quietSince > 0 && (timestamp - yawBurst.quietSince) >= quietWindow else { return false }
        return true
    }
}
