// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.DaycareController
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DaycareEditIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var daycareController: DaycareController

    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val standardOpTime = TimeRange(LocalTime.parse("08:00"), LocalTime.parse("18:00"))
    private val fields =
        DaycareFields(
            name = "Uusi päiväkoti",
            openingDate = LocalDate.of(2020, 1, 1),
            closingDate = LocalDate.of(2120, 1, 1),
            areaId = testArea.id,
            type = setOf(CareType.CENTRE),
            dailyPreschoolTime = null,
            dailyPreparatoryTime = null,
            daycareApplyPeriod = DateRange(LocalDate.of(2020, 3, 1), null),
            preschoolApplyPeriod = null,
            clubApplyPeriod = null,
            providerType = ProviderType.MUNICIPAL,
            capacity = 1,
            language = Language.fi,
            ghostUnit = false,
            uploadToVarda = false,
            uploadChildrenToVarda = false,
            uploadToKoski = false,
            invoicedByMunicipality = false,
            costCenter = "123456",
            dwCostCenter = "dw-test",
            additionalInfo = "Tämä on testi",
            phone = "123 456 7890",
            email = "test@example.com",
            url = "https://espoo.fi",
            visitingAddress =
                VisitingAddress(
                    streetAddress = "Testikatu 1",
                    postalCode = "00000",
                    postOffice = "Espoo"
                ),
            location = Coordinate(1.0, 2.0),
            mailingAddress =
                MailingAddress(poBox = "PL 999", postalCode = "99999", postOffice = "Espoo"),
            unitManager =
                UnitManager(
                    name = "Joh taja",
                    email = "joh.taja@example.com",
                    phone = "000-0000000"
                ),
            decisionCustomization =
                DaycareDecisionCustomization(
                    daycareName = "Uusi päiväkoti (vaka)",
                    preschoolName = "Uusi päiväkoti (eskari)",
                    handler = "Käsittelijä",
                    handlerAddress = "Käsittelijänkuja 1"
                ),
            ophUnitOid = "1.2.3.4.5",
            ophOrganizerOid = "1.22.33.44.55",
            operationTimes =
                listOf(
                    standardOpTime,
                    standardOpTime,
                    standardOpTime,
                    standardOpTime,
                    standardOpTime,
                    null,
                    null
                ),
            financeDecisionHandlerId = null,
            roundTheClock = false,
            businessId = "Y-123456789",
            iban = "FI123456789",
            providerId = "123456789",
            mealtimeBreakfast = null,
            mealtimeLunch = null,
            mealtimeSnack = null,
            mealtimeSupper = null,
            mealtimeEveningSnack = null
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(
                DevDaycare(id = testDaycare.id, areaId = testArea.id, name = testDaycare.name)
            )
        }
    }

    @Test
    fun testCreate() {
        val result = daycareController.createDaycare(dbInstance(), admin, RealEvakaClock(), fields)
        getAndAssertDaycareFields(result.id, fields)
    }

    @Test
    fun testUpdate() {
        daycareController.updateDaycare(
            dbInstance(),
            admin,
            RealEvakaClock(),
            testDaycare.id,
            fields
        )
        getAndAssertDaycareFields(testDaycare.id, fields)
    }

    @Test
    fun testUpdatePreschool() {
        val preschoolFields =
            fields.copy(
                type = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION),
                dailyPreschoolTime = TimeRange(LocalTime.of(9, 30), LocalTime.of(13, 30)),
                dailyPreparatoryTime = TimeRange(LocalTime.of(8, 45), LocalTime.of(13, 45)),
                preschoolApplyPeriod = DateRange(LocalDate.of(2021, 2, 1), null),
                uploadToKoski = true
            )
        daycareController.updateDaycare(
            dbInstance(),
            admin,
            RealEvakaClock(),
            testDaycare.id,
            preschoolFields
        )
        getAndAssertDaycareFields(testDaycare.id, preschoolFields)
    }

    private fun getAndAssertDaycareFields(daycareId: DaycareId, fields: DaycareFields) {
        val body = daycareController.getDaycare(dbInstance(), admin, RealEvakaClock(), daycareId)
        val daycare = body.daycare

        assertEquals(fields.name, daycare.name)
        assertEquals(fields.openingDate, daycare.openingDate)
        assertEquals(fields.closingDate, daycare.closingDate)
        assertEquals(fields.areaId, daycare.area.id)
        assertEquals(fields.type, daycare.type)
        assertEquals(fields.dailyPreschoolTime, daycare.dailyPreschoolTime)
        assertEquals(fields.dailyPreparatoryTime, daycare.dailyPreparatoryTime)
        assertEquals(fields.daycareApplyPeriod, daycare.daycareApplyPeriod)
        assertEquals(fields.preschoolApplyPeriod, daycare.preschoolApplyPeriod)
        assertEquals(fields.clubApplyPeriod, daycare.clubApplyPeriod)
        assertEquals(fields.providerType, daycare.providerType)
        assertEquals(fields.capacity, daycare.capacity)
        assertEquals(fields.language, daycare.language)
        assertEquals(fields.ghostUnit, daycare.ghostUnit)
        assertEquals(fields.uploadToVarda, daycare.uploadToVarda)
        assertEquals(fields.uploadChildrenToVarda, daycare.uploadChildrenToVarda)
        assertEquals(fields.invoicedByMunicipality, daycare.invoicedByMunicipality)
        assertEquals(fields.costCenter, daycare.costCenter)
        assertEquals(fields.dwCostCenter, daycare.dwCostCenter)
        assertEquals(fields.additionalInfo, daycare.additionalInfo)
        assertEquals(fields.phone, daycare.phone)
        assertEquals(fields.email, daycare.email)
        assertEquals(fields.url, daycare.url)
        assertEquals(fields.visitingAddress, daycare.visitingAddress)
        assertEquals(fields.location, daycare.location)
        assertEquals(fields.mailingAddress, daycare.mailingAddress)
        assertEquals(fields.unitManager, daycare.unitManager)
        assertEquals(fields.decisionCustomization, daycare.decisionCustomization)
        assertEquals(fields.ophOrganizerOid, daycare.ophOrganizerOid)
        assertEquals(fields.ophUnitOid, daycare.ophUnitOid)
        assertEquals(fields.operationTimes, daycare.operationTimes)
        val calculatedOpDays =
            fields.operationTimes
                .mapIndexedNotNull { index, tr -> if (tr != null) index + 1 else null }
                .toSet()
        assertEquals(calculatedOpDays, daycare.operationDays)
    }
}
