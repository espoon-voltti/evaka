// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import com.auth0.jwt.algorithms.Algorithm
import evaka.core.BucketEnv
import evaka.core.shared.config.getTestDataSource
import evaka.core.shared.db.configureJdbi
import evaka.trevaka.s3.createBucketsIfNeeded
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import javax.sql.DataSource
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.utils.AttributeMap

@TestConfiguration
class IntegrationTestConfiguration {
    @Bean fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean fun dataSource(): DataSource = getTestDataSource()

    @Bean
    fun s3Client(bucketEnv: BucketEnv): S3Client =
        S3Client.builder()
            .httpClient(
                DefaultSdkHttpClientBuilder()
                    .buildWithDefaults(
                        AttributeMap.builder()
                            .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                            .build()
                    )
            )
            .region(Region.EU_WEST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(bucketEnv.localS3Url)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        bucketEnv.localS3AccessKeyId,
                        bucketEnv.localS3SecretAccessKey,
                    )
                )
            )
            .build()
            .also { client -> createBucketsIfNeeded(client, bucketEnv.allBuckets()) }

    @Bean
    @Profile("tampere_evaka")
    fun testS3AsyncClient(bucketEnv: BucketEnv): S3AsyncClient =
        S3AsyncClient.crtBuilder()
            .httpConfiguration { it.trustAllCertificatesEnabled(true) }
            .region(Region.EU_WEST_1)
            .forcePathStyle(true)
            .endpointOverride(bucketEnv.localS3Url)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        bucketEnv.localS3AccessKeyId,
                        bucketEnv.localS3SecretAccessKey,
                    )
                )
            )
            .build()

    @Bean
    fun s3Presigner(bucketEnv: BucketEnv): S3Presigner =
        S3Presigner.builder()
            .region(Region.EU_WEST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(bucketEnv.localS3Url)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        bucketEnv.localS3AccessKeyId,
                        bucketEnv.localS3SecretAccessKey,
                    )
                )
            )
            .build()

    @Bean
    fun jwtAlgorithm(): Algorithm {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()
        val jwtPublicKey = keyPair.public as RSAPublicKey
        return Algorithm.RSA256(jwtPublicKey, null)
    }
}
