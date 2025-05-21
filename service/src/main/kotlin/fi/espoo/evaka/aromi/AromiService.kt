// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.AromiEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.isUnitOperationDay
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.jdbi.v3.json.Json
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
private val breakfastRefusalStartTime = LocalTime.of(10, 0)

@Service
class AromiService(private val aromiEnv: AromiEnv?) {
    private val startDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    private val endTimeFormatter = DateTimeFormatter.ofPattern("HHmm")

    fun sendOrders(
        db: Database.Connection,
        clock: EvakaClock,
        earliestStartDate: LocalDate = LocalDate.of(2025, 5, 19),
    ) {
        val today = clock.today()
        logger.info { "Scheduled sending of Aromi attendance CSV started ($today)" }
        val sftpClient =
            aromiEnv?.let { SftpClient(it.sftp) }
                ?: error("Cannot send Aromi orders: AromiEnv is not configured")
        val formatter = DateTimeFormatter.ofPattern(aromiEnv.filePattern)
        val endDate = today.plusDays(21)
        if (endDate.isBefore(earliestStartDate))
            error("End date of meal order is before earliest start date, aborting")
        val startDate = maxOf(earliestStartDate, today.plusDays(3))
        val data = getMealOrdersCsv(db, FiniteDateRange(startDate, endDate))
        val fileName = today.format(formatter)
        logger.info { "Sending Aromi attendance CSV $fileName (${data.size})" }
        data.inputStream().use { sftpClient.put(it, fileName) }
        logger.info { "Scheduled sending of Aromi attendance CSV completed" }
    }

    fun getMealOrdersCsv(dbc: Database.Connection, range: FiniteDateRange): ByteArray {
        logger.info { "Collecting Aromi attendance rows (${range.start} - ${range.end})" }
        val data = dbc.read { tx -> getData(tx, range) }
        if (data.report.isEmpty()) error("No attendance information available, aborting")
        val earliestStartDate = data.report.minOfOrNull { it.date }
        logger.info {
            "Aromi attendance rows collected (${data.report.size}), earliest attendance date $earliestStartDate"
        }
        return ByteArrayOutputStream().use { stream ->
            PrintWriter(stream, false, StandardCharsets.ISO_8859_1).use { writer ->
                printCsv(writer, data)
                writer.flush()
            }
            stream.toByteArray()
        }
    }

    private fun getData(tx: Database.Read, range: FiniteDateRange): AromiMealOrderData {
        val aromiGroupRows = tx.getAromiUnitsAndGroups(range)
        val aromiUnits =
            aromiGroupRows
                .groupBy { Pair(it.daycareId, it.daycareName) }
                .map { (key, rows) ->
                    val unitData = rows.first()
                    AromiReportingUnit(
                        unitId = key.first,
                        name = key.second,
                        operationDays = unitData.operationDays,
                        shiftCareOperationDays = unitData.shiftCareOperationDays,
                        shiftCareOpenOnHolidays = unitData.shiftCareOpenOnHolidays,
                        dailyPreparatoryTime = unitData.dailyPreparatoryTime,
                        dailyPreschoolTime = unitData.dailyPreschoolTime,
                        aromiGroups =
                            rows.map {
                                AromiReportingGroup(
                                    groupId = it.groupId,
                                    name = it.groupName,
                                    aromiCustomerId = it.aromiCustomerId,
                                )
                            },
                    )
                }
        return AromiMealOrderData(
            report =
                getAttendancePredictionRows(
                    tx,
                    range,
                    aromiUnits.associateBy { it.unitId },
                    aromiUnits.flatMap { it.aromiGroups }.associateBy { it.groupId },
                )
        )
    }

    private fun printCsv(writer: Appendable, data: AromiMealOrderData) {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').get()
        CSVPrinter(writer, format).use { printer -> printRecords(printer, data) }
    }

    private fun printRecords(printer: CSVPrinter, data: AromiMealOrderData) {
        data.report.forEach { row -> printRecord(printer, row) }
    }

    private fun printRecord(printer: CSVPrinter, item: AromiAttendanceRow) {
        val age = Period.between(item.dateOfBirth, item.date)
        val reservation =
            item.attendanceRange?.asHelsinkiDateTimeRange(item.date)
                ?: HelsinkiDateTimeRange.of(item.date, LocalTime.MIN, LocalTime.MAX)
        printer.printRecord(
            "PKPOT", // Tietuetunnus
            "EVAKA", // Ympäristötunnus
            item.aromiCustomerId, // Vastuuyksikkökoodi = ryhmäkohtainen Aromin asiakkuustunniste
            reservation.start.toLocalDateTime().format(startDateTimeFormatter), // Hoitoontuloaika
            null, // Huone, ei käytössä
            item.lastName, // Sukunimi
            item.firstName, // Etunimi
            item.dateOfBirth.ssnLike(), // Henkilötunnus tai syntymäaika
            age.format(), // Ikä
            item.childId, // Ruokailijatunnus
            null, // Projekti, ei käytössä
            null, // Tyhjää, ei käytössä
            null, // Tyhjää, ei käytössä
            null, // Informaatio lähettävästä järjestelmästä, tyhjää, ei käytössä
            item.groupName, // Päiväkotiryhmä
            item.groupName, // Toimituspisteen nimi
            reservation.end.toLocalTime().format(endTimeFormatter), // Hoidosta lähdön kellonaika
        )
    }
}

data class AromiAttendanceRow(
    val date: LocalDate,
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val groupName: String,
    val aromiCustomerId: String,
    val attendanceRange: TimeRange?,
)

data class AromiReportingUnit(
    val unitId: DaycareId,
    val name: String,
    val operationDays: Set<Int>,
    val shiftCareOperationDays: Set<Int>?,
    val shiftCareOpenOnHolidays: Boolean,
    val dailyPreparatoryTime: TimeRange?,
    val dailyPreschoolTime: TimeRange?,
    val aromiGroups: List<AromiReportingGroup>,
)

data class AromiReportingGroup(val groupId: GroupId, val name: String, val aromiCustomerId: String)

private data class AromiReportingRow(
    val daycareId: DaycareId,
    val daycareName: String,
    val operationDays: Set<Int>,
    val shiftCareOperationDays: Set<Int>?,
    val shiftCareOpenOnHolidays: Boolean,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val groupId: GroupId,
    val groupName: String,
    val aromiCustomerId: String,
)

private fun Database.Read.getAromiUnitsAndGroups(window: FiniteDateRange): List<AromiReportingRow> {
    return createQuery {
            sql(
                """
SELECT
    d.id AS daycare_id,
    d.name AS daycare_name,
    d.operation_days,
    d.shift_care_operation_days,
    d.shift_care_open_on_holidays,
    d.daily_preschool_time,
    d.daily_preparatory_time,
    dg.id AS group_id,
    dg.name AS group_name,
    dg.aromi_customer_id
FROM daycare_group dg
JOIN daycare d ON dg.daycare_id = d.id
WHERE dg.aromi_customer_id IS NOT NULL
AND daterange(dg.start_date, dg.end_date, '[]') && ${bind(window)}
AND daterange(d.opening_date, d.closing_date, '[]') && ${bind(window)}
ORDER BY d.name, dg.name
"""
            )
        }
        .toList<AromiReportingRow>()
}

data class AromiOrderDailyChildInfo(
    val placementType: PlacementType,
    val groupId: GroupId,
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val reservations: List<TimeRange>?,
    val absences: Set<AbsenceCategory>?,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val eatsBreakfast: Boolean,
)

data class AromiDailyChildData(
    val childId: ChildId,
    val dateOfBirth: LocalDate,
    val unitId: DaycareId,
    val groupId: GroupId,
    val placementType: PlacementType,
    val hasShiftCare: Boolean,
    val firstName: String,
    val lastName: String,
    @Json val reservations: List<TimeRange>,
    val absences: Set<AbsenceCategory>,
    val eatsBreakfast: Boolean,
)

fun Database.Read.getAromiDailyChildData(date: LocalDate): List<AromiDailyChildData> =
    createQuery {
            sql(
                """
SELECT
    rp.child_id,
    rp.unit_id,
    rp.placement_type,
    (sn.shift_care IS NOT NULL AND sn.shift_care != 'NONE') AS has_shift_care,
    p.first_name,
    p.last_name,
    p.date_of_birth,
    dg.id AS group_id,
    ch.nekku_eats_breakfast IS NOT FALSE as eats_breakfast,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('start', ar.start_time, 'end', ar.end_time))
        FROM attendance_reservation ar
        JOIN evaka_user eu ON ar.created_by = eu.id
        WHERE
            ar.child_id = rp.child_id AND
            ar.date = ${bind(date)} AND
            -- Ignore NO_TIMES reservations
            ar.start_time IS NOT NULL AND ar.end_time IS NOT NULL
    ), '[]'::jsonb) AS reservations,
    coalesce((
        SELECT array_agg(a.category)
        FROM absence a
        WHERE a.child_id = rp.child_id AND a.date = ${bind(date)}
    ), '{}'::absence_category[]) AS absences
FROM realized_placement_one(${bind(date)}) rp
JOIN daycare_group dg ON dg.id = rp.group_id
JOIN person p ON p.id = rp.child_id
JOIN daycare d ON rp.unit_id = d.id
JOIN child ch ON p.id = ch.id
LEFT JOIN service_need sn ON sn.placement_id = rp.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
WHERE dg.aromi_customer_id IS NOT NULL 
AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(date)}
AND daterange(d.opening_date, d.closing_date, '[]') @> ${bind(date)}
                    """
            )
        }
        .toList()

private fun Database.Read.getAromiChildInfos(
    date: LocalDate,
    aromiReportingUnits: Map<DaycareId, AromiReportingUnit>,
): List<AromiOrderDailyChildInfo> {
    val holidays = getHolidays(FiniteDateRange(date, date))
    val childData = getAromiDailyChildData(date)

    return childData.mapNotNull { child ->
        val unit =
            aromiReportingUnits[child.unitId]
                ?: error("Daycare not found for unitId ${child.unitId}")
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

        AromiOrderDailyChildInfo(
            placementType = child.placementType,
            firstName = child.firstName,
            lastName = child.lastName,
            reservations = child.reservations,
            absences = child.absences,
            dailyPreschoolTime = unit.dailyPreschoolTime,
            dailyPreparatoryTime = unit.dailyPreparatoryTime,
            groupId = child.groupId,
            childId = child.childId,
            dateOfBirth = child.dateOfBirth,
            eatsBreakfast = child.eatsBreakfast,
        )
    }
}

private fun getAttendancePredictionRows(
    tx: Database.Read,
    range: FiniteDateRange,
    aromiUnitMap: Map<DaycareId, AromiReportingUnit>,
    aromiGroupMap: Map<GroupId, AromiReportingGroup>,
): List<AromiAttendanceRow> {
    val preschoolTerms = tx.getPreschoolTerms()
    return range
        .dates()
        .flatMap { date ->
            val dailyChildren = tx.getAromiChildInfos(date, aromiUnitMap)
            dailyChildren
                .flatMap { childInfo ->
                    val group = aromiGroupMap[childInfo.groupId]!!
                    val fullDayAbsent =
                        childInfo.absences == childInfo.placementType.absenceCategories()
                    val scheduleType =
                        childInfo.placementType.scheduleType(date, emptyList(), preschoolTerms)
                    val effectivelyAbsent = scheduleType == ScheduleType.TERM_BREAK || fullDayAbsent

                    // list of time ranges when child will be present according to fixed schedule or
                    // reservation times
                    val evakaAttendanceTimes =
                        if (scheduleType == ScheduleType.FIXED_SCHEDULE)
                            listOfNotNull(
                                childInfo.placementType.fixedScheduleRange(
                                    childInfo.dailyPreschoolTime,
                                    childInfo.dailyPreparatoryTime,
                                )
                            )
                        else childInfo.reservations ?: emptyList()

                    // if a child does not eat breakfast, modify reservation/fixed schedule to begin
                    // at breakfastRefusalStartTime
                    val breakfastCorrectedTimes =
                        if (!childInfo.eatsBreakfast) {
                            if (
                                evakaAttendanceTimes.isEmpty()
                            ) // default attendance without breakfast
                             listOf(TimeRange(breakfastRefusalStartTime, LocalTime.of(23, 59)))
                            else // modified attendance without breakfast
                                evakaAttendanceTimes.mapNotNull { timeRange ->
                                    return@mapNotNull when {
                                        breakfastRefusalStartTime.isBefore(timeRange.start.inner) ->
                                            timeRange

                                        breakfastRefusalStartTime.equals(timeRange.end.inner) ||
                                            breakfastRefusalStartTime.isAfter(
                                                timeRange.end.inner
                                            ) -> null

                                        else ->
                                            TimeRange(
                                                breakfastRefusalStartTime,
                                                timeRange.end.inner,
                                            )
                                    }
                                }
                        } else evakaAttendanceTimes

                    val entry = { reservation: TimeRange? ->
                        AromiAttendanceRow(
                            firstName = childInfo.firstName,
                            lastName = childInfo.lastName,
                            date = date,
                            aromiCustomerId = group.aromiCustomerId,
                            groupName = group.name,
                            childId = childInfo.childId,
                            attendanceRange = reservation,
                            dateOfBirth = childInfo.dateOfBirth,
                        )
                    }

                    if (effectivelyAbsent) {
                        emptyList()
                    } else {
                        if (breakfastCorrectedTimes.isEmpty()) {
                            if (evakaAttendanceTimes.isNotEmpty()) {
                                // child only has excepted reservations -> no meal order impact ->
                                // marked effectively absent
                                emptyList()
                            } else {
                                // full day for any non-reserved and non-absent with schedule type
                                // RESERVATION_REQUIRED
                                listOf(entry(null))
                            }
                        } else {
                            breakfastCorrectedTimes.map { entry(it) }
                        }
                    }
                }
                .sortedWith(
                    compareBy({ it.lastName }, { it.firstName }, { it.attendanceRange?.start })
                )
        }
        .toList()
}

private data class AromiMealOrderData(val report: List<AromiAttendanceRow>)

private fun LocalDate.ssnLike(): String {
    val centuryMarker =
        when (val centuryPart = this.year / 100) {
            18 -> '+'
            19 -> '-'
            20 -> 'A'
            else -> throw Error("Unsupported century part $centuryPart")
        }
    return this.format(DateTimeFormatter.ofPattern("ddMMyy")) + centuryMarker
}

private fun Period.format(): String {
    val yearsStr = years.toString().padStart(3, '0')
    val monthsStr = months.toString().padStart(2, '0')
    return "$yearsStr,$monthsStr"
}
