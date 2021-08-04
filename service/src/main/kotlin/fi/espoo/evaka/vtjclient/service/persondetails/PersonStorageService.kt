// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.lockPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.deleteChildGuardianRelationships
import fi.espoo.evaka.pis.service.deleteGuardianChildRelationShips
import fi.espoo.evaka.pis.service.insertChildGuardians
import fi.espoo.evaka.pis.service.insertGuardianChildren
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PersonStorageService {
    fun upsertVtjGuardians(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
        val child = upsertVtjPerson(tx, vtjPersonDTO)
        val guardians = vtjPersonDTO.guardians.map { upsertVtjPerson(tx, it) }
        createOrReplaceChildRelationships(
            tx,
            childId = child.id,
            guardianIds = guardians.map { it.id }
        )
        initChildIfNotExists(tx, listOf(child.id))
        return map(child, vtjPersonDTO.source)
            .copy(guardians = guardians.map { map(it, vtjPersonDTO.source) })
    }

    fun upsertVtjChildren(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
        val guardian = upsertVtjPerson(tx, vtjPersonDTO)
        val children = vtjPersonDTO.children.map { upsertVtjPerson(tx, it) }
        createOrReplaceGuardianRelationships(
            tx,
            guardianId = guardian.id,
            childIds = children.map { it.id }
        )
        initChildIfNotExists(tx, children.map { it.id })
        return map(guardian, vtjPersonDTO.source)
            .copy(children = children.map { map(it, vtjPersonDTO.source) })
    }

    private fun initChildIfNotExists(tx: Database.Transaction, childIds: List<UUID>) {
        childIds.forEach { childId ->
            if (tx.getChild(childId) == null)
                tx.createChild(Child(id = childId, additionalInformation = AdditionalInformation()))
        }
    }

    private fun createOrReplaceGuardianRelationships(tx: Database.Transaction, guardianId: UUID, childIds: List<UUID>) {
        tx.deleteGuardianChildRelationShips(guardianId)
        tx.insertGuardianChildren(guardianId, childIds)
        tx.updateVtjDependantsQueriedTimestamp(guardianId)
    }

    private fun createOrReplaceChildRelationships(tx: Database.Transaction, childId: UUID, guardianIds: List<UUID>) {
        tx.deleteChildGuardianRelationships(childId)
        tx.insertChildGuardians(childId, guardianIds)
        tx.updateVtjGuardiansQueriedTimestamp(childId)
    }

    fun upsertVtjPerson(tx: Database.Transaction, inputPerson: VtjPersonDTO): PersonDTO {
        if (inputPerson.source != PersonDataSource.VTJ) {
            error("Cannot upsert VTJ person when data source is ${inputPerson.source}")
        }

        val existingPerson = tx.lockPersonBySSN(inputPerson.socialSecurityNumber)

        return if (existingPerson == null) {
            val newPerson = newPersonFromVtjData(inputPerson)
            createPersonFromVtj(tx, newPerson)
        } else {
            val updatedPerson = getPersonWithUpdatedProperties(inputPerson, existingPerson)
            tx.updatePersonFromVtj(updatedPerson)
        }
    }

    private fun newPersonFromVtjData(inputPerson: VtjPersonDTO): PersonDTO = PersonDTO(
        id = inputPerson.id, // This will be overwritten
        identity = ExternalIdentifier.SSN.getInstance(inputPerson.socialSecurityNumber),
        firstName = inputPerson.firstName,
        lastName = inputPerson.lastName,

        streetAddress = getStreetAddressByLanguage(inputPerson),
        postalCode = inputPerson.postalCode,
        postOffice = getPostOfficeByLanguage(inputPerson),
        residenceCode = inputPerson.residenceCode,

        restrictedDetailsEnabled = inputPerson.restrictedDetailsEnabled,
        restrictedDetailsEndDate = inputPerson.restrictedDetailsEndDate,
        nationalities = inputPerson.nationalities.map { it.countryCode },
        language = inputPerson.nativeLanguage?.code ?: "",

        customerId = PLACEHOLDER,
        dateOfBirth = inputPerson.dateOfBirth,
        dateOfDeath = inputPerson.dateOfDeath,
        email = null,
        phone = null,
        backupPhone = ""
    )

    private fun getPersonWithUpdatedProperties(sourcePerson: VtjPersonDTO, existingPerson: PersonDTO): PersonDTO =
        existingPerson.copy(
            firstName = sourcePerson.firstName,
            lastName = sourcePerson.lastName,
            dateOfBirth = sourcePerson.dateOfBirth,
            dateOfDeath = sourcePerson.dateOfDeath,

            streetAddress = getStreetAddressByLanguage(sourcePerson),
            postalCode = sourcePerson.postalCode,
            postOffice = getPostOfficeByLanguage(sourcePerson),
            residenceCode = sourcePerson.residenceCode,

            restrictedDetailsEnabled = sourcePerson.restrictedDetailsEnabled,
            restrictedDetailsEndDate = sourcePerson.restrictedDetailsEndDate,
            nationalities = sourcePerson.nationalities.map { it.countryCode },
            language = sourcePerson.nativeLanguage?.code ?: ""
        )

    private fun map(source: PersonDTO, dataSource: PersonDataSource): VtjPersonDTO {
        return VtjPersonDTO(
            id = source.id,
            firstName = source.firstName ?: "",
            lastName = source.lastName ?: "",
            dateOfBirth = source.dateOfBirth,
            dateOfDeath = source.dateOfDeath,
            socialSecurityNumber = source.identity.toString(),
            source = dataSource,
            restrictedDetailsEndDate = source.restrictedDetailsEndDate,
            restrictedDetailsEnabled = source.restrictedDetailsEnabled,
            nativeLanguage = NativeLanguage(
                code = source.language ?: ""
            ),
            streetAddress = source.streetAddress ?: "",
            postalCode = source.postalCode ?: "",
            city = source.postOffice ?: "",
            streetAddressSe = "",
            citySe = "",
            residenceCode = source.residenceCode
        )
    }

    private fun getStreetAddressByLanguage(vtjPerson: VtjPersonDTO): String {
        if (vtjPerson.nativeLanguage == null || vtjPerson.streetAddressSe == "") {
            return vtjPerson.streetAddress
        }

        return when (vtjPerson.nativeLanguage.code) {
            "sv" -> vtjPerson.streetAddressSe
            else -> vtjPerson.streetAddress
        }
    }

    private fun getPostOfficeByLanguage(vtjPerson: VtjPersonDTO): String {
        if (vtjPerson.nativeLanguage == null || vtjPerson.citySe == "") {
            return vtjPerson.city
        }

        return when (vtjPerson.nativeLanguage.code) {
            "sv" -> vtjPerson.citySe
            else -> vtjPerson.city
        }
    }

    companion object {
        private const val PLACEHOLDER = 0L
    }
}

private fun Database.Transaction.updateVtjDependantsQueriedTimestamp(personId: UUID) =
    createUpdate("UPDATE person SET vtj_dependants_queried = :now WHERE id = :personId")
        .bind("personId", personId)
        .bind("now", Instant.now())
        .execute()

private fun Database.Transaction.updateVtjGuardiansQueriedTimestamp(personId: UUID) =
    createUpdate("UPDATE person SET vtj_guardians_queried = :now WHERE id = :personId")
        .bind("personId", personId)
        .bind("now", Instant.now())
        .execute()
