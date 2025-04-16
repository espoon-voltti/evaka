// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.nekku

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.NekkuEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.DaycareMealtimes
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.FeatureConfig
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
import kotlin.math.roundToInt
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NekkuService(
    env: NekkuEnv?,
    jsonMapper: JsonMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val featureConfig: FeatureConfig,
) {
    private val client = env?.let { NekkuHttpClient(it, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::syncNekkuCustomers)
        asyncJobRunner.registerHandler(::syncNekkuSpecialDiets)
        asyncJobRunner.registerHandler(::syncNekkuProducts)
        asyncJobRunner.registerHandler(::sendNekkuOrder)
        asyncJobRunner.registerHandler(::sendNekkuDailyOrder)
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
        planNekkuOrderJobs(dbc, asyncJobRunner, clock.now())
    }

    fun planNekkuDailyOrders(dbc: Database.Connection, clock: EvakaClock) {
        planNekkuDailyOrderJobs(dbc, asyncJobRunner, clock.now())
    }

    fun sendNekkuOrder(dbc: Database.Connection, clock: EvakaClock, job: AsyncJob.SendNekkuOrder) {
        if (client == null) error("Cannot send Nekku order: NekkuEnv is not configured")

        try {
            createAndSendNekkuOrder(
                client,
                dbc,
                groupId = job.customerGroupId,
                date = job.date,
                featureConfig.nekkuMealDeductionFactor,
            )
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to send meal order to Nekku: date=${job.date}, groupId=${job.customerGroupId},error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun sendNekkuDailyOrder(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendNekkuDailyOrder,
    ) {
        if (client == null) error("Cannot send Nekku order: NekkuEnv is not configured")
        try {
            createAndSendNekkuOrder(
                client,
                dbc,
                groupId = job.customerGroupId,
                date = job.date,
                featureConfig.nekkuMealDeductionFactor,
            )
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to send meal order to Nekku: date=${job.date}, groupId=${job.customerGroupId},error=${e.localizedMessage}"
            }
            throw e
        }
    }
}

interface NekkuClient {

    data class NekkuOrder(
        @JsonProperty("delivery_date") val deliveryDate: String,
        @JsonProperty("customer_number") val customerNumber: String,
        @JsonProperty("group_id") val groupId: String,
        val items: List<Item>,
        val description: String?,
    )

    data class Item(
        val sku: String,
        val quantity: Int,
        @JsonProperty("product_options") val productOptions: List<ProductOption>?,
    )

    data class ProductOption(@JsonProperty("field_id") val fieldId: String, val value: String)

    data class NekkuOrders(
        val orders: List<NekkuOrder>,
        @JsonProperty("dry_run") val dryRun: Boolean,
    )

    fun getCustomers(): List<NekkuCustomer>

    fun getSpecialDiets(): List<NekkuApiSpecialDiet>

    fun getProducts(): List<NekkuApiProduct>

    fun createNekkuMealOrder(nekkuOrders: NekkuOrders): NekkuOrderResult
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

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        val requestBody =
            jsonMapper
                .writeValueAsString(nekkuOrders)
                .toRequestBody("application/json".toMediaTypeOrNull())
        val request =
            getBaseRequest().post(requestBody).url(env.url.resolve("orders").toString()).build()

        return executeRequest(request)
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

fun planNekkuOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    val range = now.toLocalDate().startOfWeekAfterNextWeek().weekSpan()
    dbc.transaction { tx ->
        val nekkuGroupIds = tx.getNekkuDaycareGroupId(range)
        asyncJobRunner.plan(
            tx,
            range.dates().flatMap { date ->
                nekkuGroupIds.map { nekkuGroupId ->
                    AsyncJob.SendNekkuOrder(customerGroupId = nekkuGroupId, date = date)
                }
            },
            runAt = now,
            retryInterval = Duration.ofHours(1),
            retryCount = 3,
        )
    }
}

fun planNekkuDailyOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    val range = now.toLocalDate().daySpan()

    dbc.transaction { tx ->
        val nekkuDaycareGroupIds = tx.getNekkuDaycareGroupId(range)
        asyncJobRunner.plan(
            tx,
            nekkuDaycareGroupIds.map { nekkuDaycareGroupId ->
                AsyncJob.SendNekkuDailyOrder(
                    customerGroupId = nekkuDaycareGroupId,
                    date = now.plusDays(1).toLocalDate(),
                )
            },
            runAt = now,
            retryInterval = Duration.ofHours(1),
            retryCount = 3,
        )
    }
}

private fun LocalDate.startOfWeekAfterNextWeek(): LocalDate {
    val daysUntilNextWeekAfterMonday = (8 - this.dayOfWeek.value)
    return this.plusDays(daysUntilNextWeekAfterMonday.toLong())
}

private fun LocalDate.weekSpan(): FiniteDateRange {
    val start = this.startOfWeekAfterNextWeek()
    val end = start.plusDays(6)
    return FiniteDateRange(start, end)
}

private fun LocalDate.daySpan(): FiniteDateRange {
    val start = this
    val end = start.plusDays(1)
    return FiniteDateRange(start, end)
}

fun createAndSendNekkuOrder(
    client: NekkuClient,
    dbc: Database.Connection,
    groupId: GroupId,
    date: LocalDate,
    nekkuMealDeductionFactor: Double,
) {
    val (preschoolTerms, children) =
        dbc.read { tx ->
            val preschoolTerms = tx.getPreschoolTerms()
            val children = getNekkuChildInfos(tx, groupId, date)
            preschoolTerms to children
        }
    val nekkuDaycareCustomerMapping = dbc.read { tx -> tx.getNekkuDaycareCustomerMapping(groupId) }

    val nekkuProducts = dbc.read { tx -> tx.getNekkuProducts() }

    val order =
        NekkuClient.NekkuOrders(
            listOf(
                NekkuClient.NekkuOrder(
                    deliveryDate = date.toString(),
                    customerNumber = nekkuDaycareCustomerMapping.customerNumber,
                    groupId = groupId.toString(),
                    items =
                        nekkuMealReportData(
                            children,
                            date,
                            preschoolTerms,
                            nekkuProducts,
                            nekkuDaycareCustomerMapping.unitSize,
                            nekkuMealDeductionFactor,
                        ),
                    description = nekkuDaycareCustomerMapping.groupName,
                )
            ),
            dryRun = false,
        )

    if (order.orders.isNotEmpty()) {
        val nekkuOrderResult = client.createNekkuMealOrder(order)
        logger.info {
            "Sent Nekku order for date $date for customerNumber=${nekkuDaycareCustomerMapping.customerNumber} groupId=$groupId and Nekku orders created: ${nekkuOrderResult.created}"
        }
    } else {
        logger.info {
            "Skipped Nekku order with no rows for date $date for customerNumber=${nekkuDaycareCustomerMapping.customerNumber} groupId=$groupId"
        }
    }
}

fun nekkuMealReportData(
    children: Collection<NekkuChildInfo>,
    date: LocalDate,
    preschoolTerms: List<PreschoolTerm>,
    nekkuProducts: List<NekkuProduct>,
    unitSize: String,
    nekkuMealDeductionFactor: Double,
): List<NekkuClient.Item> {
    val mealInfoMap =
        children
            .flatMap { childInfo ->
                val absenceRecord =
                    childInfo.absences?.size == childInfo.placementType.absenceCategories().size

                val scheduleType =
                    childInfo.placementType.scheduleType(date, emptyList(), preschoolTerms)
                val effectivelyAbsent =
                    if (scheduleType == ScheduleType.TERM_BREAK) true else absenceRecord

                // list of time ranges when child will be present according to fixed schedule or
                // reservation times
                val presentTimeRanges =
                    if (scheduleType == ScheduleType.FIXED_SCHEDULE)
                        listOfNotNull(
                            childInfo.placementType.fixedScheduleRange(
                                childInfo.dailyPreschoolTime,
                                childInfo.dailyPreparatoryTime,
                            )
                        )
                    else childInfo.reservations ?: emptyList()

                nekkuChildMeals(
                        presentTimeRanges,
                        effectivelyAbsent,
                        childInfo.mealTimes,
                        childInfo.eatsBreakfast,
                    )
                    .map {
                        NekkuMealInfo(
                            sku = getNekkuProductNumber(nekkuProducts, it, childInfo, unitSize),
                            options = null, // getNekkuChildDiets() //Todo: get child diets
                            nekkuMealType = childInfo.mealType,
                        )
                    }
                    .distinct()
            }
            .groupBy { it }
            .mapValues { it.value.size }

    return mealInfoMap.map {
        NekkuClient.Item(
            sku = it.key.sku,
            quantity =
                if (it.key.nekkuMealType == null && it.key.options == null)
                    (it.value * nekkuMealDeductionFactor).roundToInt()
                else it.value,
            productOptions = it.key.options,
        )
    }
}

private fun getNekkuProductNumber(
    nekkuProducts: List<NekkuProduct>,
    nekkuProductMealTime: NekkuProductMealTime,
    nekkuChildInfo: NekkuChildInfo,
    unitSize: String,
): String {

    val filteredNekkuProducts =
        nekkuProducts.filter {
            it.mealTime?.contains(nekkuProductMealTime) ?: false &&
                it.mealType == nekkuChildInfo.mealType &&
                it.optionsId == nekkuChildInfo.optionsId &&
                it.unitSize == unitSize
        }

    if (filteredNekkuProducts.isEmpty()) {
        logger.info {
            "Cannot find any Nekku Product from database with unitsize=$unitSize optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        }
        error(
            "Cannot find any Nekku Product from database with unitsize=$unitSize optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        )
    } else if (filteredNekkuProducts.count() > 1) {
        logger.info {
            "Found too many Nekku Products from database with unitsize=$unitSize optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        }
        error(
            "Found too many Nekku Products from database with unitsize=$unitSize optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        )
    } else {
        return filteredNekkuProducts.first().sku
    }
}

private fun nekkuChildMeals(
    presentTimeRanges: List<TimeRange>,
    absent: Boolean,
    mealtimes: DaycareMealtimes,
    eatsBreakfast: Boolean,
): Set<NekkuProductMealTime> {
    // if absent -> no meals
    if (absent) {
        return emptySet()
    }
    // if we don't have data about when child will be present, default to breakfast + lunch + snack
    if (presentTimeRanges.isEmpty()) {

        return if (eatsBreakfast)
            setOf(
                NekkuProductMealTime.BREAKFAST,
                NekkuProductMealTime.LUNCH,
                NekkuProductMealTime.SNACK,
            )
        else setOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.SNACK)
    }
    // otherwise check unit meal times against the present time ranges
    val meals = mutableSetOf<NekkuProductMealTime>()

    fun addMealIfPresent(mealTime: TimeRange?, mealType: NekkuProductMealTime) {
        if (mealTime != null && presentTimeRanges.any { it.overlaps(mealTime) }) {
            meals.add(mealType)
        }
    }

    if (eatsBreakfast) addMealIfPresent(mealtimes.breakfast, NekkuProductMealTime.BREAKFAST)
    addMealIfPresent(mealtimes.lunch, NekkuProductMealTime.LUNCH)
    addMealIfPresent(mealtimes.snack, NekkuProductMealTime.SNACK)
    addMealIfPresent(mealtimes.supper, NekkuProductMealTime.DINNER)
    addMealIfPresent(mealtimes.eveningSnack, NekkuProductMealTime.SUPPER)

    return meals
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

    val mealTypes = tx.mealTypesForChildren(childIds)
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
            reservations = child.reservations,
            absences = child.absences,
            mealType = mealTypes[child.childId],
            optionsId = "", // Todo: generate value here when ready
            specialDiet = null, // Todo: generate value here when ready
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            mealTimes = unit.mealTimes,
            eatsBreakfast = child.eatsBreakfast,
        )
    }
}

data class NekkuCustomer(
    val number: String,
    val name: String,
    val group: String,
    @JsonProperty("unit_size") val unitSize: String,
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

data class NekkuSpecialDietWithoutFields(val id: String, val name: String)

data class NekkuSpecialDietsField(
    val id: String,
    val name: String,
    val type: NekkuSpecialDietType,
    val options: List<NekkuSpecialDietOption>? = null,
)

data class NekkuSpecialDietsFieldWithoutOptions(
    val id: String,
    val name: String,
    val type: NekkuSpecialDietType,
    val diet_id: String,
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
    @JsonProperty("options_id") val optionsId: String,
    @JsonProperty("unit_size") val unitSize: String,
    @JsonProperty("meal_time") val mealTime: List<NekkuProductMealTime>? = null,
    @JsonProperty("meal_type") val mealType: NekkuApiProductMealType? = null,
) {
    fun toEvaka(): NekkuProduct =
        NekkuProduct(name, sku, optionsId, unitSize, mealTime, mealType?.toEvaka())
}

data class NekkuSpecialDietOptionWithFieldId(
    val weight: Int,
    val key: String,
    val value: String,
    val fieldId: String,
)

data class NekkuProduct(
    val name: String,
    val sku: String,
    @JsonProperty("options_id") val optionsId: String,
    @JsonProperty("unit_size") val unitSize: String,
    @JsonProperty("meal_time") val mealTime: List<NekkuProductMealTime>? = null,
    @JsonProperty("meal_type") val mealType: NekkuProductMealType? = null,
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
    val reservations: List<TimeRange>?,
    val absences: Set<AbsenceCategory>?,
    val mealType: NekkuProductMealType?,
    val optionsId: String?, // Todo:check proper type
    val specialDiet: String?, // Todo:check proper type
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val mealTimes: DaycareMealtimes,
    val eatsBreakfast: Boolean,
)

data class NekkuMealInfo(
    val sku: String,
    val options: List<NekkuClient.ProductOption>? = null,
    val nekkuMealType: NekkuProductMealType? = null,
)

data class NekkuOrderResult(
    val message: String?,
    val created: List<String>?,
    val cancelled: List<String>?,
)

data class NekkuSpecialDietChoices(val dietId: String, val fieldId: String, val value: String)
