// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku

import com.auth0.jwt.algorithms.Algorithm
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.turku.invoice.service.SftpConnector
import fi.espoo.evaka.turku.invoice.service.SftpSender
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.utils.AttributeMap

@TestConfiguration
class IntegrationTestConfiguration {
    @Bean
    fun s3Client(bucketEnv: BucketEnv, turkuProperties: TurkuProperties): S3Client {
        val attrs =
            AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build()

        val client =
            S3Client.builder()
                .httpClient(DefaultSdkHttpClientBuilder().buildWithDefaults(attrs))
                .region(Region.EU_WEST_1)
                .serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
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

        val existingBuckets = client.listBuckets().buckets().map { it.name()!! }
        val allBuckets = bucketEnv.allBuckets() + turkuProperties.bucket.allBuckets()
        for (bucket in allBuckets.filterNot { existingBuckets.contains(it) }) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
        }
        return client
    }

    @Bean
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
    fun sftpSender(properties: TurkuProperties, connector: SftpConnector): SftpSender =
        SftpSender(properties.dwExport.sftp, connector)

    @Bean
    fun jwtAlgorithm(): Algorithm {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()
        val jwtPublicKey = keyPair.public as RSAPublicKey
        return Algorithm.RSA256(jwtPublicKey, null)
    }
}
