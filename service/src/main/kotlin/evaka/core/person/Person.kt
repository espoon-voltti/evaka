// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.person

import evaka.core.decision.DecisionSendAddress
import evaka.core.family.Modifier
import evaka.core.family.ModifySource
import evaka.core.family.getParentships
import evaka.core.family.getPartnersForPerson
import evaka.core.family.updateParentshipDuration
import evaka.core.family.updatePartnershipDuration
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.messaging.createPersonMessageAccount
import evaka.core.pdfgen.PdfGenerator
import evaka.core.s3.Document
import evaka.core.shared.HtmlBuilder
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.Rectangle
import java.time.LocalDate

fun createPerson(tx: Database.Transaction, person: CreatePersonBody): PersonId {
    val personId = tx.createPerson(person)
    tx.createPersonMessageAccount(personId)
    return personId
}

private val whitespace = "\\s+".toRegex()

/** Splits the first name field, which holds all of a person's first names */
fun splitFirstNames(firstNames: String): List<String> =
    firstNames.trim().split(whitespace).filter { it.isNotEmpty() }

fun isMinor(dateOfBirth: LocalDate, today: LocalDate): Boolean = today < dateOfBirth.plusYears(18)

/**
 * Ends the fridge relations of a person who has died, and plans new finance decisions. Must be
 * called only once per death, when the stored date of death changes from NULL to a date.
 */
fun Database.Transaction.applyDeathEffects(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    personId: PersonId,
    dateOfDeath: LocalDate,
) {
    getPartnersForPerson(
            personId,
            includeConflicts = true,
            period = DateRange(dateOfDeath, dateOfDeath),
        )
        .forEach {
            updatePartnershipDuration(
                it.partnershipId,
                it.startDate,
                dateOfDeath,
                ModifySource.DVV,
                now,
                null,
            )
        }

    val parentships =
        getParentships(
            headOfChildId = personId,
            childId = null,
            includeConflicts = true,
            period = DateRange(dateOfDeath, dateOfDeath),
        ) +
            getParentships(
                headOfChildId = null,
                childId = personId,
                includeConflicts = true,
                period = DateRange(dateOfDeath, dateOfDeath),
            )
    parentships.forEach {
        updateParentshipDuration(
            id = it.id,
            startDate = it.startDate,
            endDate = dateOfDeath,
            now = now,
            modifier = Modifier.DVV,
        )
    }

    asyncJobRunner.plan(
        this,
        listOf(
            AsyncJob.GenerateFinanceDecisions.forAdult(personId, DateRange(dateOfDeath, null)),
            AsyncJob.GenerateFinanceDecisions.forChild(personId, DateRange(dateOfDeath, null)),
        ),
        runAt = now,
    )
}

fun Database.Read.personsLiveInTheSameAddress(person1Id: PersonId, person2Id: PersonId): Boolean =
    personsLiveInTheSameAddress(getPersonById(person1Id), getPersonById(person2Id))

fun personsLiveInTheSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
    return personsHaveSameResidenceCode(person1, person2) ||
        personsHaveSameAddress(person1, person2)
}

private fun personsHaveSameResidenceCode(person1: PersonDTO?, person2: PersonDTO?): Boolean {
    return person1 != null &&
        person2 != null &&
        !person1.restrictedDetailsEnabled &&
        !person2.restrictedDetailsEnabled &&
        !person1.streetAddress.lowercase().contains("poste restante") &&
        !person2.streetAddress.lowercase().contains("poste restante") &&
        person1.residenceCode.isNotBlank() &&
        person2.residenceCode.isNotBlank() &&
        person1.residenceCode == person2.residenceCode
}

private fun personsHaveSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
    return person1 != null &&
        person2 != null &&
        !person1.restrictedDetailsEnabled &&
        !person2.restrictedDetailsEnabled &&
        person1.streetAddress.isNotBlank() &&
        person2.streetAddress.isNotBlank() &&
        !person1.streetAddress.lowercase().contains("poste restante") &&
        !person2.streetAddress.lowercase().contains("poste restante") &&
        person1.postalCode.isNotBlank() &&
        person2.postalCode.isNotBlank() &&
        person1.postOffice.isNotBlank() &&
        person2.postOffice.isNotBlank() &&
        person1.streetAddress.lowercase() == person2.streetAddress.lowercase() &&
        person1.postalCode == person2.postalCode &&
        person1.postOffice == person2.postOffice
}

@IgnorableReturnValue
fun Database.Transaction.patchUserDetails(id: PersonId, data: PersonPatch): PersonDTO {
    val person = getPersonById(id) ?: throw NotFound("Person $id not found")

    // People with SSN get basic details from VTJ which should not be modified
    if (person.ssn != null) {
        updatePersonNonVtjDetails(id, data)
    } else {
        updateNonSsnPersonDetails(id, data)
    }

    return getPersonById(id)!!
}

fun Database.Transaction.disableSsn(personId: PersonId, disabled: Boolean) {
    val person = getPersonById(personId) ?: throw NotFound("Person $personId not found")

    if (person.ssn != null) {
        throw BadRequest("Cannot disable a SSN, person $personId already has a SSN")
    }

    updatePersonSsnAddingDisabled(personId, disabled)
}

fun createAddressPagePdf(
    pdfGenerator: PdfGenerator,
    today: LocalDate,
    envelopeWindowPosition: Rectangle,
    guardian: PersonDTO,
): Document {
    val personDetails = guardian.let {
        val firstWordOfFirstName = splitFirstNames(it.firstName).firstOrNull() ?: ""

        PersonDetailed(
            dateOfBirth = it.dateOfBirth,
            mailingStreetAddress = it.mailingStreetAddress,
            mailingPostalCode = it.mailingPostalCode,
            mailingPostOffice = it.mailingPostOffice,
            firstName = firstWordOfFirstName,
            lastName = it.lastName,
            restrictedDetailsEnabled = it.restrictedDetailsEnabled,
            id = it.id,
        )
    }

    val sendAddress = DecisionSendAddress.fromPerson(personDetails)
    val window = envelopeWindowPosition
    val html =
        HtmlBuilder.document(
            css =
                """
                @page {
                    size: A4 portrait;
                    margin-left: ${window.x}mm;
                    margin-top: ${window.y}mm;
                    margin-right: 15mm;
                    margin-bottom: 15mm;
                }
                body { margin: 0; }
                .address {
                    width: ${window.width}mm;
                    height: ${window.height}mm;
                    font-family: 'Open Sans', sans-serif;
                    font-weight: 400;
                    font-size: 10pt;
                    line-height: 1.4;
                }
                """
        ) {
            listOf(
                div(className = "address") {
                    listOfNotNull(
                        div("${personDetails.lastName} ${personDetails.firstName}"),
                        sendAddress?.let { addr ->
                            div {
                                listOfNotNull(
                                    div(addr.row1),
                                    div(addr.row2),
                                    addr.row3.takeIf { it.isNotEmpty() }?.let { div(it) },
                                )
                            }
                        },
                    )
                }
            )
        }

    return Document(
        name = "osoitesivu_${guardian.lastName}_$today.pdf",
        bytes = pdfGenerator.render(html),
        contentType = "application/pdf",
    )
}
