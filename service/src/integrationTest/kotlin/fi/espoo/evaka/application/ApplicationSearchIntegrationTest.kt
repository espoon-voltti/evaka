// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snPreschoolClub45
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testClub
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationSearchIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationControllerV2: ApplicationControllerV2

    private val now =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 30), LocalTime.of(11, 48)))

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    lateinit var applicationId_1: ApplicationId
    lateinit var applicationId_2: ApplicationId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
        applicationId_1 = createApplication(child = testChild_1, guardian = testAdult_1)
        applicationId_2 =
            createApplication(
                child = testChild_2,
                guardian = testAdult_1,
                extendedCare = true,
                attachment = true
            )
        createApplication(child = testChild_3, guardian = testAdult_1, urgent = true)
    }

    @Test
    fun `application summary with minimal parameters returns data`() {
        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT)
            )
        assertEquals(3, summary.total)
    }

    @Test
    fun `application summary can be be filtered by attachments`() {
        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.HAS_ATTACHMENTS)
            )
        assertEquals(1, summary.total)
        assertEquals(1, summary.data[0].attachmentCount)
    }

    @Test
    fun `application summary can be be filtered by urgency`() {
        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.URGENT)
            )
        assertEquals(1, summary.total)
        assertEquals(true, summary.data[0].urgent)
    }

    @Test
    fun `application summary can be filtered by preschool types`() {
        val noServiceNeed =
            createApplication(
                testChild_4,
                testAdult_1,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption = null // service need option disabled (=espoo)
            )
        val preschoolDaycare =
            createApplication(
                testChild_5,
                testAdult_1,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption =
                    ofServiceNeedOption(
                        snPreschoolDaycare45
                    ) // service need option enabled (=tampere)
            )
        val preschoolClub =
            createApplication(
                testChild_6,
                testAdult_1,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption = ofServiceNeedOption(snPreschoolClub45)
            )
        val preschoolAdditionalDaycare =
            createApplication(
                testChild_7,
                testAdult_1,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                additionalDaycareApplication = true,
                serviceNeedOption = null,
            )
        val preparatoryAdditionalDaycare =
            createApplication(
                testChild_7,
                testAdult_1,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                preparatory = true,
                additionalDaycareApplication = true,
                serviceNeedOption = null,
            )

        fun getPreschoolApplications(vararg preschoolTypes: ApplicationPreschoolTypeToggle) =
            getApplicationSummaries(
                    type = ApplicationTypeToggle.PRESCHOOL,
                    status = ApplicationStatusOption.values().toSet(),
                    preschoolType = preschoolTypes.toSet()
                )
                .data
                .map { it.id }

        assertEquals(
            listOf(noServiceNeed, preschoolDaycare).sorted(),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_DAYCARE).sorted(),
        )
        assertEquals(
            listOf(preschoolClub),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_CLUB)
        )
        assertEquals(
            listOf(preschoolAdditionalDaycare, preparatoryAdditionalDaycare).sorted(),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.DAYCARE_ONLY).sorted()
        )
        assertEquals(
            listOf(
                    noServiceNeed,
                    preschoolDaycare,
                    preschoolClub,
                    preschoolAdditionalDaycare,
                    preparatoryAdditionalDaycare
                )
                .sorted(),
            getPreschoolApplications(*ApplicationPreschoolTypeToggle.values()).sorted()
        )
    }

    @Test
    fun `getChildApplicationSummaries returns only given child's applications`() {
        assertEquals(
            listOf(testChild_1.id),
            applicationControllerV2
                .getChildApplicationSummaries(dbInstance(), serviceWorker, now, testChild_1.id)
                .map { it.childId }
        )
    }

    @Test
    fun `getChildApplicationSummaries returns empty list with child without applications`() {
        assertEquals(
            listOf(),
            applicationControllerV2.getChildApplicationSummaries(
                dbInstance(),
                serviceWorker,
                now,
                testChild_7.id
            )
        )
    }

    @Test
    fun `getChildApplicationSummaries returns empty list with unknown child id`() {
        assertEquals(
            listOf(),
            applicationControllerV2.getChildApplicationSummaries(
                dbInstance(),
                serviceWorker,
                now,
                ChildId(UUID.randomUUID())
            )
        )
    }

    @Test
    fun `application summary can be be filtered by children with existing club placement`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testClub.id,
                type = PlacementType.CLUB,
                startDate = now.today().minusMonths(12),
                endDate = now.today().minusMonths(6)
            )

            it.insertTestPlacement(
                childId = testChild_2.id,
                unitId = testClub.id,
                type = PlacementType.CLUB,
                startDate = now.today().minusMonths(12),
                endDate = now.today().plusMonths(6)
            )

            // Should not show because club placement is in the future
            it.insertTestPlacement(
                childId = testChild_3.id,
                unitId = testClub.id,
                type = PlacementType.CLUB,
                startDate = now.today().plusMonths(1),
                endDate = now.today().plusMonths(6)
            )
        }

        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.CLUB_CARE)
            )
        assertEquals(2, summary.total)
        assertEquals(applicationId_2, summary.data[0].id)
        assertEquals(applicationId_1, summary.data[1].id)
    }

    private fun getApplicationSummaries(
        page: Int? = null,
        pageSize: Int? = null,
        sortBy: ApplicationSortColumn? = null,
        sortDir: ApplicationSortDirection? = null,
        area: List<AreaId>? = null,
        units: List<DaycareId>? = null,
        basis: Set<ApplicationBasis>? = null,
        type: ApplicationTypeToggle,
        preschoolType: Set<ApplicationPreschoolTypeToggle>? = null,
        status: Set<ApplicationStatusOption>,
        dateType: Set<ApplicationDateType>? = null,
        distinctions: Set<ApplicationDistinctions>? = null,
        periodStart: LocalDate? = null,
        periodEnd: LocalDate? = null,
        searchTerms: String? = null,
        transferApplications: TransferApplicationFilter? = null,
        voucherApplications: VoucherApplicationFilter? = null
    ): PagedApplicationSummaries =
        applicationControllerV2.getApplicationSummaries(
            dbInstance(),
            serviceWorker,
            now,
            SearchApplicationRequest(
                page = page,
                pageSize = pageSize,
                sortBy = sortBy,
                sortDir = sortDir,
                areas = area,
                units = units,
                basis = basis?.toList(),
                type = type,
                preschoolType = preschoolType?.toList(),
                statuses = status.toList(),
                dateType = dateType?.toList(),
                distinctions = distinctions?.toList(),
                periodStart = periodStart,
                periodEnd = periodEnd,
                searchTerms = searchTerms,
                transferApplications = transferApplications,
                voucherApplications = voucherApplications
            )
        )

    private fun createApplication(
        child: DevPerson,
        guardian: DevPerson,
        type: ApplicationType = ApplicationType.DAYCARE,
        attachment: Boolean = false,
        urgent: Boolean = false,
        extendedCare: Boolean = false,
        connectedDaycare: Boolean = false,
        preparatory: Boolean = false,
        additionalDaycareApplication: Boolean = false,
        serviceNeedOption: ServiceNeedOption? = null
    ): ApplicationId {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = child.id,
                        guardianId = guardian.id,
                        type = type,
                        additionalDaycareApplication = additionalDaycareApplication
                    )
                    .also { id ->
                        val form =
                            DaycareFormV0.fromApplication2(
                                    validDaycareApplication.copy(
                                        childId = child.id,
                                        guardianId = guardian.id,
                                        type = type
                                    )
                                )
                                .copy(urgent = urgent)
                                .copy(extendedCare = extendedCare)
                                .copy(connectedDaycare = connectedDaycare)
                                .copy(serviceNeedOption = serviceNeedOption)
                                .let {
                                    if (preparatory)
                                        it.copy(
                                            careDetails = it.careDetails.copy(preparatory = true)
                                        )
                                    else it
                                }
                        tx.insertTestApplicationForm(id, form)
                    }
            }

        if (attachment) {
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
            )
        }

        return applicationId
    }
}

private fun ofServiceNeedOption(option: fi.espoo.evaka.serviceneed.ServiceNeedOption) =
    ServiceNeedOption(
        option.id,
        option.nameFi,
        option.nameSv,
        option.nameEn,
        option.validPlacementType
    )
