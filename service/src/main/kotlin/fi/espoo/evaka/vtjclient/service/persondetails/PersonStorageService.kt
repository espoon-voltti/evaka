// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.lockPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.deleteChildGuardianRelationships
import fi.espoo.evaka.pis.service.deleteGuardianChildRelationShips
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonStorageService {
    fun upsertVtjChildAndGuardians(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
        val child = upsertVtjPerson(tx, vtjPersonDTO)
        child.guardians.addAll(vtjPersonDTO.guardians.sortedBy { it.socialSecurityNumber }.map { upsertVtjPerson(tx, it) })
        createOrReplaceChildRelationships(
            tx,
            childId = child.id,
            guardianIds = child.guardians.map { guardian -> guardian.id }
        )
        return child
    }

    fun upsertVtjGuardianAndChildren(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
        val guardian = upsertVtjPerson(tx, vtjPersonDTO)
        guardian.children.addAll(vtjPersonDTO.children.sortedBy { it.socialSecurityNumber }.map { upsertVtjPerson(tx, it) })
        createOrReplaceGuardianRelationships(
            tx,
            guardianId = guardian.id,
            childIds = guardian.children.map { child -> child.id }
        )
        return guardian
    }

    private fun createOrReplaceGuardianRelationships(tx: Database.Transaction, guardianId: UUID, childIds: List<UUID>) {
        tx.deleteGuardianChildRelationShips(guardianId)
        childIds.forEach { childId -> tx.insertGuardian(guardianId, childId) }
    }

    private fun createOrReplaceChildRelationships(tx: Database.Transaction, childId: UUID, guardianIds: List<UUID>) {
        tx.deleteChildGuardianRelationships(childId)
        guardianIds.forEach { guardianId -> tx.insertGuardian(guardianId, childId) }
    }

    fun upsertVtjPerson(tx: Database.Transaction, inputPerson: VtjPersonDTO): VtjPersonDTO {
        if (inputPerson.source != PersonDataSource.VTJ) {
            return inputPerson
        }

        val existingPerson = tx.lockPersonBySSN(inputPerson.socialSecurityNumber)

        return if (existingPerson == null) {
            val newPerson = newPersonFromVtjData(inputPerson)
            map(tx.createPersonFromVtj(newPerson), inputPerson.source)
        } else {
            val updatedPerson = getPersonWithUpdatedProperties(inputPerson, existingPerson)
            map(tx.updatePersonFromVtj(updatedPerson), inputPerson.source)
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
