package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.getBlockedGuardians
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.serviceneed.getServiceNeedOptionPublicInfos
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.io.InputStream
import java.util.UUID
import org.apache.commons.csv.CSVFormat
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

enum class PlacementToolCsvField(val fieldName: String) {
    CHILD_ID("lapsen id"),
    PRESCHOOL_GROUP_ID("esiopetusryhma_id")
}

@Service
class PlacementToolService(
    private val applicationStateService: ApplicationStateService,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler(::doCreatePlacementToolApplications)
    }

    fun doCreatePlacementToolApplications(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.PlacementTool
    ) {
        msg.user?.let {
            createApplication(
                db,
                it,
                clock,
                msg.data,
                msg.defaultServiceNeedOption,
                msg.nextPreschoolTerm
            )
        }
    }

    fun createPlacementToolApplications(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        file: MultipartFile
    ) {

        db.connect { dbc ->
            dbc.transaction { tx ->
                val defaultServiceNeedOption =
                    tx.getServiceNeedOptionPublicInfos(listOf(PlacementType.PRESCHOOL_DAYCARE))
                        .firstOrNull()
                        ?.let {
                            ServiceNeedOption(
                                id = it.id,
                                nameFi = it.nameFi,
                                nameSv = it.nameSv,
                                nameEn = it.nameEn,
                                validPlacementType = it.validPlacementType
                            )
                        }
                if (null == defaultServiceNeedOption) {
                    throw NotFound("No default service need option found")
                }
                val nextPreschoolTerm =
                    tx.getPreschoolTerms().firstOrNull { it.finnishPreschool.start > clock.today() }
                if (null == nextPreschoolTerm) {
                    throw NotFound("No next preschool term found")
                }
                val placements = parsePlacementToolCsv(file.inputStream)
                placements
                    .forEach { data ->
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.PlacementTool(
                                    data,
                                    defaultServiceNeedOption,
                                    nextPreschoolTerm
                                )
                            ),
                            runAt = clock.now(),
                            retryCount = 1
                        )
                    }
                    .also { Audit.PlacementTool.log(meta = mapOf("total" to placements.size)) }
            }
        }
    }

    fun createApplication(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        data: PlacementToolData,
        defaultServiceNeedOption: ServiceNeedOption?,
        nextPreschoolTerm: PreschoolTerm
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
                                period = DateRange(clock.today(), null)
                            )
                            .firstOrNull()
                            ?.headOfChildId
                } ?: guardianIds.first()

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
                        hideFromGuardian = false,
                        transferApplication = false
                    ),
                    personService,
                    applicationStateService
                )

            val application = tx.fetchApplicationDetails(applicationId)!!

            updateApplicationPreferences(
                tx,
                user,
                clock,
                application,
                data,
                guardianIds,
                defaultServiceNeedOption,
                nextPreschoolTerm
            )

            applicationStateService.sendPlacementToolApplication(tx, user, clock, application)
        }
    }

    fun parsePlacementToolCsv(inputStream: InputStream): List<PlacementToolData> =
        CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .apply { setIgnoreSurroundingSpaces(true) }
            .build()
            .parse(inputStream.reader())
            .map { row ->
                PlacementToolData(
                    childId =
                        ChildId(UUID.fromString(row.get(PlacementToolCsvField.CHILD_ID.fieldName))),
                    preschoolId =
                        DaycareId(
                            UUID.fromString(
                                row.get(PlacementToolCsvField.PRESCHOOL_GROUP_ID.fieldName)
                            )
                        )
                )
            }

    private fun updateApplicationPreferences(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        application: ApplicationDetails,
        data: PlacementToolData,
        guardianIds: List<PersonId>,
        defaultServiceNeedOption: ServiceNeedOption?,
        preschoolTerm: PreschoolTerm
    ) {
        val preferredUnit = tx.getDaycare(data.preschoolId)!!
        val partTime =
            tx.getPlacementsForChildDuring(data.childId, clock.today(), null)
                .firstOrNull()
                ?.type in
                listOf(
                    PlacementType.DAYCARE_PART_TIME,
                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
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
                                    if (partTime) {
                                        ServiceNeed(
                                            startTime = "07:00", // todo: parametrize
                                            endTime = "17:00", // todo: parametrize
                                            shiftCare = false,
                                            partTime = false,
                                            serviceNeedOption = defaultServiceNeedOption
                                        )
                                    } else {
                                        null
                                    },
                                urgent = false
                            ),
                        secondGuardian =
                            guardianIds
                                .firstOrNull { it != application.guardianId }
                                ?.let {
                                    val guardian2 = tx.getPersonById(it)!!
                                    SecondGuardian(
                                        phoneNumber = guardian2.phone,
                                        email = guardian2.email ?: "",
                                        agreementStatus = OtherGuardianAgreementStatus.AGREED
                                    )
                                }
                    )
            )

        applicationStateService.updateApplicationContentsServiceWorker(
            tx,
            user,
            clock.now(),
            application.id,
            ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form)),
            user.evakaUserId
        )
    }
}

data class PlacementToolData(val childId: ChildId, val preschoolId: DaycareId)
