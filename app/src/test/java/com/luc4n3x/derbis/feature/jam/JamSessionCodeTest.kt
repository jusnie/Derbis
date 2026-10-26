package com.luc4n3x.derbis.feature.jam

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamSessionCodeTest {

    @Test
    fun alphabeticInviteCodeAndDeepLinkRoundTrip() {
        val code = JamSessionCode("192.168.1.42", 42_424, "00112233445566778899aabbccddeeff")
        val invite = code.formatted()

        assertEquals(code, JamSessionCode.parse(invite))
        assertEquals(code, JamSessionCode.parse(code.deepLink()))
        assertEquals(JamSessionCode.ENCODED_LENGTH, invite.length)
        assertTrue(invite.length >= JamSessionCode.MIN_INVITE_LETTERS)
        assertTrue(invite.all(Char::isLetter))
        assertEquals(invite, code.encoded())
    }

    @Test
    fun decorationsDigitsAndWrongLengthAreRejected() {
        val code = JamSessionCode("10.0.0.8", 54_321, "aabbccddeeff00112233445566778899")

        assertNull(JamSessionCode.parse("#${code.formatted()}!"))
        assertNull(JamSessionCode.parse("derbis://other/${code.encoded()}"))
        assertNull(JamSessionCode.parse("not-a-jam-code"))
        assertNull(JamSessionCode.parse(code.encoded().replaceRange(0, 1, "1")))
        assertNull(JamSessionCode.parse(code.encoded().chunked(6).joinToString("-")))
        assertNull(JamSessionCode.parse("ABCDEF"))
    }

    @Test
    fun publicAddressesAndInvalidPortsAreRejected() {
        assertNull(JamSessionCode.parse(encodeRaw("8.8.8.8", 42_424, "00112233445566778899aabbccddeeff")))
        assertNull(JamSessionCode.parse(encodeRaw("192.168.1.42", 80, "00112233445566778899aabbccddeeff")))
        assertNull(JamSessionCode.parse(encodeRaw("192.168.1.42", 0, "00112233445566778899aabbccddeeff")))
    }

    @Test
    fun privateAddressCatalogMatchesLanRanges() {
        assertTrue(JamSessionCode.isPrivateIpv4("10.0.0.1"))
        assertTrue(JamSessionCode.isPrivateIpv4("172.16.0.1"))
        assertTrue(JamSessionCode.isPrivateIpv4("172.31.255.254"))
        assertTrue(JamSessionCode.isPrivateIpv4("192.168.4.5"))
        assertTrue(JamSessionCode.isPrivateIpv4("169.254.1.2"))
        assertFalse(JamSessionCode.isPrivateIpv4("172.32.0.1"))
        assertFalse(JamSessionCode.isPrivateIpv4("127.0.0.1"))
        assertFalse(JamSessionCode.isPrivateIpv4("224.0.0.1"))
    }

    @Test
    fun generatedSecretsUseAllConfiguredEntropyBytes() {
        val generated = List(128) { JamSessionCode.newSecret() }

        assertEquals(128, JamSessionCode.SECRET_BITS)
        assertTrue(generated.all { JamSessionCode.isValidSecret(it) })
        assertEquals(generated.size, generated.toSet().size)
        assertNotEquals(generated.first(), generated.last())
    }

    @Test
    fun legacyNumericSessionCodesAreRejected() {
        assertNull(JamSessionCode.parse("R2M02AN5Q00128HK8G"))
        assertFalse(JamSessionCode.isValidSecret("0011223344"))
    }

    private fun encodeRaw(address: String, port: Int, secret: String): String {
        val bytes = ByteArray(JamSessionCode.TOTAL_BYTES)
        address.split('.').forEachIndexed { index, value -> bytes[index] = value.toInt().toByte() }
        bytes[JamSessionCode.ADDRESS_BYTES] = (port ushr 8).toByte()
        bytes[JamSessionCode.ADDRESS_BYTES + 1] = port.toByte()
        secret.chunked(2).forEachIndexed { index, value ->
            bytes[JamSessionCode.ADDRESS_BYTES + JamSessionCode.PORT_BYTES + index] = value.toInt(16).toByte()
        }
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val base = BigInteger.valueOf(alphabet.length.toLong())
        var number = BigInteger(1, bytes)
        return CharArray(JamSessionCode.ENCODED_LENGTH) {
            val division = number.divideAndRemainder(base)
            number = division[0]
            alphabet[division[1].toInt()]
        }.reversedArray().concatToString()
    }
}
