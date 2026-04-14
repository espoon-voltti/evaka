// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.webpush.WebPush
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/web-push")
class CitizenWebPushController(
    private val store: CitizenPushSubscriptionStore,
    private val sender: CitizenPushSender,
    private val webPush: WebPush?,
) {
    data class VapidKeyResponse(val publicKey: String)

    data class SubscribeRequest(
        val endpoint: URI,
        val ecdhKey: List<Byte>,
        val authSecret: List<Byte>,
        val enabledCategories: Set<CitizenPushCategory>,
        val userAgent: String?,
    )

    data class SubscribeResponse(val sentTest: Boolean)

    data class UnsubscribeRequest(val endpoint: URI)

    @GetMapping("/vapid-key")
    fun vapidKey(user: AuthenticatedUser.Citizen): ResponseEntity<VapidKeyResponse> =
        webPush?.let { ResponseEntity.ok(VapidKeyResponse(it.applicationServerKey)) }
            ?: ResponseEntity.status(503).build()

    @PutMapping("/subscription")
    fun putSubscription(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: SubscribeRequest,
    ): SubscribeResponse {
        val entry =
            CitizenPushSubscriptionEntry(
                endpoint = body.endpoint,
                ecdhKey = body.ecdhKey,
                authSecret = body.authSecret,
                enabledCategories = body.enabledCategories,
                userAgent = body.userAgent,
                createdAt = clock.now(),
            )
        val result = store.upsertSubscription(user.id, entry)
        Audit.CitizenWebPushSubscriptionUpsert.log(
            targetId = AuditId(user.id),
            meta =
                mapOf(
                    "endpoint" to body.endpoint.toString(),
                    "firstWrite" to result.wasFirstWrite,
                ),
        )
        val wp = webPush
        if (result.wasFirstWrite && wp != null) {
            val language = CitizenPushLanguage.fromPersonLanguage(resolveLanguage(db, user.id))
            sender.sendTest(
                personId = user.id,
                language = language,
                jwtProvider =
                    VapidJwtProvider { uri ->
                        db.connect { dbc -> dbc.transaction { tx -> wp.getValidToken(tx, clock, uri) } }
                    },
            )
        }
        return SubscribeResponse(sentTest = result.wasFirstWrite && wp != null)
    }

    @DeleteMapping("/subscription")
    fun deleteSubscription(
        user: AuthenticatedUser.Citizen,
        @RequestBody body: UnsubscribeRequest,
    ) {
        store.removeSubscription(user.id, body.endpoint)
        Audit.CitizenWebPushSubscriptionDelete.log(
            targetId = AuditId(user.id),
            meta = mapOf("endpoint" to body.endpoint.toString()),
        )
    }

    @PostMapping("/test")
    fun postTest(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ) {
        val wp = webPush ?: return
        val language = CitizenPushLanguage.fromPersonLanguage(resolveLanguage(db, user.id))
        Audit.CitizenWebPushTestSent.log(targetId = AuditId(user.id))
        sender.sendTest(
            personId = user.id,
            language = language,
            jwtProvider =
                VapidJwtProvider { uri ->
                    db.connect { dbc -> dbc.transaction { tx -> wp.getValidToken(tx, clock, uri) } }
                },
        )
    }

    private fun resolveLanguage(db: Database, personId: PersonId): String? =
        db.connect { dbc ->
            dbc.read { tx ->
                tx.createQuery { sql("SELECT language FROM person WHERE id = ${bind(personId)}") }
                    .exactlyOneOrNull<String?>()
            }
        }
}
