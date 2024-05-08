// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.JamixEnv
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.reports.MealReportChildInfo
import fi.espoo.evaka.reports.mealReportData
import fi.espoo.evaka.reports.specialDietsForChildren
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.specialdiet.JamixSpecialDiet
import fi.espoo.evaka.specialdiet.SpecialDiet
import fi.espoo.evaka.specialdiet.resetSpecialDietsNotContainedWithin
import fi.espoo.evaka.specialdiet.setSpecialDiets
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class JamixService(
    env: JamixEnv?,
    fuel: FuelManager,
    jsonMapper: JsonMapper,
    private val mealTypeMapper: MealTypeMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val client = env?.let { JamixHttpClient(it, fuel, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::sendOrder)
    }

    fun planOrders(dbc: Database.Connection, clock: EvakaClock) {
        if (client == null) error("Cannot plan Jamix order: JamixEnv is not configured")
        planJamixOrderJobs(dbc, asyncJobRunner, client, clock.now())
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
                date = job.date
            )
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to send meal order to Jamix: date=${job.date}, customerNumber=${job.customerNumber}, customerId=${job.customerId}, error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun syncDiets(db: Database.Connection, clock: EvakaClock) {
        if (client == null) error("Cannot sync diet list: JamixEnv is not configured")
        fetchAndUpdateJamixDiets(client, db)
    }
}

fun fetchAndUpdateJamixDiets(
    client: JamixClient,
    db: Database.Connection,
    warner: (s: String) -> Unit = { s -> logger.warn(s) }
) {
    val dietsFromJamix = client.getDiets()

    val cleanedDietList = cleanupJamixDietList(dietsFromJamix)
    logger.info(
        "Jamix returned ${dietsFromJamix.size} cleaned list contains: ${cleanedDietList.size} diets"
    )
    if (cleanedDietList.isEmpty()) error("Refusing to sync empty diet list into database")
    db.transaction { tx ->
        val nulledChildrenCount = tx.resetSpecialDietsNotContainedWithin(cleanedDietList)
        if (nulledChildrenCount != 0)
            warner(
                "Jamix diet list update caused $nulledChildrenCount child special diets to be set to null"
            )
        val deletedDietsCount = tx.setSpecialDiets(cleanedDietList)
        logger.info("Deleted: $deletedDietsCount diets, inserted ${cleanedDietList.size}")
    }
}

fun planJamixOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    client: JamixClient,
    now: HelsinkiDateTime
) {
    val range = now.toLocalDate().startOfNextWeek().weekSpan()

    logger.info { "Getting Jamix customers" }
    val customers = client.getCustomers()
    val customerMapping = customers.associateBy({ it.customerNumber }, { it.customerId })

    dbc.transaction { tx ->
        val customerNumbers = tx.getJamixCustomerNumbers()
        logger.info { "Planning Jamix orders for ${customerNumbers.size} customers" }
        asyncJobRunner.plan(
            tx,
            range.dates().flatMap { date ->
                customerNumbers.mapNotNull { customerNumber ->
                    val customerId = customerMapping[customerNumber]
                    if (customerId == null) {
                        logger.error {
                            "Jamix customerId not found for customerNumber $customerNumber"
                        }
                        null
                    } else {
                        AsyncJob.SendJamixOrder(
                            customerNumber = customerNumber,
                            customerId = customerId,
                            date = date
                        )
                    }
                }
            },
            runAt = now,
            retryInterval = Duration.ofHours(1),
            retryCount = 3
        )
    }
}

fun createAndSendJamixOrder(
    client: JamixClient,
    dbc: Database.Connection,
    mealTypeMapper: MealTypeMapper,
    customerNumber: Int,
    customerId: Int,
    date: LocalDate
) {
    val (preschoolTerms, children) =
        dbc.read { tx ->
            val preschoolTerms = tx.getPreschoolTerms()
            val children = getChildInfos(tx, customerNumber, date)
            preschoolTerms to children
        }
    val order =
        JamixClient.MealOrder(
            customerID = customerId,
            deliveryDate = date,
            mealOrderRows =
                mealReportData(children, date, preschoolTerms, mealTypeMapper).map {
                    JamixClient.MealOrderRow(
                        orderAmount = it.mealCount,
                        mealTypeID = it.mealId,
                        dietID = it.dietId,
                        additionalInfo = it.additionalInfo
                    )
                }
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
    date: LocalDate
): List<MealReportChildInfo> {
    val holidays = tx.getHolidays(FiniteDateRange(date, date))

    val childData = tx.getJamixChildData(jamixCustomerNumber, date)
    val unitIds = childData.map { it.unitId }.toSet()
    val childIds = childData.map { it.childId }.toSet()

    val specialDiets = tx.specialDietsForChildren(childIds)
    val units = tx.getDaycaresById(unitIds)

    return childData.mapNotNull { child ->
        val unit = units[child.unitId] ?: error("Daycare not found for unitId ${child.unitId}")
        if (!isUnitOperationDay(unit.operationDays, holidays, date)) return@mapNotNull null

        MealReportChildInfo(
            placementType = child.placementType,
            firstName = child.firstName,
            lastName = child.lastName,
            reservations = child.reservations,
            absences = child.absences,
            dietInfo = specialDiets[child.childId],
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            mealTimes = unit.mealTimes
        )
    }
}

interface JamixClient {
    data class Customer(val customerId: Int, val customerNumber: Int)

    fun getCustomers(): List<Customer>

    data class MealOrder(
        val customerID: Int,
        val deliveryDate: LocalDate,
        val mealOrderRows: List<MealOrderRow>
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class MealOrderRow(
        val orderAmount: Int,
        val mealTypeID: Int,
        val dietID: Int?,
        val additionalInfo: String?
    )

    fun createMealOrder(order: MealOrder)

    fun getDiets(): List<JamixSpecialDiet>
}

class JamixHttpClient(
    private val env: JamixEnv,
    private val fuel: FuelManager,
    private val jsonMapper: JsonMapper
) : JamixClient {
    override fun getCustomers(): List<JamixClient.Customer> =
        request(Method.GET, env.url.resolve("customers"))

    override fun createMealOrder(order: JamixClient.MealOrder): Unit =
        request(Method.POST, env.url.resolve("v2/mealorders"), order)

    override fun getDiets(): List<JamixSpecialDiet> = request(Method.GET, env.url.resolve("diets"))

    private inline fun <reified R> request(method: Method, url: URI, body: Any? = null): R {
        val (request, response, result) =
            fuel
                .request(method, url.toString())
                .authentication()
                .basic(env.user, env.password.value)
                .let { if (body != null) it.jsonBody(jsonMapper.writeValueAsString(body)) else it }
                .responseString()
        return when (result) {
            is Result.Success -> {
                if (Unit is R) {
                    Unit
                } else {
                    jsonMapper.readValue(result.get())
                }
            }
            is Result.Failure -> {
                error(
                    "failed to request ${request.method} ${request.url}: status=${response.statusCode} error=${result.error.errorData.decodeToString()}"
                )
            }
        }
    }
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
    return specialDietList
        .map {
            SpecialDiet(
                it.modelId,
                cleanupJamixDietNameString(it.fields.dietName),
                cleanupJamixDietAbbreviationString(it.fields.dietAbbreviation)
            )
        }
        .filterNot { it.name.contains("POISTA") }
        .filterNot { it.name.isEmpty() && it.abbreviation.isEmpty() }
}

fun cleanupJamixDietNameString(s: String): String {
    return s.replace("tsekattava", "", true)
        .replace("tsek", "", true)
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun cleanupJamixDietAbbreviationString(s: String): String {
    return s.replace("ätarkasta", "", true).replace(Regex("\\s+"), " ").trim()
}
