// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.report

import evaka.core.daycare.CareType
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PalvelukykykyselyReportTest : AbstractTampereIntegrationTest() {

    @Test
    fun `palvelukykykysely report returns correct values`() {
        val childUrl = "https://varhaiskasvatus.tampere.fi/employee/child-information/"
        val adultUrl = "https://varhaiskasvatus.tampere.fi/employee/profile/"

        val areaId = db.read { tx ->
            tx.createQuery { sql("select id from care_area order by name limit 1") }
                .exactlyOne<AreaId>()
        }
        val unit =
            DevDaycare(
                name = "Yksikkö",
                areaId = areaId,
                type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
            )
        val child1 = DevPerson(firstName = "Pauliina", lastName = "Päiväkotilainen")
        val child2 = DevPerson(firstName = "Taneli", lastName = "Täydentävä")

        val child3 = DevPerson(firstName = "Eemeli", lastName = "Esioppilas")

        val parent1 = DevPerson(firstName = "Veikko", lastName = "Vanhempi", email = "veikon@säpo")

        val parent2 =
            DevPerson(firstName = "Petunia", lastName = "Puoliso", email = "petunian@säpo")

        val startDate = LocalDate.of(2025, 8, 1)
        val endDate = LocalDate.of(2025, 12, 1)

        db.transaction { tx ->
            tx.insert(unit)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(child3, DevPersonType.CHILD)

            tx.insert(parent1, DevPersonType.ADULT)
            tx.insert(parent2, DevPersonType.ADULT)

            tx.insert(
                DevFridgeChild(
                    childId = child1.id,
                    headOfChild = parent1.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevFridgeChild(
                    childId = child2.id,
                    headOfChild = parent1.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevFridgeChild(
                    childId = child3.id,
                    headOfChild = parent1.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            tx.insert(
                DevFridgePartnership(
                    first = parent1.id,
                    second = parent2.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = unit.id,
                    startDate = startDate,
                    endDate = endDate,
                    type = PlacementType.DAYCARE,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = unit.id,
                    startDate = startDate,
                    endDate = endDate,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child3.id,
                    unitId = unit.id,
                    startDate = startDate,
                    endDate = endDate,
                    type = PlacementType.PRESCHOOL,
                )
            )
        }

        val rows = db.read { tx ->
            tx.createQuery { sql("SELECT * FROM palvelukykykysely") }.toList<PalvelukykykyselyRow>()
        }

        assertThat(rows)
            .containsExactlyInAnyOrder(
                PalvelukykykyselyRow(
                    childUrl = "$childUrl${child1.id}",
                    childFirstName = child1.firstName,
                    childLastName = child1.lastName,
                    placementUnit = unit.name,
                    placementType = PlacementType.DAYCARE,
                    placementStartDate = startDate,
                    headUrl = "$adultUrl${parent1.id}",
                    headFirstName = parent1.firstName,
                    headLastName = parent1.lastName,
                    headEmail = parent1.email ?: "",
                    partnerUrl = "$adultUrl${parent2.id}",
                    partnerFirstName = parent2.firstName,
                    partnerLastName = parent2.lastName,
                    partnerEmail = parent2.email ?: "",
                )
            )
    }
}

private data class PalvelukykykyselyRow(
    val childUrl: String,
    val childFirstName: String,
    val childLastName: String,
    val placementUnit: String,
    val placementType: PlacementType,
    val placementStartDate: LocalDate,
    val headUrl: String,
    val headLastName: String,
    val headFirstName: String,
    val headEmail: String,
    val partnerUrl: String,
    val partnerLastName: String,
    val partnerFirstName: String,
    val partnerEmail: String,
)
