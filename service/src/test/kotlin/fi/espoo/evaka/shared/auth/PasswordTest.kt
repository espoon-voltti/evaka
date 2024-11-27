// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.util.stream.Stream
import kotlin.test.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordTest {
    fun algorithms(): Stream<PasswordHashAlgorithm> =
        listOf(
                PasswordHashAlgorithm.DEFAULT,
                // Keycloak default Argon2id
                PasswordHashAlgorithm.Argon2id(
                    hashLength = 32,
                    version = PasswordHashAlgorithm.Argon2id.Version.VERSION_13,
                    memoryKbytes = 7168,
                    iterations = 5,
                    parallelism = 1,
                ),
                // Keycloak old pbkdf2-sha256
                PasswordHashAlgorithm.Pbkdf2(
                    hashType = PasswordHashAlgorithm.Pbkdf2.HashType.SHA256,
                    keySize = 512,
                    iterationCount = 27500,
                ),
            )
            .stream()

    private val jsonMapper = defaultJsonMapperBuilder().build()

    @ParameterizedTest
    @MethodSource("algorithms")
    fun `no validation error is thrown when a placeholder password is generated`(
        algorithm: PasswordHashAlgorithm
    ) {
        assertDoesNotThrow { algorithm.placeholder() }
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    fun `encoding the same raw password twice gives passwords that match the original but have different salt and hash values`(
        algorithm: PasswordHashAlgorithm
    ) {
        val raw = Sensitive("password")

        val first = algorithm.encode(raw)
        assertTrue(first.isMatch(raw))

        val second = algorithm.encode(raw)
        assertTrue(second.isMatch(raw))

        assertNotEquals(first, second)
        assertEquals(first.algorithm, second.algorithm)
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    fun `an encoded password does not match a different raw password`(
        algorithm: PasswordHashAlgorithm
    ) {
        val password = algorithm.encode(Sensitive("password"))
        assertFalse(password.isMatch(Sensitive("wontmatch")))
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    fun `an encoded password can be formatted to a string and parsed back to its original form`(
        algorithm: PasswordHashAlgorithm
    ) {
        val password = algorithm.encode(Sensitive("password"))
        val parsed = jsonMapper.readValue<EncodedPassword>(jsonMapper.writeValueAsString(password))
        assertEquals(password, parsed)
    }

    @Test
    fun `matching an Argon2 password originating from Keycloak is possible`() {
        // Raw example keycloak credential data from the `credential` database table:
        // secret_data:
        // {"value":"O7vS90g14jWr4ESbUpJ3KX5y1NMZcMgjuqPHrZ4Eq8U=","salt":"LgSKeCa5qZH6Dh0PE17AwQ==","additionalParameters":{}}
        // credential_data:
        // {"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}
        val password =
            EncodedPassword(
                PasswordHashAlgorithm.Argon2id(
                    hashLength = 32,
                    version = PasswordHashAlgorithm.Argon2id.Version.VERSION_13,
                    memoryKbytes = 7168,
                    iterations = 5,
                    parallelism = 1,
                ),
                EncodedPassword.Salt.parse("LgSKeCa5qZH6Dh0PE17AwQ=="),
                EncodedPassword.Hash.parse("O7vS90g14jWr4ESbUpJ3KX5y1NMZcMgjuqPHrZ4Eq8U="),
            )
        assertTrue(password.isMatch(Sensitive("testpassword")))
    }

    @Test
    fun `matching a PBKDF2-SHA256 password originating from Keycloak is possible`() {
        // Raw example keycloak credential data from the `credential` database table:
        // secret_data:
        // {"value":"9yUI9up7DA09THuasmN5Z9pN+X5GUvJZY3lnXZYNB/gsGBtL8PjHANnR/H1G3IhhipUr27sBNJ4eek7AMP5UBw==","salt":"VUBELKb6poajPUjlaK1zCQ==","additionalParameters":{}}
        // credential_data:
        // {"hashIterations":27500,"algorithm":"pbkdf2-sha256","additionalParameters":{}}
        val password =
            EncodedPassword(
                PasswordHashAlgorithm.Pbkdf2(
                    hashType = PasswordHashAlgorithm.Pbkdf2.HashType.SHA256,
                    keySize = 512,
                    iterationCount = 27500,
                ),
                EncodedPassword.Salt.parse("VUBELKb6poajPUjlaK1zCQ=="),
                EncodedPassword.Hash.parse(
                    "9yUI9up7DA09THuasmN5Z9pN+X5GUvJZY3lnXZYNB/gsGBtL8PjHANnR/H1G3IhhipUr27sBNJ4eek7AMP5UBw=="
                ),
            )
        assertTrue(password.isMatch(Sensitive("test123")))
    }

    @Test
    fun `matching a PBKDF2-SHA256 password originating from Keycloak is possible (alternative settings)`() {
        // Raw example keycloak credential data from the `credential` database table:
        // secret_data:
        // {"value":"uqz68HhH43ZYJhTWB1L/dudCRIhsud4Xx2NbeG/nOGs=","salt":"0tjU8miBns3EPuTh63LYUA==","additionalParameters":{}}
        // credential_data:
        // {"hashIterations":600000,"algorithm":"pbkdf2-sha256","additionalParameters":{}}
        val password =
            EncodedPassword(
                PasswordHashAlgorithm.Pbkdf2(
                    hashType = PasswordHashAlgorithm.Pbkdf2.HashType.SHA256,
                    keySize = 256,
                    iterationCount = 600000,
                ),
                EncodedPassword.Salt.parse("0tjU8miBns3EPuTh63LYUA=="),
                EncodedPassword.Hash.parse("uqz68HhH43ZYJhTWB1L/dudCRIhsud4Xx2NbeG/nOGs="),
            )
        assertTrue(password.isMatch(Sensitive("testpassword")))
    }

    @Test
    fun `matching a PBKDF2-SHA512 password originating from Keycloak is possible`() {
        // Raw example keycloak credential data from the `credential` database table:
        // secret_data:
        // {"value":"x4QqNb57CHVDsuIM+UMaOeryY7WsOGhxPFzjhEQgoisZ2hrviOSvuokCIXFXHkvBF47BLXDj2MA/g4vUBMuJcw==","salt":"IQU7tTvDbMtynBAFmSOrpA==","additionalParameters":{}}
        // credential_data:
        // {"hashIterations":210000,"algorithm":"pbkdf2-sha512","additionalParameters":{}}
        val password =
            EncodedPassword(
                PasswordHashAlgorithm.Pbkdf2(
                    hashType = PasswordHashAlgorithm.Pbkdf2.HashType.SHA512,
                    keySize = 512,
                    iterationCount = 210000,
                ),
                EncodedPassword.Salt.parse("IQU7tTvDbMtynBAFmSOrpA=="),
                EncodedPassword.Hash.parse(
                    "x4QqNb57CHVDsuIM+UMaOeryY7WsOGhxPFzjhEQgoisZ2hrviOSvuokCIXFXHkvBF47BLXDj2MA/g4vUBMuJcw=="
                ),
            )
        assertTrue(password.isMatch(Sensitive("testpassword")))
    }
}
