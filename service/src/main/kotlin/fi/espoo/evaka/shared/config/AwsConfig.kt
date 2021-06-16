// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.amazonaws.ClientConfigurationFactory
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class AwsConfig {
    @Bean
    @Profile("local")
    fun staticCredentialsProvider(): AWSCredentialsProvider =
        AWSStaticCredentialsProvider(BasicAWSCredentials("", ""))

    @Bean
    @Profile("production")
    fun containerCredentialsProvider() = DefaultAWSCredentialsProviderChain()

    @Bean
    @Profile("local")
    fun amazonS3Local(
        @Value("\${fi.espoo.voltti.s3mock.url}") s3MockUrl: String,
        @Value("\${fi.espoo.voltti.document.bucket.paymentdecision}") feeDecisionBucket: String,
        @Value("\${fi.espoo.voltti.document.bucket.vouchervaluedecision}") voucherValueDecisionBucket: String,
        @Value("\${fi.espoo.voltti.document.bucket.daycaredecision}") daycareDecisionBucket: String,
        @Value("\${fi.espoo.voltti.document.bucket.attachments}") attachmentsBucket: String,
        @Value("\${fi.espoo.voltti.document.bucket.data}") dataBucket: String
    ): AmazonS3 {
        val client = AmazonS3ClientBuilder
            .standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3MockUrl, "us-east-1"))
            .withClientConfiguration(ClientConfigurationFactory().config.withProtocol(Protocol.HTTP))
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("foo", "bar")))
            .build()

        client.createBucket(daycareDecisionBucket)
        client.createBucket(feeDecisionBucket)
        client.createBucket(voucherValueDecisionBucket)
        client.createBucket(attachmentsBucket)
        client.createBucket(dataBucket)

        return client
    }

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

    @Bean
    fun amazonSES(
        @Value("\${aws.region}") region: String,
        awsCredentialsProvider: AWSCredentialsProvider?
    ): AmazonSimpleEmailService? =
        AmazonSimpleEmailServiceClientBuilder
            .standard()
            .withCredentials(awsCredentialsProvider)
            .withRegion(region)
            .build()
}
