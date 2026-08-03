// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.ExcludeCodeGen
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
    @ExcludeCodeGen
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerustiedot(@RequestBody body: PerustiedotPocRequest): String =
        client.getPerustiedot(ssns = body.ssns, tietoryhmat = body.tietoryhmat)

    /** Same query, mapped onto eVaka's `VtjPerson` — shows the REST data reconstructs the model. */
    @ExcludeCodeGen
    @PostMapping("/mapped", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerustiedotMapped(@RequestBody body: PerustiedotPocRequest): List<VtjPerson> =
        client.getPerustiedotAsVtjPersons(
            ssns = body.ssns,
            today = LocalDate.now(europeHelsinki),
            tietoryhmat = body.tietoryhmat,
        )

    data class PerustiedotPocRequest(
        val ssns: List<String>,
        val tietoryhmat: List<String> = emptyList(),
    )
}
