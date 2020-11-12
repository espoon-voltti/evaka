// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType.CENTRE
import fi.espoo.evaka.daycare.CareType.CLUB
import fi.espoo.evaka.daycare.CareType.PREPARATORY_EDUCATION
import fi.espoo.evaka.daycare.CareType.PRESCHOOL
import fi.espoo.evaka.daycare.DaycareDecisionCustomization
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.Location
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.UnitManager
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.controllers.getAreas
import fi.espoo.evaka.daycare.createDaycare
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.domain.ProviderType.MUNICIPAL
import fi.espoo.evaka.daycare.domain.ProviderType.MUNICIPAL_SCHOOL
import fi.espoo.evaka.daycare.domain.ProviderType.PRIVATE
import fi.espoo.evaka.daycare.updateDaycare
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.Coordinate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class LocationServiceIntegrationTest : PureJdbiTest() {
    private val areaId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        jdbi.handle {
            it.execute("INSERT INTO care_area (id, name, short_name) VALUES ('$areaId', 'test', 'test')")
        }
    }

    @AfterEach
    internal fun tearDown() {
        jdbi.handle {
            it.execute("DELETE FROM daycare WHERE care_area_id = '$areaId'")
            it.execute("DELETE FROM care_area WHERE id = '$areaId'")
        }
    }

    @Test
    fun `List of location contains created preschools`() {
        val preschool = createGenericUnit(
            areaId = areaId,
            type = setOf(PRESCHOOL), providerType = PRIVATE
        )
        val preschoolId = createDaycare(preschool)

        val prepPreschool = createGenericUnit(
            areaId = areaId,
            type = setOf(PRESCHOOL, PREPARATORY_EDUCATION), providerType = MUNICIPAL_SCHOOL
        )
        val prepPreschoolId = createDaycare(prepPreschool)

        val areas = jdbi.handle { it.getAreas().filter { it.id == areaId } }
        assertThat(areas).size().isEqualTo(1)
        val locationResults = areas.first().locations

        with(assertThat(locationResults)) {
            filteredOn { it.id == preschoolId }.size().isEqualTo(1)
            filteredOn { it.id == prepPreschoolId }.size().isEqualTo(1)
        }

        val resultPreschool = locationResults.first { it.id == preschoolId }
        val expectedPreschool = preschool.copy(id = preschoolId)
        assertThat(resultPreschool).isEqualTo(expectedPreschool)

        val resultPrepPreschool = locationResults.first { it.id == prepPreschoolId }
        val expectedPrepPreschool = prepPreschool.copy(id = prepPreschoolId)
        assertThat(resultPrepPreschool).isEqualTo(expectedPrepPreschool)
    }

    @Test
    fun `List of location contains future units`() {
        val preschool1 = createDaycare(createGenericUnit(areaId = areaId), openingDate = LocalDate.now().minusYears(1), closingDate = LocalDate.now().minusMonths(1))
        val preschool2 = createDaycare(createGenericUnit(areaId = areaId), openingDate = LocalDate.now().minusYears(1), closingDate = LocalDate.now().plusMonths(1))
        val preschool3 = createDaycare(createGenericUnit(areaId = areaId), openingDate = LocalDate.now().plusYears(1), closingDate = LocalDate.now().plusYears(2))

        val areas = jdbi.handle { it.getAreas().filter { it.id == areaId } }
        assertThat(areas).size().isEqualTo(1)
        val locationResults = areas.first().locations

        with(assertThat(locationResults)) {
            filteredOn { it.id == preschool1 }.size().isEqualTo(0)
            filteredOn { it.id == preschool2 }.size().isEqualTo(1)
            filteredOn { it.id == preschool3 }.size().isEqualTo(1)
        }
    }

    private fun createDaycare(location: Location, openingDate: LocalDate? = null, closingDate: LocalDate? = null): UUID = jdbi.handle {
        val id = it.createDaycare(location.care_area_id, location.name)
        it.updateDaycare(
            id,
            DaycareFields(
                name = location.name,
                type = location.type,
                areaId = location.care_area_id,
                visitingAddress = location.visitingAddress,
                mailingAddress = location.mailingAddress,
                canApplyDaycare = location.canApplyDaycare,
                canApplyPreschool = location.canApplyPreschool,
                canApplyClub = location.canApplyClub,
                providerType = location.provider_type ?: ProviderType.MUNICIPAL,
                language = location.language ?: Language.fi,
                location = location.location,
                phone = location.phone,
                url = location.url,
                email = null,
                closingDate = closingDate,
                openingDate = openingDate,
                additionalInfo = null,
                costCenter = null,
                decisionCustomization = DaycareDecisionCustomization(
                    daycareName = location.name,
                    preschoolName = location.name,
                    handlerAddress = "",
                    handler = ""
                ),
                unitManager = UnitManager(name = null, email = null, phone = null),
                invoicedByMunicipality = false,
                uploadToVarda = false,
                uploadToKoski = false,
                capacity = 0,
                ghostUnit = false,
                ophOrganizationOid = null,
                ophOrganizerOid = null,
                ophUnitOid = null,
                operationDays = null
            )
        )
        id
    }

    private fun createGenericUnit(
        areaId: UUID,
        type: Set<fi.espoo.evaka.daycare.CareType> = setOf(CENTRE),
        providerType: ProviderType = MUNICIPAL
    ) = Location(
        id = UUID.randomUUID(),
        name = "Test ${UUID.randomUUID()}",
        care_area_id = areaId,
        type = type,
        language = Language.fi,
        location = Coordinate(lat = 20.20, lon = 10.10),
        mailingAddress = MailingAddress(
            streetAddress = null,
            poBox = "PL 32",
            postalCode = "02070",
            postOffice = "ESPOO"
        ),
        visitingAddress = VisitingAddress(
            streetAddress = "Katukatu 1",
            postalCode = "12345",
            postOffice = "Espoo"
        ),
        url = "https://espoo.fi",
        phone = "123456",
        provider_type = providerType,
        canApplyDaycare = type.contains(CENTRE),
        canApplyPreschool = type.contains(PRESCHOOL),
        canApplyClub = type.contains(CLUB)
    )
}
