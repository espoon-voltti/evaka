// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReportSmokeTests : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired private lateinit var applicationsReportController: ApplicationsReportController

    @Autowired
    private lateinit var assistanceNeedsAndActionsReportController:
        AssistanceNeedsAndActionsReportController

    @Autowired
    private lateinit var childAgeLanguageReportController: ChildAgeLanguageReportController

    @Autowired
    private lateinit var childrenInDifferentAddressReportController:
        ChildrenInDifferentAddressReportController

    @Autowired private lateinit var decisionsReportController: DecisionsReportController
    @Autowired private lateinit var duplicatePeopleReportController: DuplicatePeopleReportController
    @Autowired private lateinit var endedPlacementsReportController: EndedPlacementsReportController
    @Autowired private lateinit var familyConflictReportController: FamilyConflictReportController
    @Autowired private lateinit var familyContactReportController: FamilyContactReportController
    @Autowired private lateinit var invoiceReportController: InvoiceReportController

    @Autowired
    private lateinit var missingHeadOfFamilyReportController: MissingHeadOfFamilyReportController

    @Autowired
    private lateinit var missingServiceNeedReportController: MissingServiceNeedReportController

    @Autowired private lateinit var occupancyReportController: OccupancyReportController

    @Autowired
    private lateinit var partnersInDifferentAddressReportController:
        PartnersInDifferentAddressReportController

    @Autowired private lateinit var rawReportController: RawReportController
    @Autowired private lateinit var serviceNeedReportController: ServiceNeedReport

    @Autowired
    private lateinit var startingPlacementsReportController: StartingPlacementsReportController

    @Autowired
    private lateinit var placementSketchingReportController: PlacementSketchingReportController

    @Autowired private lateinit var vardaErrorReportController: VardaErrorReport
    @Autowired private lateinit var unitsReportController: UnitsReportController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val child = DevPerson()
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))

    private val clock = MockEvakaClock(2020, 8, 1, 12, 0)

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(employee)
        }
    }

    @Test
    fun `applications report returns http 200`() {
        applicationsReportController.getApplicationsReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 8, 1),
        )
    }

    @Test
    fun `assistance needs and actions report returns http 200`() {
        assistanceNeedsAndActionsReportController.getAssistanceNeedsAndActionsReport(
            dbInstance(),
            employee.user,
            clock,
            date = LocalDate.of(2020, 8, 1),
        )
    }

    @Test
    fun `child-age-language report returns http 200`() {
        childAgeLanguageReportController.getChildAgeLanguageReport(
            dbInstance(),
            employee.user,
            clock,
            date = LocalDate.of(2020, 8, 1),
        )
    }

    @Test
    fun `children-in-different-address report returns http 200`() {
        childrenInDifferentAddressReportController.getChildrenInDifferentAddressReport(
            dbInstance(),
            employee.user,
            clock,
        )
    }

    @Test
    fun `decisions report returns http 200`() {
        decisionsReportController.getDecisionsReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 8, 1),
            applicationType = null,
        )

        decisionsReportController.getDecisionsReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 8, 1),
            applicationType = ApplicationType.DAYCARE,
        )
    }

    @Test
    fun `duplicate-people report returns http 200`() {
        duplicatePeopleReportController.getDuplicatePeopleReport(dbInstance(), employee.user, clock)
    }

    @Test
    fun `ended-placements report returns http 200`() {
        endedPlacementsReportController.getEndedPlacementsReport(
            dbInstance(),
            employee.user,
            clock,
            year = 2020,
            month = 1,
        )
    }

    @Test
    fun `family-conflicts report returns http 200`() {
        familyConflictReportController.getFamilyConflictsReport(dbInstance(), employee.user, clock)
    }

    @Test
    fun `family-contacts report returns http 200`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2022, 3, 1),
                )
            )
        }
        familyContactReportController.getFamilyContactsReport(
            dbInstance(),
            employee.user,
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 2, 1), LocalTime.of(12, 0))),
            unitId = daycare.id,
            date = LocalDate.of(2022, 2, 1),
        )
    }

    @Test
    fun `invoice report returns http 200`() {
        invoiceReportController.getInvoiceReport(
            dbInstance(),
            employee.user,
            clock,
            yearMonth = YearMonth.of(2020, 8),
        )
    }

    @Test
    fun `missing-head-of-family report returns http 200`() {
        missingHeadOfFamilyReportController.getMissingHeadOfFamilyReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 8, 1),
        )
    }

    @Test
    fun `missing-service-need report returns http 200`() {
        missingServiceNeedReportController.getMissingServiceNeedReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 8, 1),
        )
    }

    @Test
    fun `occupancy-by-unit report returns http 200`() {
        occupancyReportController.getOccupancyUnitReport(
            dbInstance(),
            employee.user,
            clock,
            type = OccupancyType.PLANNED,
            careAreaId = area.id,
            unitIds = null,
            providerType = null,
            unitTypes = null,
            year = 2020,
            month = 1,
        )

        occupancyReportController.getOccupancyUnitReport(
            dbInstance(),
            employee.user,
            clock,
            type = OccupancyType.CONFIRMED,
            careAreaId = area.id,
            unitIds = null,
            providerType = null,
            unitTypes = null,
            year = 2020,
            month = 1,
        )

        occupancyReportController.getOccupancyUnitReport(
            dbInstance(),
            employee.user,
            clock,
            type = OccupancyType.REALIZED,
            careAreaId = area.id,
            unitIds = null,
            providerType = null,
            unitTypes = null,
            year = 2020,
            month = 1,
        )
    }

    @Test
    fun `occupancy-by-group report returns http 200`() {
        occupancyReportController.getOccupancyGroupReport(
            dbInstance(),
            employee.user,
            clock,
            type = OccupancyType.CONFIRMED,
            careAreaId = area.id,
            unitIds = null,
            providerType = null,
            unitTypes = null,
            year = 2020,
            month = 1,
        )

        occupancyReportController.getOccupancyGroupReport(
            dbInstance(),
            employee.user,
            clock,
            type = OccupancyType.REALIZED,
            careAreaId = area.id,
            unitIds = null,
            providerType = null,
            unitTypes = null,
            year = 2020,
            month = 1,
        )
    }

    @Test
    fun `partners-in-different-address report returns http 200`() {
        partnersInDifferentAddressReportController.getPartnersInDifferentAddressReport(
            dbInstance(),
            employee.user,
            clock,
        )
    }

    @Test
    fun `raw report returns http 200`() {
        rawReportController.getRawReport(
            dbInstance(),
            employee.user,
            clock,
            from = LocalDate.of(2020, 5, 1),
            to = LocalDate.of(2020, 5, 2),
        )
    }

    @Test
    fun `service-need report returns http 200`() {
        serviceNeedReportController.getServiceNeedReport(
            dbInstance(),
            employee.user,
            clock,
            date = LocalDate.of(2020, 8, 1),
            areaId = null,
            providerType = null,
            placementType = null,
        )
    }

    @Test
    fun `starting-placements report returns http 200`() {
        startingPlacementsReportController.getStartingPlacementsReport(
            dbInstance(),
            employee.user,
            clock,
            year = 2020,
            month = 1,
        )
    }

    @Test
    fun `placement sketching report returns http 200`() {
        placementSketchingReportController.getPlacementSketchingReport(
            dbInstance(),
            employee.user,
            clock,
            placementStartDate = LocalDate.of(2021, 1, 1),
            earliestPreferredStartDate = LocalDate.of(2021, 8, 13),
        )
    }

    @Test
    fun `varda error reports return http 200`() {
        vardaErrorReportController.getVardaChildErrorsReport(dbInstance(), employee.user, clock)
        vardaErrorReportController.getVardaUnitErrorsReport(dbInstance(), employee.user, clock)
    }

    @Test
    fun `units report returns http 200`() {
        unitsReportController.getUnitsReport(dbInstance(), employee.user, clock)
    }
}
