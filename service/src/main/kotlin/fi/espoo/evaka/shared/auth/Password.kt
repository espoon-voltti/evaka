// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import fi.espoo.evaka.Sensitive
import java.security.SecureRandom
import java.util.*
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.util.DigestFactory
import org.bouncycastle.util.Arrays

// Password encoding/hashing
// The Bouncy Castle library handles all heavy lifting, such as:
// - Argon2 hash implementation
// - PBKDF2 key derivation implementation
// - constant-time byte array comparison

/** A hashed and encoded password */
data class EncodedPassword(val algorithm: PasswordHashAlgorithm, val salt: Salt, val hash: Hash) {
    init {
        require(algorithm.hashLength() == hash.length()) {
            "Invalid password hash length: expected ${algorithm.hashLength()} bytes, got ${hash.length()}"
        }
    }

    /**
     * Returns true if this encoded password matches the given raw password string.
     *
     * This check uses a constant-time comparison to protect against timing attacks.
     */
    fun isMatch(password: Sensitive<String>): Boolean =
        this.algorithm.hash(this.salt, password) == this.hash

    /** A wrapper of salt bytes for better type-safety and simpler equals/hashCode/toString */
    class Salt(val value: ByteArray) {
        override fun equals(other: Any?): Boolean =
            other is Salt && Arrays.areEqual(this.value, other.value)

        override fun hashCode(): Int = Arrays.hashCode(value)

        @JsonValue override fun toString(): String = Base64.getEncoder().encodeToString(value)

        companion object {
            @JsonCreator
            @JvmStatic
            fun parse(value: String): Salt = Salt(Base64.getDecoder().decode(value))

            fun generate(random: SecureRandom): Salt {
                val bytes = ByteArray(size = 16)
                random.nextBytes(bytes)
                return Salt(bytes)
            }
        }
    }

    /** A wrapper of hash bytes for better type-safety and simpler equals/hashCode/toString */
    class Hash(private val value: ByteArray) {
        override fun equals(other: Any?): Boolean =
            other is Hash &&
                // *IMPORTANT*: uses constant time comparison to protect against timing attacks
                Arrays.constantTimeAreEqual(this.value, other.value)

        override fun hashCode(): Int = Arrays.hashCode(value)

        fun length(): Int = value.size

        @JsonValue override fun toString(): String = Base64.getEncoder().encodeToString(value)

        companion object {
            @JsonCreator
            @JvmStatic
            fun parse(value: String): Hash = Hash(Base64.getDecoder().decode(value))
        }
    }
}

/**
 * A supported password hash algorithm.
 *
 * This is a sealed class and new algorithms can be added in a backwards-compatible way by adding
 * more variants
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
sealed class PasswordHashAlgorithm {
    fun encode(password: Sensitive<String>): EncodedPassword {
        val salt = EncodedPassword.Salt.generate(SECURE_RANDOM)
        return EncodedPassword(this, salt, hash(salt, password))
    }

    abstract fun hash(salt: EncodedPassword.Salt, password: Sensitive<String>): EncodedPassword.Hash

    /** Length of the generated hash in bytes */
    abstract fun hashLength(): Int

    /**
     * Returns a placeholder password that can be used for constant-time checks when a real encoded
     * password is not available.
     *
     * The returned placeholder should have the correct hash length, but otherwise doesn't need to
     * be a valid password
     */
    abstract fun placeholder(): EncodedPassword

    data class Argon2id(
        val hashLength: Int,
        val version: Version,
        val memoryKbytes: Int,
        val iterations: Int,
        val parallelism: Int,
    ) : PasswordHashAlgorithm() {
        enum class Version(val rawValue: Int) {
            VERSION_13(Argon2Parameters.ARGON2_VERSION_13)
        }

        override fun hashLength(): Int = hashLength

        override fun hash(
            salt: EncodedPassword.Salt,
            password: Sensitive<String>,
        ): EncodedPassword.Hash {
            val output = ByteArray(size = hashLength)
            val generator = Argon2BytesGenerator()
            generator.init(
                Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(this.version.rawValue)
                    .withMemoryAsKB(this.memoryKbytes)
                    .withIterations(this.iterations)
                    .withParallelism(this.parallelism)
                    .withSalt(salt.value)
                    .build()
            )
            generator.generateBytes(password.value.toByteArray(Charsets.UTF_8), output)
            return EncodedPassword.Hash(output)
        }

        override fun placeholder(): EncodedPassword =
            EncodedPassword(
                this,
                EncodedPassword.Salt.generate(SECURE_RANDOM),
                EncodedPassword.Hash(ByteArray(size = hashLength)),
            )
    }

    data class Pbkdf2(val hashType: HashType, val keySize: Int, val iterationCount: Int) :
        PasswordHashAlgorithm() {
        enum class HashType {
            SHA256,
            SHA512,
        }

        override fun hashLength(): Int = keySize / 8

        override fun hash(
            salt: EncodedPassword.Salt,
            password: Sensitive<String>,
        ): EncodedPassword.Hash {
            val gen =
                PKCS5S2ParametersGenerator(
                    when (hashType) {
                        HashType.SHA256 -> DigestFactory.createSHA256()
                        HashType.SHA512 -> DigestFactory.createSHA512()
                    }
                )
            gen.init(password.value.toByteArray(Charsets.UTF_8), salt.value, iterationCount)
            val parameters = gen.generateDerivedParameters(keySize)
            return EncodedPassword.Hash((parameters as KeyParameter).key)
        }

        override fun placeholder(): EncodedPassword =
            EncodedPassword(
                this,
                EncodedPassword.Salt.generate(SECURE_RANDOM),
                EncodedPassword.Hash(ByteArray(size = hashLength())),
            )
    }

    companion object {
        // OWASP recommendation: Argon2id, m=19456 (19 MiB), t=2, p=1
        // Reference: OWASP Password Storage Cheat Sheet
        // https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
        val DEFAULT: PasswordHashAlgorithm =
            Argon2id(
                hashLength = 32,
                version = Argon2id.Version.VERSION_13,
                memoryKbytes = 19_456,
                iterations = 2,
                parallelism = 1,
            )

        private val SECURE_RANDOM: SecureRandom = SecureRandom()
    }
}
