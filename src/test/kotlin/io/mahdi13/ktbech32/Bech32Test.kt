package io.mahdi13.ktbech32

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.komputing.khex.extensions.hexToByteArray

class Bech32Test {

    val testCases = listOf(
        "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4" to ("0014751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray() to "BC"),
        "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4" to ("0014751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray() to "BC")
    )

    @Test
    fun `Test Hex to Bech32`() {
        testCases.forEach {
            assertEquals(it.first, it.second.first.toBech32(it.second.second))
        }
    }

    @Test
    fun `Test Bech32 to Hex`() {
        testCases.forEach {
            it.first.decodeBech32().let { r ->
                assertTrue(r.first.contentEquals(it.second.first))
                assertEquals(r.second, it.second.second)
            }
        }
    }

}