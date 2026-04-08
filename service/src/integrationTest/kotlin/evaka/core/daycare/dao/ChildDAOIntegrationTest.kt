// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.dao

import evaka.core.PureJdbiTest
import evaka.core.daycare.controllers.AdditionalInformation
import evaka.core.daycare.controllers.Child
import evaka.core.daycare.createChild
import evaka.core.daycare.getChild
import evaka.core.daycare.updateChild
import evaka.core.shared.ChildId
import java.time.LocalDate
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChildDAOIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val childId = ChildId(randomUUID())
    private lateinit var child: Child

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.execute {
                sql(
                    "INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')"
                )
            }
            tx.createChild(
                Child(
                    id = childId,
                    additionalInformation =
                        AdditionalInformation(
                            allergies = "Pähkinäallergia",
                            diet = "Kasvisruokavalio",
                            additionalInfo = "Ei osaa solmia kengännauhoja",
                        ),
                )
            )
            child = tx.getChild(childId)!!
        }
    }

    @Test
    fun `add and fetch child data`() {
        val fetchedChild = db.transaction { it.getChild(childId) }

        assertEquals(child, fetchedChild)
    }

    @Test
    fun `Update child updates correct properties`() {
        val updated =
            child.copy(
                additionalInformation =
                    AdditionalInformation(
                        allergies = "Retiisi",
                        diet = "Vähäglugoosinen",
                        additionalInfo = "Hankala luonne",
                    )
            )

        db.transaction { it.updateChild(updated) }

        val actual = db.transaction { it.getChild(childId) }
        assertEquals(actual, updated)
    }
}
