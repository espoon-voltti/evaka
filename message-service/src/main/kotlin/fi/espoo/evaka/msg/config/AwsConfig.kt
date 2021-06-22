// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import org.springframework.beans.factory.annotation.Value
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
import java.net.URI

@Configuration
class AwsConfig {
    @Bean
    @Profile("production")
    fun containerCredentialsProvider() = DefaultCredentialsProvider.create()

    @Bean
    @Profile("local", "integration-test")
    fun amazonS3Local(
        @Value("\${fi.espoo.voltti.s3mock.url}") s3MockUrl: String
    ): S3Client = S3Client.builder()
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .endpointOverride(URI.create(s3MockUrl))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
        )
        .build()

    @Bean
    @Profile("production")
    fun amazonS3Prod(
        @Value("\${aws.region}") region: String,
        credentialsProvider: AwsCredentialsProvider
    ): S3Client = S3Client.builder().region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build()
}
