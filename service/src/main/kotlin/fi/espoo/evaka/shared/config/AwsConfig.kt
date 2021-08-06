// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EvakaEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.ses.SesClient

@Configuration
class AwsConfig {
    @Bean
    @Profile("production")
    fun containerCredentialsProvider() = DefaultCredentialsProvider.create()

    @Bean
    @Profile("local")
    fun amazonS3Local(env: BucketEnv): S3Client {
        val client = S3Client.builder()
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(env.s3MockUrl)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .build()

        for (bucket in env.allBuckets()) {
            val request = CreateBucketRequest.builder().bucket(bucket).build()
            client.createBucket(request)
        }

        return client
    }

    @Bean
    @Profile("production")
    fun amazonS3Prod(env: EvakaEnv, credentialsProvider: AwsCredentialsProvider): S3Client = S3Client.builder()
        .region(env.awsRegion)
        .credentialsProvider(credentialsProvider)
        .build()

    @Bean
    fun amazonSES(env: EvakaEnv, awsCredentialsProvider: AwsCredentialsProvider?): SesClient = SesClient.builder()
        .credentialsProvider(awsCredentialsProvider)
        .region(env.awsRegion)
        .build()
}
