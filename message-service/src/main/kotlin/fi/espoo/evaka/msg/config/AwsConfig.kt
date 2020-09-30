// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.amazonaws.ClientConfigurationFactory
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class AwsConfig {
    @Bean
    @Profile("dev", "local")
    fun staticCredentialsProvider(): AWSCredentialsProvider =
        AWSStaticCredentialsProvider(BasicAWSCredentials("", ""))

    @Bean
    @Profile("production")
    fun containerCredentialsProvider() = DefaultAWSCredentialsProviderChain()

    @Bean
    @Profile("local", "integration-test")
    fun amazonS3Local(
        @Value("\${fi.espoo.voltti.s3mock.url}") s3MockUrl: String
    ): AmazonS3 = AmazonS3ClientBuilder
        .standard()
        .enablePathStyleAccess()
        .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3MockUrl, "us-east-1"))
        .withClientConfiguration(ClientConfigurationFactory().config.withProtocol(Protocol.HTTP))
        .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("foo", "bar")))
        .build()

    @Bean
    @Profile("production")
    fun amazonS3Prod(
        @Value("\${aws.region}") region: String,
        credentialsProvider: AWSCredentialsProvider
    ): AmazonS3 =
        AmazonS3ClientBuilder.standard()
            .withRegion(region)
            .withCredentials(credentialsProvider)
            .build()
}
