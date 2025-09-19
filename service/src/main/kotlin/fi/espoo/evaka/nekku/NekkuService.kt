// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.nekku

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.NekkuEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.absence.getGroupName
import fi.espoo.evaka.daycare.DaycareMealtimes
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.EmailContent
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.isHoliday
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import org.jdbi.v3.json.Json
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NekkuService(
    env: NekkuEnv?,
    jsonMapper: JsonMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailEnv: EmailEnv,
    private val emailClient: EmailClient,
) {
    private val client = env?.let { NekkuHttpClient(it, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::syncNekkuCustomers)
        asyncJobRunner.registerHandler(::syncNekkuSpecialDiets)
        asyncJobRunner.registerHandler(::syncNekkuProducts)
        asyncJobRunner.registerHandler(::sendNekkuOrder)
        asyncJobRunner.registerHandler(::sendNekkuCustomerNumberNullificationWarningEmail)
        asyncJobRunner.registerHandler(::sendNekkuSpecialDietRemovalWarningEmail)
        asyncJobRunner.registerHandler(::sendNekkuOrderFailureWarningEmail)
    }

    fun syncNekkuCustomers(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuCustomers,
    ) {
        if (client == null) error("Cannot sync Nekku customers: NekkuEnv is not configured")
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, clock.now())
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
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, clock.now())
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
        try {
            planNekkuOrderJobs(dbc, asyncJobRunner, clock.now())
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to plan Nekku order to Nekku: date=${clock.now()} ,error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun planNekkuDailyOrders(dbc: Database.Connection, clock: EvakaClock) {
        try {
            planNekkuDailyOrderJobs(dbc, asyncJobRunner, clock.now())
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to plan Nekku daily order to Nekku: date=${clock.now()} ,error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun planNekkuSpecifyOrders(dbc: Database.Connection, clock: EvakaClock) {
        try {
            planNekkuSpecifyOrderJobs(dbc, asyncJobRunner, clock.now())
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to plan Nekku specify order to Nekku: date=${clock.now()} ,error=${e.localizedMessage}"
            }
            throw e
        }
    }

    fun sendNekkuOrder(dbc: Database.Connection, clock: EvakaClock, job: AsyncJob.SendNekkuOrder) {
        if (client == null) error("Cannot send Nekku order: NekkuEnv is not configured")

        createAndSendNekkuOrder(
            client,
            dbc,
            groupId = job.groupId,
            date = job.date,
            asyncJobRunner = asyncJobRunner,
            clock.now(),
        )
    }

    fun sendNekkuCustomerNumberNullificationWarningEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendNekkuCustomerNumberNullificationWarningEmail,
    ) {
        val content =
            EmailContent.fromHtml(
                subject = "Muutoksia Nekku-asiakasnumeroihin",
                html =
                    "<p>Seuraavien ryhmien asiakasnumerot on poistettu johtuen asiakasnumeron poistumisesta Nekusta:</p>\n" +
                        job.groupNames.joinToString("\n", transform = { "<p>- $it</p>" }),
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

    fun sendNekkuSpecialDietRemovalWarningEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendNekkuSpecialDietRemovalWarningEmail,
    ) {
        val groupInfo =
            job.childrenByGroup
                .toSortedMap()
                .map { (group, children) ->
                    "<p>$group</p>" +
                        children
                            .map { (childId, diets) ->
                                "Lapsen tunniste: $childId, lapsen ruokavaliot: " +
                                    diets.map { it.value }.sortedBy { it }.joinToString(", ")
                            }
                            .joinToString("<br/>")
                }
                .joinToString("<br/>")
        val content =
            EmailContent.fromHtml(
                subject = "Muutoksia Nekku-allergiatietoihin",
                html =
                    "<p>Seuraavilta lapsilta on poistunut allergiatietoja koska kyseinen kenttä on poistunut Nekusta. Tässä viestissä on lasten alkuperäiset allergiatiedot. Varmista että lasten tiedot päivitetään uusien Nekku-kenttien mukaisiksi.</p>" +
                        groupInfo,
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

    fun sendNekkuOrderFailureWarningEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendNekkuOrderFailureWarningEmail,
    ) {
        val formattedDate = job.orderDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val content =
            EmailContent.fromHtml(
                subject = "Ryhmän ${job.groupName} Nekku-tilaus epäonnistui",
                html =
                    "<p>Ryhmän ${job.groupName} Nekku-tilaus päivälle $formattedDate epäonnistui</p><p>Virheilmoitus: ${job.errorMessage}</p>",
            )

        Email.createForEmployee(
                dbc,
                job.employeeId,
                content = content,
                traceId = "${job.groupId}:${job.employeeId}",
                emailEnv.sender(Language.fi),
            )
            ?.let { emailClient.send(it) }
    }
}

fun planNekkuOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    val orderDates = now.toLocalDate().weeklyJobsForThirdWeekFromNow()
    dbc.transaction { tx ->
        val openGroups = tx.getNekkuOpenDaycareGroupDates(orderDates)
        asyncJobRunner.plan(
            tx,
            openGroups.flatMap { nekkuGroup ->
                val groupOperationDays =
                    tx.getGroupOperationDays(nekkuGroup.id) ?: return@flatMap emptySequence()
                orderDates.dates().mapNotNull { date ->
                    if (
                        isGroupValidOnDate(date, nekkuGroup) &&
                            isGroupOpenOnDate(date, groupOperationDays)
                    ) {
                        AsyncJob.SendNekkuOrder(groupId = nekkuGroup.id, date = date)
                    } else null
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
    val today = now.toLocalDate()
    dbc.transaction { tx ->
        val openGroups = tx.getNekkuOpenDaycareGroupDates(today)
        val openingGroups = tx.findNekkuGroupsOpeningInNextWeek(today)
        val groupOperationDays =
            (openGroups + openingGroups).map { it.id to tx.getGroupOperationDays(it.id) }.toMap()
        val openingGroupsToOrder =
            openingGroups
                .filter { isGroupOpenOnDate(today, groupOperationDays[it.id]) }
                .filter {
                    val operationDays = groupOperationDays[it.id]
                    if (operationDays == null) false
                    else daycareOpenNextTime(today, operationDays) >= it.validFrom
                }
        val orderedGroupIds = openGroups + openingGroupsToOrder
        asyncJobRunner.plan(
            tx,
            orderedGroupIds.mapNotNull { nekkuGroup ->
                val currentGroupOperationDays = groupOperationDays[nekkuGroup.id]
                if (
                    currentGroupOperationDays != null &&
                        isGroupOpenOnDate(today, groupOperationDays[nekkuGroup.id])
                ) {
                    val daycareOpenNextTime = daycareOpenNextTime(today, currentGroupOperationDays)

                    val nekkuOrders =
                        tx.getNekkuOrderReport(
                            tx.getDaycareIdByGroup(nekkuGroup.id),
                            nekkuGroup.id,
                            daycareOpenNextTime,
                        )

                    if (nekkuOrders.isNotEmpty()) {
                        AsyncJob.SendNekkuOrder(groupId = nekkuGroup.id, date = daycareOpenNextTime)
                    } else null
                } else null
            },
            runAt = now,
            retryInterval = Duration.ofHours(1),
            retryCount = 3,
        )
    }
}

fun planNekkuSpecifyOrderJobs(
    dbc: Database.Connection,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    val fourDaysFromNow = now.toLocalDate().plusDays(4)

    dbc.transaction { tx ->
        val openGroups = tx.getNekkuOpenDaycareGroupDates(fourDaysFromNow)

        asyncJobRunner.plan(
            tx,
            openGroups.mapNotNull { nekkuGroup ->
                val nekkuOrders =
                    tx.getNekkuOrderReport(
                        tx.getDaycareIdByGroup(nekkuGroup.id),
                        nekkuGroup.id,
                        fourDaysFromNow,
                    )

                val groupOperationDays = tx.getGroupOperationDays(nekkuGroup.id)
                if (
                    nekkuOrders.isNotEmpty() &&
                        groupOperationDays != null &&
                        isGroupOpenOnDate(fourDaysFromNow, groupOperationDays)
                ) {
                    AsyncJob.SendNekkuOrder(groupId = nekkuGroup.id, date = fourDaysFromNow)
                } else null
            },
            runAt = now,
            retryInterval = Duration.ofHours(1),
            retryCount = 3,
        )
    }
}

fun planNekkuManualOrderJob(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    groupId: GroupId,
    date: LocalDate,
) {
    val today = now.toLocalDate()
    if (date.isOnOrBefore(today))
        throw BadRequest("Can only make a manual order beginning tomorrow")
    // this week we have made the orders for up to the week after next
    if (date.isAfter(today.nextWeeksSunday().plusDays(14)))
        throw BadRequest(
            "Can only make a manual order if an automatic order has been made for the day"
        )

    val groupOperationDays =
        tx.getGroupOperationDays(groupId)
            ?: throw BadRequest("Daycare operation info not found for group $groupId")
    if (!isGroupOpenOnDate(date, groupOperationDays))
        throw BadRequest("Group $groupId is not open on $date")

    if (tx.getNekkuOpenDaycareGroupDates(date).none { it.id == groupId })
        throw BadRequest("No customer number for group $groupId or group or unit is not open")

    asyncJobRunner.plan(
        tx,
        listOf(AsyncJob.SendNekkuOrder(groupId, date)),
        runAt = now,
        retryInterval = Duration.ofHours(1),
        retryCount = 3,
    )
}

private fun LocalDate.thisWeeksSunday() = this.plusDays((7 - this.dayOfWeek.value).toLong())

private fun LocalDate.nextWeeksMonday() = this.thisWeeksSunday().plusDays(1)

private fun LocalDate.nextWeeksSunday() = this.thisWeeksSunday().plusDays(7)

private fun LocalDate.isOnOrBefore(other: LocalDate): Boolean = this.isBefore(other.plusDays(1))

private fun LocalDate.weeklyJobsForThirdWeekFromNow(): FiniteDateRange {
    val nextMondayPlusTwoWeeks = this.nextWeeksMonday().plusWeeks(2)
    val jobDateRangeEnd = nextMondayPlusTwoWeeks.plusDays(6)
    return FiniteDateRange(nextMondayPlusTwoWeeks, jobDateRangeEnd)
}

private fun getNekkuWeekday(date: LocalDate): NekkuCustomerWeekday {
    return if (isHoliday(date)) {
        NekkuCustomerWeekday.WEEKDAYHOLIDAY
    } else {
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> NekkuCustomerWeekday.MONDAY
            DayOfWeek.TUESDAY -> NekkuCustomerWeekday.TUESDAY
            DayOfWeek.WEDNESDAY -> NekkuCustomerWeekday.WEDNESDAY
            DayOfWeek.THURSDAY -> NekkuCustomerWeekday.THURSDAY
            DayOfWeek.FRIDAY -> NekkuCustomerWeekday.FRIDAY
            DayOfWeek.SATURDAY -> NekkuCustomerWeekday.SATURDAY
            DayOfWeek.SUNDAY -> NekkuCustomerWeekday.SUNDAY
        }
    }
}

fun createAndSendNekkuOrder(
    client: NekkuClient,
    dbc: Database.Connection,
    groupId: GroupId,
    date: LocalDate,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
) {
    try {

        val (preschoolTerms, children) =
            dbc.read { tx ->
                val preschoolTerms = tx.getPreschoolTerms()
                val children = getNekkuChildInfos(tx, groupId, date)
                preschoolTerms to children
            }
        val nekkuWeekday = getNekkuWeekday(date)

        val nekkuDaycareCustomerMapping =
            dbc.read { tx -> tx.getNekkuGroupCustomerMapping(groupId, nekkuWeekday) }

        val nekkuProducts = dbc.read { tx -> tx.getNekkuProducts() }

        if (nekkuDaycareCustomerMapping != null) {
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
                                    nekkuDaycareCustomerMapping.customerType,
                                    nekkuMealDeductionFactor =
                                        1 -
                                            (dbc.read { tx ->
                                                    tx.getNekkuOrderReductionForDaycareByGroup(
                                                        groupId
                                                    )
                                                }
                                                .toDouble() / 100),
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

                val parts =
                    listOfNotNull(
                        nekkuOrderResult.created?.let { "Luotu: $it" },
                        nekkuOrderResult.cancelled?.let { "Peruttu: $it" },
                    )
                val orderString = parts.joinToString(", ")

                dbc.transaction { tx ->
                    tx.setNekkuReportOrderReport(order, groupId, nekkuProducts, orderString)
                }
            } else {
                logger.info {
                    "Skipped Nekku order with no rows for date $date for customerNumber=${nekkuDaycareCustomerMapping.customerNumber} groupId=$groupId"
                }
            }
        } else {
            logger.info {
                "Could not find any customer with given date: ${date.dayOfWeek} groupId=$groupId"
            }
            error("Could not find any customer with given date: ${date.dayOfWeek} groupId=$groupId")
        }
    } catch (e: Exception) {
        logger.warn(e) {
            "Failed to send meal order to Nekku: date=$date, groupId=$groupId,error=${e.localizedMessage}"
        }

        dbc.transaction { tx ->
            tx.setNekkuReportOrderErrorReport(groupId, date, e.localizedMessage)

            val daycareId = tx.getDaycareIdByGroup(groupId)
            val groupName = tx.getGroupName(groupId) ?: "Tuntematon ryhmä"
            val supervisors =
                tx.getDaycareAclRows(
                    daycareId = daycareId,
                    includeStaffOccupancy = false,
                    includeStaffEmployeeNumber = false,
                    role = UserRole.UNIT_SUPERVISOR,
                )
            asyncJobRunner.plan(
                tx,
                supervisors.map {
                    AsyncJob.SendNekkuOrderFailureWarningEmail(
                        groupId,
                        groupName,
                        date,
                        it.employee.id,
                        e.localizedMessage,
                    )
                },
                runAt = now,
            )
        }
    }
}

fun nekkuMealReportData(
    children: Collection<NekkuChildInfo>,
    date: LocalDate,
    preschoolTerms: List<PreschoolTerm>,
    nekkuProducts: List<NekkuProduct>,
    customerType: String,
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
                    .mapNotNull { mealTime ->
                        val sku =
                            getNekkuProductNumber(nekkuProducts, mealTime, childInfo, customerType)
                        if (sku == null) null
                        else
                            NekkuMealInfo(
                                sku = sku,
                                options =
                                    childInfo.specialDiet
                                        ?.map { NekkuClient.ProductOption(it.fieldId, it.value) }
                                        ?.toSet(),
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
    customerType: String,
): String? {

    val filteredNekkuProducts =
        nekkuProducts.filter {
            it.mealTime?.contains(nekkuProductMealTime) ?: false &&
                it.mealType == nekkuChildInfo.mealType &&
                it.optionsId == nekkuChildInfo.optionsId &&
                it.customerTypes.contains(customerType)
        }

    if (filteredNekkuProducts.count() > 1) {
        logger.info {
            "Found too many Nekku Products from database with customertype=$customerType optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        }
        error(
            "Found too many Nekku Products from database with customertype=$customerType optionsId=${nekkuChildInfo.optionsId} mealtype=${nekkuChildInfo.mealType} mealtime=${nekkuProductMealTime.description}"
        )
    } else {
        return if (filteredNekkuProducts.isEmpty()) null else filteredNekkuProducts.first().sku
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
    val specialDietChoices = tx.specialDietChoicesForChildren(childIds)
    val units = tx.getDaycaresById(unitIds)
    val textFieldsPerSpecialDiet = tx.getNekkuTextFields()
    val defaultSpecialDiet =
        textFieldsPerSpecialDiet.keys.maxOrNull() ?: error("No special diets found")

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

        val isUnderOneYearOld = child.dateOfBirth > date.minusYears(1)
        val optionsId =
            specialDietChoices[child.childId]?.first()?.dietId
                ?: if (isUnderOneYearOld) defaultSpecialDiet else ""

        NekkuChildInfo(
            placementType = child.placementType,
            reservations = child.reservations,
            absences = child.absences,
            mealType = mealTypes[child.childId],
            optionsId = optionsId,
            specialDiet =
                if (!isUnderOneYearOld) specialDietChoices[child.childId]
                else
                    addUnderOneYearOldDiet(
                        specialDietChoices[child.childId],
                        textFieldsPerSpecialDiet,
                        optionsId,
                    ),
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            mealTimes = unit.mealTimes,
            eatsBreakfast = child.eatsBreakfast,
        )
    }
}

private const val UNDER_ONE_YEAR_OLD_DIET = "Alle 1-vuotiaan ruokavalio"

fun addUnderOneYearOldDiet(
    nekkuSpecialDietChoices: List<NekkuSpecialDietChoices>?,
    textFieldsPerSpecialDiet: Map<String, String>,
    defaultOptionsId: String,
): List<NekkuSpecialDietChoices> {
    if (nekkuSpecialDietChoices == null)
        return listOf(
            NekkuSpecialDietChoices(
                defaultOptionsId,
                textFieldsPerSpecialDiet[defaultOptionsId]
                    ?: error("Special diet $defaultOptionsId not found"),
                UNDER_ONE_YEAR_OLD_DIET,
            )
        )
    // if the child's special diet choices already contain the free text field
    // we append to it, otherwise we will create a new free text field
    val textField =
        nekkuSpecialDietChoices.find { it.fieldId == textFieldsPerSpecialDiet[it.dietId] }
    return if (textField == null) {
        nekkuSpecialDietChoices +
            NekkuSpecialDietChoices(
                nekkuSpecialDietChoices.first().dietId,
                textFieldsPerSpecialDiet[nekkuSpecialDietChoices.first().dietId]
                    ?: error("Special diet ${nekkuSpecialDietChoices.first().dietId} not found"),
                UNDER_ONE_YEAR_OLD_DIET,
            )
    } else
        nekkuSpecialDietChoices.filter { it != textField } +
            textField.copy(value = textField.value + ", " + UNDER_ONE_YEAR_OLD_DIET)
}

fun isGroupValidOnDate(date: LocalDate, groupDates: GroupDates): Boolean =
    (groupDates.validFrom == null || date >= groupDates.validFrom) &&
        (groupDates.validTo == null || date <= groupDates.validTo)

fun isGroupOpenOnDate(date: LocalDate, daycareOperationInfo: NekkuDaycareOperationInfo?): Boolean =
    daycareOperationInfo != null &&
        (date.dayOfWeek.value in daycareOperationInfo.combinedDays &&
            (daycareOperationInfo.shiftCareOpenOnHolidays || !isHoliday(date)))

fun daycareOpenNextTime(
    date: LocalDate,
    daycareOperationInfo: NekkuDaycareOperationInfo,
): LocalDate {

    if (daycareOperationInfo.combinedDays.isEmpty()) {
        throw IllegalStateException(
            "Päiväkodin aukioloaikoja ei olla asetettu Nekku-tilausta luotaessa"
        )
    }

    for (i in 1..7) {
        val candidateDate = date.plusDays(i.toLong())

        if (isGroupOpenOnDate(candidateDate, daycareOperationInfo)) {
            return candidateDate
        }
    }

    throw IllegalStateException("Päiväkodin seuraavaa aukioloa ei löydetty")
}

data class NekkuCustomer(
    val number: String,
    val name: String,
    val group: String,
    @Json val customerType: List<CustomerType>,
)

data class CustomerType(val weekdays: List<NekkuCustomerWeekday>, val type: String)

@ConstList("nekku_customer_weekday")
enum class NekkuCustomerWeekday : DatabaseEnum {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    WEEKDAYHOLIDAY;

    override val sqlType: String = "nekku_customer_weekday"
}

data class NekkuUnitNumber(val number: String, val name: String)

data class NekkuSpecialDiet(
    val id: String,
    val name: String,
    val fields: List<NekkuSpecialDietsField>,
)

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
    CHECKBOXLIST,
    CHECKBOX,
    RADIO,
    TEXTAREA,
    EMAIL;

    override val sqlType: String = "nekku_special_diet_type"
}

data class NekkuSpecialDietOption(val weight: Int, val key: String, val value: String)

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
    @JsonProperty("customer_types") val customerTypes: List<String>,
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
    val optionsId: String,
    val specialDiet: List<NekkuSpecialDietChoices>?,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val mealTimes: DaycareMealtimes,
    val eatsBreakfast: Boolean,
)

data class NekkuMealInfo(
    val sku: String,
    val options: Set<NekkuClient.ProductOption>? = null,
    val nekkuMealType: NekkuProductMealType? = null,
)

data class NekkuOrderResult(
    val message: String?,
    val created: List<String>?,
    val cancelled: List<String>?,
)

data class NekkuSpecialDietChoices(val dietId: String, val fieldId: String, val value: String)

data class NekkuSpecialDietChoicesWithChild(
    val childId: ChildId,
    val dietId: String,
    val fieldId: String,
    val value: String,
)

data class NekkuOrdersReport(
    val deliveryDate: LocalDate,
    val daycareId: DaycareId,
    val groupId: GroupId,
    val mealSku: String,
    val totalQuantity: Int,
    val mealTime: List<NekkuProductMealTime>?,
    val mealType: NekkuProductMealType?,
    val mealsBySpecialDiet: List<String>?,
    val nekkuOrderInfo: String,
)
