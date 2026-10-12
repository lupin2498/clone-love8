package com.clone.love8.sync

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Mã hóa đầu-cuối: khóa AES-256 và tên kênh đều được suy ra từ mã kết nối,
 * máy chủ trung gian chỉ thấy dữ liệu đã mã hóa.
 */
object Crypto {
    private const val PREFIX = "L8C1:"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun newCode(): String = (1..8).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    fun normalize(code: String): String = code.trim().uppercase().filter { it.isLetterOrDigit() }

    fun pretty(code: String): String = if (code.length == 8) code.substring(0, 4) + "-" + code.substring(4) else code

    private fun sha256(s: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    fun topic(code: String): String = "l8c_" + hex(sha256("love8clone-topic:" + normalize(code)).copyOfRange(0, 12))

    private fun key(code: String): SecretKeySpec = SecretKeySpec(sha256("love8clone-key:" + normalize(code)), "AES")

    fun encrypt(code: String, plain: String): String {
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(code), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    fun decrypt(code: String, payload: String): String? {
        if (!payload.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.decode(payload.substring(PREFIX.length), Base64.NO_WRAP)
            if (raw.size < 13) return null
            val iv = raw.copyOfRange(0, 12)
            val ct = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(code), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
