// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.videocall

import fi.espoo.evaka.BucketEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class VideoCallConfig {
    @Bean
    fun videoCallRoomStore(s3Client: S3Client, bucketEnv: BucketEnv): VideoCallRoomStore =
        // Uses the `decisions` bucket for the same reason as citizen-push and citizen-passkey:
        // it has no GuardDuty AV deny policy that would block fresh GetObject calls, and the IAM
        // grant already allows PutObject/GetObject/ListObjectsV2. We never call DeleteObject; the
        // bucket policy denies that too and the store is designed around that constraint.
        VideoCallRoomStore(s3Client, bucketEnv.decisions)
}
