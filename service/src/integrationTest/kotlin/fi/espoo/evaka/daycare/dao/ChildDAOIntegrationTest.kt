// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.daycare.service.AdditionalInformation
import fi.espoo.evaka.daycare.service.Child
import fi.espoo.evaka.shared.db.handle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID.randomUUID

class ChildDAOIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var childDAO: ChildDAO

    private val childId = randomUUID()
    private lateinit var child: Child

    @BeforeEach
    internal fun setUp() {
        jdbi.handle {
            it.execute("INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')")
        }
        child = childDAO.createChild(
            Child(
                id = childId,
                additionalInformation = AdditionalInformation(
                    allergies = "Pähkinäallergia",
                    diet = "Kasvisruokavalio",
                    additionalInfo = "Ei osaa solmia kengännauhoja"
                )
            )
        )
    }

    @AfterEach
    internal fun tearDown() {
        jdbi.handle { it.execute("DELETE FROM person WHERE id = '$childId'") }
    }

    @Test
    fun `add and fetch child data`() {
        val fetchedChild = childDAO.getChild(childId)

        assertEquals(child, fetchedChild)
    }

    @Test
    fun `Update child updates correct properties`() {
        val updated = child.copy(
            additionalInformation = AdditionalInformation(
                allergies = "Retiisi",
                diet = "Vähäglugoosinen",
                additionalInfo = "Hankala luonne"
            )
        )

        childDAO.updateChild(updated)

        val actual = childDAO.getChild(childId)
        assertEquals(actual, updated)
    }
}
