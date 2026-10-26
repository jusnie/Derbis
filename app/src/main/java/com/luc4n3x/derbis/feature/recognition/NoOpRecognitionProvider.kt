package com.luc4n3x.derbis.feature.recognition

object NoOpRecognitionProvider : RecognitionProvider {
    override val id: String = "noop"

    override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome =
        RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)
}
