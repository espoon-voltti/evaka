// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.utils.AttributeMap

@Configuration
class AwsConfig {
    @Bean
    @Profile("local")
    fun credentialsProviderLocal(bucketEnv: BucketEnv): AwsCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                bucketEnv.localS3AccessKeyId,
                bucketEnv.localS3SecretAccessKey,
            )
        )

    @Bean
    @Profile("local")
    fun amazonS3Local(env: BucketEnv, credentialsProvider: AwsCredentialsProvider): S3Client {
        val attrs =
            AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build()
        val client =
            S3Client.builder()
                .httpClient(DefaultSdkHttpClientBuilder().buildWithDefaults(attrs))
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
                .endpointOverride(env.localS3Url)
                .credentialsProvider(credentialsProvider)
                .build()

        val existingBuckets = client.listBuckets().buckets().map { it.name()!! }
        for (bucket in env.allBuckets().filterNot { existingBuckets.contains(it) }) {
            val request = CreateBucketRequest.builder().bucket(bucket).build()
            client.createBucket(request)
        }
        return client
    }

    @Bean
    @Profile("local")
    fun amazonS3PresignerLocal(
        env: BucketEnv,
        credentialsProvider: AwsCredentialsProvider,
    ): S3Presigner =
        S3Presigner.builder()
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(env.localS3Url)
            .credentialsProvider(credentialsProvider)
            .build()

    @Bean
    @Profile("production")
    fun credentialsProviderProd(): AwsCredentialsProvider =
        DefaultCredentialsProvider.builder().build()

    @Bean
    @Profile("production")
    fun amazonS3Prod(env: EvakaEnv, credentialsProvider: AwsCredentialsProvider): S3Client =
        S3Client.builder().credentialsProvider(credentialsProvider).build()

    @Bean
    @Profile("production")
    fun amazonS3PresignerProd(
        env: EvakaEnv,
        credentialsProvider: AwsCredentialsProvider,
    ): S3Presigner = S3Presigner.builder().credentialsProvider(credentialsProvider).build()

    @Lazy
    @Bean
    fun amazonSES(
        env: EvakaEnv,
        emailEnv: EmailEnv,
        awsCredentialsProvider: AwsCredentialsProvider?,
    ): SesClient =
        SesClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .run { if (emailEnv.awsRegion != null) region(Region.of(emailEnv.awsRegion)) else this }
            .build()

    @Lazy
    @Bean
    fun amazonSSM(env: EvakaEnv, awsCredentialsProvider: AwsCredentialsProvider?): SsmClient =
        SsmClient.builder().credentialsProvider(awsCredentialsProvider).build()
}
