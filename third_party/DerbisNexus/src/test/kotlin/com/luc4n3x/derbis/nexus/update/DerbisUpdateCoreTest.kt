package com.luc4n3x.derbis.nexus.update

import com.luc4n3x.derbis.nexus.core.DerbisFailureType
import com.luc4n3x.derbis.nexus.core.DerbisResult
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisUpdateCoreTest {
    @Test
    fun selectsNativeAbiBeforeUniversal() {
        val selected = DerbisUpdateSelector.selectArtifact(
            artifacts = listOf(
                DerbisUpdateArtifact("derbis-universal-release.apk", "https://a/u.apk", abi = "universal"),
                DerbisUpdateArtifact("derbis-arm64-v8a-release.apk", "https://a/a.apk", abi = "arm64-v8a")
            ),
            sdk = 35,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )
        assertEquals("derbis-arm64-v8a-release.apk", selected?.name)
    }

    @Test
    fun stableVersionBeatsPrereleaseWithSameNumbers() {
        assertTrue(DerbisVersionComparator.compare("2.3.16", "2.3.16-beta2") > 0)
        assertTrue(DerbisVersionComparator.compare("2.3.17-beta2", "2.3.16") > 0)
        assertTrue(DerbisVersionComparator.compare("2.3.16-beta2", "2.3.16-beta1") > 0)
        assertTrue(DerbisVersionComparator.compare("2.3.16-rc1", "2.3.16-beta9") > 0)
    }

    @Test
    fun infersAbiFromFilenameAndRejectsIncompatibleArtifacts() {
        val inferred = DerbisUpdateSelector.selectArtifact(
            artifacts = listOf(DerbisUpdateArtifact("derbis-arm64-v8a-release.apk", "https://a/a.apk")),
            sdk = 35,
            supportedAbis = listOf("arm64-v8a")
        )
        assertEquals("derbis-arm64-v8a-release.apk", inferred?.name)
        val incompatible = DerbisUpdateSelector.selectArtifact(
            artifacts = listOf(DerbisUpdateArtifact("derbis-arm64-v8a-release.apk", "https://a/a.apk")),
            sdk = 35,
            supportedAbis = listOf("x86_64")
        )
        assertEquals(null, incompatible)
    }

    @Test
    fun rolloutIsDeterministic() {
        val first = DerbisUpdateSelector.rolloutBucket("device-1")
        assertEquals(first, DerbisUpdateSelector.rolloutBucket("device-1"))
        assertFalse(DerbisUpdateSelector.isEligibleForRollout("device-1", 0))
        assertTrue(DerbisUpdateSelector.isEligibleForRollout("device-1", 100))
    }

    @Test
    fun rejectsArtifactWithoutDigestOrSignature() {
        val result = DerbisUpdateVerifier.verifyArtifact(
            input = ByteArrayInputStream("untrusted".toByteArray()),
            artifact = DerbisUpdateArtifact(
                name = "derbis-universal-release.apk",
                downloadUrl = "https://example.com/derbis.apk"
            ),
            inspection = DerbisArtifactInspection(
                packageName = "com.luc4n3x.derbis",
                signerSha256 = "signer",
                sdk = 35,
                abi = "arm64-v8a"
            ),
            expectedPackageName = "com.luc4n3x.derbis"
        )
        val failure = result as DerbisResult.Failure
        assertEquals(DerbisFailureType.SECURITY, failure.type)
        assertFalse(failure.retryable)
        assertTrue(failure.message.contains("SHA-256 or signature"))
    }

    @Test
    fun rejectsDetachedSignatureWithoutPublicKey() {
        val result = DerbisUpdateVerifier.verifyArtifact(
            input = ByteArrayInputStream("untrusted".toByteArray()),
            artifact = DerbisUpdateArtifact(
                name = "derbis-universal-release.apk",
                downloadUrl = "https://example.com/derbis.apk",
                signatureBase64 = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
            ),
            inspection = DerbisArtifactInspection(
                packageName = "com.luc4n3x.derbis",
                signerSha256 = "signer",
                sdk = 35,
                abi = "arm64-v8a"
            ),
            expectedPackageName = "com.luc4n3x.derbis"
        )
        val failure = result as DerbisResult.Failure
        assertEquals(DerbisFailureType.SECURITY, failure.type)
        assertFalse(failure.retryable)
        assertTrue(failure.message.contains("public key"))
    }

    @Test
    fun verifiesManifestAndArtifact() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val bytes = "derbis-apk".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val artifactSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(bytes)
            Base64.getEncoder().encodeToString(sign())
        }
        val artifact = DerbisUpdateArtifact(
            name = "derbis-arm64-v8a-release.apk",
            downloadUrl = "https://example.com/derbis.apk",
            sizeBytes = bytes.size.toLong(),
            abi = "arm64-v8a",
            minSdk = 26,
            sha256 = hash,
            signatureBase64 = artifactSignature
        )
        val unsignedManifest = DerbisUpdateManifest(
            channel = DerbisUpdateChannel.STABLE,
            versionCode = 2_030_017L,
            versionName = "2.3.17",
            packageName = "com.luc4n3x.derbis",
            artifacts = listOf(artifact)
        )
        val manifestSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(unsignedManifest.canonicalPayload())
            Base64.getEncoder().encodeToString(sign())
        }
        val manifest = unsignedManifest.copy(signatureBase64 = manifestSignature)
        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertTrue(DerbisUpdateVerifier.verifyManifestSignature(manifest, publicKey) is DerbisResult.Success)
        val verified = DerbisUpdateVerifier.verifyArtifact(
            input = ByteArrayInputStream(bytes),
            artifact = artifact,
            inspection = DerbisArtifactInspection(
                packageName = "com.luc4n3x.derbis",
                signerSha256 = "signer",
                sdk = 35,
                abi = "arm64-v8a"
            ),
            expectedPackageName = "com.luc4n3x.derbis",
            publicKeyBase64 = publicKey
        )
        assertTrue(verified is DerbisResult.Success)
    }
}
