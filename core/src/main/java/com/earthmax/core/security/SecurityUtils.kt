package com.earthmax.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security utilities for encryption, hashing, and validation
 */
object SecurityUtils {

    private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val AES_KEY_LENGTH = 256
    private const val IV_LENGTH = 16

    /**
     * Generate a secure random salt
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Hash a password with salt using SHA-256
     */
    fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashedBytes = digest.digest(password.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * Verify a password against a hash
     */
    fun verifyPassword(password: String, salt: ByteArray, expectedHash: String): Boolean {
        val actualHash = hashPassword(password, salt)
        return actualHash == expectedHash
    }

    /**
     * Generate AES key
     */
    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_LENGTH)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt data using AES
     */
    fun encryptAES(data: String, key: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        
        return EncryptedData(
            data = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypt data using AES
     */
    fun decryptAES(encryptedData: EncryptedData, key: SecretKey): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val encryptedBytes = Base64.decode(encryptedData.data, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes)
    }

    /**
     * Create SecretKey from string
     */
    fun createSecretKey(keyString: String): SecretKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Convert SecretKey to string
     */
    fun secretKeyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    /**
     * Validate password strength
     */
    fun validatePasswordStrength(password: String): PasswordValidation {
        val minLength = 8
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        val issues = mutableListOf<String>()
        
        if (password.length < minLength) {
            issues.add("Password must be at least $minLength characters long")
        }
        if (!hasUpperCase) {
            issues.add("Password must contain at least one uppercase letter")
        }
        if (!hasLowerCase) {
            issues.add("Password must contain at least one lowercase letter")
        }
        if (!hasDigit) {
            issues.add("Password must contain at least one digit")
        }
        if (!hasSpecialChar) {
            issues.add("Password must contain at least one special character")
        }
        
        return PasswordValidation(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }

    /**
     * Generate secure random token
     */
    fun generateSecureToken(length: Int = 32): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Hash data using SHA-256
     */
    fun sha256Hash(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(data.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * Sanitize input to prevent injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .trim()
    }

    /**
     * Data class for encrypted data
     */
    data class EncryptedData(
        val data: String,
        val iv: String
    )

    /**
     * Data class for password validation result
     */
    data class PasswordValidation(
        val isValid: Boolean,
        val issues: List<String>
    )
}