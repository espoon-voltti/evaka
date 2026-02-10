// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import com.fasterxml.jackson.annotation.JsonInclude
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.JamixEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerm
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.EmailContent
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.reports.MealReportChildInfo
import fi.espoo.evaka.reports.mealReportData
import fi.espoo.evaka.reports.mealTexturesForChildren
import fi.espoo.evaka.reports.specialDietsForChildren
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.TimeoutConfig
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.buildHttpClient
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.utils.basicAuthInterceptor
import fi.espoo.evaka.shared.utils.executeGetRequest
import fi.espoo.evaka.shared.utils.executePostJsonRequest
import fi.espoo.evaka.specialdiet.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.LocalDate
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

@Service
class JamixService(
    env: JamixEnv?,
    jsonMapper: JsonMapper,
    private val mealTypeMapper: MealTypeMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailEnv: EmailEnv,
    private val emailClient: EmailClient,
) {
    private val client = env?.let { JamixClient(it, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::sendOrder)
        asyncJobRunner.registerHandler(::syncDiets)
        asyncJobRunner.registerHandler(::sendSpecialDietNullificationWarningEmail)
    }

    fun planOrders(dbc: Database.Connection, clock: EvakaClock) {
        if (client == null) error("Cannot plan Jamix order: JamixEnv is not configured")
        planJamixOrderJobs(dbc, asyncJobRunner, client, clock.now())
    }

    fun planOrdersForUnitAndDate(
        dbc: Database.Connection,
        clock: EvakaClock,
        unitId: DaycareId,
        date: LocalDate,
    ) {
        if (client == null) error("Cannot plan Jamix order: JamixEnv is not configured")
        planJamixOrderJobsForUnitAndDate(dbc, asyncJobRunner, client, clock.now(), unitId, date)
    }

    fun sendOrder(dbc: Database.Connection, clock: EvakaClock, job: AsyncJob.SendJamixOrder) {
        if (client == null) error("Cannot send Jamix order: JamixEnv is not configured")
        try {
            createAndSendJamixOrder(
                client,
                dbc,
                mealTypeMapper,
                customerNumber = job.customerNumber,
                customerId = job.customerId,
                date = job.date,
            )
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to send meal order to Jamix: date=${job.date}, customerNumber=${job.customerNumber}, customerId=${job.customerId}, error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun planDietSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncJamixDiets::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncJamixDiets()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun syncDiets(db: Database.Connection, clock: EvakaClock, job: AsyncJob.SyncJamixDiets) {
        if (client == null) error("Cannot sync diet list: JamixEnv is not configured")
        fetchAndUpdateJamixDiets(client, db, asyncJobRunner, clock.now())
        fetchAndUpdateJamixTextures(client, db)
    }

    fun sendSpecialDietNullificationWarningEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendSpecialDietNullificationWarningEmail,
    ) {
        val content =
            EmailContent.fromHtml(
                subject = "Muutoksia Jamix erityisruokavalioihin",
                html =
                    "<p>Seuraavien lasten erityisruokavaliot on poistettu johtuen erityisruokavalioiden poistumisesta Jamixista:</p>\n" +
                        job.diets
                            .sortedWith(compareBy({ it.second.abbreviation }, { it.first }))
                            .joinToString("\n") { (childId, diet) ->
                                "<p>- Lapsen tunniste: '$childId', Alkuperäinen erityisruokavalio: '${diet.abbreviation}' ERV tunniste: ${diet.id}</p>"
                            },
            )

        Email.createForEmployee(
                dbc,
                job.employeeId,
                content = content,
                traceId = "${job.unitId}:${job.employeeId}",
                emailEnv.sender(Language.fi),
            )
            ?.let { emailClient.send(it) }
    }
}

/** Throws an IllegalStateException if Jamix returns an empty diet list. */
fun fetchAndUpdateJamixDiets(
    client: JamixClient,
    db: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    val dietsFromJamix = client.getDiets()

    val cleanedDietList = cleanupJamixDietList(dietsFromJamix)
    logger.info {
        "Jamix returned ${dietsFromJamix.size} cleaned list contains: ${cleanedDietList.size} diets"
    }
    if (cleanedDietList.isEmpty()) error("Refusing to sync empty diet list into database")
    return db.transaction { tx ->
        val nulledSpecialDiets =
            tx.resetSpecialDietsNotContainedWithin(now.toLocalDate(), cleanedDietList)
        val deletedDietsCount = tx.setSpecialDiets(cleanedDietList)
        logger.info { "Deleted: $deletedDietsCount diets, inserted ${cleanedDietList.size}" }

        if (nulledSpecialDiets.isNotEmpty()) {
            val byUnit = nulledSpecialDiets.groupBy({ it.unitId }, { it.childId to it.specialDiet })
            asyncJobRunner.plan(
                tx,
                byUnit.flatMap { (unitId, nulled) ->
                    val supervisors =
                        tx.getDaycareAclRows(
                                daycareId = unitId,
                                includeStaffOccupancy = false,
                                includeStaffEmployeeNumber = false,
                                role = UserRole.UNIT_SUPERVISOR,
                            )
                            .map { it.employee }
                            .sortedBy { it.id }
                    supervisors.map { supervisor ->
                        AsyncJob.SendSpecialDietNullificationWarningEmail(
                            unitId,
                            supervisor.id,
                            nulled,
                        )
                    }
                },
                runAt = now,
            )
        }
    }
}

/** Throws an IllegalStateException if Jamix returns an empty texture list. */
fun fetchAndUpdateJamixTextures(client: JamixClient, db: Database.Connection) {
    val texturesFromJamix =
        client.getTextures().map { MealTexture(it.modelId, it.fields.textureName) }

    if (texturesFromJamix.isEmpty())
        error("Refusing to sync empty meal textures list into database")
    db.transaction { tx ->
        val nulledChildrenCount = tx.resetMealTexturesNotContainedWithin(texturesFromJamix)
        if (nulledChildrenCount != 0)
            logger.warn {
                "Jamix meal texture list update caused $nulledChildrenCount child meal texture to be set to null"
            }
        val deletedMealTexturesCount = tx.setMealTextures(texturesFromJamix)
        logger.info {
            "Deleted: $deletedMealTexturesCount meal textures, inserted ${texturesFromJamix.size}"
        }
    }
}

fun planJamixOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    client: JamixClient,
    now: HelsinkiDateTime,
) {
    val range = now.toLocalDate().startOfNextWeek().weekSpan()
    val customerMapping = getCustomerMapping(client)
    dbc.transaction { tx ->
        val customerNumbers = tx.getJamixCustomerNumbers(range)
        planJamixOrderJobs(tx, asyncJobRunner, now, range, customerNumbers, customerMapping)
    }
}

fun planJamixOrderJobsForUnitAndDate(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    client: JamixClient,
    now: HelsinkiDateTime,
    unitId: DaycareId,
    date: LocalDate,
) {
    if (date.isBefore(now.toLocalDate().plusDays(2))) {
        throw IllegalArgumentException("Cannot send orders anymore for date $date")
    }
    val range = FiniteDateRange(date, date)
    val customerMapping = getCustomerMapping(client)
    dbc.transaction { tx ->
        val customerNumbers =
            tx.getDaycareGroups(unitId, range.start, range.end)
                .mapNotNull { it.jamixCustomerNumber }
                .toSet()
        planJamixOrderJobs(tx, asyncJobRunner, now, range, customerNumbers, customerMapping)
    }
}

private fun getCustomerMapping(client: JamixClient): Map<Int, Int> {
    logger.info { "Getting Jamix customers" }
    val customers = client.getCustomers()
    val customerMapping = customers.associateBy({ it.customerNumber }, { it.customerId })
    return customerMapping
}

private fun planJamixOrderJobs(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    range: FiniteDateRange,
    customerNumbers: Set<Int>,
    customerMapping: Map<Int, Int>,
) {
    logger.info { "Planning Jamix orders for ${customerNumbers.size} customers" }
    asyncJobRunner.plan(
        tx,
        range.dates().flatMap { date ->
            customerNumbers.mapNotNull { customerNumber ->
                val customerId = customerMapping[customerNumber]
                if (customerId == null) {
                    logger.error { "Jamix customerId not found for customerNumber $customerNumber" }
                    null
                } else {
                    AsyncJob.SendJamixOrder(
                        customerNumber = customerNumber,
                        customerId = customerId,
                        date = date,
                    )
                }
            }
        },
        runAt = now,
        retryInterval = Duration.ofHours(1),
        retryCount = 3,
    )
}

fun createAndSendJamixOrder(
    client: JamixClient,
    dbc: Database.Connection,
    mealTypeMapper: MealTypeMapper,
    customerNumber: Int,
    customerId: Int,
    date: LocalDate,
) {
    val (preschoolTerm, children) =
        dbc.read { tx ->
            val preschoolTerm = tx.getPreschoolTerm(date)
            val children = getChildInfos(tx, customerNumber, date)
            preschoolTerm to children
        }
    val order =
        JamixClient.MealOrder(
            customerID = customerId,
            deliveryDate = date,
            mealOrderRows =
                mealReportData(children, date, preschoolTerm, mealTypeMapper).map {
                    JamixClient.MealOrderRow(
                        orderAmount = it.mealCount,
                        mealTypeID = it.mealId,
                        dietID = it.dietId,
                        additionalInfo = it.additionalInfo,
                        textureID = it.mealTextureId,
                    )
                },
        )

    if (order.mealOrderRows.isNotEmpty()) {
        client.createMealOrder(order)
        logger.info {
            "Sent Jamix order for date $date for customerNumber=$customerNumber customerId=$customerId"
        }
    } else {
        logger.info {
            "Skipped Jamix order with no rows for date $date for customerNumber=$customerNumber customerId=$customerId"
        }
    }
}

private fun getChildInfos(
    tx: Database.Read,
    jamixCustomerNumber: Int,
    date: LocalDate,
): List<MealReportChildInfo> {
    val holidays = getHolidays(FiniteDateRange(date, date))

    val childData = tx.getJamixChildData(jamixCustomerNumber, date)
    val unitIds = childData.map { it.unitId }.toSet()
    val childIds = childData.map { it.childId }.toSet()

    val specialDiets = tx.specialDietsForChildren(childIds)
    val mealTextures = tx.mealTexturesForChildren(childIds)
    val units = tx.getDaycaresById(unitIds)

    return childData.mapNotNull { child ->
        val unit = units[child.unitId] ?: error("Daycare not found for unitId ${child.unitId}")
        if (
            !isUnitOperationDay(
                normalOperationDays = unit.operationDays,
                shiftCareOperationDays = unit.shiftCareOperationDays,
                shiftCareOpenOnHolidays = unit.shiftCareOpenOnHolidays,
                holidays = holidays,
                date = date,
                childHasShiftCare = child.hasShiftCare,
            )
        )
            return@mapNotNull null

        MealReportChildInfo(
            placementType = child.placementType,
            firstName = child.firstName,
            lastName = child.lastName,
            reservations = child.reservations,
            absences = child.absences,
            dietInfo = specialDiets[child.childId],
            mealTextureInfo = mealTextures[child.childId],
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            mealTimes = unit.mealTimes,
        )
    }
}

class JamixClient(env: JamixEnv, private val jsonMapper: JsonMapper) {
    data class Customer(val customerId: Int, val customerNumber: Int)

    data class MealOrder(
        val customerID: Int,
        val deliveryDate: LocalDate,
        val mealOrderRows: List<MealOrderRow>,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class MealOrderRow(
        val orderAmount: Int,
        val mealTypeID: Int,
        val dietID: Int?,
        val additionalInfo: String?,
        val textureID: Int?,
    )

    private val httpClient =
        buildHttpClient(
            rootUrl = env.url,
            jsonMapper = jsonMapper,
            interceptors = listOf(basicAuthInterceptor(env.user, env.password.value)),
            timeouts =
                TimeoutConfig(
                    connectTimeout = Duration.ofSeconds(120),
                    readTimeout = Duration.ofSeconds(120),
                    writeTimeout = Duration.ofSeconds(120),
                ),
        )

    fun getCustomers(): List<Customer> = httpClient.executeGetRequest("customers")

    fun createMealOrder(order: MealOrder) {
        httpClient.executePostJsonRequest("v2/mealorders", order)
    }

    fun getDiets(): List<JamixSpecialDiet> = httpClient.executeGetRequest("diets")

    fun getTextures(): List<JamixTexture> = httpClient.executeGetRequest("textures")
}

private fun LocalDate.startOfNextWeek(): LocalDate {
    val daysUntilNextMonday = (8 - this.dayOfWeek.value) % 7
    return this.plusDays(daysUntilNextMonday.toLong())
}

private fun LocalDate.weekSpan(): FiniteDateRange {
    val start = this.startOfNextWeek()
    val end = start.plusDays(6)
    return FiniteDateRange(start, end)
}

fun cleanupJamixDietList(specialDietList: List<JamixSpecialDiet>): List<SpecialDiet> {
    return specialDietList.map {
        SpecialDiet(it.modelId, cleanupJamixString(it.fields.dietAbbreviation))
    }
}

fun cleanupJamixString(s: String): String {
    return s.replace(Regex("\\s+"), " ").trim()
}

data class JamixSpecialDiet(val modelId: Int, val fields: JamixSpecialDietFields)

data class JamixSpecialDietFields(val dietName: String, val dietAbbreviation: String)

data class JamixTexture(val modelId: Int, val fields: JamixTextureFields)

data class JamixTextureFields(val textureName: String)
