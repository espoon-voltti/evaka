// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.deleteChildGuardianRelationships
import fi.espoo.evaka.pis.service.deleteGuardianChildRelationShips
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.usecases.dto.PersonResult
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonStorageService {
    fun upsertVtjPerson(tx: Database.Transaction, personResult: PersonResult): PersonResult {
        return when (personResult) {
            is PersonResult.Result -> PersonResult.Result(createOrUpdateOnePerson(tx, personResult.vtjPersonDTO))
            else -> personResult
        }
    }

    fun upsertVtjGuardianAndChildren(tx: Database.Transaction, personResult: PersonResult): PersonResult {
        return when (personResult) {
            is PersonResult.Result -> upsertVtjGuardianAndChildren(tx, personResult.vtjPersonDTO)
            else -> personResult
        }
    }

    fun upsertVtjChildAndGuardians(tx: Database.Transaction, personResult: PersonResult, updateIfExists: Boolean = true): PersonResult {
        return when (personResult) {
            is PersonResult.Result -> {
                val child = createOrUpdateOnePerson(tx, personResult.vtjPersonDTO, updateIfExists)
                child.guardians.addAll(personResult.vtjPersonDTO.guardians.map { createOrUpdateOnePerson(tx, it, updateIfExists) })
                createOrReplaceChildRelationships(
                    tx,
                    childId = child.id,
                    guardianIds = child.guardians.map { guardian -> guardian.id }
                )

                return PersonResult.Result(child)
            }
            else -> personResult
        }
    }

    private fun upsertVtjGuardianAndChildren(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): PersonResult {
        val guardian = createOrUpdateOnePerson(tx, vtjPersonDTO, false)
        guardian.children.addAll(vtjPersonDTO.children.map { createOrUpdateOnePerson(tx, it, false) })
        createOrReplaceGuardianRelationships(
            tx,
            guardianId = guardian.id,
            childIds = guardian.children.map { child -> child.id }
        )

        return PersonResult.Result(guardian)
    }

    private fun createOrReplaceGuardianRelationships(tx: Database.Transaction, guardianId: UUID, childIds: List<UUID>) {
        deleteGuardianChildRelationShips(tx.handle, guardianId)
        childIds.forEach { childId -> insertGuardian(tx.handle, guardianId, childId) }
    }

    private fun createOrReplaceChildRelationships(tx: Database.Transaction, childId: UUID, guardianIds: List<UUID>) {
        deleteChildGuardianRelationships(tx.handle, childId)
        guardianIds.forEach { guardianId -> insertGuardian(tx.handle, guardianId, childId) }
    }

    private fun createOrUpdateOnePerson(tx: Database.Transaction, inputPerson: VtjPersonDTO, updateIfExists: Boolean = true): VtjPersonDTO {
        if (inputPerson.source != PersonDataSource.VTJ) {
            return inputPerson
        }

        val existingPerson = tx.handle.getPersonBySSN(inputPerson.socialSecurityNumber)

        return if (existingPerson == null) {
            val newPerson = newPersonFromVtjData(inputPerson)
            map(tx.handle.createPersonFromVtj(newPerson), inputPerson.source)
        } else if (updateIfExists) {
            val updatedPerson = getPersonWithUpdatedProperties(inputPerson, existingPerson)
            map(tx.handle.updatePersonFromVtj(updatedPerson), inputPerson.source)
        } else map(existingPerson, PersonDataSource.DATABASE)
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
        phone = null
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
