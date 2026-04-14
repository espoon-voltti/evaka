// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.webpush.WebPush
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class CitizenWebPushConfig {
    @Bean
    fun citizenPushSubscriptionStore(
        s3Client: S3Client,
        bucketEnv: BucketEnv,
    ): CitizenPushSubscriptionStore = CitizenPushSubscriptionStore(s3Client, bucketEnv.data)

    @Bean
    fun citizenPushSender(
        store: CitizenPushSubscriptionStore,
        webPush: WebPush?,
    ): CitizenPushSender = CitizenPushSender(store = store, webPush = webPush)
}
