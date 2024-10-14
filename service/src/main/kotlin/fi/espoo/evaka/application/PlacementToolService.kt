// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerm
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.FridgeFamilyService
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.getBlockedGuardians
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import org.apache.commons.csv.CSVFormat
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

enum class PlacementToolCsvField(val fieldName: String) {
    CHILD_ID("lapsen_id"),
    PRESCHOOL_UNIT_ID("esiopetusyksikon_id"),
}

@Service
class PlacementToolService(
    private val featureConfig: FeatureConfig,
    private val applicationStateService: ApplicationStateService,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val fridgeFamilyService: FridgeFamilyService,
) {
    init {
        asyncJobRunner.registerHandler(::doCreatePlacementToolApplications)
        asyncJobRunner.registerHandler(::createPlacementToolApplicationsFromSsn)
    }

    fun doCreatePlacementToolApplications(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.PlacementTool,
    ) {
        createApplication(
            db,
            msg.user,
            clock,
            msg.data,
            msg.partTimeServiceNeedOption,
            msg.defaultServiceNeedOption,
            msg.nextPreschoolTerm,
        )
    }

    fun createPlacementToolApplications(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        file: MultipartFile,
    ) {
        val serviceNeedOptions = tx.getServiceNeedOptions()
        val partTimeServiceNeedOptionId =
            serviceNeedOptions
                .firstOrNull {
                    it.validPlacementType == PlacementType.PRESCHOOL && it.defaultOption
                }
                ?.id
        if (null == partTimeServiceNeedOptionId) {
            throw NotFound("No part time service need option found")
        }
        val defaultServiceNeedOptionId =
            serviceNeedOptions
                .firstOrNull {
                    it.validPlacementType == PlacementType.PRESCHOOL_DAYCARE && it.defaultOption
                }
                ?.id
        if (null == defaultServiceNeedOptionId) {
            throw NotFound("No default service need option found")
        }
        val nextPreschoolTermId =
            tx.getPreschoolTerms().firstOrNull { it.finnishPreschool.start > clock.today() }?.id
        if (null == nextPreschoolTermId) {
            throw NotFound("No next preschool term found")
        }
        val placements = file.inputStream.use { parsePlacementToolCsv(it) }
        asyncJobRunner
            .plan(
                tx,
                placements.map { (childIdentifier, preschoolId) ->
                    when {
                        isValidSSN(childIdentifier) ->
                            AsyncJob.PlacementToolFromSSN(
                                user,
                                childIdentifier,
                                preschoolId,
                                partTimeServiceNeedOptionId,
                                defaultServiceNeedOptionId,
                                nextPreschoolTermId,
                            )
                        else ->
                            AsyncJob.PlacementTool(
                                user,
                                PlacementToolData(
                                    ChildId(UUID.fromString(childIdentifier)),
                                    preschoolId,
                                ),
                                partTimeServiceNeedOptionId,
                                defaultServiceNeedOptionId,
                                nextPreschoolTermId,
                            )
                    }
                },
                runAt = clock.now(),
                retryCount = 1,
            )
            .also { Audit.PlacementTool.log(meta = mapOf("total" to placements.size)) }
    }

    fun createPlacementToolApplicationsFromSsn(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.PlacementToolFromSSN,
    ) {
        val childId =
            db.read { tx -> tx.getPersonBySSN(msg.ssn)?.id }
                ?: initFromVtj(db, msg.user, clock, msg.ssn)
        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(
                    AsyncJob.PlacementTool(
                        msg.user,
                        PlacementToolData(childId, msg.preschoolId),
                        msg.partTimeServiceNeedOption,
                        msg.defaultServiceNeedOption,
                        msg.nextPreschoolTerm,
                    )
                ),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    private fun initFromVtj(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        ssn: String,
    ): PersonId {
        val child =
            db.transaction { tx ->
                personService.getOrCreatePerson(tx, user, ExternalIdentifier.SSN.getInstance(ssn))
            }
        fridgeFamilyService.updateChildAndFamilyFromVtj(db, user, clock, child!!.id)
        return child.id
    }

    fun createApplication(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        data: PlacementToolData,
        partTimeServiceNeedOptionId: ServiceNeedOptionId?,
        defaultServiceNeedOptionId: ServiceNeedOptionId?,
        nextPreschoolTermId: PreschoolTermId,
    ) {
        dbc.transaction { tx ->
            if (tx.getChild(data.childId) == null) {
                throw Exception("Child id is null or child not found")
            }
            val guardianIds =
                tx.getChildGuardiansAndFosterParents(data.childId, clock.today()) -
                    tx.getBlockedGuardians(data.childId).toSet()
            if (guardianIds.isEmpty()) {
                throw Exception("No guardians found for child ${data.childId}")
            }
            val guardianId =
                guardianIds.find { id ->
                    id ==
                        tx.getParentships(
                                headOfChildId = null,
                                childId = data.childId,
                                period = DateRange(clock.today(), null),
                            )
                            .firstOrNull()
                            ?.headOfChildId
                } ?: guardianIds.first()
            val desiredStatus = featureConfig.placementToolApplicationStatus

            val (_, applicationId) =
                savePaperApplication(
                    tx,
                    user,
                    clock,
                    PaperApplicationCreateRequest(
                        childId = data.childId,
                        guardianId = guardianId,
                        guardianToBeCreated = null,
                        guardianSsn = null,
                        type = ApplicationType.PRESCHOOL,
                        sentDate = clock.today(),
                        hideFromGuardian = desiredStatus > ApplicationStatus.SENT,
                        transferApplication = false,
                    ),
                    personService,
                    applicationStateService,
                )

            val application = tx.fetchApplicationDetails(applicationId)!!
            val serviceNeedOptions = tx.getServiceNeedOptions()
            val partTimeServiceNeedOption =
                serviceNeedOptions
                    .firstOrNull { it.id == partTimeServiceNeedOptionId }
                    ?.let {
                        ServiceNeedOption(
                            it.id,
                            it.nameFi,
                            it.nameSv,
                            it.nameEn,
                            it.validPlacementType,
                        )
                    }
            val defaultServiceNeedOption =
                serviceNeedOptions
                    .firstOrNull { it.id == defaultServiceNeedOptionId }
                    ?.let {
                        ServiceNeedOption(
                            it.id,
                            it.nameFi,
                            it.nameSv,
                            it.nameEn,
                            it.validPlacementType,
                        )
                    }
            val nextPreschoolTerm = tx.getPreschoolTerm(nextPreschoolTermId)!!

            updateApplicationPreferences(
                tx,
                user,
                clock,
                application,
                data,
                guardianIds,
                partTimeServiceNeedOption!!,
                defaultServiceNeedOption!!,
                nextPreschoolTerm,
            )

            if (desiredStatus >= ApplicationStatus.SENT) {
                applicationStateService.sendPlacementToolApplication(tx, user, clock, application)
            }
            if (desiredStatus >= ApplicationStatus.WAITING_PLACEMENT) {
                applicationStateService.moveToWaitingPlacement(tx, user, clock, application.id)
            }
            if (desiredStatus >= ApplicationStatus.WAITING_DECISION) {
                val period = nextPreschoolTerm.finnishPreschool
                applicationStateService.createPlacementPlan(
                    tx,
                    user,
                    clock,
                    application.id,
                    DaycarePlacementPlan(
                        data.preschoolId,
                        period,
                        period.copy(end = LocalDate.of(period.end.year, 7, 31)),
                    ),
                )
            }
            if (desiredStatus >= ApplicationStatus.WAITING_MAILING) {
                throw UnsupportedOperationException(
                    "Application status $desiredStatus is not supported"
                )
            }
        }
    }

    private fun updateApplicationPreferences(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        application: ApplicationDetails,
        data: PlacementToolData,
        guardianIds: List<PersonId>,
        partTimeServiceNeedOption: ServiceNeedOption,
        defaultServiceNeedOption: ServiceNeedOption,
        preschoolTerm: PreschoolTerm,
    ) {
        val preferredUnit = tx.getDaycare(data.preschoolId)!!
        val partTime =
            tx.getPlacementsForChildDuring(data.childId, clock.today(), null).firstOrNull()?.type in
                listOf(
                    PlacementType.DAYCARE_PART_TIME,
                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
                )

        // update preferences to application
        val updatedApplication =
            application.copy(
                form =
                    application.form.copy(
                        preferences =
                            application.form.preferences.copy(
                                preferredStartDate = preschoolTerm.finnishPreschool.start,
                                preferredUnits =
                                    listOf(PreferredUnit(preferredUnit.id, preferredUnit.name)),
                                serviceNeed =
                                    ServiceNeed(
                                        startTime = "",
                                        endTime = "",
                                        shiftCare = false,
                                        partTime = false,
                                        serviceNeedOption =
                                            if (partTime) partTimeServiceNeedOption
                                            else defaultServiceNeedOption,
                                    ),
                                urgent = false,
                            ),
                        secondGuardian =
                            guardianIds
                                .firstOrNull { it != application.guardianId }
                                ?.let {
                                    val guardian2 = tx.getPersonById(it)!!
                                    SecondGuardian(
                                        phoneNumber = guardian2.phone,
                                        email = guardian2.email ?: "",
                                        agreementStatus = OtherGuardianAgreementStatus.AGREED,
                                    )
                                },
                    ),
                allowOtherGuardianAccess = true,
            )

        applicationStateService.updateApplicationContentsServiceWorker(
            tx,
            user,
            clock.now(),
            application.id,
            ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form)),
            user.evakaUserId,
        )
    }
}

fun parsePlacementToolCsv(inputStream: InputStream): Map<String, DaycareId> =
    CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setHeader()
        .apply { setIgnoreSurroundingSpaces(true) }
        .apply { setDelimiter(';') }
        .build()
        .parse(inputStream.reader())
        .filter { row ->
            row.get(PlacementToolCsvField.CHILD_ID.fieldName).isNotBlank() &&
                row.get(PlacementToolCsvField.PRESCHOOL_UNIT_ID.fieldName).isNotBlank()
        }
        .associate { row ->
            row.get(PlacementToolCsvField.CHILD_ID.fieldName) to
                DaycareId(
                    UUID.fromString(row.get(PlacementToolCsvField.PRESCHOOL_UNIT_ID.fieldName))
                )
        }

data class PlacementToolData(val childId: ChildId, val preschoolId: DaycareId)
