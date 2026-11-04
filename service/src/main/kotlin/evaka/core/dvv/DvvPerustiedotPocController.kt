// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.ExcludeCodeGen
import evaka.core.identity.ExternalIdentifier
import evaka.core.identity.isValidSSN
import evaka.core.pis.service.PersonJSON
import evaka.core.pis.service.PersonService
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.europeHelsinki
import evaka.core.vtjclient.dto.VtjPerson
import java.time.LocalDate
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * POC endpoint for eyeballing what DVV's REST `/perustiedot` returns.
 *
 * The response contains personal data. This controller and [DvvPerustiedotPocClient] are both
 * `@Profile("enable_dev_api")` — active under the `local` profile group and for `VOLTTI_ENV`
 * `dev`/`test`, so neither bean exists in production. There the `dev-api` prefix bypasses
 * authentication (see `HttpFilterConfig`), so keeping this pointed at the DVV sandbox is a config
 * decision that nothing in the code enforces.
 *
 * Not part of the eventual migration.
 */
@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api/dvv-perustiedot-poc")
class DvvPerustiedotPocController(private val client: DvvPerustiedotPocClient) {
    private val personService = PersonService(DvvRestPersonDetailsService(client))

    @ExcludeCodeGen
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerustiedot(@RequestBody body: PerustiedotPocRequest): String =
        client.getPerustiedot(ssns = body.ssns, tietoryhmat = body.tietoryhmat)

    /** Shows the REST data reconstructs eVaka's model. */
    @ExcludeCodeGen
    @PostMapping("/mapped", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerustiedotMapped(@RequestBody body: PerustiedotPocRequest): List<VtjPerson> =
        client.getPerustiedotAsVtjPersons(
            ssns = body.ssns,
            today = LocalDate.now(europeHelsinki),
            tietoryhmat = body.tietoryhmat,
        )

    /**
     * Runs eVaka's own "Tuo henkilö VTJ:stä" import with DVV's REST endpoint standing in for the
     * SOAP VTJkysely, showing the REST data is sufficient to create a real `person` row.
     *
     * With `readonly` the person is mapped and returned but nothing is written. Note that
     * [PersonService.getOrCreatePerson] skips the DVV call entirely once a person has been
     * imported, so re-running the same SSN is a no-op until that person is deleted.
     */
    @ExcludeCodeGen
    @PostMapping("/import")
    fun importPerson(db: Database, @RequestBody body: ImportPocRequest): PersonJSON {
        if (!isValidSSN(body.ssn)) throw BadRequest("Invalid SSN")
        return db.connect { dbc ->
                dbc.transaction {
                    personService.getOrCreatePerson(
                        it,
                        AuthenticatedUser.SystemInternalUser,
                        ExternalIdentifier.SSN.getInstance(body.ssn),
                        body.readonly,
                    )
                } ?: throw NotFound("No person for ${body.ssn}")
            }
            .let { PersonJSON.from(it) }
    }

    data class PerustiedotPocRequest(
        val ssns: List<String>,
        val tietoryhmat: List<String> = emptyList(),
    )

    data class ImportPocRequest(val ssn: String, val readonly: Boolean = true)
}
