// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.PersonEmailVerificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

/**
 * Gets the latest email verification for the current non-verified e-mail address for a person.
 *
 * The person may have multiple verifications, but this function returns only the one that matches
 * the current non-verified e-mail address.
 */
fun Database.Read.getLatestEmailVerification(person: PersonId): EmailVerification? =
    createQuery {
            sql(
                """
SELECT pev.id, pev.email, pev.expires_at, pev.sent_at
FROM person p
JOIN person_email_verification pev ON p.id = pev.person_id AND p.email = pev.email
WHERE p.id = ${bind(person)}
"""
            )
        }
        .exactlyOneOrNull()

/**
 * Upserts a new email verification based on a (person_id, email) target.
 * - if a row doesn't exist, a new row inserted
 * - if a row exists, but it has expired, it's updated
 * - if a row exists and is still valid, nothing is changed
 */
fun Database.Transaction.upsertEmailVerification(
    now: HelsinkiDateTime,
    target: EmailVerificationTarget,
    verification: NewEmailVerification,
): EmailVerification =
    createUpdate {
            sql(
                """
INSERT INTO person_email_verification AS pev (person_id, email, verification_code, expires_at)
VALUES (${bind(target.person)}, ${bind(target.email)}, ${bind(verification.verificationCode)}, ${bind(verification.expiresAt)})
ON CONFLICT (person_id, email) DO UPDATE SET
    expires_at = CASE WHEN pev.expires_at < ${bind(now)} THEN excluded.expires_at ELSE pev.expires_at END,
    verification_code = CASE WHEN pev.expires_at < ${bind(now)} THEN excluded.verification_code ELSE pev.verification_code END,
    sent_at = CASE WHEN pev.expires_at < ${bind(now)} THEN NULL ELSE pev.sent_at END
RETURNING id, email, expires_at, sent_at
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<EmailVerification>()

/**
 * Verifies a person's e-mail address using a verification code.
 * - if the code is wrong or verification has expired, BadRequest is thrown
 * - if everything is ok, the e-mail address of the person is updated and the verification is
 *   consumed
 */
fun Database.Transaction.verifyAndUpdateEmail(
    now: HelsinkiDateTime,
    personId: PersonId,
    id: PersonEmailVerificationId,
    verificationCode: String,
) {
    val email =
        createUpdate {
                sql(
                    """
DELETE FROM person_email_verification
WHERE id = ${bind(id)}
AND person_id = ${bind(personId)}
AND expires_at > ${bind(now)}
AND verification_code = ${bind(verificationCode)}
RETURNING email
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOneOrNull<String>() ?: throw BadRequest("Failed to verify e-mail")
    execute {
        sql(
            """
UPDATE person
SET email = ${bind(email)}, verified_email = ${bind(email)}
WHERE id = ${bind(personId)}
"""
        )
    }
}

/**
 * Updates a person's weak login username to be the same as their verified e-mail address, if it has
 * been set.
 */
fun Database.Transaction.syncWeakLoginUsername(now: HelsinkiDateTime, personId: PersonId) {
    execute {
        sql(
            """
UPDATE citizen_user
SET username = verified_email, username_updated_at = ${bind(now)}
FROM (SELECT id, verified_email FROM person WHERE id = ${bind(personId)}) target
WHERE citizen_user.id = target.id AND verified_email IS NOT NULL AND verified_email != username
"""
        )
    }
}

fun Database.Transaction.markEmailVerificationSent(
    id: PersonEmailVerificationId,
    now: HelsinkiDateTime,
) {
    execute {
        sql("UPDATE person_email_verification SET sent_at = ${bind(now)} WHERE id = ${bind(id)}")
    }
}
