// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.europeHelsinki
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import java.time.LocalDate

/**
 * Only [getBasicDetailsFor] is implemented, because it is the only method the import flow calls.
 * The other two would need the guardian/dependant hydration that `/perustiedot` returns as name+SSN
 * stubs — a second query, and outside this POC.
 *
 * Deliberately not a Spring bean: registering an [IPersonDetailsService] would replace the real one
 * application-wide. [DvvPerustiedotPocController] constructs it instead, the way `DevApi` builds
 * its own `DummyIdpPersonDetailsService`.
 *
 * `DetailsQuery.requestingUser` is ignored — it exists to fill the SOAP request envelope, and the
 * REST endpoint authenticates with basic auth.
 */
class DvvRestPersonDetailsService(private val client: DvvPerustiedotPocClient) :
    IPersonDetailsService {
    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        client
            .getPerustiedotAsVtjPersons(
                ssns = listOf(query.targetIdentifier.ssn),
                today = LocalDate.now(europeHelsinki),
            )
            .singleOrNull() ?: throw NotFound("DVV returned no person for the requested SSN")

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        error("Dependant hydration is not part of the /perustiedot POC")

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        error("Guardian hydration is not part of the /perustiedot POC")
}
