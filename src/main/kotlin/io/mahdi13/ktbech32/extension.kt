package io.mahdi13.ktbech32

fun ByteArray.toBech32(humanReadablePart: String): String =
    Bech32.bech32Encode(humanReadablePart.toByteArray(), this)


fun String.decodeBech32(): Pair<ByteArray, String> =
    Bech32.bech32Decode(this).let { it.data to it.hrp.toString() }