// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.JamixEnv
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.reports.MealReportChildInfo
import fi.espoo.evaka.reports.mealReportData
import fi.espoo.evaka.reports.specialDietsForChildren
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.getHolidays
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
        val now = clock.now()
        val range = now.toLocalDate().startOfNextWeek().weekSpan()
        dbc.transaction { tx ->
            val customerIds = tx.getJamixCustomerIds()
            asyncJobRunner.plan(
                tx,
                range.dates().flatMap { date ->
                    customerIds.map { customerId -> AsyncJob.SendJamixOrder(customerId, date) }
                },
                runAt = now,
                retryInterval = Duration.ofHours(1),
                retryCount = 3
            )
        }
    }

    fun sendOrder(dbc: Database.Connection, clock: EvakaClock, job: AsyncJob.SendJamixOrder) {
        if (client == null) error("Cannot send Jamix order: JamixEnv is not configured")
        try {
            createAndSendJamixOrder(client, dbc, mealTypeMapper, job.customerId, job.date)
            logger.info { "Sent Jamix order for date ${job.date} for customer ${job.customerId}" }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to send meal order to Jamix: customerId=${job.customerId}, date=${job.date}, error=${e.localizedMessage}"
            }
            throw e
        }
    }
}

fun createAndSendJamixOrder(
    client: JamixClient,
    dbc: Database.Connection,
    mealTypeMapper: MealTypeMapper,
    customerId: Int,
    date: LocalDate
) {
    val (preschoolTerms, children) =
        dbc.read { tx ->
            val preschoolTerms = tx.getPreschoolTerms()
            val children = getChildInfos(tx, customerId, date)
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
    }
}

private fun getChildInfos(
    tx: Database.Read,
    jamixCustomerId: Int,
    date: LocalDate
): List<MealReportChildInfo> {
    val holidays = tx.getHolidays(FiniteDateRange(date, date))

    val childData = tx.getJamixChildData(jamixCustomerId, date)
    val unitIds = childData.map { it.unitId }.toSet()
    val childIds = childData.map { it.childId }.toSet()

    val specialDiets = tx.specialDietsForChildren(childIds)
    val units = tx.getDaycaresById(unitIds)

    return childData.mapNotNull { child ->
        val unit = units[child.unitId] ?: error("Daycare not found for unitId ${child.unitId}")

        if (!unit.operationDays.contains(date.dayOfWeek.value)) return@mapNotNull null

        val isRoundTheClockUnit = unit.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)
        if (!isRoundTheClockUnit && holidays.contains(date)) {
            return@mapNotNull null
        }

        MealReportChildInfo(
            placementType = child.placementType,
            firstName = child.firstName,
            lastName = child.lastName,
            reservations = child.reservations,
            absences = child.absences,
            dietInfo = specialDiets[child.childId],
            daycare = unit
        )
    }
}

interface JamixClient {
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
}

class JamixHttpClient(
    private val env: JamixEnv,
    private val fuel: FuelManager,
    private val jsonMapper: JsonMapper
) : JamixClient {
    override fun createMealOrder(order: JamixClient.MealOrder) {
        val url = env.url.resolve("v2/mealorders").toString()
        val (request, response, result) =
            fuel
                .post(url)
                .authentication()
                .basic(env.user, env.password.value)
                .jsonBody(jsonMapper.writeValueAsString(order))
                .response()
        if (result is Result.Failure) {
            error(
                "Failed to send meal order to Jamix: ${request.method} ${request.url}, status=${response.statusCode} error=${result.error.errorData.decodeToString()}"
            )
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
