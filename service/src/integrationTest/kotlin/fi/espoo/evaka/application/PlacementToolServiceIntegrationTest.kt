// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.invoicing.testFeeThresholds
import fi.espoo.evaka.messaging.AccountType
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.getUnreadMessagesCounts
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.MockVtjDataset
import fi.espoo.evaka.vtjclient.service.persondetails.MockVtjPerson
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class PlacementToolServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: PlacementToolController
    @Autowired lateinit var service: PlacementToolService
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var scheduledJobs: ScheduledJobs
    @Autowired lateinit var accessCpontrol: AccessControl

    private val clock = MockEvakaClock(2021, 1, 7, 12, 0)
    final val employee =
        DevEmployee(id = EmployeeId(UUID.randomUUID()), firstName = "Test", lastName = "Employee")
    private val admin = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
    val currentPlacementStart = LocalDate.of(2020, 11, 1)
    val currentPlacementEnd = LocalDate.of(2021, 5, 31)
    val serviceNeedOption =
        ServiceNeedOption(
            ServiceNeedOptionId(UUID.randomUUID()),
            "Esiopetus ja liittyvä varhaiskasvatus",
            "Esiopetus ja liittyvä varhaiskasvatus",
            "Esiopetus ja liittyvä varhaiskasvatus",
            PlacementType.PRESCHOOL_DAYCARE,
        )
    val partTimeServiceNeedOption =
        ServiceNeedOption(
            ServiceNeedOptionId(UUID.randomUUID()),
            "Esiopetus",
            "Esiopetus",
            "Esiopetus",
            PlacementType.PRESCHOOL,
        )
    val preschoolTerm =
        PreschoolTerm(
            PreschoolTermId(UUID.randomUUID()),
            FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3)),
            FiniteDateRange(LocalDate.of(2021, 8, 13), LocalDate.of(2022, 6, 3)),
            FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2022, 6, 3)),
            FiniteDateRange(LocalDate.of(2021, 1, 8), LocalDate.of(2021, 1, 20)),
            DateSet.empty(),
        )

    val adult =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(1980, 1, 1),
            ssn = "010180-1232",
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false,
        )

    val child =
        DevPerson(
            id = ChildId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(2016, 6, 1),
            ssn = "010616A941Y",
            firstName = "Ricky",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false,
        )

    final val area = DevCareArea(id = AreaId(UUID.randomUUID()), name = "Test Area", areaCode = 200)

    val unit =
        DevDaycare(
            id = DaycareId(UUID.randomUUID()),
            name = "Test Daycare",
            areaId = area.id,
            ophOrganizerOid = defaultMunicipalOrganizerOid,
            enabledPilotFeatures =
                setOf(
                    PilotFeature.MESSAGING,
                    PilotFeature.MOBILE,
                    PilotFeature.RESERVATIONS,
                    PilotFeature.PLACEMENT_TERMINATION,
                ),
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(
                DevDaycareGroup(
                    id = GroupId(UUID.randomUUID()),
                    daycareId = unit.id,
                    name = "Test group 1",
                )
            )
            tx.insert(
                fi.espoo.evaka.serviceneed.ServiceNeedOption(
                    id = serviceNeedOption.id,
                    nameFi = "Esiopetus",
                    nameSv = "Esiopetus",
                    nameEn = "Esiopetus",
                    validPlacementType = PlacementType.PRESCHOOL,
                    defaultOption = true,
                    feeCoefficient = BigDecimal("0.00"),
                    occupancyCoefficient = BigDecimal("0.50"),
                    occupancyCoefficientUnder3y = BigDecimal("1.75"),
                    realizedOccupancyCoefficient = BigDecimal("0.50"),
                    realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
                    daycareHoursPerWeek = 0,
                    contractDaysPerMonth = null,
                    daycareHoursPerMonth = null,
                    partDay = true,
                    partWeek = true,
                    feeDescriptionFi = "",
                    feeDescriptionSv = "",
                    voucherValueDescriptionFi = "",
                    voucherValueDescriptionSv = "",
                    validFrom = LocalDate.of(2000, 1, 1),
                    validTo = null,
                )
            )
            tx.insert(
                fi.espoo.evaka.serviceneed.ServiceNeedOption(
                    id = partTimeServiceNeedOption.id,
                    nameFi = "Esiopetus ja liittyvä varhaiskasvatus",
                    nameSv = "Esiopetus ja liittyvä varhaiskasvatus",
                    nameEn = "Esiopetus ja liittyvä varhaiskasvatus",
                    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                    defaultOption = true,
                    feeCoefficient = BigDecimal("0.80"),
                    occupancyCoefficient = BigDecimal("1.00"),
                    occupancyCoefficientUnder3y = BigDecimal("1.75"),
                    realizedOccupancyCoefficient = BigDecimal("1.00"),
                    realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
                    daycareHoursPerWeek = 25,
                    contractDaysPerMonth = null,
                    daycareHoursPerMonth = null,
                    partDay = false,
                    partWeek = false,
                    feeDescriptionFi = "",
                    feeDescriptionSv = "",
                    voucherValueDescriptionFi = "",
                    voucherValueDescriptionSv = "",
                    validFrom = LocalDate.of(2000, 1, 1),
                    validTo = null,
                )
            )
            tx.insert(
                DevPreschoolTerm(
                    preschoolTerm.id,
                    preschoolTerm.finnishPreschool,
                    preschoolTerm.swedishPreschool,
                    preschoolTerm.extendedTerm,
                    preschoolTerm.applicationPeriod,
                    preschoolTerm.termBreaks,
                )
            )
            MockPersonDetailsService.addPersons(adult, child)
            MockPersonDetailsService.addDependants(adult, child)
            tx.upsertEmployeeMessageAccount(employee.id, AccountType.SERVICE_WORKER)
        }
    }

    private fun insertPersonData() {
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(adult.id, child.id))
            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = adult.id,
                    startDate = child.dateOfBirth,
                    endDate = child.dateOfBirth.plusYears(18),
                )
            )
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child.id,
                unit.id,
                currentPlacementStart,
                currentPlacementEnd,
                false,
            )
        }
    }

    @Test
    fun `parse csv`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${child.id}";"${unit.id}"
        """
                .trimIndent()

        val data = parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(1, data.size)
        assertEquals(unit.id, data[child.id.raw.toString()])
    }

    @Test
    fun `parse csv with ssn`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${child.ssn!!}";"${unit.id}"
        """
                .trimIndent()

        val data = parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(1, data.size)
        assertEquals(unit.id, data[child.ssn!!])
    }

    @Test
    fun `parse csv with faulty child id`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "";"${unit.id}"
        """
                .trimIndent()

        val data = parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `parse csv with faulty group id`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${child.id}";""
        """
                .trimIndent()

        val data = parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `parse csv with missing group id field`() {
        val csv =
            """
            "lapsen_id";"esiopetusyksikon_id"
            "${child.id}";
        """
                .trimIndent()

        val data = parsePlacementToolCsv(csv.byteInputStream())
        assertEquals(0, data.size)
    }

    @Test
    fun `create application with one guardian`() {
        insertPersonData()
        val data = PlacementToolData(childId = child.id, preschoolId = unit.id)
        service.createApplication(
            db,
            admin,
            clock,
            data,
            partTimeServiceNeedOption.id,
            serviceNeedOption.id,
            preschoolTerm.id,
        )

        clock.tick()
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries = db.read { it.fetchApplicationSummariesForGuardian(adult.id) }
        assertEquals(1, applicationSummaries.size)

        val summary = applicationSummaries.first()
        assertEquals(summary.childId, child.id)
        assertEquals(summary.preferredUnitId, unit.id)
        assertEquals(summary.status, ApplicationStatus.SENT)

        val messagingAccount = db.read { it.getCitizenMessageAccount(adult.id) }
        assertNotNull(messagingAccount)
        val messageCount =
            db.read {
                it.getUnreadMessagesCounts(
                        accessCpontrol.requireAuthorizationFilter(
                            it,
                            AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK),
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .firstOrNull()
                    ?.unreadCount ?: 0
            }
        assertEquals(1, messageCount)
    }

    @Test
    fun `create application with two guardians`() {
        insertPersonData()
        val adult2 =
            DevPerson(
                id = PersonId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(1979, 2, 1),
                ssn = "010279-123L",
                firstName = "Joan",
                lastName = "Doe",
                streetAddress = "Kamreerintie 2",
                postalCode = "02770",
                postOffice = "Espoo",
                restrictedDetailsEnabled = false,
                email = "joan.doe@example.com",
            )
        db.transaction { tx ->
            tx.insert(adult2, DevPersonType.ADULT)
            tx.insert(DevGuardian(adult2.id, child.id))
            MockPersonDetailsService.addPersons(adult2)
            MockPersonDetailsService.addDependants(adult2, child)
        }
        val data = PlacementToolData(childId = child.id, preschoolId = unit.id)
        service.createApplication(
            db,
            admin,
            clock,
            data,
            partTimeServiceNeedOption.id,
            serviceNeedOption.id,
            preschoolTerm.id,
        )

        clock.tick()
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries = db.read { it.fetchApplicationSummariesForGuardian(adult.id) }
        assertEquals(1, applicationSummaries.size)

        val summary = applicationSummaries.first()
        assertEquals(summary.childId, child.id)
        assertEquals(summary.preferredUnitId, unit.id)
        assertEquals(summary.status, ApplicationStatus.SENT)

        val application = db.read { it.fetchApplicationDetails(summary.applicationId) }
        assertNotNull(application)
        assert(application.allowOtherGuardianAccess)
        assert(!application.hideFromGuardian)
    }

    @Test
    fun `create application for waiting decision`() {
        insertPersonData()
        whenever(featureConfig.placementToolApplicationStatus)
            .thenReturn(ApplicationStatus.WAITING_DECISION)
        val data = PlacementToolData(childId = child.id, preschoolId = unit.id)
        service.createApplication(
            db,
            admin,
            clock,
            data,
            partTimeServiceNeedOption.id,
            serviceNeedOption.id,
            preschoolTerm.id,
        )

        clock.tick()
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries = db.read { it.fetchApplicationSummariesForGuardian(adult.id) }
        assertEquals(1, applicationSummaries.size)

        val summary = applicationSummaries.first()
        assertEquals(summary.childId, child.id)
        assertEquals(summary.preferredUnitId, unit.id)
        assertEquals(summary.status, ApplicationStatus.WAITING_DECISION)

        val application = db.read { it.fetchApplicationDetails(summary.applicationId) }
        assertNotNull(application)
        assert(application.allowOtherGuardianAccess)
        assert(application.hideFromGuardian)

        val messagingAccount = db.read { it.getCitizenMessageAccount(adult.id) }
        assertNotNull(messagingAccount)
        val messageCount =
            db.read {
                it.getUnreadMessagesCounts(
                        accessCpontrol.requireAuthorizationFilter(
                            it,
                            AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK),
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .firstOrNull()
                    ?.unreadCount ?: 0
            }
        assertEquals(0, messageCount)
    }

    @Test
    fun `create application without proper child`() {
        insertPersonData()
        val data = PlacementToolData(childId = ChildId(UUID.randomUUID()), preschoolId = unit.id)
        assertThrows<Exception> {
            service.createApplication(
                db,
                admin,
                clock,
                data,
                partTimeServiceNeedOption.id,
                serviceNeedOption.id,
                preschoolTerm.id,
            )
        }
    }

    @Test
    fun `create application without proper unit`() {
        insertPersonData()
        val data = PlacementToolData(childId = child.id, preschoolId = DaycareId(UUID.randomUUID()))
        assertThrows<Exception> {
            service.createApplication(
                db,
                admin,
                clock,
                data,
                partTimeServiceNeedOption.id,
                serviceNeedOption.id,
                preschoolTerm.id,
            )
        }
    }

    @Test
    fun `create application from ssn`() {
        MockPersonDetailsService.add(
            MockVtjDataset(
                persons = listOf(MockVtjPerson.from(child), MockVtjPerson.from(adult)),
                guardianDependants = mapOf(adult.ssn!! to listOf(child.ssn!!)),
            )
        )
        db.transaction { tx -> tx.insert(testFeeThresholds) }

        controller.createPlacementToolApplications(
            dbInstance(),
            admin,
            clock,
            MockMultipartFile(
                "test.csv",
                """
lapsen_id;esiopetusyksikon_id
${child.ssn!!};${unit.id}
        """
                    .trimIndent()
                    .toByteArray(StandardCharsets.UTF_8),
            ),
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val applicationSummaries =
            db.read {
                val person = it.getPersonBySSN(adult.ssn!!)
                it.fetchApplicationSummariesForGuardian(person!!.id)
            }
        assertEquals(1, applicationSummaries.size)
        val summary = applicationSummaries.first()
        assertEquals(summary.preferredUnitId, unit.id)
        val child = db.read { it.getPersonBySSN(child.ssn!!) }
        assertEquals(summary.childId, child!!.id)
    }
}
