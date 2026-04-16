// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.BucketEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class CitizenPasskeyConfig {
    @Bean
    fun citizenPasskeyCredentialStore(
        s3Client: S3Client,
        bucketEnv: BucketEnv,
    ): CitizenPasskeyCredentialStore =
        // The `data` bucket has a GuardDuty Malware Scan deny policy that
        // blocks GetObject for any object not yet tagged as NO_THREATS_FOUND,
        // which permanently breaks the passkey credential round-trip. Use the
        // `decisions` bucket (no AV deny, same IAM permissions) instead.
        CitizenPasskeyCredentialStore(s3Client, bucketEnv.decisions)
}
