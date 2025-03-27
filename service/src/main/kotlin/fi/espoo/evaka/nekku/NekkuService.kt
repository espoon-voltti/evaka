// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.nekku

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.NekkuEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.DaycareMealtimes
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NekkuService(
    env: NekkuEnv?,
    jsonMapper: JsonMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    private val client = env?.let { NekkuHttpClient(it, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::syncNekkuCustomers)
        asyncJobRunner.registerHandler(::syncNekkuSpecialDiets)
        asyncJobRunner.registerHandler(::syncNekkuProducts)
        asyncJobRunner.registerHandler(::sendNekkuOrder)
    }

    fun syncNekkuCustomers(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuCustomers,
    ) {
        if (client == null) error("Cannot sync Nekku customers: NekkuEnv is not configured")
        fetchAndUpdateNekkuCustomers(client, db)
    }

    fun planNekkuCustomersSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncNekkuCustomers::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncNekkuCustomers()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun syncNekkuSpecialDiets(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuSpecialDiets,
    ) {
        if (client == null) error("Cannot sync Nekku special diets: NekkuEnv is not configured")
        fetchAndUpdateNekkuSpecialDiets(client, db)
    }

    fun planNekkuSpecialDietsSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncNekkuSpecialDiets::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncNekkuSpecialDiets()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun syncNekkuProducts(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuProducts,
    ) {
        if (client == null) error("Cannot sync Nekku products: NekkuEnv is not configured")
        fetchAndUpdateNekkuProducts(client, db)
    }

    fun planNekkuProductsSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncNekkuProducts::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncNekkuProducts()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun planNekkuOrders(dbc: Database.Connection, clock: EvakaClock) {
        if (client == null) error("Cannot plan Nekku order: NekkuEnv is not configured")
        planNekkuOrderJobs(dbc, asyncJobRunner, client, clock.now())
    }

    fun planNekkuOrderJobs(
        dbc: Database.Connection,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        client: NekkuClient,
        now: HelsinkiDateTime,
    ) {
        val range = now.toLocalDate().startOfNextWeek().twoWeekSpan()
        dbc.transaction { tx ->
            val customerGroupsAndNumbers = tx.getNekkuDaycareGroupIdCustomerNumberMapping(range)
            asyncJobRunner.plan(
                tx,
                range.dates().flatMap { date ->
                    customerGroupsAndNumbers.map { customerGroupsAndNumber ->
                        AsyncJob.SendNekkuOrder(
                            customerGroupId = customerGroupsAndNumber.first,
                            customerNumber = customerGroupsAndNumber.second,
                            date = date,
                        )
                    }
                },
                runAt = now,
                retryInterval = Duration.ofHours(1),
                retryCount = 3,
            )
        }
    }

    fun sendNekkuOrder(dbc: Database.Connection, clock: EvakaClock, job: AsyncJob.SendNekkuOrder) {
        if (client == null) error("Cannot send Nekku order: NekkuEnv is not configured")
        try {
            createAndSendNekkuOrder(
                client,
                dbc,
                customerNumber = job.customerNumber,
                groupId = job.customerGroupId,
                date = job.date,
            )
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to send meal order to Nekku: date=${job.date}, customerNumber=${job.customerNumber}, customerId=${job.customerId}, error=${e.localizedMessage}"
            }
            throw e
        }
    }
}

interface NekkuClient {

    data class NekkuOrder(
        val delivery_date: String,
        val customer_id: String,
        val group_id: String,
        val items: List<Item>,
        val description: String,
    )

    data class Item(
        val product_sku: String,
        val quantity: Int,
        val product_options: List<ProductOption>,
    )

    data class ProductOption(val field_id: String, val value: String)

    data class NekkuOrders(val orders: List<NekkuOrder>, val dry_run: Boolean)

    fun getCustomers(): List<NekkuCustomer>

    fun getSpecialDiets(): List<NekkuApiSpecialDiet>

    fun getProducts(): List<NekkuApiProduct>

    fun createNekkuMealOrder(nekkuOrders: NekkuOrders)
}

class NekkuHttpClient(private val env: NekkuEnv, private val jsonMapper: JsonMapper) : NekkuClient {
    val client = OkHttpClient()

    override fun getCustomers(): List<NekkuCustomer> {
        val request = getBaseRequest().get().url(env.url.resolve("customers").toString()).build()

        return executeRequest(request)
    }

    override fun getSpecialDiets(): List<NekkuApiSpecialDiet> {
        val request =
            getBaseRequest().get().url(env.url.resolve("products/options").toString()).build()

        return executeRequest(request)
    }

    override fun getProducts(): List<NekkuApiProduct> {
        val request = getBaseRequest().get().url(env.url.resolve("products").toString()).build()

        return executeRequest(request)
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders) {
        // Todo: create post with orders
    }

    private fun getBaseRequest() =
        Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("X-Api-Key", env.apikey.value)

    private inline fun <reified T> executeRequest(request: Request): T {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMessage =
                    try {
                        response.body?.string() ?: "No error body"
                    } catch (e: Exception) {
                        "Failed to read error body: ${e.message}"
                    }
                logger.error {
                    "Nekku API request failed with code ${response.code}: $errorMessage"
                }
                throw IOException("Nekku API request failed with status ${response.code}")
            }

            val body = response.body
            if (body == null) {
                logger.error { "Nekku API returned null body with status code ${response.code}" }
                throw IOException("Response body was null with status code ${response.code}")
            }

            return try {
                jsonMapper.readValue<T>(body.string())
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse Nekku API response" }
                throw IOException("Failed to parse Nekku API response", e)
            }
        }
    }
}

private fun LocalDate.startOfNextWeek(): LocalDate {
    val daysUntilNextMonday = (8 - this.dayOfWeek.value) % 7
    return this.plusDays(daysUntilNextMonday.toLong())
}

private fun LocalDate.twoWeekSpan(): FiniteDateRange {
    val start = this.startOfNextWeek()
    val end = start.plusDays(13)
    return FiniteDateRange(start, end)
}

private fun createAndSendNekkuOrder(
    client: NekkuClient,
    dbc: Database.Connection,
    customerNumber: String,
    groupId: GroupId,
    date: LocalDate,
) {
    val (preschoolTerms, children) =
        dbc.read { tx ->
            val preschoolTerms = tx.getPreschoolTerms()
            val children = getNekkuChildInfos(tx, groupId, date)
            preschoolTerms to children
        }
    val orders = null // Todo mapping

    if (orders.isNotEmpty()) {
        client.createNekkuMealOrder(orders)
        logger.info {
            "Sent Nekku order for date $date for customerNumber=$customerNumber customerId=$customerId"
        }
    } else {
        logger.info {
            "Skipped Nekku order with no rows for date $date for customerNumber=$customerNumber customerId=$customerId"
        }
    }
}

private fun getNekkuChildInfos(
    tx: Database.Read,
    nekkuGroupId: GroupId,
    date: LocalDate,
): List<NekkuChildInfo> {
    val holidays = getHolidays(FiniteDateRange(date, date))

    val childData = tx.getNekkuChildData(nekkuGroupId, date)
    val unitIds = childData.map { it.unitId }.toSet()
    val childIds = childData.map { it.childId }.toSet()

    val specialDiets = tx.mealTypesForChildren(childIds)
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

        NekkuChildInfo(
            placementType = child.placementType,
            firstName = child.firstName,
            lastName = child.lastName,
            reservations = child.reservations,
            absences = child.absences,
            dietInfo = specialDiets[child.childId], // replace
            specialDiet = null, // replace
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            mealTimes = unit.mealTimes,
        )
    }
}

data class NekkuCustomer(
    val number: String,
    val name: String,
    val group: String,
    val unit_size: String,
)

data class NekkuUnitNumber(val number: String, val name: String)

data class NekkuApiSpecialDiet(
    val id: String,
    val name: String,
    val fields: List<NekkuApiSpecialDietsField>,
) {
    fun toEvaka(): NekkuSpecialDiet = NekkuSpecialDiet(id, name, fields.map { it.toEvaka() })
}

data class NekkuSpecialDiet(
    val id: String,
    val name: String,
    val fields: List<NekkuSpecialDietsField>,
)

data class NekkuApiSpecialDietsField(
    val id: String,
    val name: String,
    val type: NekkuApiSpecialDietType,
    val options: List<NekkuSpecialDietOption>? = null,
) {
    fun toEvaka(): NekkuSpecialDietsField =
        NekkuSpecialDietsField(id, name, type.toEvaka(), options)
}

data class NekkuSpecialDietsField(
    val id: String,
    val name: String,
    val type: NekkuSpecialDietType,
    val options: List<NekkuSpecialDietOption>? = null,
)

@ConstList("nekku_special_diet_type")
enum class NekkuSpecialDietType : DatabaseEnum {
    TEXT,
    CHECKBOXLIST;

    override val sqlType: String = "nekku_special_diet_type"
}

enum class NekkuApiSpecialDietType(@JsonValue val jsonValue: String) {
    Text("text") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.TEXT
    },
    CheckBoxLst("checkboxlst") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.CHECKBOXLIST
    };

    abstract fun toEvaka(): NekkuSpecialDietType
}

data class NekkuSpecialDietOption(val weight: Int, val key: String, val value: String)

data class NekkuApiProduct(
    val name: String,
    val sku: String,
    val options_id: String,
    val unit_size: String,
    val meal_time: List<NekkuProductMealTime>? = null,
    val meal_type: NekkuApiProductMealType? = null,
) {
    fun toEvaka(): NekkuProduct =
        NekkuProduct(name, sku, options_id, unit_size, meal_time, meal_type?.toEvaka())
}

data class NekkuProduct(
    val name: String,
    val sku: String,
    val options_id: String,
    val unit_size: String,
    val meal_time: List<NekkuProductMealTime>? = null,
    val meal_type: NekkuProductMealType? = null,
)

data class NekkuMealType(val type: NekkuProductMealType?, val name: String)

@ConstList("nekku_product_meal_time")
enum class NekkuProductMealTime(@JsonValue val description: String) : DatabaseEnum {
    BREAKFAST("aamupala"),
    LUNCH("lounas"),
    SNACK("välipala"),
    DINNER("päivällinen"),
    SUPPER("iltapala");

    override val sqlType: String = "nekku_product_meal_time"
}

@ConstList("nekku_product_meal_type")
enum class NekkuApiProductMealType(@JsonValue val description: String) {
    Vegaani("vegaani") {
        override fun toEvaka(): NekkuProductMealType = NekkuProductMealType.VEGAN
    },
    Kasvis("kasvis") {
        override fun toEvaka(): NekkuProductMealType = NekkuProductMealType.VEGETABLE
    };

    abstract fun toEvaka(): NekkuProductMealType
}

@ConstList("nekku_product_meal_type")
enum class NekkuProductMealType(val description: String) : DatabaseEnum {
    VEGAN("Vegaani"),
    VEGETABLE("Kasvis");

    override val sqlType: String = "nekku_product_meal_type"
}

data class NekkuChildInfo(
    val placementType: PlacementType,
    val firstName: String,
    val lastName: String,
    val reservations: List<TimeRange>?,
    val absences: Set<AbsenceCategory>?,
    val dietInfo: String?, // check proper type
    val specialDiet: String?, // check proper type
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val mealTimes: DaycareMealtimes,
)
