// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import org.jdbi.v3.core.mapper.PropagateNull

data class ChildWithDateOfBirth(@PropagateNull val id: ChildId, val dateOfBirth: LocalDate)

data class EmployeeWithName(
    @PropagateNull val id: EmployeeId,
    val firstName: String,
    val lastName: String,
)

data class PersonBasic(
    @PropagateNull val id: PersonId,
    override val dateOfBirth: LocalDate,
    val firstName: String,
    val lastName: String,
    val ssn: String? = null,
) : HasDateOfBirth

data class PersonDetailed(
    @PropagateNull val id: PersonId,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val firstName: String,
    val lastName: String,
    val ssn: String? = null,
    val streetAddress: String = "",
    val postalCode: String = "",
    val postOffice: String = "",
    val residenceCode: String = "",
    val email: String? = null,
    val phone: String = "",
    val language: String? = null,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val restrictedDetailsEnabled: Boolean,
    val forceManualFeeDecisions: Boolean = false,
)

fun addressUsable(streetAddress: String?, postalCode: String?, city: String?): Boolean =
    listOf(streetAddress, postalCode, city).none { it.isNullOrBlank() }

data class FridgeFamily(
    val headOfFamily: PersonId,
    val partner: PersonId?,
    val children: List<PersonBasic>,
    val period: DateRange,
) {
    fun getSize(): Int {
        return 1 + (if (partner != null) 1 else 0) + children.size
    }
}
