// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.dao.ChildDAO
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ChildService(
    private val childDAO: ChildDAO
) {

    fun getAdditionalInformation(childId: UUID): AdditionalInformation {
        val child = childDAO.getChild(childId)
        return if (child != null) {
            AdditionalInformation(
                allergies = child.additionalInformation.allergies,
                diet = child.additionalInformation.diet,
                additionalInfo = child.additionalInformation.additionalInfo
            )
        } else AdditionalInformation()
    }

    @Transactional
    fun upsertAdditionalInformation(childId: UUID, data: AdditionalInformation) {
        val child = childDAO.getChild(childId)
        if (child != null) {
            childDAO.updateChild(child.copy(additionalInformation = data))
        } else {
            childDAO.createChild(
                Child(
                    id = childId,
                    additionalInformation = data
                )
            )
        }
    }

    @Transactional
    fun saveChildFromApplication(childInfoFromApplication: ChildInfoFromApplication) {
        val additionalInformation = AdditionalInformation(
            allergies = childInfoFromApplication.allergyType,
            diet = childInfoFromApplication.dietType,
            additionalInfo = childInfoFromApplication.otherInfo
        )

        val child = childDAO.getChild(childInfoFromApplication.childId)

        if (child == null) {
            childDAO.createChild(
                Child(
                    id = childInfoFromApplication.childId,
                    additionalInformation = additionalInformation
                )
            )
        } else if (child.additionalInformation.isBlank) {
            childDAO.updateChild(child.copy(additionalInformation = additionalInformation))
        }
    }

    @Transactional
    fun initEmptyIfNotExists(personId: UUID) {
        if (childDAO.getChild(personId) == null) {
            childDAO.createChild(
                Child(
                    id = personId,
                    additionalInformation = AdditionalInformation()
                )
            )
        }
    }
}

data class Child(
    val id: UUID,
    @Nested val additionalInformation: AdditionalInformation
)

data class AdditionalInformation(
    val allergies: String = "",
    val diet: String = "",
    val additionalInfo: String = ""
) {
    val isBlank: Boolean
        get() = allergies.isBlank() && diet.isBlank() && additionalInfo.isBlank()
}

data class ChildInfoFromApplication(
    val applicationId: UUID,
    val childId: UUID,
    val allergyType: String = "",
    val dietType: String = "",
    val otherInfo: String = ""
)
