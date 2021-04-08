// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.DaycareController
import fi.espoo.evaka.daycare.controllers.DaycareResponse
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DaycareEditIntegrationTest : FullApplicationTest() {
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val fields = DaycareFields(
        name = "Uusi päiväkoti",
        openingDate = LocalDate.of(2020, 1, 1),
        closingDate = LocalDate.of(2120, 1, 1),
        areaId = testAreaId,
        type = setOf(CareType.CENTRE),
        daycareApplyPeriod = DateRange(LocalDate.of(2020, 3, 1), null),
        preschoolApplyPeriod = null,
        clubApplyPeriod = null,
        providerType = ProviderType.MUNICIPAL,
        capacity = 1,
        language = Language.fi,
        ghostUnit = false,
        uploadToVarda = false,
        uploadToKoski = false,
        invoicedByMunicipality = false,
        costCenter = "123456",
        additionalInfo = "Tämä on testi",
        phone = "123 456 7890",
        email = "test@example.com",
        url = "https://espoo.fi",
        visitingAddress = VisitingAddress(
            streetAddress = "Testikatu 1",
            postalCode = "00000",
            postOffice = "Espoo"
        ),
        location = Coordinate(1.0, 2.0),
        mailingAddress = MailingAddress(
            poBox = "PL 999",
            postalCode = "99999",
            postOffice = "Espoo"
        ),
        unitManager = UnitManager(
            name = "Joh taja",
            email = "joh.taja@example.com",
            phone = "000-0000000"
        ),
        decisionCustomization = DaycareDecisionCustomization(
            daycareName = "Uusi päiväkoti (vaka)",
            preschoolName = "Uusi päiväkoti (eskari)",
            handler = "Käsittelijä",
            handlerAddress = "Käsittelijänkuja 1"
        ),
        ophUnitOid = "1.2.3.4.5",
        ophOrganizerOid = "1.22.33.44.55",
        ophOrganizationOid = "1.222.333.444.555",
        operationDays = setOf(1, 2, 3, 4, 5),
        financeDecisionHandlerId = null,
        roundTheClock = false
    )

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            h.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
            h.insertTestDaycare(DevDaycare(id = testDaycare.id, areaId = testAreaId, name = testDaycare.name))
        }
    }

    @Test
    fun testCreate() {
        val (_, res, body) = http.put("/daycares")
            .jsonBody(objectMapper.writeValueAsString(fields))
            .asUser(admin)
            .responseObject<DaycareController.CreateDaycareResponse>()
        Assertions.assertTrue(res.isSuccessful)

        getAndAssertDaycareFields(body.get().id, fields)
    }

    @Test
    fun testUpdate() {
        val (_, res, _) = http.put("/daycares/${testDaycare.id}")
            .jsonBody(objectMapper.writeValueAsString(fields))
            .asUser(admin)
            .response()
        Assertions.assertTrue(res.isSuccessful)

        getAndAssertDaycareFields(testDaycare.id, fields)
    }

    private fun getAndAssertDaycareFields(daycareId: UUID, fields: DaycareFields) {
        val (_, _, body) = http.get("/daycares/$daycareId")
            .asUser(admin)
            .responseObject<DaycareResponse>(objectMapper)
        val daycare = body.get().daycare

        assertEquals(fields.name, daycare.name)
        assertEquals(fields.openingDate, daycare.openingDate)
        assertEquals(fields.closingDate, daycare.closingDate)
        assertEquals(fields.areaId, daycare.area.id)
        assertEquals(fields.type, daycare.type)
        assertEquals(fields.daycareApplyPeriod, daycare.daycareApplyPeriod)
        assertEquals(fields.preschoolApplyPeriod, daycare.preschoolApplyPeriod)
        assertEquals(fields.clubApplyPeriod, daycare.clubApplyPeriod)
        assertEquals(fields.providerType, daycare.providerType)
        assertEquals(fields.capacity, daycare.capacity)
        assertEquals(fields.language, daycare.language)
        assertEquals(fields.ghostUnit, daycare.ghostUnit)
        assertEquals(fields.uploadToVarda, daycare.uploadToVarda)
        assertEquals(fields.invoicedByMunicipality, daycare.invoicedByMunicipality)
        assertEquals(fields.costCenter, daycare.costCenter)
        assertEquals(fields.additionalInfo, daycare.additionalInfo)
        assertEquals(fields.phone, daycare.phone)
        assertEquals(fields.email, daycare.email)
        assertEquals(fields.url, daycare.url)
        assertEquals(fields.visitingAddress, daycare.visitingAddress)
        assertEquals(fields.location, daycare.location)
        assertEquals(fields.mailingAddress, daycare.mailingAddress)
        assertEquals(fields.unitManager, daycare.unitManager)
        assertEquals(fields.decisionCustomization, daycare.decisionCustomization)
        assertEquals(fields.ophOrganizationOid, daycare.ophOrganizationOid)
        assertEquals(fields.ophOrganizerOid, daycare.ophOrganizerOid)
        assertEquals(fields.ophUnitOid, daycare.ophUnitOid)
    }
}
