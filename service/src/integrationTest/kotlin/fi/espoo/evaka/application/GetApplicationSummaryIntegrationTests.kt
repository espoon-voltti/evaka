// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.EvakaClock
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
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GetApplicationSummaryIntegrationTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationControllerV2: ApplicationControllerV2

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
        createApplication(child = testChild_1, guardian = testAdult_1)
        createApplication(
            child = testChild_2,
            guardian = testAdult_1,
            extendedCare = true,
            attachment = true
        )
        createApplication(child = testChild_3, guardian = testAdult_1, urgent = true)
        createApplication(
            testChild_4,
            testAdult_1,
            type = ApplicationType.PRESCHOOL,
            connectedDaycare = true,
            serviceNeedOption = null // service need option disabled (=espoo)
        )
        createApplication(
            testChild_5,
            testAdult_1,
            type = ApplicationType.PRESCHOOL,
            connectedDaycare = true,
            serviceNeedOption =
                ServiceNeedOption.of(snPreschoolDaycare45) // service need option enabled (=tampere)
        )
        createApplication(
            testChild_6,
            testAdult_1,
            type = ApplicationType.PRESCHOOL,
            connectedDaycare = true,
            serviceNeedOption = ServiceNeedOption.of(snPreschoolClub45)
        )
    }

    @Test
    fun `application summary with minimal parameters returns data`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 30), LocalTime.of(11, 48)))
        val summary =
            getApplicationSummaries(
                user = serviceWorker,
                clock = clock,
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT)
            )
        assertEquals(6, summary.total)
    }

    @Test
    fun `application summary can be be filtered by attachments`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 30), LocalTime.of(11, 48)))
        val summary =
            getApplicationSummaries(
                user = serviceWorker,
                clock = clock,
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.HAS_ATTACHMENTS)
            )
        assertEquals(1, summary.total)
        assertEquals(1, summary.data[0].attachmentCount)
    }

    @Test
    fun `application summary can be be filtered by urgency`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 30), LocalTime.of(11, 48)))
        val summary =
            getApplicationSummaries(
                user = serviceWorker,
                clock = clock,
                type = ApplicationTypeToggle.ALL,
                status = setOf(ApplicationStatusOption.SENT),
                basis = setOf(ApplicationBasis.URGENT)
            )
        assertEquals(1, summary.total)
        assertEquals(true, summary.data[0].urgent)
    }

    @Test
    fun `application summary can be filtered by preschool types`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 30), LocalTime.of(11, 48)))
        fun getPreschoolApplications(vararg preschoolTypes: ApplicationPreschoolTypeToggle) =
            getApplicationSummaries(
                    user = serviceWorker,
                    clock = clock,
                    type = ApplicationTypeToggle.PRESCHOOL,
                    status = ApplicationStatusOption.values().toSet(),
                    preschoolType = preschoolTypes.toSet()
                )
                .data

        assertThat(getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_DAYCARE))
            .extracting({ it.type }, { it.serviceNeed?.validPlacementType })
            .containsExactlyInAnyOrder(
                Tuple(ApplicationType.PRESCHOOL, null),
                Tuple(ApplicationType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE)
            )
        assertThat(getPreschoolApplications(ApplicationPreschoolTypeToggle.PRESCHOOL_CLUB))
            .extracting({ it.type }, { it.serviceNeed?.validPlacementType })
            .containsExactly(Tuple(ApplicationType.PRESCHOOL, PlacementType.PRESCHOOL_CLUB))
        assertThat(getPreschoolApplications(*ApplicationPreschoolTypeToggle.values()))
            .extracting({ it.type }, { it.serviceNeed?.validPlacementType })
            .containsExactlyInAnyOrder(
                Tuple(ApplicationType.PRESCHOOL, null),
                Tuple(ApplicationType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE),
                Tuple(ApplicationType.PRESCHOOL, PlacementType.PRESCHOOL_CLUB)
            )
    }

    private fun getApplicationSummaries(
        user: AuthenticatedUser,
        clock: EvakaClock,
        page: Int? = null,
        pageSize: Int? = null,
        sortBy: ApplicationSortColumn? = null,
        sortDir: ApplicationSortDirection? = null,
        area: List<String>? = null,
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
    ): Paged<ApplicationSummary> =
        applicationControllerV2.getApplicationSummaries(
            dbInstance(),
            user,
            clock,
            SearchApplicationRequest(
                page = page,
                pageSize = pageSize,
                sortBy = sortBy,
                sortDir = sortDir,
                area = area?.joinToString(","),
                units = units?.joinToString(","),
                basis = basis?.joinToString(","),
                type = type,
                preschoolType = preschoolType?.joinToString(","),
                status = status.joinToString(","),
                dateType = dateType?.joinToString(","),
                distinctions = distinctions?.joinToString(","),
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
        serviceNeedOption: ServiceNeedOption? = null
    ) {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(childId = child.id, guardianId = guardian.id, type = type)
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
                        tx.insertTestApplicationForm(id, form)
                    }
            }

        if (attachment) {
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
            )
        }
    }
}
