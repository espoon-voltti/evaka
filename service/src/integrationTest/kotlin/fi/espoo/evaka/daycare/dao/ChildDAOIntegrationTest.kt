// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.updateChild
import fi.espoo.evaka.shared.db.handle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID.randomUUID

class ChildDAOIntegrationTest : AbstractIntegrationTest() {
    private val childId = randomUUID()
    private lateinit var child: Child

    @BeforeEach
    internal fun setUp() {
        jdbi.handle { h ->
            h.execute("INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')")
            child = h.createChild(
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
    }

    @AfterEach
    internal fun tearDown() {
        jdbi.handle { it.execute("DELETE FROM person WHERE id = '$childId'") }
    }

    @Test
    fun `add and fetch child data`() {
        val fetchedChild = jdbi.handle { it.getChild(childId) }

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

        jdbi.handle { it.updateChild(updated) }

        val actual = jdbi.handle { it.getChild(childId) }
        assertEquals(actual, updated)
    }
}
