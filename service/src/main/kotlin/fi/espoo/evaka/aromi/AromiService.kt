// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.AromiEnv
import fi.espoo.evaka.absence.getAbsencesOfChildrenByRange
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reports.AttendanceReservationReportByChildItem
import fi.espoo.evaka.reports.getReservationsByRange
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.sftp.SftpClient
import fi.espoo.evaka.shared.domain.TimeRange
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Service

@Service
class AromiService(private val aromiEnv: AromiEnv?) {
    private val startDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    private val endTimeFormatter = DateTimeFormatter.ofPattern("HHmm")

    fun sendOrders(db: Database.Connection, clock: EvakaClock) {
        val sftpClient =
            aromiEnv?.let { SftpClient(it.sftp) }
                ?: error("Cannot send Aromi orders: AromiEnv is not configured")
        val formatter = DateTimeFormatter.ofPattern(aromiEnv.filePattern)
        val today = clock.today()
        val range = FiniteDateRange(today.plusDays(3), today.plusDays(21))
        val data = getMealOrdersCsv(db, range)
        data.inputStream().use { sftpClient.put(it, today.format(formatter)) }
    }

    fun getMealOrdersCsv(dbc: Database.Connection, range: FiniteDateRange): ByteArray {
        val data = dbc.read { tx -> getData(tx, range) }
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
        val aromiGroupMap = aromiGroupRows.associateBy { it.groupId }
        val aromiUnits =
            aromiGroupRows
                .groupBy { Pair(it.daycareId, it.daycareName) }
                .map { (key, rows) ->
                    AromiReportingUnit(
                        unitId = key.first,
                        name = key.second,
                        operationDays = rows.first().operationDays,
                        shiftCareOperationDays = rows.first().shiftCareOperationDays,
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

        val fullAromidata =
            getAttendanceReservationReportByChild(
                tx,
                range,
                aromiUnits.associateBy { it.unitId },
                aromiUnits.flatMap { it.aromiGroups }.map { it.groupId },
            ).map {
                val customerId = aromiGroupMap[it.groupId]?.aromiCustomerId!!
                AromiGroupAttendanceData(
                groupId = it.groupId,
                groupName = it.groupName,
                aromiCustomerId = customerId,
                attendanceData = it.items,
            ) }

        return AromiMealOrderData(report = fullAromidata)
    }

    private fun printCsv(writer: Appendable, data: AromiMealOrderData) {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').get()
        CSVPrinter(writer, format).use { printer -> printRecords(printer, data) }
    }

    private fun printRecords(printer: CSVPrinter, data: AromiMealOrderData) {
        data.report.forEach { groupData ->
                groupData.attendanceData
                    .filterNot { it.fullDayAbsence }
                    .forEach {
                        printRecord(printer, it, groupData.groupName, groupData.aromiCustomerId)
                    }
            }
        }

    private fun printRecord(
        printer: CSVPrinter,
        item: AttendanceReservationReportByChildItem,
        groupName: String,
        aromiCustomerId: String,
    ) {
        val age = Period.between(item.childDateOfBirth, item.date)
        val reservation =
            item.reservation?.asHelsinkiDateTimeRange(item.date)
                ?: HelsinkiDateTimeRange.of(item.date, LocalTime.MIN, LocalTime.MAX)
        printer.printRecord(
            "PKPOT", // Tietuetunnus
            "EVAKA", // Ympäristötunnus
            aromiCustomerId, // Vastuuyksikkökoodi = ryhmäkohtainen Aromin asiakkuustunniste
            reservation.start.toLocalDateTime().format(startDateTimeFormatter), // Hoitoontuloaika
            null, // Huone, ei käytössä
            item.childLastName, // Sukunimi
            item.childFirstName, // Etunimi
            item.childDateOfBirth.ssnLike(), // Henkilötunnus tai syntymäaika
            age.format(), // Ikä
            item.childId, // Ruokailijatunnus
            null, // Projekti, ei käytössä
            null, // Tyhjää, ei käytössä
            null, // Tyhjää, ei käytössä
            null, // Informaatio lähettävästä järjestelmästä, tyhjää, ei käytössä
            groupName, // Päiväkotiryhmä
            groupName, // Toimituspisteen nimi
            reservation.end.toLocalTime().format(endTimeFormatter), // Hoidosta lähdön kellonaika
        )
    }
}

data class AromiReportingUnit(
    val unitId: DaycareId,
    val name: String,
    val operationDays: Set<Int>,
    val shiftCareOperationDays: Set<Int>?,
    val aromiGroups: List<AromiReportingGroup>,
)

data class AromiReportingGroup(val groupId: GroupId, val name: String, val aromiCustomerId: String)

private data class AromiReportingRow(
    val daycareId: DaycareId,
    val daycareName: String,
    val operationDays: Set<Int>,
    val shiftCareOperationDays: Set<Int>?,
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

private data class ChildPlacementRow(
    val date: LocalDate,
    val childId: ChildId,
    val groupId: GroupId,
    val daycareId: DaycareId,
    val groupName: String,
    val placementType: PlacementType,
    val backupCare: Boolean,
)

private data class AromiChildRow(
    val childId: ChildId,
    val dateOfBirth: LocalDate,
    val lastName: String,
    val firstName: String,
)

private data class ChildAttendanceEntry(
    val childId: ChildId,
    val groupId: GroupId,
    val groupName: String,
    val date: LocalDate,
    val reservation: TimeRange?,
    val fullDayAbsence: Boolean,
    val backupCare: Boolean,
)

private data class AromiServiceNeedRow(
    val range: FiniteDateRange,
    val childId: ChildId,
    val shiftCare: ShiftCareType,
)

data class AromiAttendanceByChildGroup(
    val groupId: GroupId,
    val groupName: String,
    val items: List<AttendanceReservationReportByChildItem>,
)

private fun Database.Read.getChildPlacementDays(
    range: FiniteDateRange,
    groupIds: List<GroupId>,
): List<ChildPlacementRow> {
    return createQuery {
            sql(
                """
WITH dates AS (
    SELECT unnest(${bind(range.dates().toList())}::date[]) AS date
)
SELECT 
    dates.date,
    pl.child_id,
    dgp.daycare_group_id AS group_id,
    dg.name AS group_name,
    pl.type AS placement_type,
    pl.unit_id AS daycare_id,
    false AS backup_care
FROM dates
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> dates.date
JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> dates.date
JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
WHERE dg.id = ANY(${bind(groupIds)})
AND NOT EXISTS(
    SELECT 1
    FROM backup_care bc
    WHERE bc.child_id = pl.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> dates.date
)
UNION ALL
SELECT 
    dates.date, 
    bc.child_id,
    bc.group_id, 
    dg.name AS group_name,
    pl.type AS placement_type,
    bc.unit_id AS daycare_id,
    true AS backup_care
FROM dates
JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> dates.date
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> dates.date AND pl.child_id = bc.child_id
JOIN daycare_group dg ON dg.id = bc.group_id
WHERE dg.id = ANY(${bind(groupIds)});
"""
            )
        }
        .toList<ChildPlacementRow>()
}

private fun Database.Read.getChildInfo(children: Set<ChildId>): List<AromiChildRow> {
    return createQuery {
            sql(
                """
SELECT p.id as child_id, p.date_of_birth, p.last_name, p.first_name
FROM person p
WHERE p.id = ANY(${bind(children)})
"""
            )
        }
        .toList<AromiChildRow>()
}

private fun Database.Read.getServiceNeeds(
    start: LocalDate,
    end: LocalDate,
    children: Set<ChildId>,
): List<AromiServiceNeedRow> {
    return createQuery {
            sql(
                """
SELECT
    pl.child_id,
    daterange(sn.start_date, sn.end_date, '[]') * daterange(${bind(start)}, ${bind(end)}, '[]') as range,
    sn.shift_care
FROM service_need sn
JOIN placement pl on pl.id = sn.placement_id
WHERE pl.child_id = ANY(${bind(children)}) AND daterange(sn.start_date, sn.end_date, '[]') && daterange(${bind(start)}, ${bind(end)}, '[]');
    """
            )
        }
        .toList<AromiServiceNeedRow>()
}

private fun toItems(
    rows: List<ChildAttendanceEntry>,
    childInfoMap: Map<ChildId, AromiChildRow>,
): List<AttendanceReservationReportByChildItem> =
    rows
        .map { row ->
            val childInfo = childInfoMap[row.childId]!!
            AttendanceReservationReportByChildItem(
                childId = row.childId,
                childFirstName = childInfo.firstName,
                childLastName = childInfo.lastName,
                childDateOfBirth = childInfo.dateOfBirth,
                date = row.date,
                reservation = row.reservation,
                fullDayAbsence = row.fullDayAbsence,
                backupCare = row.backupCare,
            )
        }
        .sortedWith(compareBy({ it.date }, { it.childLastName }, { it.childFirstName }))

fun getAttendanceReservationReportByChild(
    tx: Database.Read,
    range: FiniteDateRange,
    aromiUnits: Map<DaycareId, AromiReportingUnit>,
    groupIds: List<GroupId>,
): List<AromiAttendanceByChildGroup> {

    // all placed children based on group placement / backup placement
    val placementStuff = tx.getChildPlacementDays(range, groupIds)
    val allChildren = placementStuff.map { it.childId }.toSet()
    val childInfoMap = tx.getChildInfo(allChildren).associateBy { it.childId }

    // shift care info from service need
    val serviceNeedsMap =
        tx.getServiceNeeds(range.start, range.end, allChildren).groupBy { it.childId }

    // reservations for child during range
    val reservationsMap = tx.getReservationsByRange(allChildren, range)

    // absences for children by range
    val absences =
        tx.getAbsencesOfChildrenByRange(allChildren, range.asDateRange())
            .groupBy { it.childId }
            .mapValues { (_, absences) ->
                absences.groupBy({ it.date }, { it.category }).mapValues { it.value.toSet() }
            }

    // removed daily service times

    // iterate placements and figure out service days?
    val rows =
        placementStuff.flatMap { placementInfo ->
            val aromiReportingUnit = aromiUnits[placementInfo.daycareId]!!
            val hasShiftCare =
                (serviceNeedsMap[placementInfo.childId]
                    ?.firstOrNull { it.range.includes(placementInfo.date) }
                    ?.shiftCare ?: ShiftCareType.NONE) != ShiftCareType.NONE
            val operationDays =
                aromiReportingUnit.shiftCareOperationDays.takeIf { hasShiftCare }
                    ?: aromiReportingUnit.operationDays
            if (!operationDays.contains(placementInfo.date.dayOfWeek.value)) {
                return@flatMap emptyList()
            }

            val reservations =
                (reservationsMap[Pair(placementInfo.childId, placementInfo.date)] ?: emptyList())
                    .map { it.reservation.asTimeRange() }
                    .sortedBy { it?.start }

            val absenceCategories =
                absences[placementInfo.childId]?.get(placementInfo.date) ?: emptySet()
            val fullDayAbsence =
                absenceCategories == placementInfo.placementType.absenceCategories()

            val entry = { reservation: TimeRange?, isAbsent: Boolean ->
                ChildAttendanceEntry(
                    childId = placementInfo.childId,
                    groupId = placementInfo.groupId,
                    groupName = placementInfo.groupName,
                    date = placementInfo.date,
                    reservation = reservation,
                    fullDayAbsence = isAbsent,
                    backupCare = placementInfo.backupCare,
                )
            }
            if (fullDayAbsence) {
                listOf(entry(null, true))
            } else {
                if (reservations.isEmpty()) {
                    listOf(entry(null, false))
                } else {
                    reservations.map { entry(it, false) }
                }
            }
        }

    return rows
        .groupBy { it.groupId }
        .map { (groupId, rowsOfGroup) ->
            AromiAttendanceByChildGroup(
                groupId = groupId,
                groupName = rowsOfGroup.first().groupName,
                items = toItems(rowsOfGroup, childInfoMap),
            )
        }
        .sortedBy { it.groupName }
}

private data class AromiGroupAttendanceData(
    val groupId: GroupId,
    val groupName: String,
    val aromiCustomerId: String,
    val attendanceData: List<AttendanceReservationReportByChildItem>,
)

private data class AromiMealOrderData(val report: List<AromiGroupAttendanceData>)

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
