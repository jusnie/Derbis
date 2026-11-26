import Foundation
import AVFoundation
import MediaToolbox
import CoreMedia
import Combine
import os

struct DetectedBeatHits: Sendable {
    var kick: Float = 0
    var snap: Float = 0
}

private struct BiquadCoeffs {
    var b0: Float = 1
    var b1: Float = 0
    var b2: Float = 0
    var a1: Float = 0
    var a2: Float = 0

    static let identity = BiquadCoeffs()

    static func peakingEQ(sampleRate: Double, freq: Double, q: Double, gainDB: Double) -> BiquadCoeffs {
        guard abs(gainDB) > 0.01, freq > 10, freq < sampleRate * 0.48 else { return .identity }
        let a = pow(10.0, gainDB / 40.0)
        let w0 = 2.0 * Double.pi * freq / sampleRate
        let cosW = cos(w0)
        let alpha = sin(w0) / (2.0 * max(0.15, q))

        let b0 = 1.0 + alpha * a
        let b1 = -2.0 * cosW
        let b2 = 1.0 - alpha * a
        let a0 = 1.0 + alpha / a
        let a1 = -2.0 * cosW
        let a2 = 1.0 - alpha / a

        return BiquadCoeffs(
            b0: Float(b0 / a0),
            b1: Float(b1 / a0),
            b2: Float(b2 / a0),
            a1: Float(a1 / a0),
            a2: Float(a2 / a0)
        )
    }

    static func lowShelf(sampleRate: Double, freq: Double, gainDB: Double) -> BiquadCoeffs {
        guard abs(gainDB) > 0.02, freq > 10, freq < sampleRate * 0.45 else { return .identity }
        let a = pow(10.0, gainDB / 40.0)
        let w0 = 2.0 * Double.pi * freq / sampleRate
        let cosW = cos(w0)
        let sinW = sin(w0)
        let alpha = sinW / 2.0 * sqrt(2.0)
        let twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        let b0 = a * ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)
        let b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)
        let b2 = a * ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha)
        let a0 = (a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha
        let a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW)
        let a2 = (a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha

        return BiquadCoeffs(
            b0: Float(b0 / a0),
            b1: Float(b1 / a0),
            b2: Float(b2 / a0),
            a1: Float(a1 / a0),
            a2: Float(a2 / a0)
        )
    }

    static func lowPass(sampleRate: Double, freq: Double, q: Double = 0.70710678) -> BiquadCoeffs {
        let clampedFreq = min(sampleRate * 0.45, max(20.0, freq))
        let w0 = 2.0 * Double.pi * clampedFreq / sampleRate
        let cosW = cos(w0)
        let alpha = sin(w0) / (2.0 * max(0.2, q))

        let b1 = 1.0 - cosW
        let b0 = b1 * 0.5
        let b2 = b0
        let a0 = 1.0 + alpha
        let a1 = -2.0 * cosW
        let a2 = 1.0 - alpha

        return BiquadCoeffs(
            b0: Float(b0 / a0),
            b1: Float(b1 / a0),
            b2: Float(b2 / a0),
            a1: Float(a1 / a0),
            a2: Float(a2 / a0)
        )
    }

    static func highPass(sampleRate: Double, freq: Double, q: Double = 0.70710678) -> BiquadCoeffs {
        let clampedFreq = min(sampleRate * 0.45, max(20.0, freq))
        let w0 = 2.0 * Double.pi * clampedFreq / sampleRate
        let cosW = cos(w0)
        let alpha = sin(w0) / (2.0 * max(0.2, q))

        let onePlusCos = 1.0 + cosW
        let b0 = onePlusCos * 0.5
        let b1 = -onePlusCos
        let b2 = b0
        let a0 = 1.0 + alpha
        let a1 = -2.0 * cosW
        let a2 = 1.0 - alpha

        return BiquadCoeffs(
            b0: Float(b0 / a0),
            b1: Float(b1 / a0),
            b2: Float(b2 / a0),
            a1: Float(a1 / a0),
            a2: Float(a2 / a0)
        )
    }
}

private struct BiquadChannelState {
    var z1: Float = 0
    var z2: Float = 0

    @inline(__always)
    mutating func process(_ x: Float, coeffs: BiquadCoeffs) -> Float {
        let y = coeffs.b0 * x + z1
        z1 = coeffs.b1 * x - coeffs.a1 * y + z2
        z2 = coeffs.b2 * x - coeffs.a2 * y
        return y
    }

    mutating func reset() {
        z1 = 0
        z2 = 0
    }
}

private struct TenBandGains {
    var b0: Float = 0
    var b1: Float = 0
    var b2: Float = 0
    var b3: Float = 0
    var b4: Float = 0
    var b5: Float = 0
    var b6: Float = 0
    var b7: Float = 0
    var b8: Float = 0
    var b9: Float = 0

    func value(at index: Int) -> Float {
        switch index {
        case 0: return b0
        case 1: return b1
        case 2: return b2
        case 3: return b3
        case 4: return b4
        case 5: return b5
        case 6: return b6
        case 7: return b7
        case 8: return b8
        default: return b9
        }
    }
}

private struct SharedDSPParameters {
    var generation: UInt32 = 1
    var eqEnabled: Bool = false
    var preampLinear: Float = 1.0
    var bandGains = TenBandGains()
    var adaptiveBassAmount: Float = 0
    var stereoWidthAmount: Float = 0
    var crossfeedAmount: Float = 0
    var warmTubeEnabled: Bool = false
    var analyzerEnabled: Bool = false

    var hasAnyAudioEffect: Bool {
        eqEnabled
            || abs(preampLinear - 1.0) > 0.01
            || adaptiveBassAmount > 0.01
            || stereoWidthAmount > 0.01
            || crossfeedAmount > 0.01
            || warmTubeEnabled
    }
}

private struct SharedBeatMailbox {
    var kickPeak: Float = 0
    var snapPeak: Float = 0
}

private final class RealtimeTapStorage {
    var isFloat32PCM: Bool = false
    var isNonInterleaved: Bool = true
    var channels: Int = 2
    var sampleRate: Double = 44_100

    var cachedParams = SharedDSPParameters()
    var appliedGeneration: UInt32 = 0

    var eqCoeffs: [BiquadCoeffs] = Array(repeating: .identity, count: 10)
    var eqLeft: [BiquadChannelState] = Array(repeating: BiquadChannelState(), count: 10)
    var eqRight: [BiquadChannelState] = Array(repeating: BiquadChannelState(), count: 10)

    var bassDetectorLPF: BiquadCoeffs = .identity
    var bassDetectorState = BiquadChannelState()
    var bassEnvelope: Float = 0.05
    var adaptiveBassCoeffs: BiquadCoeffs = .identity
    var bassShelfLeft = BiquadChannelState()
    var bassShelfRight = BiquadChannelState()
    var bassUpdateCounter: Int = 0

    var sideHPFCoeffs: BiquadCoeffs = .identity
    var sideHPFState = BiquadChannelState()

    var crossfeedLPFCoeffs: BiquadCoeffs = .identity
    var crossfeedLeftState = BiquadChannelState()
    var crossfeedRightState = BiquadChannelState()

    var kickLPF1: BiquadCoeffs = .identity
    var kickLPF2: BiquadCoeffs = .identity
    var kickState1 = BiquadChannelState()
    var kickState2 = BiquadChannelState()

    var snapHPF1: BiquadCoeffs = .identity
    var snapHPF2: BiquadCoeffs = .identity
    var snapState1 = BiquadChannelState()
    var snapState2 = BiquadChannelState()

    var hopFramesAccum: Int = 0
    var kickEnergySum: Float = 0
    var snapEnergySum: Float = 0
    var kickRollingMean: Float = 0.0005
    var snapRollingMean: Float = 0.0002
    var kickCooldownFrames: Int = 0
    var snapCooldownFrames: Int = 0

    func resetStates() {
        for idx in 0..<10 {
            eqLeft[idx].reset()
            eqRight[idx].reset()
        }
        bassDetectorState.reset()
        bassShelfLeft.reset()
        bassShelfRight.reset()
        sideHPFState.reset()
        crossfeedLeftState.reset()
        crossfeedRightState.reset()
        kickState1.reset()
        kickState2.reset()
        snapState1.reset()
        snapState2.reset()
        bassEnvelope = 0.05
        bassUpdateCounter = 0
        hopFramesAccum = 0
        kickEnergySum = 0
        snapEnergySum = 0
        kickCooldownFrames = 0
        snapCooldownFrames = 0
    }

    func rebuildFilters(sampleRate sr: Double, params: SharedDSPParameters) {
        let validSR = sr > 8_000 ? sr : 44_100
        sampleRate = validSR
        appliedGeneration = params.generation

        let freqs: [Double] = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000]

        for idx in 0..<10 {
            if params.eqEnabled {
                eqCoeffs[idx] = BiquadCoeffs.peakingEQ(
                    sampleRate: validSR,
                    freq: freqs[idx],
                    q: 1.41,
                    gainDB: Double(params.bandGains.value(at: idx))
                )
            } else {
                eqCoeffs[idx] = .identity
            }
        }

        bassDetectorLPF = BiquadCoeffs.lowPass(sampleRate: validSR, freq: 150.0)
        let targetBassDB = Double(params.adaptiveBassAmount) * 8.5
        adaptiveBassCoeffs = BiquadCoeffs.lowShelf(sampleRate: validSR, freq: 100.0, gainDB: targetBassDB)
        sideHPFCoeffs = BiquadCoeffs.highPass(sampleRate: validSR, freq: 150.0)
        crossfeedLPFCoeffs = BiquadCoeffs.lowPass(sampleRate: validSR, freq: 700.0, q: 0.65)

        kickLPF1 = BiquadCoeffs.lowPass(sampleRate: validSR, freq: 110.0, q: 0.541)
        kickLPF2 = BiquadCoeffs.lowPass(sampleRate: validSR, freq: 110.0, q: 1.306)
        snapHPF1 = BiquadCoeffs.highPass(sampleRate: validSR, freq: 2000.0, q: 0.541)
        snapHPF2 = BiquadCoeffs.highPass(sampleRate: validSR, freq: 2000.0, q: 1.306)
    }

    @inline(__always)
    static func softSaturate(_ x: Float, warm: Bool) -> Float {
        if warm {
            let driven = x * 1.14
            let x2 = driven * driven
            return driven * (27.0 + x2) / (27.0 + 9.0 * x2)
        }
        let absX = abs(x)
        if absX <= 0.92 { return x }
        let sign: Float = x >= 0 ? 1.0 : -1.0
        let excess = absX - 0.92
        return sign * (0.92 + 0.08 * (excess / (0.08 + excess)))
    }
}

@MainActor
final class AudioEffectsEngine: ObservableObject {
    static let shared = AudioEffectsEngine()

    nonisolated(unsafe) fileprivate static let paramLock: UnsafeMutablePointer<os_unfair_lock_s> = {
        let p = UnsafeMutablePointer<os_unfair_lock_s>.allocate(capacity: 1)
        p.initialize(to: os_unfair_lock_s())
        return p
    }()

    nonisolated(unsafe) fileprivate static let sharedParamsPtr: UnsafeMutablePointer<SharedDSPParameters> = {
        let p = UnsafeMutablePointer<SharedDSPParameters>.allocate(capacity: 1)
        p.initialize(to: SharedDSPParameters())
        return p
    }()

    nonisolated(unsafe) fileprivate static let beatLock: UnsafeMutablePointer<os_unfair_lock_s> = {
        let p = UnsafeMutablePointer<os_unfair_lock_s>.allocate(capacity: 1)
        p.initialize(to: os_unfair_lock_s())
        return p
    }()

    nonisolated(unsafe) fileprivate static let beatMailboxPtr: UnsafeMutablePointer<SharedBeatMailbox> = {
        let p = UnsafeMutablePointer<SharedBeatMailbox>.allocate(capacity: 1)
        p.initialize(to: SharedBeatMailbox())
        return p
    }()

    @Published var playbackSpeed: Double {
        didSet {
            let clamped = min(2.0, max(0.5, (playbackSpeed * 20.0).rounded() / 20.0))
            if abs(clamped - playbackSpeed) > 0.0001 {
                playbackSpeed = clamped
                return
            }
            UserDefaults.standard.set(playbackSpeed, forKey: "derbis.fx.playbackSpeed")
            onPlaybackRateSettingsChanged?()
        }
    }

    @Published var pitchFollowsSpeed: Bool {
        didSet {
            UserDefaults.standard.set(pitchFollowsSpeed, forKey: "derbis.fx.pitchFollowsSpeed")
            onPlaybackRateSettingsChanged?()
        }
    }

    @Published var eqEnabled: Bool {
        didSet {
            UserDefaults.standard.set(eqEnabled, forKey: "derbis.fx.eqEnabled")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var selectedPreset: EqualizerPreset {
        didSet {
            UserDefaults.standard.set(selectedPreset.rawValue, forKey: "derbis.fx.selectedPreset")
        }
    }

    @Published var bandGainsDB: [Double] {
        didSet {
            if let data = try? JSONEncoder().encode(bandGainsDB) {
                UserDefaults.standard.set(data, forKey: "derbis.fx.bandGainsDB")
            }
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var preampDB: Double {
        didSet {
            UserDefaults.standard.set(preampDB, forKey: "derbis.fx.preampDB")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var adaptiveBassBoost: Double {
        didSet {
            UserDefaults.standard.set(adaptiveBassBoost, forKey: "derbis.fx.adaptiveBassBoost")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var stereoWidener: Double {
        didSet {
            UserDefaults.standard.set(stereoWidener, forKey: "derbis.fx.stereoWidener")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var crossfeedAmount: Double {
        didSet {
            UserDefaults.standard.set(crossfeedAmount, forKey: "derbis.fx.crossfeedAmount")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published var warmTubeSaturation: Bool {
        didSet {
            UserDefaults.standard.set(warmTubeSaturation, forKey: "derbis.fx.warmTubeSaturation")
            pushParametersToRealtimeSnapshot()
        }
    }

    @Published private(set) var dspTapActiveOnCurrentStream: Bool = false

    var onPlaybackRateSettingsChanged: (() -> Void)?

    private var generationCounter: UInt32 = 1

    private init() {
        let savedSpeed = UserDefaults.standard.object(forKey: "derbis.fx.playbackSpeed") as? Double ?? 1.0
        self.playbackSpeed = min(2.0, max(0.5, savedSpeed))
        self.pitchFollowsSpeed = UserDefaults.standard.bool(forKey: "derbis.fx.pitchFollowsSpeed")
        self.eqEnabled = UserDefaults.standard.bool(forKey: "derbis.fx.eqEnabled")

        let savedPreset = UserDefaults.standard.string(forKey: "derbis.fx.selectedPreset") ?? EqualizerPreset.flat.rawValue
        let preset = EqualizerPreset(rawValue: savedPreset) ?? .flat
        self.selectedPreset = preset

        if let data = UserDefaults.standard.data(forKey: "derbis.fx.bandGainsDB"),
           let decoded = try? JSONDecoder().decode([Double].self, from: data),
           decoded.count == 10 {
            self.bandGainsDB = decoded.map { min(12.0, max(-12.0, $0)) }
        } else {
            self.bandGainsDB = preset.gains
        }

        let savedPreamp = UserDefaults.standard.object(forKey: "derbis.fx.preampDB") as? Double ?? 0.0
        self.preampDB = min(6.0, max(-6.0, savedPreamp))

        let savedBass = UserDefaults.standard.object(forKey: "derbis.fx.adaptiveBassBoost") as? Double ?? 0.0
        self.adaptiveBassBoost = min(1.0, max(0.0, savedBass))

        let savedWide = UserDefaults.standard.object(forKey: "derbis.fx.stereoWidener") as? Double ?? 0.0
        self.stereoWidener = min(1.0, max(0.0, savedWide))

        let savedCross = UserDefaults.standard.object(forKey: "derbis.fx.crossfeedAmount") as? Double ?? 0.0
        self.crossfeedAmount = min(1.0, max(0.0, savedCross))

        self.warmTubeSaturation = UserDefaults.standard.bool(forKey: "derbis.fx.warmTubeSaturation")

        pushParametersToRealtimeSnapshot()
    }

    var hasAnyActiveModification: Bool {
        abs(playbackSpeed - 1.0) > 0.01
            || pitchFollowsSpeed
            || eqEnabled
            || abs(preampDB) > 0.05
            || adaptiveBassBoost > 0.01
            || stereoWidener > 0.01
            || crossfeedAmount > 0.01
            || warmTubeSaturation
    }

    func applyPreset(_ preset: EqualizerPreset) {
        selectedPreset = preset
        if preset != .custom {
            bandGainsDB = preset.gains
            if preset != .flat && !eqEnabled {
                eqEnabled = true
            }
        }
    }

    func updateBand(index: Int, gainDB: Double) {
        guard bandGainsDB.indices.contains(index) else { return }
        bandGainsDB[index] = min(12.0, max(-12.0, gainDB))
        selectedPreset = .custom
        if !eqEnabled {
            eqEnabled = true
        }
    }

    func resetAllEffects() {
        playbackSpeed = 1.0
        pitchFollowsSpeed = false
        eqEnabled = false
        selectedPreset = .flat
        bandGainsDB = EqualizerPreset.flat.gains
        preampDB = 0.0
        adaptiveBassBoost = 0.0
        stereoWidener = 0.0
        crossfeedAmount = 0.0
        warmTubeSaturation = false
    }

    func markCurrentStreamTapActive(_ active: Bool) {
        dspTapActiveOnCurrentStream = active
    }

    func preferredPitchAlgorithm(isHLS: Bool) -> AVAudioTimePitchAlgorithm {
        if pitchFollowsSpeed {
            return .varispeed
        }
        return .timeDomain
    }

    func attachAudioMixIfSupported(to item: AVPlayerItem, isDirectFile: Bool) async {
        guard isDirectFile else {
            dspTapActiveOnCurrentStream = false
            return
        }

        var resolvedTrack: AVAssetTrack?
        switch item.asset.status(of: .tracks) {
        case .loaded(let tracks):
            resolvedTrack = tracks.first(where: { $0.mediaType == .audio })
        default:
            resolvedTrack = try? await item.asset.loadTracks(withMediaType: .audio).first
        }

        guard let audioTrack = resolvedTrack,
              let tap = Self.makeAudioProcessingTap() else {
            dspTapActiveOnCurrentStream = false
            return
        }

        let inputParams = AVMutableAudioMixInputParameters(track: audioTrack)
        inputParams.audioTapProcessor = tap
        let mix = AVMutableAudioMix()
        mix.inputParameters = [inputParams]
        item.audioMix = mix
        dspTapActiveOnCurrentStream = true
    }

    func estimatedMagnitudeDB(at frequencyHz: Double) -> Double {
        let sr = 48_000.0
        var totalMagSq = 1.0
        let w = 2.0 * Double.pi * frequencyHz / sr
        let cosW = cos(w)
        let cos2W = cos(2.0 * w)

        if eqEnabled {
            for (idx, freq) in EqualizerPreset.bandFrequencies.enumerated() where idx < bandGainsDB.count {
                let c = BiquadCoeffs.peakingEQ(sampleRate: sr, freq: freq, q: 1.41, gainDB: bandGainsDB[idx])
                totalMagSq *= Self.biquadMagnitudeSquared(c, cosW: cosW, cos2W: cos2W)
            }
        }

        if adaptiveBassBoost > 0.01 {
            let c = BiquadCoeffs.lowShelf(sampleRate: sr, freq: 100.0, gainDB: adaptiveBassBoost * 8.5)
            totalMagSq *= Self.biquadMagnitudeSquared(c, cosW: cosW, cos2W: cos2W)
        }

        let db = 10.0 * log10(max(1e-6, totalMagSq)) + preampDB
        return min(15.0, max(-15.0, db))
    }

    private static func biquadMagnitudeSquared(_ c: BiquadCoeffs, cosW: Double, cos2W: Double) -> Double {
        let b0 = Double(c.b0)
        let b1 = Double(c.b1)
        let b2 = Double(c.b2)
        let a1 = Double(c.a1)
        let a2 = Double(c.a2)

        let numSq = b0 * b0 + b1 * b1 + b2 * b2
        let numCross = 2.0 * (b0 * b1 + b1 * b2) * cosW + 2.0 * b0 * b2 * cos2W
        let num = numSq + numCross

        let denSq = 1.0 + a1 * a1 + a2 * a2
        let denCross = 2.0 * (a1 + a1 * a2) * cosW + 2.0 * a2 * cos2W
        let den = denSq + denCross

        return max(1e-6, num / max(1e-6, den))
    }

    private func pushParametersToRealtimeSnapshot() {
        generationCounter &+= 1
        let g = bandGainsDB.count == 10 ? bandGainsDB : EqualizerPreset.flat.gains
        let preampLin = Float(pow(10.0, preampDB / 20.0))
        var ten = TenBandGains()
        ten.b0 = Float(g[0])
        ten.b1 = Float(g[1])
        ten.b2 = Float(g[2])
        ten.b3 = Float(g[3])
        ten.b4 = Float(g[4])
        ten.b5 = Float(g[5])
        ten.b6 = Float(g[6])
        ten.b7 = Float(g[7])
        ten.b8 = Float(g[8])
        ten.b9 = Float(g[9])

        os_unfair_lock_lock(Self.paramLock)
        var next = Self.sharedParamsPtr.pointee
        next.generation = generationCounter
        next.eqEnabled = eqEnabled
        next.preampLinear = preampLin
        next.bandGains = ten
        next.adaptiveBassAmount = Float(adaptiveBassBoost)
        next.stereoWidthAmount = Float(stereoWidener)
        next.crossfeedAmount = Float(crossfeedAmount)
        next.warmTubeEnabled = warmTubeSaturation
        Self.sharedParamsPtr.pointee = next
        os_unfair_lock_unlock(Self.paramLock)
    }

    nonisolated static func setMusicAnalyzerActive(_ active: Bool) {
        os_unfair_lock_lock(paramLock)
        sharedParamsPtr.pointee.analyzerEnabled = active
        sharedParamsPtr.pointee.generation &+= 1
        os_unfair_lock_unlock(paramLock)

        if !active {
            os_unfair_lock_lock(beatLock)
            beatMailboxPtr.pointee = SharedBeatMailbox()
            os_unfair_lock_unlock(beatLock)
        }
    }

    nonisolated static func consumeDetectedBeatHits() -> DetectedBeatHits {
        guard os_unfair_lock_trylock(beatLock) else { return DetectedBeatHits() }
        let box = beatMailboxPtr.pointee
        beatMailboxPtr.pointee = SharedBeatMailbox()
        os_unfair_lock_unlock(beatLock)
        return DetectedBeatHits(kick: box.kickPeak, snap: box.snapPeak)
    }

    nonisolated private static func makeAudioProcessingTap() -> MTAudioProcessingTap? {
        let storageOpaque = Unmanaged.passRetained(RealtimeTapStorage()).toOpaque()

        var callbacks = MTAudioProcessingTapCallbacks(
            version: kMTAudioProcessingTapCallbacksVersion_0,
            clientInfo: storageOpaque,
            init: { _, clientInfo, tapStorageOut in
                tapStorageOut.pointee = clientInfo
            },
            finalize: { tap in
                Unmanaged<RealtimeTapStorage>.fromOpaque(MTAudioProcessingTapGetStorage(tap)).release()
            },
            prepare: { tap, _, processingFormat in
                let storage = Unmanaged<RealtimeTapStorage>.fromOpaque(MTAudioProcessingTapGetStorage(tap)).takeUnretainedValue()
                let desc = processingFormat.pointee
                storage.isFloat32PCM = (desc.mFormatID == kAudioFormatLinearPCM)
                    && ((desc.mFormatFlags & kAudioFormatFlagIsFloat) != 0)
                    && (desc.mBitsPerChannel == 32)
                storage.isNonInterleaved = (desc.mFormatFlags & kAudioFormatFlagIsNonInterleaved) != 0
                storage.channels = max(1, Int(desc.mChannelsPerFrame))
                storage.resetStates()

                os_unfair_lock_lock(AudioEffectsEngine.paramLock)
                let snap = AudioEffectsEngine.sharedParamsPtr.pointee
                os_unfair_lock_unlock(AudioEffectsEngine.paramLock)

                storage.cachedParams = snap
                storage.rebuildFilters(sampleRate: desc.mSampleRate, params: snap)
            },
            unprepare: { tap in
                let storage = Unmanaged<RealtimeTapStorage>.fromOpaque(MTAudioProcessingTapGetStorage(tap)).takeUnretainedValue()
                storage.resetStates()
            },
            process: { tap, numberFrames, _, bufferListInOut, numberFramesOut, flagsOut in
                let status = MTAudioProcessingTapGetSourceAudio(
                    tap,
                    numberFrames,
                    bufferListInOut,
                    flagsOut,
                    nil,
                    numberFramesOut
                )
                guard status == noErr else { return }

                let frames = Int(numberFramesOut.pointee)
                guard frames > 0 else { return }

                let storage = Unmanaged<RealtimeTapStorage>.fromOpaque(MTAudioProcessingTapGetStorage(tap)).takeUnretainedValue()
                guard storage.isFloat32PCM else { return }

                if os_unfair_lock_trylock(AudioEffectsEngine.paramLock) {
                    storage.cachedParams = AudioEffectsEngine.sharedParamsPtr.pointee
                    os_unfair_lock_unlock(AudioEffectsEngine.paramLock)
                }

                let params = storage.cachedParams
                if params.generation != storage.appliedGeneration {
                    storage.rebuildFilters(sampleRate: storage.sampleRate, params: params)
                }

                let runAudioFX = params.hasAnyAudioEffect
                let runAnalyzer = params.analyzerEnabled
                guard runAudioFX || runAnalyzer else { return }

                let buffers = UnsafeMutableAudioBufferListPointer(bufferListInOut)
                guard !buffers.isEmpty else { return }

                if storage.isNonInterleaved && buffers.count >= 2 {
                    guard let leftRaw = buffers[0].mData, let rightRaw = buffers[1].mData else { return }
                    let leftCount = Int(buffers[0].mDataByteSize) / MemoryLayout<Float>.size
                    let rightCount = Int(buffers[1].mDataByteSize) / MemoryLayout<Float>.size
                    let safeFrames = min(frames, min(leftCount, rightCount))
                    let leftPtr = leftRaw.assumingMemoryBound(to: Float.self)
                    let rightPtr = rightRaw.assumingMemoryBound(to: Float.self)
                    AudioEffectsEngine.processStereoPlanar(
                        storage: storage,
                        params: params,
                        left: leftPtr,
                        right: rightPtr,
                        frameCount: safeFrames,
                        applyFX: runAudioFX,
                        analyze: runAnalyzer
                    )
                } else if !storage.isNonInterleaved && storage.channels == 2, let interleavedRaw = buffers[0].mData {
                    let floatCount = Int(buffers[0].mDataByteSize) / MemoryLayout<Float>.size
                    let safeFrames = min(frames, floatCount / 2)
                    let ptr = interleavedRaw.assumingMemoryBound(to: Float.self)
                    AudioEffectsEngine.processStereoInterleaved(
                        storage: storage,
                        params: params,
                        interleaved: ptr,
                        frameCount: safeFrames,
                        applyFX: runAudioFX,
                        analyze: runAnalyzer
                    )
                } else if let monoRaw = buffers[0].mData {
                    let floatCount = Int(buffers[0].mDataByteSize) / MemoryLayout<Float>.size
                    let safeFrames = min(frames, floatCount)
                    let ptr = monoRaw.assumingMemoryBound(to: Float.self)
                    AudioEffectsEngine.processMono(
                        storage: storage,
                        params: params,
                        mono: ptr,
                        frameCount: safeFrames,
                        applyFX: runAudioFX,
                        analyze: runAnalyzer
                    )
                }
            }
        )

        #if compiler(>=6.2)
        var tap: MTAudioProcessingTap?
        let status = MTAudioProcessingTapCreate(
            kCFAllocatorDefault,
            &callbacks,
            kMTAudioProcessingTapCreationFlag_PostEffects,
            &tap
        )
        guard status == noErr, let tap else {
            Unmanaged<RealtimeTapStorage>.fromOpaque(storageOpaque).release()
            return nil
        }
        return tap
        #else
        var unmanagedTap: Unmanaged<MTAudioProcessingTap>?
        let status = MTAudioProcessingTapCreate(
            kCFAllocatorDefault,
            &callbacks,
            kMTAudioProcessingTapCreationFlag_PostEffects,
            &unmanagedTap
        )
        guard status == noErr, let created = unmanagedTap?.takeRetainedValue() else {
            Unmanaged<RealtimeTapStorage>.fromOpaque(storageOpaque).release()
            return nil
        }
        return created
        #endif
    }

    nonisolated private static func processStereoPlanar(
        storage: RealtimeTapStorage,
        params: SharedDSPParameters,
        left: UnsafeMutablePointer<Float>,
        right: UnsafeMutablePointer<Float>,
        frameCount: Int,
        applyFX: Bool,
        analyze: Bool
    ) {
        var detectedKick: Float = 0
        var detectedSnap: Float = 0

        for i in 0..<frameCount {
            var l = left[i]
            var r = right[i]

            if analyze {
                let monoIn = 0.5 * (l + r)
                stepAnalyzer(storage: storage, sample: monoIn, kickOut: &detectedKick, snapOut: &detectedSnap)
            }

            if applyFX {
                stepStereoSample(storage: storage, params: params, l: &l, r: &r)
                left[i] = l
                right[i] = r
            }
        }

        if analyze && (detectedKick > 0 || detectedSnap > 0) {
            publishBeatPeaks(kick: detectedKick, snap: detectedSnap)
        }
    }

    nonisolated private static func processStereoInterleaved(
        storage: RealtimeTapStorage,
        params: SharedDSPParameters,
        interleaved: UnsafeMutablePointer<Float>,
        frameCount: Int,
        applyFX: Bool,
        analyze: Bool
    ) {
        var detectedKick: Float = 0
        var detectedSnap: Float = 0

        for i in 0..<frameCount {
            let base = i * 2
            var l = interleaved[base]
            var r = interleaved[base + 1]

            if analyze {
                let monoIn = 0.5 * (l + r)
                stepAnalyzer(storage: storage, sample: monoIn, kickOut: &detectedKick, snapOut: &detectedSnap)
            }

            if applyFX {
                stepStereoSample(storage: storage, params: params, l: &l, r: &r)
                interleaved[base] = l
                interleaved[base + 1] = r
            }
        }

        if analyze && (detectedKick > 0 || detectedSnap > 0) {
            publishBeatPeaks(kick: detectedKick, snap: detectedSnap)
        }
    }

    nonisolated private static func processMono(
        storage: RealtimeTapStorage,
        params: SharedDSPParameters,
        mono: UnsafeMutablePointer<Float>,
        frameCount: Int,
        applyFX: Bool,
        analyze: Bool
    ) {
        var detectedKick: Float = 0
        var detectedSnap: Float = 0

        for i in 0..<frameCount {
            var s = mono[i]
            if analyze {
                stepAnalyzer(storage: storage, sample: s, kickOut: &detectedKick, snapOut: &detectedSnap)
            }
            if applyFX {
                var r = s
                stepStereoSample(storage: storage, params: params, l: &s, r: &r)
                mono[i] = 0.5 * (s + r)
            }
        }

        if analyze && (detectedKick > 0 || detectedSnap > 0) {
            publishBeatPeaks(kick: detectedKick, snap: detectedSnap)
        }
    }

    @inline(__always)
    nonisolated private static func stepStereoSample(
        storage: RealtimeTapStorage,
        params: SharedDSPParameters,
        l: inout Float,
        r: inout Float
    ) {
        l *= params.preampLinear
        r *= params.preampLinear

        if params.eqEnabled {
            for b in 0..<10 {
                let c = storage.eqCoeffs[b]
                l = storage.eqLeft[b].process(l, coeffs: c)
                r = storage.eqRight[b].process(r, coeffs: c)
            }
        }

        if params.adaptiveBassAmount > 0.01 {
            let lowMono = storage.bassDetectorState.process(0.5 * (l + r), coeffs: storage.bassDetectorLPF)
            let absLow = abs(lowMono)
            storage.bassEnvelope = storage.bassEnvelope * 0.9985 + absLow * 0.0015
            storage.bassUpdateCounter += 1
            if storage.bassUpdateCounter >= 32 {
                storage.bassUpdateCounter = 0
                let kneeHeadroom = max(0.18, min(1.0, (0.707 - storage.bassEnvelope) / 0.55))
                let effectiveDB = Double(params.adaptiveBassAmount * kneeHeadroom) * 8.5
                storage.adaptiveBassCoeffs = BiquadCoeffs.lowShelf(
                    sampleRate: storage.sampleRate,
                    freq: 100.0,
                    gainDB: effectiveDB
                )
            }
            l = storage.bassShelfLeft.process(l, coeffs: storage.adaptiveBassCoeffs)
            r = storage.bassShelfRight.process(r, coeffs: storage.adaptiveBassCoeffs)
        }

        if params.stereoWidthAmount > 0.01 {
            let mid = 0.5 * (l + r)
            let side = 0.5 * (l - r)
            let highSide = storage.sideHPFState.process(side, coeffs: storage.sideHPFCoeffs)
            let boostedSide = side + highSide * (params.stereoWidthAmount * 0.85)
            l = mid + boostedSide
            r = mid - boostedSide
        }

        if params.crossfeedAmount > 0.01 {
            let lowL = storage.crossfeedLeftState.process(l, coeffs: storage.crossfeedLPFCoeffs)
            let lowR = storage.crossfeedRightState.process(r, coeffs: storage.crossfeedLPFCoeffs)
            let blend = params.crossfeedAmount * 0.34
            let dry = 1.0 - blend * 0.42
            let nextL = l * dry + lowR * blend
            let nextR = r * dry + lowL * blend
            l = nextL
            r = nextR
        }

        l = RealtimeTapStorage.softSaturate(l, warm: params.warmTubeEnabled)
        r = RealtimeTapStorage.softSaturate(r, warm: params.warmTubeEnabled)
    }

    @inline(__always)
    nonisolated private static func stepAnalyzer(
        storage: RealtimeTapStorage,
        sample: Float,
        kickOut: inout Float,
        snapOut: inout Float
    ) {
        let low1 = storage.kickState1.process(sample, coeffs: storage.kickLPF1)
        let low2 = storage.kickState2.process(low1, coeffs: storage.kickLPF2)

        let hi1 = storage.snapState1.process(sample, coeffs: storage.snapHPF1)
        let hi2 = storage.snapState2.process(hi1, coeffs: storage.snapHPF2)

        storage.kickEnergySum += low2 * low2
        storage.snapEnergySum += hi2 * hi2
        storage.hopFramesAccum += 1

        if storage.kickCooldownFrames > 0 { storage.kickCooldownFrames -= 1 }
        if storage.snapCooldownFrames > 0 { storage.snapCooldownFrames -= 1 }

        if storage.hopFramesAccum >= 256 {
            let kickInst = storage.kickEnergySum / 256.0
            let snapInst = storage.snapEnergySum / 256.0
            storage.kickEnergySum = 0
            storage.snapEnergySum = 0
            storage.hopFramesAccum = 0

            let prevKickMean = max(0.00015, storage.kickRollingMean)
            let prevSnapMean = max(0.00008, storage.snapRollingMean)
            storage.kickRollingMean = storage.kickRollingMean * 0.94 + kickInst * 0.06
            storage.snapRollingMean = storage.snapRollingMean * 0.94 + snapInst * 0.06

            let sr = Int(storage.sampleRate)
            if storage.kickCooldownFrames == 0 && kickInst > 0.0012 && (kickInst / prevKickMean) >= 1.65 {
                let norm = min(1.0, sqrt(kickInst) * 2.8)
                if norm > kickOut { kickOut = norm }
                storage.kickCooldownFrames = max(256, sr / 10)
            }

            if storage.snapCooldownFrames == 0 && snapInst > 0.0005 && (snapInst / prevSnapMean) >= 2.2 {
                let norm = min(1.0, sqrt(snapInst) * 3.6)
                if norm > snapOut { snapOut = norm }
                storage.snapCooldownFrames = max(256, (sr * 18) / 100)
            }
        }
    }

    nonisolated private static func publishBeatPeaks(kick: Float, snap: Float) {
        guard os_unfair_lock_trylock(beatLock) else { return }
        if kick > beatMailboxPtr.pointee.kickPeak {
            beatMailboxPtr.pointee.kickPeak = kick
        }
        if snap > beatMailboxPtr.pointee.snapPeak {
            beatMailboxPtr.pointee.snapPeak = snap
        }
        os_unfair_lock_unlock(beatLock)
    }
}
