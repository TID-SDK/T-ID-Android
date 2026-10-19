package ru.tbank.core.tid.codeVerifier

import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Custom thread-safe implementation of the SHA-256 hashing algorithm
 *
 * @see <a href="https://ru.wikipedia.org/wiki/SHA-2">Algorithm implementation description #1</a>
 * @see <a href="https://habr.com/ru/articles/729260/">Algorithm implementation description #2</a>
 *
 * @see <a href="https://github.com/meyfa/java-sha256/blob/main/src/main/java/net/meyfa/sha256/Sha256.java">3rd party SHA-256 implementation (Java)</a>
 * @see <a href="https://github.com/asyncant/sha256-kt/blob/master/src/commonMain/kotlin/com/asyncant/crypto/Sha256.kt">3rd party SHA-256 implementation (Kotlin)</a>
 *
 * @see <a href="https://sha256algorithm.com">Visualization of the algorithm work</a>
 *
 */
@Suppress("MagicNumber")
internal object SHA256Utils {

    // constants
    @Suppress("VariableNaming")
    private val K: IntArray = longArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    ).map { it.toInt() }.toIntArray()

    private const val BITS_IN_BLOCK: Int = 512
    private const val BYTES_IN_BLOCK: Int = BITS_IN_BLOCK / 8
    private const val BITS_IN_WORD: Int = 32
    private const val WORDS_IN_BLOCK: Int = BITS_IN_BLOCK / BITS_IN_WORD
    private const val EXTRA_WORDS_IN_BLOCK: Int = 48
    private const val BYTES: Int = Integer.SIZE / java.lang.Byte.SIZE

    /**
     * Hashes the given message with SHA-256 and returns the hash
     *
     * @param message The bytes to hash
     * @return The hash's bytes
     */
    @Throws(RuntimeException::class)
    internal fun hash(message: ByteArray): ByteArray {
        // hashes with constants
        val hashes: IntArray = longArrayOf(
            0x6a09e667,
            0xbb67ae85,
            0x3c6ef372,
            0xa54ff53a,
            0x510e527f,
            0x9b05688c,
            0x1f83d9ab,
            0x5be0cd19,
        ).map { it.toInt() }.toIntArray()

        val words = pad(message) // split message into 32-bit words

        for (blockOffset in 0 until words.size / WORDS_IN_BLOCK) { // total number of 512-bit blocks
            // 16 original words from current block + 48 generated extra words
            val currentWords = IntArray(WORDS_IN_BLOCK + EXTRA_WORDS_IN_BLOCK)
            System.arraycopy(
                /* src = */ words,
                /* srcPos = */ blockOffset * WORDS_IN_BLOCK,
                /* dest = */ currentWords,
                /* destPos = */0,
                /* length = */WORDS_IN_BLOCK,
            )
            // generate extra 48 words
            for (j in WORDS_IN_BLOCK until (WORDS_IN_BLOCK + EXTRA_WORDS_IN_BLOCK)) {
                currentWords[j] =
                    currentWords[j - 16] + c0(currentWords[j - 15]) + currentWords[j - 7] + c1(currentWords[j - 2])
            }
            compress(currentWords, hashes)
        }

        return toByteArray(hashes)
    }

    private fun compress(words: IntArray, hashes: IntArray) {
        // auxiliary variables
        var a: Int = hashes[0]
        var b: Int = hashes[1]
        var c: Int = hashes[2]
        var d: Int = hashes[3]
        var e: Int = hashes[4]
        var f: Int = hashes[5]
        var g: Int = hashes[6]
        var h: Int = hashes[7]
        var temp1: Int
        var temp2: Int

        for (i in 0 until 64) {
            temp1 = h + sum1(e) + choice(e, f, g) + K[i] + words[i]
            temp2 = sum0(a) + majority(a, b, c)
            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        hashes[0] += a
        hashes[1] += b
        hashes[2] += c
        hashes[3] += d
        hashes[4] += e
        hashes[5] += f
        hashes[6] += g
        hashes[7] += h
    }

    /**
     * Pads the given message to have a length
     * that is a multiple of 512 bits (64 bytes), including the addition of a
     * 1-bit, some padding 0-bits, and the message length as a 64-bit number.
     * The result is a 32-bit integer array with big-endian byte representation.
     *
     * @param message message to pad
     * @return new array with the padded message bytes
     */
    @Throws(RuntimeException::class)
    private fun pad(message: ByteArray): IntArray {
        // new message length in bytes: original length in bytes +
        // 1-bit end padding (rounded up to 1 byte) + 8 bytes for length
        // block count: fully filled blocks + partially filled last block(s) + padding + length
        val finalBlockLength = message.size % BYTES_IN_BLOCK
        val lastBlocksCount = if (finalBlockLength + 1 + 8 > BYTES_IN_BLOCK) {
            2
        } else {
            1
        }
        val blockCount = message.size / BYTES_IN_BLOCK + lastBlocksCount
        val result = IntBuffer.allocate(blockCount * (BYTES_IN_BLOCK / BYTES))

        // copy as much of the message as possible (except the last partially filled block)
        val buffer = ByteBuffer.wrap(message)
        repeat(message.size / BYTES) {
            result.put(buffer.getInt())
        }

        // copy the remaining bytes (less than 4) and append 1 bit (rest is zero)
        val remainder = ByteBuffer.allocate(4)
        remainder.put(buffer).put(0b10000000.toByte()).rewind()
        result.put(remainder.getInt())

        // move current position back by 2 integers (2 * 4 bytes = 8 bytes)
        result.position(result.capacity() - 2)

        // place length (in bits) of the original message at the end as a 64-bit number
        val msgLength = message.size * 8L // calculate number of bits
        result.put((msgLength ushr 32).toInt()) // place 32 most significant bits
        result.put(msgLength.toInt()) // place 32 least significant bits

        return result.array()
    }

    /**
     * Converts the given int array into a byte array via big-endian conversion (1 int becomes 4 bytes)
     *
     * @param ints source array
     * @return converted array
     */
    @Throws(RuntimeException::class)
    private fun toByteArray(ints: IntArray): ByteArray {
        val buffer = ByteBuffer.allocate(ints.size * BYTES)
        for (i in ints) {
            buffer.putInt(i)
        }
        return buffer.array()
    }

    private fun choice(x: Int, y: Int, z: Int): Int {
        return (x and y) xor ((x.inv()) and z)
    }

    private fun majority(x: Int, y: Int, z: Int): Int {
        return (x and y) xor (x and z) xor (y and z)
    }

    private fun sum0(x: Int): Int {
        return (Integer.rotateRight(x, 2)
                xor Integer.rotateRight(x, 13)
                xor Integer.rotateRight(x, 22))
    }

    private fun sum1(x: Int): Int {
        return (Integer.rotateRight(x, 6)
                xor Integer.rotateRight(x, 11)
                xor Integer.rotateRight(x, 25))
    }

    private fun c0(x: Int): Int {
        return (Integer.rotateRight(x, 7)
                xor Integer.rotateRight(x, 18)
                xor (x ushr 3))
    }

    private fun c1(x: Int): Int {
        return (Integer.rotateRight(x, 17)
                xor Integer.rotateRight(x, 19)
                xor (x ushr 10))
    }
}
