package com.luc4n3x.levyra.feature.motion

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class MotionArtworkDestinationPolicyTest {
    private val endpoint = "https://canvas.derbis.example/v1/motion-artwork/resolve".toHttpUrl()

    @Test
    fun allowsOnlyOwnedHttpsMediaAtTheConfiguredCatalogOrigin() {
        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://canvas.derbis.example/media/artist/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://canvas.derbis.example/media/artist/loop.m3u8".toHttpUrl(),
                endpoint
            )
        )

        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://elsewhere.example/media/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://canvas.derbis.example/not-media/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://canvas.derbis.example/media/loop.webm".toHttpUrl(),
                endpoint
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "unknown-provider",
                "https://canvas.derbis.example/media/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "http://canvas.derbis.example/media/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://user:pass@canvas.derbis.example/media/loop.mp4".toHttpUrl(),
                endpoint
            )
        )
    }

    @Test
    fun rejectsMalformedResolverEndpointsAndMediaThatDoesNotMeetResolverContract() {
        assertFalse(isValidDerbisCatalogEndpoint("http://canvas.derbis.example/v1/motion-artwork/resolve".toHttpUrl()))
        assertFalse(isValidDerbisCatalogEndpoint("https://canvas.derbis.example:8443/v1/motion-artwork/resolve".toHttpUrl()))
        assertFalse(isValidDerbisCatalogEndpoint("https://canvas.derbis.example/v1/motion-artwork/resolve?debug=1".toHttpUrl()))
        assertFalse(isValidDerbisCatalogEndpoint("https://canvas.derbis.example/v1/other".toHttpUrl()))

        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                DerbisCanvasCatalogProvider.PROVIDER_ID,
                "https://canvas.derbis.example/media/loop.mp4".toHttpUrl(),
                "https://canvas.derbis.example/v1/motion-artwork/resolve?debug=1".toHttpUrl()
            )
        )
        assertTrue(
            derbisCanvasCatalogMediaUrl(
                "https://canvas.derbis.example/media/loop.mp4",
                endpoint
            ) != null
        )
        assertFalse(
            derbisCanvasCatalogMediaUrl(
                "https://elsewhere.example/media/loop.mp4",
                endpoint
            ) != null
        )
    }

    @Test
    fun rejectsPrivateAndLocalDestinations() {
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("192.168.1.20")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("169.254.1.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("fc00::1")))
        assertTrue(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("1.1.1.1")))
    }
}
