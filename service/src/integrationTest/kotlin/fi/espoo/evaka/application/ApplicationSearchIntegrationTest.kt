// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snPreschoolClub45
import fi.espoo.evaka.snPreschoolDaycare45
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val club = DevDaycare(areaId = area.id, name = "Test Club", type = setOf(CareType.CLUB))
    private val employee = DevEmployee()
    private val serviceWorker =
        AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER))

    private val adult = DevPerson()
    private val child1 = DevPerson(lastName = "Doe", firstName = "Ricky")
    private val child2 = DevPerson(ssn = "010316A1235", lastName = "Doe", firstName = "Micky")
    private val child3 = DevPerson()
    private val child4 = DevPerson()
    private val child5 = DevPerson()
    private val child6 = DevPerson()
    private val child7 = DevPerson()

    lateinit var applicationId_1: ApplicationId
    lateinit var applicationId_2: ApplicationId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(club)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2, child3, child4, child5, child6, child7).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
        applicationId_1 = createApplication(child = child1, guardian = adult)
        applicationId_2 =
            createApplication(
                child = child2,
                guardian = adult,
                extendedCare = true,
                attachment = true,
            )
        createApplication(child = child3, guardian = adult, urgent = true)
    }

    @Test
    fun `application summary with minimal parameters returns data`() {
        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
            )
        assertEquals(3, summary.total)
    }

    @Test
    fun `application summary can be be filtered by attachments`() {
        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.HAS_ATTACHMENTS),
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
                basis = setOf(ApplicationBasis.URGENT),
            )
        assertEquals(1, summary.total)
        assertEquals(true, summary.data[0].urgent)
    }

    @Test
    fun `application summary can be filtered by preschool types`() {
        val noServiceNeed =
            createApplication(
                child4,
                adult,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption = null, // service need option disabled (=espoo)
            )
        val preschoolDaycare =
            createApplication(
                child5,
                adult,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption =
                    ofServiceNeedOption(
                        snPreschoolDaycare45
                    ), // service need option enabled (=tampere)
            )
        val preschoolClub =
            createApplication(
                child6,
                adult,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                serviceNeedOption = ofServiceNeedOption(snPreschoolClub45),
            )
        val preschoolAdditionalDaycare =
            createApplication(
                child7,
                adult,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                additionalDaycareApplication = true,
                serviceNeedOption = null,
            )
        val preparatoryAdditionalDaycare =
            createApplication(
                child7,
                adult,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
                preparatory = true,
                additionalDaycareApplication = true,
                serviceNeedOption = null,
            )

        fun getPreschoolApplications(vararg preschoolTypes: ApplicationPreschoolTypeToggle) =
            getApplicationSummaries(
                    type = ApplicationTypeToggle.PRESCHOOL,
                    status = ApplicationStatusOption.entries.toSet(),
                    preschoolType = preschoolTypes.toSet(),
                )
                .data
                .map { it.id }

        assertEquals(
            listOf(noServiceNeed, preschoolDaycare).sorted(),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_DAYCARE).sorted(),
        )
        assertEquals(
            listOf(preschoolClub),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_CLUB),
        )
        assertEquals(
            listOf(preschoolAdditionalDaycare, preparatoryAdditionalDaycare).sorted(),
            getPreschoolApplications(ApplicationPreschoolTypeToggle.DAYCARE_ONLY).sorted(),
        )
        assertEquals(
            listOf(
                    noServiceNeed,
                    preschoolDaycare,
                    preschoolClub,
                    preschoolAdditionalDaycare,
                    preparatoryAdditionalDaycare,
                )
                .sorted(),
            getPreschoolApplications(*ApplicationPreschoolTypeToggle.entries.toTypedArray())
                .sorted(),
        )
    }

    @Test
    fun `getChildApplicationSummaries returns only given child's applications`() {
        assertEquals(
            listOf(child1.id),
            applicationControllerV2
                .getChildApplicationSummaries(dbInstance(), serviceWorker, now, child1.id)
                .map { it.childId },
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
                child7.id,
            ),
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
                ChildId(UUID.randomUUID()),
            ),
        )
    }

    @Test
    fun `application summary can be be filtered by children with existing club placement`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = child1.id,
                    unitId = club.id,
                    startDate = now.today().minusMonths(12),
                    endDate = now.today().minusMonths(6),
                )
            )

            it.insert(
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = child2.id,
                    unitId = club.id,
                    startDate = now.today().minusMonths(12),
                    endDate = now.today().plusMonths(6),
                )
            )

            // Should not show because club placement is in the future
            it.insert(
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = child3.id,
                    unitId = club.id,
                    startDate = now.today().plusMonths(1),
                    endDate = now.today().plusMonths(6),
                )
            )
        }

        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.CLUB_CARE),
            )
        assertEquals(2, summary.total)
        assertEquals(applicationId_2, summary.data[0].id)
        assertEquals(applicationId_1, summary.data[1].id)
    }

    @Test
    fun `application summary can be be filtered by children with existing placement to the preferred unit`() {
        db.transaction {
            // Active placement
            it.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = now.today().minusMonths(12),
                    endDate = now.today().plusMonths(6),
                )
            )

            // Past placement (not included)
            it.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = now.today().minusMonths(12),
                    endDate = now.today().minusDays(1),
                )
            )
        }

        val summary =
            getApplicationSummaries(
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.CONTINUATION),
            )
        assertEquals(1, summary.total)
        assertEquals(applicationId_1, summary.data[0].id)
    }

    @Test
    fun `application summary has details of sibling basis`() {
        val applicationId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = now.today().minusMonths(12),
                        endDate = now.today().plusMonths(6),
                    )
                )
                tx.insertTestApplication(
                    childId = child4.id,
                    guardianId = adult.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply =
                                Apply(
                                    preferredUnits = listOf(daycare.id),
                                    siblingBasis = true,
                                    siblingSsn = child2.ssn!!,
                                    siblingName = "does not matter",
                                ),
                        ),
                )
            }
        val summary =
            getApplicationSummaries(
                    type = ApplicationTypeToggle.ALL,
                    status = ApplicationStatusOption.entries.toSet(),
                )
                .data
                .first { it.id == applicationId }
        assertEquals(true, summary.siblingBasis)
        assertEquals("${child2.lastName} ${child2.firstName}", summary.siblingName)
        assertEquals(daycare.name, summary.siblingUnitName)
    }

    private fun getApplicationSummaries(
        page: Int? = null,
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
        voucherApplications: VoucherApplicationFilter? = null,
    ): PagedApplicationSummaries =
        applicationControllerV2.getApplicationSummaries(
            dbInstance(),
            serviceWorker,
            now,
            SearchApplicationRequest(
                page = page,
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
                voucherApplications = voucherApplications,
            ),
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
        serviceNeedOption: ServiceNeedOption? = null,
    ): ApplicationId {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = type,
                    additionalDaycareApplication = additionalDaycareApplication,
                    document =
                        DaycareFormV0(
                            type = type,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                            urgent = urgent,
                            extendedCare = extendedCare,
                            connectedDaycare =
                                if (type == ApplicationType.PRESCHOOL) connectedDaycare else null,
                            serviceNeedOption = serviceNeedOption,
                            careDetails =
                                CareDetails(
                                    preparatory =
                                        if (type == ApplicationType.PRESCHOOL) preparatory else null
                                ),
                        ),
                )
            }

        if (attachment) {
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG),
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
        option.validPlacementType,
    )
