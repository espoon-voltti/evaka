// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.DvvModificationsEnv
import evaka.core.FullApplicationTest
import evaka.core.Sensitive
import evaka.core.VtjXroadEnv
import evaka.core.application.ApplicationControllerCitizen
import evaka.core.application.ApplicationControllerV2
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.PaperApplicationCreateRequest
import evaka.core.application.PlacementToolService
import evaka.core.application.persistence.daycare.Address
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.dvv.DvvModificationsService
import evaka.core.dvv.DvvModificationsServiceClient
import evaka.core.dvv.storeDvvModificationToken
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.SystemController
import evaka.core.pis.controllers.GetOrCreatePersonBySsnRequest
import evaka.core.pis.controllers.PersonController
import evaka.core.pis.getPersonBySSN
import evaka.core.shared.PersonId
import evaka.core.shared.PreschoolTermId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.PasswordService
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.user.updateLastStrongLogin
import evaka.core.user.updateWeakLoginCredentials
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.persondetails.VtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.PERUSSANOMA3
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.convention.TestBean

/**
 * Characterizes which VTJ requests each entry point makes, and which persons, guardian rows and
 * fridge rows it writes.
 *
 * Fixture family: A is a citizen. X is a child of A and B. B is also a guardian of Z, whose other
 * guardian is C. A and X live at one address, B and Z at another, C at a third.
 */
class VtjSyncPathsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @TestBean(name = "mockVtjClient", methodName = "countingVtjClient")
    private lateinit var vtjClient: VtjClient

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var personController: PersonController
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired private lateinit var applicationControllerV2: ApplicationControllerV2
    @Autowired private lateinit var placementToolService: PlacementToolService
    @Autowired private lateinit var familyInitializerService: FamilyInitializerService
    @Autowired private lateinit var passwordService: PasswordService

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0)))

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, daycareApplyPeriod = DateRange(LocalDate.of(2020, 1, 1), null))
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    private val a = person("150385-9013", "Aino", "Kotikatu 1")
    private val b = person("200687-903A", "Bertta", "Toinenkatu 2")
    private val c = person("101090-905X", "Cecilia", "Kolmaskatu 3")
    private val x = person("050120A910J", "Xavier", "Kotikatu 1")
    private val z = person("080622A9123", "Zeta", "Toinenkatu 2")

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(admin)
        }
        MockVtjSoapTransport.resetQueryCounts()
        addToVtj(a, dependants = listOf(x))
        addToVtj(b, dependants = listOf(x, z))
        addToVtj(c, dependants = listOf(z))
        addToVtj(x, guardians = listOf(a, b))
        addToVtj(z, guardians = listOf(b, c))
    }

    @Test
    fun `first strong login creates the citizen and the dependants, but not the other guardian`() {
        strongLogin(a)

        MockVtjSoapTransport.assertRequests(
            PERUSSANOMA3 to a,
            HUOLTAJA_HUOLLETTAVA to a,
            PERUSSANOMA3 to x,
        )
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren()
    }

    @Test
    fun `strong login of a citizen whose dependants are synced makes no VTJ requests`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
            tx.setVtjDependantsQueried(a)
        }

        strongLogin(a)

        MockVtjSoapTransport.assertRequests()
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren()
    }

    @Test
    fun `strong login of a known citizen whose dependants are not synced queries the dependants`() {
        db.transaction { tx -> tx.insert(a, DevPersonType.ADULT) }

        strongLogin(a)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a, PERUSSANOMA3 to x)
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren()
    }

    @Test
    fun `weak login of a citizen whose dependants are not synced queries the dependants`() {
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
            tx.updateWeakLoginCredentials(
                clock.now(),
                a.id,
                "aino@example.com",
                passwordService.encode(password),
            )
        }

        systemController.citizenWeakLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenWeakLoginRequest("aino@example.com", password, emptyList()),
        )

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a, PERUSSANOMA3 to x)
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren()
    }

    @Test
    fun `VTJ button on a citizen refreshes the dependants and adds them to the fridge family`() {
        db.transaction { tx ->
            tx.insertCitizen(a)
            tx.insert(x, DevPersonType.CHILD)
        }

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, a.id)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a)
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren(a to x)
    }

    @Test
    fun `VTJ button on a leaf adult refreshes the dependants and adds them to the fridge family`() {
        db.transaction { tx -> tx.insert(b, DevPersonType.RAW_ROW) }

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, b.id)

        MockVtjSoapTransport.assertRequests(
            HUOLTAJA_HUOLLETTAVA to b,
            PERUSSANOMA3 to x,
            PERUSSANOMA3 to z,
        )
        assertPersons(b, x, z)
        assertGuardians(b to x, b to z)
        assertFridgeChildren(b to z)
    }

    @Test
    fun `VTJ button on a child refreshes the families of its current guardians`() {
        db.transaction { tx ->
            tx.insertCitizen(a)
            tx.insert(c, DevPersonType.RAW_ROW)
            tx.insert(x, DevPersonType.CHILD)
            // C is no longer a guardian of X in VTJ, so the refresh does not query C
            tx.insert(DevGuardian(guardianId = c.id, childId = x.id))
        }

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, x.id)

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to b,
            HUOLTAJA_HUOLLETTAVA to a,
            HUOLTAJA_HUOLLETTAVA to b,
            PERUSSANOMA3 to z,
        )
        assertPersons(a, b, c, x, z)
        assertGuardians(a to x, b to x, b to z)
        assertFridgeChildren(a to x, b to z)
    }

    @Test
    fun `unblocking a guardian refreshes the guardians of the child`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.insert(b, DevPersonType.ADULT)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
            tx.blockGuardian(x.id, b.id)
        }

        personController.updateGuardianEvakaRights(
            dbInstance(),
            admin.user,
            clock,
            x.id,
            PersonController.EvakaRightsRequest(guardianId = b.id, denied = false),
        )

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to x)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren()
    }

    @Test
    fun `UpdateFromVtj on a leaf guardian refreshes only the basic details and clears the dependants marker`() {
        insertFamilyOfA()
        db.transaction { it.setVtjDependantsQueried(b) }

        runUpdateFromVtj(b)

        MockVtjSoapTransport.assertRequests(PERUSSANOMA3 to b)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren()
        assertNull(db.read { it.getPersonBySSN(b.ssn!!) }?.vtjDependantsQueried)
    }

    @Test
    fun `UpdateFromVtj on a citizen refreshes the dependants and adds them to the fridge family`() {
        insertFamilyOfA()

        runUpdateFromVtj(a)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren(a to x)
    }

    @Test
    fun `UpdateFromVtj on a child queries the child's guardians and adds it to the citizen's family`() {
        insertFamilyOfA()

        runUpdateFromVtj(x)

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to x)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren(a to x)
    }

    @Test
    fun `UpdateFromVtj on a minor citizen creates the new child and adds it to the minor's family`() {
        val p = person("010170-903M", "Pertti", "Kotikatu 1")
        val m = person("010609A901V", "Minna", "Kotikatu 1")
        val y = person("011025A9028", "Ykä", "Kotikatu 1")
        addToVtj(p, dependants = listOf(m))
        addToVtj(m, dependants = listOf(y), guardians = listOf(p))
        addToVtj(y, guardians = listOf(m))
        db.transaction { tx ->
            tx.insert(p, DevPersonType.RAW_ROW)
            tx.insertCitizen(m)
            tx.insert(DevGuardian(guardianId = p.id, childId = m.id))
        }

        runUpdateFromVtj(m)

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to m,
            HUOLTAJA_HUOLLETTAVA to m,
            PERUSSANOMA3 to y,
        )
        assertPersons(p, m, y)
        assertGuardians(p to m, m to y)
        assertFridgeChildren(m to y)
    }

    @Test
    fun `UpdateFromVtj on an SSN that is not in eVaka does nothing`() {
        runUpdateFromVtj(b)

        MockVtjSoapTransport.assertRequests()
        assertPersons()
        assertGuardians()
        assertFridgeChildren()
    }

    @Test
    fun `DVV death is written directly and ends the fridge relations`() {
        val dead = person("010180-999A", "Kuollut", "Kotikatu 1")
        val child = person("010617A123U", "Lapsi", "Kotikatu 1")
        db.transaction { tx ->
            tx.insert(dead, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = dead.id,
                    startDate = LocalDate.of(2018, 1, 1),
                    endDate = LocalDate.of(2035, 5, 31),
                )
            )
        }

        runDvvUpdate(dead)

        MockVtjSoapTransport.assertRequests()
        assertEquals(
            LocalDate.of(2019, 7, 30),
            db.read { it.getPersonBySSN(dead.ssn!!) }?.dateOfDeath,
        )
        assertPersons(dead, child)
        assertFridgeChildren()
    }

    @Test
    fun `a death found by the VTJ button ends the fridge relations and plans finance decisions`() {
        val (dead, child) = insertDeadHeadOfFamily()

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, dead.id)

        assertEquals(dateOfDeath, db.read { it.getPersonBySSN(dead.ssn!!) }?.dateOfDeath)
        assertPersons(dead, child)
        assertFridgeChildren()
        assertEquals(2, countFinanceDecisionJobs())
    }

    @Test
    fun `a death found by the VTJ button does not make the deceased citizen a head of family again`() {
        val (dead, _) = insertDeadHeadOfFamily()
        // The citizen_user row makes the deceased person a dependant-sync person, so the fridge
        // step runs for them after the death effects have ended their fridge rows
        db.transaction { tx -> tx.updateLastStrongLogin(clock.now(), dead.id) }

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, dead.id)

        assertFridgeChildren()
    }

    @Test
    fun `a DVV death followed by a refresh runs the death effects once`() {
        val (dead, _) = insertDeadHeadOfFamily()

        runDvvUpdate(dead)
        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, dead.id)

        assertEquals(2, countFinanceDecisionJobs())
    }

    @Test
    fun `a refresh followed by a DVV death runs the death effects once`() {
        val (dead, _) = insertDeadHeadOfFamily()

        personController.updatePersonAndFamilyFromVtj(dbInstance(), admin.user, clock, dead.id)
        runDvvUpdate(dead)

        assertEquals(2, countFinanceDecisionJobs())
    }

    @Test
    fun `DVV restricted details are written directly, and the address change refreshes the person`() {
        val restricted = person("020180-999Y", "Turva", "Kotikatu 1")
        db.transaction { tx -> tx.insert(restricted, DevPersonType.RAW_ROW) }
        addToVtj(restricted.copy(restrictedDetailsEnabled = true))

        runDvvUpdate(restricted)

        MockVtjSoapTransport.assertRequests(PERUSSANOMA3 to restricted)
        assertTrue(db.read { it.getPersonBySSN(restricted.ssn!!) }!!.restrictedDetailsEnabled)
        assertPersons(restricted)
        assertGuardians()
        assertFridgeChildren()
    }

    @Test
    fun `DVV SSN correction is written directly`() {
        val corrected = person("010181-999K", "Korjattu", "Kotikatu 1")
        db.transaction { tx -> tx.insert(corrected, DevPersonType.RAW_ROW) }

        runDvvUpdate(corrected)

        MockVtjSoapTransport.assertRequests()
        assertEquals(corrected.id, db.read { it.getPersonBySSN("010281-999C") }?.id)
        assertPersons(corrected.copy(ssn = "010281-999C"))
        assertGuardians()
        assertFridgeChildren()
    }

    @Test
    fun `DVV HUOLTAJA_SUPPEA on a child refreshes the guardians of the child`() {
        val guardian = person("060180-999J", "Huoltaja", "Kotikatu 1")
        val child = person("060118A999J", "Lapsi", "Kotikatu 1")
        db.transaction { tx -> tx.insert(child, DevPersonType.CHILD) }
        addToVtj(guardian, dependants = listOf(child))
        addToVtj(child, guardians = listOf(guardian))

        runDvvUpdate(child)

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to child,
            PERUSSANOMA3 to guardian,
        )
        assertPersons(guardian, child)
        assertGuardians(guardian to child)
        assertFridgeChildren()
    }

    @Test
    fun `placement tool with a new child creates the child and sets up the families of all guardians`() {
        runPlacementTool(x)

        MockVtjSoapTransport.assertRequests(
            PERUSSANOMA3 to x,
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to a,
            PERUSSANOMA3 to b,
            HUOLTAJA_HUOLLETTAVA to a,
            HUOLTAJA_HUOLLETTAVA to b,
            PERUSSANOMA3 to z,
        )
        assertPersons(a, b, x, z)
        assertGuardians(a to x, b to x, b to z)
        assertFridgeChildren(a to x, b to z)
    }

    @Test
    fun `placement tool with a child whose guardians are synced sets up no family`() {
        db.transaction { tx ->
            tx.insertCitizen(a)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
            tx.setVtjGuardiansQueried(x)
        }

        runPlacementTool(x)

        MockVtjSoapTransport.assertRequests()
        assertPersons(a, x)
        assertGuardians(a to x)
        assertFridgeChildren()
    }

    @Test
    fun `placement tool sets up the family of an old guardian that VTJ no longer lists`() {
        val d = person("030182-9983", "Daniel", "Kotikatu 1")
        addToVtj(d)
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.insert(d, DevPersonType.ADULT)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
            tx.insert(DevGuardian(guardianId = d.id, childId = x.id))
        }

        runPlacementTool(x)

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to b,
            HUOLTAJA_HUOLLETTAVA to a,
            HUOLTAJA_HUOLLETTAVA to d,
            HUOLTAJA_HUOLLETTAVA to b,
            PERUSSANOMA3 to z,
        )
        assertPersons(a, b, d, x, z)
        assertGuardians(a to x, b to x, b to z)
        assertFridgeChildren(a to x, b to z)
        assertNotNull(db.read { it.getPersonBySSN(d.ssn!!) }?.vtjDependantsQueried)
    }

    @Test
    fun `employee create by SSN creates a new person as a leaf`() {
        personController.getOrCreatePersonBySsn(
            dbInstance(),
            admin.user,
            clock,
            GetOrCreatePersonBySsnRequest(a.ssn!!),
        )

        MockVtjSoapTransport.assertRequests(PERUSSANOMA3 to a)
        assertPersons(a)
        assertGuardians()
        assertFridgeChildren()
    }

    @Test
    fun `paper application syncs the dependants of a guardian given by SSN and the guardians of the child`() {
        db.transaction { tx -> tx.insert(x, DevPersonType.CHILD) }

        applicationControllerV2.createPaperApplication(
            dbInstance(),
            admin.user,
            clock,
            paperApplication(guardianSsn = a.ssn),
        )

        MockVtjSoapTransport.assertRequests(
            PERUSSANOMA3 to a,
            HUOLTAJA_HUOLLETTAVA to a,
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to b,
        )
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren()
    }

    @Test
    fun `paper application makes no request for a guardian and a child whose relations are synced`() {
        insertFamilyOfA()

        applicationControllerV2.createPaperApplication(
            dbInstance(),
            admin.user,
            clock,
            paperApplication(guardianId = a.id),
        )

        MockVtjSoapTransport.assertRequests()
    }

    @Test
    fun `family init queries the guardians of a child whose guardians are not synced`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
        }
        val applicationId = db.transaction { tx ->
            tx.insertApplication(ApplicationStatus.WAITING_PLACEMENT)
        }

        familyInitializerService.handleInitializeFamilyFromApplication(
            db,
            clock,
            AsyncJob.InitializeFamilyFromApplication(
                applicationId,
                AuthenticatedUser.SystemInternalUser,
            ),
        )

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to a,
            PERUSSANOMA3 to b,
        )
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren(a to x)
    }

    @Test
    fun `sending an application queries the guardians of a child whose guardians are not synced`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
        }
        val applicationId = db.transaction { tx -> tx.insertApplication(ApplicationStatus.CREATED) }

        applicationControllerCitizen.sendApplication(
            dbInstance(),
            a.user(CitizenAuthLevel.STRONG),
            clock,
            applicationId,
        )

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to a,
            PERUSSANOMA3 to b,
        )
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertFridgeChildren()
    }

    private fun person(ssn: String, firstName: String, streetAddress: String) =
        DevPerson(
            ssn = ssn,
            dateOfBirth = getDobFromSsn(ssn),
            firstName = firstName,
            lastName = "Testinen",
            streetAddress = streetAddress,
            postalCode = "02100",
            postOffice = "Espoo",
        )

    private fun addToVtj(
        person: DevPerson,
        dependants: List<DevPerson> = emptyList(),
        guardians: List<DevPerson> = emptyList(),
    ) {
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(person)
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(person, dependants)
        MockVtjSoapTransport.addHUOLLETTAVAHUOLTAJATRequestExpectation(person, guardians)
    }

    private fun insertFamilyOfA() {
        db.transaction { tx ->
            tx.insertCitizen(a)
            tx.insert(b, DevPersonType.RAW_ROW)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
            tx.insert(DevGuardian(guardianId = b.id, childId = x.id))
            tx.setVtjDependantsQueried(a)
            tx.setVtjGuardiansQueried(x)
        }
    }

    private fun strongLogin(person: DevPerson) {

        systemController.citizenLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenLoginRequest(
                person.ssn!!,
                person.firstName,
                person.lastName,
            ),
        )
    }

    private val dateOfDeath = LocalDate.of(2019, 7, 30)

    /** The DVV mock reports a death on 2019-07-30 for 010180-999A. */
    private fun insertDeadHeadOfFamily(): Pair<DevPerson, DevPerson> {
        val dead = person("010180-999A", "Kuollut", "Kotikatu 1")
        val child = person("010617A123U", "Lapsi", "Kotikatu 1")
        db.transaction { tx ->
            tx.insert(dead, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = dead.id,
                    startDate = LocalDate.of(2018, 1, 1),
                    endDate = LocalDate.of(2035, 5, 31),
                )
            )
        }
        addToVtj(dead.copy(dateOfDeath = dateOfDeath), dependants = listOf(child))
        return dead to child
    }

    private fun countFinanceDecisionJobs(): Int = db.read { tx ->
        tx.createQuery {
                sql("SELECT count(*) FROM async_job WHERE type = 'GenerateFinanceDecisions'")
            }
            .exactlyOne()
    }

    private fun paperApplication(guardianId: PersonId? = null, guardianSsn: String? = null) =
        PaperApplicationCreateRequest(
            childId = x.id,
            guardianId = guardianId,
            guardianToBeCreated = null,
            guardianSsn = guardianSsn,
            type = ApplicationType.DAYCARE,
            sentDate = clock.today(),
            hideFromGuardian = false,
            transferApplication = false,
        )

    private fun runPlacementTool(child: DevPerson) {
        placementToolService.createPlacementToolApplicationsFromSsn(
            db,
            clock,
            AsyncJob.PlacementToolFromSSN(
                admin.user,
                child.ssn!!,
                daycare.id,
                null,
                PreschoolTermId(UUID.randomUUID()),
            ),
        )
    }

    private fun runUpdateFromVtj(person: DevPerson) {
        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.UpdateFromVtj(person.ssn!!)),
                runAt = clock.now(),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun runDvvUpdate(person: DevPerson) {
        db.transaction { tx -> tx.storeDvvModificationToken("100", "101", 0, 0) }
        val client =
            DvvModificationsServiceClient(
                jsonMapper,
                emptyList(),
                VtjXroadEnv.fromEnvironment(env).copy(keyStore = null, trustStore = null),
                DvvModificationsEnv.fromEnvironment(env)
                    .copy(url = "http://localhost:$httpPort/mock-integration/dvv/api/v1"),
            )

        DvvModificationsService(client, asyncJobRunner)
            .updatePersonsFromDvv(db, clock, listOf(person.ssn!!))
        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun Database.Transaction.insertApplication(status: ApplicationStatus) =
        insertTestApplication(
            type = ApplicationType.DAYCARE,
            sentDate = null,
            dueDate = null,
            status = status,
            guardianId = a.id,
            childId = x.id,
            document =
                DaycareFormV0(
                    type = ApplicationType.DAYCARE,
                    child =
                        Child(
                            dateOfBirth = x.dateOfBirth,
                            firstName = x.firstName,
                            lastName = x.lastName,
                            socialSecurityNumber = x.ssn!!,
                            address = Address(x.streetAddress, x.postalCode, x.postOffice),
                        ),
                    guardian =
                        Adult(
                            firstName = a.firstName,
                            lastName = a.lastName,
                            socialSecurityNumber = a.ssn!!,
                            phoneNumber = "0501234567",
                            email = "aino@example.com",
                            address = Address(a.streetAddress, a.postalCode, a.postOffice),
                        ),
                    apply = Apply(preferredUnits = listOf(daycare.id)),
                    preferredStartDate = clock.today().plusMonths(4),
                    serviceStart = "08:00",
                    serviceEnd = "16:00",
                ),
        )

    private fun Database.Transaction.insertCitizen(person: DevPerson) {
        insert(person, DevPersonType.ADULT)
        updateLastStrongLogin(clock.now(), person.id)
    }

    private fun Database.Transaction.setVtjDependantsQueried(person: DevPerson) = execute {
        sql(
            "UPDATE person SET vtj_dependants_queried = ${bind(clock.now())} WHERE id = ${bind(person.id)}"
        )
    }

    private fun Database.Transaction.setVtjGuardiansQueried(person: DevPerson) = execute {
        sql(
            "UPDATE person SET vtj_guardians_queried = ${bind(clock.now())} WHERE id = ${bind(person.id)}"
        )
    }

    private fun assertPersons(vararg persons: DevPerson) {
        assertEquals(
            persons.map { it.ssn!! }.toSet(),
            db.read { tx ->
                tx.createQuery { sql("SELECT social_security_number FROM person") }.toSet<String>()
            },
        )
    }

    private fun assertGuardians(vararg guardianToChild: Pair<DevPerson, DevPerson>) {
        assertEquals(
            guardianToChild.map { (guardian, child) -> guardian.ssn!! to child.ssn!! }.toSet(),
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
SELECT g.social_security_number AS guardian, c.social_security_number AS child
FROM guardian
JOIN person g ON g.id = guardian.guardian_id
JOIN person c ON c.id = guardian.child_id
"""
                        )
                    }
                    .toSet { column<String>("guardian") to column<String>("child") }
            },
        )
    }

    private fun assertFridgeChildren(vararg headToChild: Pair<DevPerson, DevPerson>) {
        assertEquals(
            headToChild.map { (head, child) -> head.ssn!! to child.ssn!! }.toSet(),
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
SELECT h.social_security_number AS head, c.social_security_number AS child
FROM fridge_child fc
JOIN person h ON h.id = fc.head_of_child
JOIN person c ON c.id = fc.child_id
WHERE daterange(fc.start_date, fc.end_date, '[]') @> ${bind(clock.today())}
"""
                        )
                    }
                    .toSet { column<String>("head") to column<String>("child") }
            },
        )
    }

    companion object {
        @JvmStatic
        fun countingVtjClient(): VtjClient =
            SoapVtjClient(MockVtjSoapTransport(), VtjHenkiloMapper())
    }
}
