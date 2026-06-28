package com.example.location_where.utils

import com.example.location_where.BuildConfig
import java.io.*
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val IV_BYTES = BuildConfig.ENCRYPTION_IV.toByteArray()

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun getSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(BuildConfig.ENCRYPTION_PASSPHRASE.toByteArray())
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val key = getSecretKey()
        val iv = IvParameterSpec(IV_BYTES)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                CipherOutputStream(fos, cipher).use { cos ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexString()
    }
}
