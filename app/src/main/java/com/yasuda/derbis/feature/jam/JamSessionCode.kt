package com.yasuda.derbis.feature.jam

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Locale

data class JamSessionCode(
    val hostAddress: String,
    val port: Int,
    val secret: String
) {
    /** A shareable, letters-only invite code. */
    fun encoded(): String {
        require(isPrivateIpv4(hostAddress))
        require(port in MIN_PORT..MAX_PORT)
        require(isValidSecret(secret))
        val octets = hostAddress.split('.').map(String::toInt)
        val bytes = ByteArray(TOTAL_BYTES)
        octets.take(ADDRESS_BYTES).forEachIndexed { index, value -> bytes[index] = value.toByte() }
        bytes[ADDRESS_BYTES] = ((port shr 8) and 0xFF).toByte()
        bytes[ADDRESS_BYTES + 1] = (port and 0xFF).toByte()
        decodeHex(secret).copyInto(bytes, ADDRESS_BYTES + PORT_BYTES)
        return encodeLetters(bytes)
    }

    /** Formatting intentionally adds no separators so the displayed invite contains letters only. */
    fun formatted(): String = encoded()

    fun deepLink(): String = "$DEEP_LINK_PREFIX${encoded()}"

    companion object {
        const val SECRET_BYTES = 16
        const val SECRET_BITS = SECRET_BYTES * 8
        const val ADDRESS_BYTES = 4
        const val PORT_BYTES = 2
        const val TOTAL_BYTES = ADDRESS_BYTES + PORT_BYTES + SECRET_BYTES
        const val MIN_INVITE_LETTERS = 6
        // ceil(log_26(2^(TOTAL_BYTES * 8))). The current payload needs 38 letters.
        const val ENCODED_LENGTH = 38
        const val DEEP_LINK_PREFIX = "derbis://jam/"
        const val MIN_PORT = 1024
        const val MAX_PORT = 65_535

        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val letterBase = BigInteger.valueOf(ALPHABET.length.toLong())
        private val payloadLimit = BigInteger.ONE.shiftLeft(TOTAL_BYTES * 8)
        private val secretPattern = Regex("[0-9a-fA-F]{${SECRET_BYTES * 2}}")
        private val random = SecureRandom()

        init {
            check(letterBase.pow(ENCODED_LENGTH) >= payloadLimit)
        }

        fun newSecret(): String {
            val bytes = ByteArray(SECRET_BYTES)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun isValidSecret(value: String): Boolean = secretPattern.matches(value)

        fun parse(raw: String): JamSessionCode? {
            val trimmed = raw.trim()
            val body = if (trimmed.startsWith(DEEP_LINK_PREFIX, ignoreCase = true)) {
                trimmed.substring(DEEP_LINK_PREFIX.length)
            } else {
                trimmed
            }
            if (body.length != ENCODED_LENGTH || body.length < MIN_INVITE_LETTERS) return null
            val normalized = body.uppercase(Locale.ROOT)
            if (normalized.any { it !in ALPHABET }) return null
            val bytes = decodeLetters(normalized) ?: return null

            val address = (0 until ADDRESS_BYTES).joinToString(".") { (bytes[it].toInt() and 0xFF).toString() }
            val port = ((bytes[ADDRESS_BYTES].toInt() and 0xFF) shl 8) or (bytes[ADDRESS_BYTES + 1].toInt() and 0xFF)
            if (port !in MIN_PORT..MAX_PORT) return null
            if (!isPrivateIpv4(address)) return null
            val secret = (ADDRESS_BYTES + PORT_BYTES until TOTAL_BYTES)
                .joinToString("") { "%02x".format(bytes[it]) }
            return JamSessionCode(address, port, secret)
        }

        fun isPrivateIpv4(address: String): Boolean {
            val parts = address.split('.')
            if (parts.size != ADDRESS_BYTES) return false
            val octets = parts.map { it.toIntOrNull() ?: return false }
            if (octets.any { it !in 0..255 }) return false
            return when {
                octets[0] == 10 -> true
                octets[0] == 172 && octets[1] in 16..31 -> true
                octets[0] == 192 && octets[1] == 168 -> true
                octets[0] == 169 && octets[1] == 254 -> true
                else -> false
            }
        }

        private fun encodeLetters(bytes: ByteArray): String {
            var value = BigInteger(1, bytes)
            val output = CharArray(ENCODED_LENGTH) { ALPHABET.first() }
            for (index in output.lastIndex downTo 0) {
                val division = value.divideAndRemainder(letterBase)
                output[index] = ALPHABET[division[1].toInt()]
                value = division[0]
            }
            check(value == BigInteger.ZERO)
            return output.concatToString()
        }

        private fun decodeLetters(value: String): ByteArray? {
            var number = BigInteger.ZERO
            value.forEach { letter ->
                val digit = ALPHABET.indexOf(letter)
                if (digit < 0) return null
                number = number.multiply(letterBase).add(BigInteger.valueOf(digit.toLong()))
            }
            if (number >= payloadLimit) return null
            val raw = number.toByteArray()
            val bytes = ByteArray(TOTAL_BYTES)
            val count = raw.size.coerceAtMost(TOTAL_BYTES)
            raw.copyInto(
                destination = bytes,
                destinationOffset = TOTAL_BYTES - count,
                startIndex = raw.size - count
            )
            return bytes
        }

        private fun decodeHex(value: String): ByteArray {
            val clean = value.trim()
            val bytes = ByteArray(SECRET_BYTES)
            for (index in 0 until SECRET_BYTES) {
                val start = index * 2
                if (start + 2 > clean.length) break
                bytes[index] = clean.substring(start, start + 2).toIntOrNull(16)?.toByte() ?: 0
            }
            return bytes
        }
    }
}
