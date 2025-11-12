package com.example.cleanup.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RsaUtil {
    fun encryptToBase64(publicKeyPem: String, plain: ByteArray): String {
        try {
            val cleaned = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            val keySpec = X509EncodedKeySpec(Base64.decode(cleaned, Base64.DEFAULT))
            val kf = KeyFactory.getInstance("RSA")
            val pubKey = kf.generatePublic(keySpec)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, pubKey)

            val inputLen = plain.size
            var offset = 0
            val outputStream = ByteArrayOutputStream()

            val encryptBlock = 2048 / 8 - 11
            while (inputLen - offset > 0) {
                val length = if (inputLen - offset > encryptBlock) {
                    encryptBlock
                } else {
                    inputLen - offset
                }
                val encryptedBlock = cipher.doFinal(plain, offset, length)
                outputStream.write(encryptedBlock)
                offset += length
            }

            outputStream.close()
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            println("encryptToBase64 failed: $e")
            return ""
        }
    }
}