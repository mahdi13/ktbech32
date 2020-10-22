package io.mahdi13.ktbech32

import java.util.*
import kotlin.experimental.and


object Bech32 {
    const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    const val SEPARATOR = 0x31.toChar() // '1'
    fun bech32Encode(hrp: ByteArray, data: ByteArray): String {
        val chk = createChecksum(hrp, data)
        val combined = ByteArray(chk.size + data.size)
        System.arraycopy(data, 0, combined, 0, data.size)
        System.arraycopy(chk, 0, combined, data.size, chk.size)
        val xlat = ByteArray(combined.size)
        for (i in combined.indices) {
            xlat[i] = CHARSET[combined[i].toInt()].toByte()
        }
        val ret = ByteArray(hrp.size + xlat.size + 1)
        System.arraycopy(hrp, 0, ret, 0, hrp.size)
        System.arraycopy(byteArrayOf(0x31), 0, ret, hrp.size, 1)
        System.arraycopy(xlat, 0, ret, hrp.size + 1, xlat.size)
        return String(ret)
    }

    fun bech32Decode(bech: String): HrpAndData {
        var bech = bech
        require(!(bech != bech.toLowerCase() && bech != bech.toUpperCase())) { "bech32 cannot mix upper and lower case" }
        val buffer = bech.toByteArray()
        for (b in buffer) {
            require(!(b < 0x21 || b > 0x7e)) { "bech32 characters out of range" }
        }
        bech = bech.toLowerCase()
        val pos = bech.lastIndexOf("1")
        require(pos >= 1) { "bech32 missing separator" }
        require(pos + 7 <= bech.length) { "bech32 separator misplaced" }
        require(bech.length >= 8) { "bech32 input too short" }
        require(bech.length <= 90) { "bech32 input too long" }
        val s = bech.substring(pos + 1)
        for (element in s) {
            require(CHARSET.indexOf(element) != -1) { "bech32 characters  out of range" }
        }
        val hrp = bech.substring(0, pos).toByteArray()
        val data = ByteArray(bech.length - pos - 1)
        var j = 0
        var i = pos + 1
        while (i < bech.length) {
            data[j] = CHARSET.indexOf(bech[i]).toByte()
            i++
            j++
        }
        require(verifyChecksum(hrp, data)) { "invalid bech32 checksum" }
        val ret = ByteArray(data.size - 6)
        System.arraycopy(data, 0, ret, 0, data.size - 6)
        return HrpAndData(hrp, ret)
    }

    private fun polymod(values: ByteArray): Int {
        val GENERATORS = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        for (b in values) {
            val top = (chk shr 0x19).toByte()
            chk = b xori (chk and 0x1ffffff shl 5)
            for (i in 0..4) {
                chk = chk xor if (top shr i and 1 == 1.toByte()) GENERATORS[i] else 0
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: ByteArray): ByteArray {
        val buf1 = ByteArray(hrp.size)
        val buf2 = ByteArray(hrp.size)
        val mid = ByteArray(1)
        for (i in hrp.indices) {
            buf1[i] = (hrp[i] shr 5) as Byte
        }
        mid[0] = 0x00
        for (i in hrp.indices) {
            buf2[i] = (hrp[i] and 0x1f) as Byte
        }
        val ret = ByteArray(hrp.size * 2 + 1)
        System.arraycopy(buf1, 0, ret, 0, buf1.size)
        System.arraycopy(mid, 0, ret, buf1.size, mid.size)
        System.arraycopy(buf2, 0, ret, buf1.size + mid.size, buf2.size)
        return ret
    }

    private fun verifyChecksum(hrp: ByteArray, data: ByteArray): Boolean {
        val exp = hrpExpand(hrp)
        val values = ByteArray(exp.size + data.size)
        System.arraycopy(exp, 0, values, 0, exp.size)
        System.arraycopy(data, 0, values, exp.size, data.size)
        return 1 == polymod(values)
    }

    private fun createChecksum(hrp: ByteArray, data: ByteArray): ByteArray {
        val zeroes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val expanded = hrpExpand(hrp)
        val values = ByteArray(zeroes.size + expanded.size + data.size)
        System.arraycopy(expanded, 0, values, 0, expanded.size)
        System.arraycopy(data, 0, values, expanded.size, data.size)
        System.arraycopy(zeroes, 0, values, expanded.size + data.size, zeroes.size)
        val polymod = polymod(values) xor 1
        val ret = ByteArray(6)
        for (i in ret.indices) {
            ret[i] = (polymod shr 5 * (5 - i) and 0x1f).toByte()
        }
        return ret
    }

    class HrpAndData(var hrp: ByteArray, var data: ByteArray) {

        override fun toString(): String {
            return "HrpAndData [hrp=" + Arrays.toString(hrp) + ", data=" + Arrays.toString(data) + "]"
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + Arrays.hashCode(data)
            result = prime * result + Arrays.hashCode(hrp)
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null) return false
            if (javaClass != obj.javaClass) return false
            val other = obj as HrpAndData
            if (!Arrays.equals(data, other.data)) return false
            return if (!Arrays.equals(hrp, other.hrp)) false else true
        }
    }
}

private infix fun Byte.xor(other: Int): Byte = (this.toInt() xor other).toByte()
private infix fun Int.xor(other: Byte): Byte = (this xor other.toInt()).toByte()
private infix fun Byte.xori(other: Int): Int = (this.toInt() xor other)
private infix fun Int.xori(other: Byte): Int = (this xor other.toInt())
private infix fun Byte.shr(bitCount: Int): Byte = (this.toInt() shr bitCount).toByte()
private infix fun Byte.and(other: Byte): Byte = (this.toInt() and other.toInt()).toByte()
