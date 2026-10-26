package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CroppedArtworkUrlTest {

    @Test
    fun wideBannerIsRequestedAsASmartCropOfTheHeroShape() {
        val banner = "https://lh3.googleusercontent.com/synthetic-artist=w2880-h1200-l90-rj"

        assertEquals(
            "https://lh3.googleusercontent.com/synthetic-artist=w1080-h939-p-l90-rj",
            DerbisPersonalOrbit.croppedArtworkUrl(banner, 1080, 939)
        )
    }

    @Test
    fun largeHeroesAreCappedWithoutChangingTheirShape() {
        val portrait = "https://yt3.ggpht.com/synthetic=s900-c-k-c0x00ffffff-no-rj"

        assertEquals(
            "https://yt3.ggpht.com/synthetic=w1200-h600-p-l90-rj",
            DerbisPersonalOrbit.croppedArtworkUrl(portrait, 2400, 1200)
        )
    }

    @Test
    fun nonGoogleOrUnsizedUrlsAreLeftUntouched() {
        val apple = "https://is1-ssl.mzstatic.com/image/thumb/synthetic/600x600bb.jpg"
        val query = "https://lh3.googleusercontent.com/synthetic=w100-h100?x=1"
        val bare = "https://lh3.googleusercontent.com/synthetic"

        assertEquals(apple, DerbisPersonalOrbit.croppedArtworkUrl(apple, 1080, 1080))
        assertEquals(query, DerbisPersonalOrbit.croppedArtworkUrl(query, 1080, 1080))
        assertEquals(bare, DerbisPersonalOrbit.croppedArtworkUrl(bare, 1080, 1080))
        assertEquals("", DerbisPersonalOrbit.croppedArtworkUrl("", 1080, 1080))
    }
}
