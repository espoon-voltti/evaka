// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import com.google.common.hash.Hashing
import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EvakaEnv
import evaka.core.Sensitive
import evaka.core.daycare.anyUnitHasFeature
import evaka.core.holidayperiod.QuestionnaireType
import evaka.core.identity.ExternalId
import evaka.core.identity.ExternalIdentifier
import evaka.core.pairing.*
import evaka.core.pis.service.PersonService
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.*
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.*
import evaka.core.user.*
import evaka.core.webpush.WebPush
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.util.*
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Controller for "system" endpoints intended to be only called from apigw as the system internal
 * user
 */
@RestController
class SystemController(
    private val personService: PersonService,
    private val accessControl: AccessControl,
    private val accessControlCitizen: AccessControlCitizen,
    private val env: EvakaEnv,
    private val passwordService: PasswordService,
    private val webPush: WebPush?,
    private val featureConfig: FeatureConfig,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    @PostMapping("/system/citizen-login")
    fun citizenLogin(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody request: CitizenLoginRequest,
    ): CitizenUserIdentity {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val citizen =
                        tx.getCitizenUserBySsn(request.socialSecurityNumber)
                            ?: personService
                                .getOrCreatePerson(
                                    tx,
                                    user,
                                    ExternalIdentifier.SSN.getInstance(request.socialSecurityNumber),
                                )
                                ?.let { CitizenUserIdentity(it.id) }
                            ?: error("No person found with ssn")
                    val now = clock.now()
                    tx.updateLastStrongLogin(now, citizen.id)
                    tx.updateCitizenOnLogin(now, citizen.id)
                    tx.upsertCitizenUser(citizen.id)
                    personService.getPersonWithChildren(tx, user, citizen.id)
                    citizen
                }
            }
            .also {
                Audit.CitizenLogin.log(
                    targetId = AuditId(request.socialSecurityNumber),
                    objectId = AuditId(it.id),
                    meta = mapOf("lastName" to request.lastName, "firstName" to request.firstName),
                )
            }
    }

    @PostMapping("/system/citizen-weak-login")
    fun citizenWeakLogin(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody request: CitizenWeakLoginRequest,
    ): CitizenUserIdentity {
        Audit.CitizenWeakLoginAttempt.log(targetId = AuditId(request.username))
        return db.connect { dbc ->
            val citizen = dbc.read { it.getCitizenWeakLoginDetails(request.username) }
            dbc.close() // avoid hogging the connection while we check the password

            // We want to run a constant-time password check even if we can't find the user,
            // in order to avoid exposing information about username validity. A dummy
            // placeholder is used if necessary, so we have *something* to compare against.
            // Reference: OWASP Authentication Cheat Sheet - Authentication Responses
            // https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#authentication-responses
            val isMatch = passwordService.isMatch(request.password, citizen?.password)
            if (!isMatch || citizen == null) throw Forbidden()

            dbc.transaction { tx ->
                    val userIdHash =
                        Hashing.sha256()
                            .hashString(citizen.id.toString(), StandardCharsets.UTF_8)
                            .toString()
                    if (!request.deviceAuthHistory.contains(userIdHash)) {
                        logger.info(
                            mapOf(
                                "eventCode" to "NEW_DEVICE_LOGIN",
                                "citizenIdHash" to userIdHash,
                                "deviceAuthHistorySize" to request.deviceAuthHistory.size,
                            )
                        ) {
                            "Login from new device detected"
                        }
                        if (env.newBrowserLoginEmailEnabled) {
                            asyncJobRunner.plan(
                                tx,
                                sequenceOf(
                                    AsyncJob.SendNewBrowserLoginEmail(personId = citizen.id)
                                ),
                                runAt = clock.now(),
                            )
                        }
                    }
                    if (passwordService.needsRehashing(citizen.password)) {
                        tx.updatePasswordWithoutTimestamp(
                            citizen.id,
                            passwordService.encode(request.password),
                        )
                    }

                    val now = clock.now()
                    tx.updateLastWeakLogin(now, citizen.id)
                    tx.updateCitizenOnLogin(now, citizen.id)
                    personService.getPersonWithChildren(tx, user, citizen.id)
                    CitizenUserIdentity(citizen.id)
                }
                .also {
                    Audit.CitizenWeakLogin.log(
                        targetId = AuditId(request.username),
                        objectId = AuditId(it.id),
                    )
                }
        }
    }

    @GetMapping("/system/citizen/{id}")
    fun citizenUser(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @PathVariable id: PersonId,
    ): CitizenUserResponse = db.connect { dbc ->
        dbc.read { tx ->
                val details = tx.getCitizenUserDetails(id) ?: throw NotFound()
                val accessibleFeatures = accessControlCitizen.getPermittedFeatures(tx, clock, id)
                // TODO: remove this extra field, which is only for backwards compatibility
                val authLevel = CitizenAuthLevel.WEAK // dummy value, which isn't really used
                CitizenUserResponse(details, accessibleFeatures, authLevel)
            }
            .also { Audit.CitizenUserDetailsRead.log(targetId = AuditId(id)) }
    }

    @PostMapping("/system/employee-login")
    fun employeeLogin(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody request: EmployeeLoginRequest,
    ): EmployeeUser {
        return db.connect { dbc ->
                dbc.transaction {
                    if (request.employeeNumber != null) {
                        it.updateExternalIdByEmployeeNumber(
                            request.employeeNumber,
                            request.externalId,
                        )
                    }
                    val inserted = it.loginEmployee(clock, request.toNewEmployee())
                    val roles = it.getEmployeeRoles(inserted.id)
                    val employee =
                        EmployeeUser(
                            id = inserted.id,
                            firstName = inserted.preferredFirstName ?: inserted.firstName,
                            lastName = inserted.lastName,
                            globalRoles = roles.globalRoles,
                            allScopedRoles = roles.allScopedRoles,
                            active = inserted.active,
                        )
                    it.upsertEmployeeUser(employee.id)
                    employee
                }
            }
            .also {
                Audit.EmployeeLogin.log(
                    targetId = AuditId(request.externalId.toString()),
                    objectId = AuditId(it.id),
                    meta =
                        mapOf(
                            "lastName" to request.lastName,
                            "firstName" to request.firstName,
                            "email" to request.email,
                            "globalRoles" to it.globalRoles,
                        ),
                )
            }
    }

    @PostMapping("/system/employee-sfi-login")
    fun employeeSuomiFiLogin(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody request: EmployeeSuomiFiLoginRequest,
    ): EmployeeUser {
        Audit.EmployeeSfiLoginAttempt.log(
            targetId = AuditId(request.ssn.value),
            meta = mapOf("lastName" to request.lastName, "firstName" to request.firstName),
        )
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val employee =
                        tx.loginEmployeeWithSuomiFi(clock.now(), request)
                            ?: throw BadRequest("Account not found", "ACCOUNT_NOT_FOUND")
                    if (!employee.active)
                        throw BadRequest("Account is not active", "ACCOUNT_INACTIVE")
                    val roles = tx.getEmployeeRoles(employee.id)
                    val employeeUser =
                        EmployeeUser(
                            id = employee.id,
                            firstName = employee.preferredFirstName ?: employee.firstName,
                            lastName = employee.lastName,
                            globalRoles = roles.globalRoles,
                            allScopedRoles = roles.allScopedRoles,
                            active = employee.active,
                        )
                    tx.upsertEmployeeUser(employee.id)
                    employeeUser
                }
            }
            .also {
                Audit.EmployeeSfiLogin.log(
                    targetId = AuditId(request.ssn.value),
                    objectId = AuditId(it.id),
                    meta =
                        mapOf(
                            "lastName" to request.lastName,
                            "firstName" to request.firstName,
                            "globalRoles" to it.globalRoles,
                        ),
                )
            }
    }

    @GetMapping("/system/employee/{id}")
    fun employeeUser(
        db: Database,
        systemUser: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ): EmployeeUserResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val employeeUser = tx.getEmployeeUser(id) ?: throw NotFound()
                    val user = AuthenticatedUser.Employee(employeeUser)
                    val permittedGlobalActions =
                        accessControl.getPermittedActions<Action.Global>(tx, user, clock)
                    val accessibleFeatures =
                        EmployeeFeatures(
                            applications =
                                permittedGlobalActions.contains(Action.Global.APPLICATIONS_PAGE),
                            employees =
                                permittedGlobalActions.contains(Action.Global.EMPLOYEES_PAGE),
                            financeBasics =
                                permittedGlobalActions.contains(Action.Global.FINANCE_BASICS_PAGE),
                            finance = permittedGlobalActions.contains(Action.Global.FINANCE_PAGE),
                            holidayAndTermPeriods =
                                permittedGlobalActions.contains(
                                    Action.Global.HOLIDAY_AND_TERM_PERIODS_PAGE
                                ),
                            messages = permittedGlobalActions.contains(Action.Global.MESSAGES_PAGE),
                            personSearch =
                                permittedGlobalActions.contains(Action.Global.PERSON_SEARCH_PAGE),
                            reports = permittedGlobalActions.contains(Action.Global.REPORTS_PAGE),
                            settings = permittedGlobalActions.contains(Action.Global.SETTINGS_PAGE),
                            systemNotifications =
                                permittedGlobalActions.contains(
                                    Action.Global.READ_SYSTEM_NOTIFICATIONS
                                ),
                            unitFeatures =
                                permittedGlobalActions.contains(Action.Global.UNIT_FEATURES_PAGE),
                            units = permittedGlobalActions.contains(Action.Global.UNITS_PAGE),
                            createUnits =
                                permittedGlobalActions.contains(Action.Global.CREATE_UNIT),
                            documentTemplates =
                                permittedGlobalActions.contains(
                                    Action.Global.DOCUMENT_TEMPLATES_PAGE
                                ),
                            personalMobileDevice =
                                permittedGlobalActions.contains(
                                    Action.Global.PERSONAL_MOBILE_DEVICE_PAGE
                                ),
                            pinCode = permittedGlobalActions.contains(Action.Global.PIN_CODE_PAGE),
                            createDraftInvoices =
                                permittedGlobalActions.contains(
                                    Action.Global.CREATE_DRAFT_INVOICES
                                ),
                            createPlacements =
                                accessControl.isPermittedForSomeTarget(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.CREATE_PLACEMENT,
                                ),
                            submitPatuReport =
                                permittedGlobalActions.contains(Action.Global.SUBMIT_PATU_REPORT),
                            placementTool =
                                permittedGlobalActions.contains(Action.Global.PLACEMENT_TOOL),
                            replacementInvoices = env.replacementInvoicesStart != null,
                            openRangesHolidayQuestionnaire =
                                featureConfig.holidayQuestionnaireType ==
                                    QuestionnaireType.OPEN_RANGES,
                            outOfOffice =
                                permittedGlobalActions.contains(Action.Global.OUT_OF_OFFICE_PAGE),
                            decisionReasonings =
                                permittedGlobalActions.contains(
                                    Action.Global.WRITE_DECISION_REASONINGS
                                ),
                        )

                    EmployeeUserResponse(
                        id = employeeUser.id,
                        firstName = employeeUser.preferredFirstName ?: employeeUser.firstName,
                        lastName = employeeUser.lastName,
                        globalRoles = employeeUser.globalRoles,
                        allScopedRoles = employeeUser.allScopedRoles,
                        accessibleFeatures = accessibleFeatures,
                        permittedGlobalActions = permittedGlobalActions,
                    )
                }
            }
            .also { Audit.EmployeeUserDetailsRead.log(targetId = AuditId(id)) }
    }

    data class MobileDeviceTracking(val userAgent: String)

    @PostMapping("/system/mobile-devices/{id}")
    fun authenticateMobileDevice(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @PathVariable id: MobileDeviceId,
        @RequestBody tracking: MobileDeviceTracking,
    ): MobileDeviceDetails =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val device = tx.getDevice(id)
                    tx.updateDeviceTracking(id, lastSeen = clock.now(), tracking)
                    device.copy(
                        pushApplicationServerKey =
                            webPush?.applicationServerKey.takeIf {
                                tx.anyUnitHasFeature(
                                    device.unitIds,
                                    PilotFeature.PUSH_NOTIFICATIONS,
                                )
                            }
                    )
                }
            }
            .also { Audit.MobileDevicesRead.log(targetId = AuditId(id)) }

    @GetMapping("/system/mobile-identity/{token}")
    fun mobileIdentity(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable token: UUID,
    ): MobileDeviceIdentity {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val device = tx.getDeviceByToken(token)
                    tx.upsertMobileDeviceUser(device.id)
                    device
                }
            }
            .also {
                Audit.MobileDevicesRead.log(targetId = AuditId(token), objectId = AuditId(it.id))
            }
    }

    @PostMapping("/system/mobile-pin-login")
    fun pinLogin(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody params: PinLoginRequest,
    ): PinLoginResponse =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val employee = tx.getEmployeeUser(params.employeeId)
                    if (employee?.active == false) {
                        throw Forbidden("User is not active")
                    }
                    when (accessControl.verifyPinCode(tx, params.employeeId, params.pin, clock)) {
                        AccessControl.PinError.PIN_LOCKED -> {
                            PinLoginResponse(PinLoginStatus.PIN_LOCKED)
                        }

                        AccessControl.PinError.WRONG_PIN -> {
                            PinLoginResponse(PinLoginStatus.WRONG_PIN)
                        }

                        null -> {
                            employee?.let {
                                PinLoginResponse(
                                    PinLoginStatus.SUCCESS,
                                    PinLoginEmployee(
                                        it.preferredFirstName ?: it.firstName,
                                        it.lastName,
                                    ),
                                )
                            } ?: PinLoginResponse(PinLoginStatus.WRONG_PIN)
                        }
                    }
                }
            }
            .also {
                Audit.PinLogin.log(
                    targetId = AuditId(params.employeeId),
                    meta = mapOf("status" to it.status),
                )
            }

    data class EmployeeLoginRequest(
        val externalId: ExternalId,
        val firstName: String,
        val lastName: String,
        val employeeNumber: String?,
        val email: String?,
    ) {
        fun toNewEmployee(): NewEmployee =
            NewEmployee(
                firstName = firstName,
                lastName = lastName,
                email = email,
                externalId = externalId,
                employeeNumber = employeeNumber,
                temporaryInUnitId = null,
                active = true,
            )
    }

    data class CitizenLoginRequest(
        val socialSecurityNumber: String,
        val firstName: String,
        val lastName: String,
    )

    data class CitizenWeakLoginRequest(
        val username: String,
        val password: Sensitive<String>,
        val deviceAuthHistory: List<String> = emptyList(),
    ) {
        init {
            if (username.lowercase() != username) {
                throw BadRequest("Invalid username")
            }
            if (password.value.length !in PasswordConstraints.SUPPORTED_LENGTH) {
                throw BadRequest("Invalid password length")
            }
        }
    }

    data class EmployeeUserResponse(
        val id: EmployeeId,
        val firstName: String,
        val lastName: String,
        val globalRoles: Set<UserRole> = setOf(),
        val allScopedRoles: Set<UserRole> = setOf(),
        val accessibleFeatures: EmployeeFeatures,
        val permittedGlobalActions: Set<Action.Global>,
    )

    data class CitizenUserResponse(
        val details: CitizenUserDetails,
        val accessibleFeatures: CitizenFeatures,
        // TODO: remove this extra field, which is only for backwards compatibility
        val authLevel: CitizenAuthLevel,
    )

    data class PinLoginRequest(val pin: String, val employeeId: EmployeeId)

    enum class PinLoginStatus {
        SUCCESS,
        WRONG_PIN,
        PIN_LOCKED,
    }

    data class PinLoginEmployee(val firstName: String, val lastName: String)

    data class PinLoginResponse(val status: PinLoginStatus, val employee: PinLoginEmployee? = null)
}
