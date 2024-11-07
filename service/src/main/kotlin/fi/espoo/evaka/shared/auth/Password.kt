// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

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
    /**
     * Returns true if this encoded password matches the given raw password string.
     *
     * This check uses a constant-time comparison to protect against timing attacks.
     */
    fun isMatch(password: Sensitive<String>): Boolean =
        this.algorithm.hash(this.salt, password) == this.hash

    override fun toString(): String = "${this.algorithm.id}:${this.salt}:${this.hash}"

    /** A wrapper of salt bytes for better type-safety and simpler equals/hashCode/toString */
    class Salt(val value: ByteArray) {
        override fun equals(other: Any?): Boolean =
            other is Salt && Arrays.areEqual(this.value, other.value)

        override fun hashCode(): Int = Arrays.hashCode(value)

        override fun toString(): String = Base64.getEncoder().encodeToString(value)

        companion object {
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

        override fun toString(): String = Base64.getEncoder().encodeToString(value)

        companion object {
            fun parse(value: String): Hash = Hash(Base64.getDecoder().decode(value))
        }
    }

    companion object {
        fun parse(value: String): EncodedPassword {
            val parts = value.split(':')
            require(parts.size == 3)
            return EncodedPassword(
                algorithm = PasswordHashAlgorithm.byId(PasswordHashAlgorithm.Id(parts[0])),
                salt = Salt.parse(parts[1]),
                hash = Hash.parse(parts[2]),
            )
        }
    }
}

/**
 * A supported password hash algorithm.
 *
 * This is a sealed class and new algorithms can be added in a backwards-compatible way by adding
 * more variants, and modifying the byId function to support them
 */
sealed class PasswordHashAlgorithm {
    /** A textual algorithm id that also stores algorithm-specific parameters */
    @JvmInline
    value class Id(val value: String) {
        override fun toString(): String = value
    }

    abstract val id: Id

    fun encode(password: Sensitive<String>): EncodedPassword {
        val salt = EncodedPassword.Salt.generate(SECURE_RANDOM)
        return EncodedPassword(this, salt, hash(salt, password))
    }

    abstract fun hash(salt: EncodedPassword.Salt, password: Sensitive<String>): EncodedPassword.Hash

    data class Argon2id(val hashLength: Int, val version: Int, val m: Int, val t: Int, val p: Int) :
        PasswordHashAlgorithm() {
        override val id: Id = Id("argon2id;$hashLength;${version.toString(radix = 16)};$m;$t;$p")

        override fun hash(
            salt: EncodedPassword.Salt,
            password: Sensitive<String>,
        ): EncodedPassword.Hash {
            val output = ByteArray(size = hashLength)
            val generator = Argon2BytesGenerator()
            generator.init(
                Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(this.version)
                    .withMemoryAsKB(this.m)
                    .withIterations(this.t)
                    .withParallelism(this.p)
                    .withSalt(salt.value)
                    .build()
            )
            generator.generateBytes(password.value.toByteArray(Charsets.UTF_8), output)
            return EncodedPassword.Hash(output)
        }

        companion object {
            fun parse(id: Id): Argon2id {
                val parts = id.value.split(';')
                require(parts.size == 6)
                require(parts[0] == "argon2id")
                return Argon2id(
                    hashLength = parts[1].toInt(radix = 10),
                    version = parts[2].toInt(radix = 16),
                    m = parts[3].toInt(radix = 10),
                    t = parts[4].toInt(radix = 10),
                    p = parts[5].toInt(radix = 10),
                )
            }
        }
    }

    data class Pbkdf2(val hashType: HashType, val keySize: Int, val iterationCount: Int) :
        PasswordHashAlgorithm() {
        override val id: Id = Id("pbkdf2;$hashType;$keySize;$iterationCount")

        enum class HashType {
            SHA256,
            SHA512,
        }

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

        companion object {
            fun parse(id: Id): Pbkdf2 {
                val parts = id.value.split(';')
                require(parts.size == 4)
                require(parts[0] == "pbkdf2")
                return Pbkdf2(
                    hashType = HashType.valueOf(parts[1]),
                    keySize = parts[2].toInt(radix = 10),
                    iterationCount = parts[3].toInt(radix = 10),
                )
            }
        }
    }

    companion object {
        // OWASP recommendation: Argon2id, m=19456 (19 MiB), t=2, p=1
        // Reference: OWASP Password Storage Cheat Sheet
        // https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
        val DEFAULT: PasswordHashAlgorithm =
            Argon2id(
                hashLength = 32,
                version = Argon2Parameters.ARGON2_VERSION_13,
                m = 19_456,
                t = 2,
                p = 1,
            )

        private val SECURE_RANDOM: SecureRandom = SecureRandom()

        fun byId(id: Id): PasswordHashAlgorithm =
            when {
                id.value.startsWith("argon2id;") -> Argon2id.parse(id)
                id.value.startsWith("pbkdf2;") -> Pbkdf2.parse(id)
                else -> throw IllegalArgumentException("Unsupported password hash $id")
            }
    }
}
