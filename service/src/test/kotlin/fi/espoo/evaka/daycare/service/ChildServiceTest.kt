// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.daycare.dao.ChildDAO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)class ChildServiceTest {

    @Mock
    lateinit var childDAO: ChildDAO

    @InjectMocks
    lateinit var childService: ChildService

    @Test
    fun `getAdditionalInformation returns correct values`() {
        val childId = UUID.randomUUID()

        val additionalInformation = AdditionalInformation()
        whenever(childDAO.getChild(childId)).thenReturn(
            Child(
                id = childId,
                additionalInformation = additionalInformation
            )
        )

        val actual = childService.getAdditionalInformation(childId)

        assertEquals(additionalInformation.allergies, actual.allergies)
        assertEquals(additionalInformation.diet, actual.diet)
        assertEquals(additionalInformation.additionalInfo, actual.additionalInfo)
    }

    @Test
    fun `saveChildFromApplication creates child when child does not exist`() {
        val request = childInfoFromApplication()
        whenever(childDAO.getChild(request.childId)).thenReturn(null)

        childService.saveChildFromApplication(request)

        argumentCaptor<Child>().apply {
            verify(childDAO).createChild(capture())

            assertEquals(request.childId, firstValue.id)
            assertEquals(request.dietType, firstValue.additionalInformation.diet)
            assertEquals(request.allergyType, firstValue.additionalInformation.allergies)
            assertEquals(request.otherInfo, firstValue.additionalInformation.additionalInfo)
        }
    }

    @Test
    fun `saveChildFromApplication updates child when child does exist`() {
        val request = childInfoFromApplication()
        whenever(childDAO.getChild(request.childId)).thenReturn(
            Child(
                request.childId,
                AdditionalInformation()
            )
        )

        childService.saveChildFromApplication(request)

        argumentCaptor<Child>().apply {
            verify(childDAO).updateChild(capture())

            assertEquals(request.childId, firstValue.id)
            assertEquals(request.dietType, firstValue.additionalInformation.diet)
            assertEquals(request.allergyType, firstValue.additionalInformation.allergies)
            assertEquals(request.otherInfo, firstValue.additionalInformation.additionalInfo)
        }
    }

    private fun childInfoFromApplication() = ChildInfoFromApplication(
        applicationId = UUID.randomUUID(),
        childId = UUID.randomUUID(),
        allergyType = "dsh35",
        dietType = "dgxhch",
        otherInfo = "dsxhfxhb"
    )
}
