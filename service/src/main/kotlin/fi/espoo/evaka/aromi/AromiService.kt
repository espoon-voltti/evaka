// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.reports.AttendanceReservationReportByChildItem
import fi.espoo.evaka.reports.getAttendanceReservationReportByChild
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
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
class AromiService {
    private val startDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    private val endTimeFormatter = DateTimeFormatter.ofPattern("HHmm")

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
            aromiUnits.map { aromiUnit ->
                AromiUnitAttendanceData(
                    daycareId = aromiUnit.unitId,
                    daycareName = aromiUnit.name,
                    groupAttendanceData =
                        getAttendanceReservationReportByChild(
                                tx,
                                range,
                                aromiUnit.unitId,
                                aromiUnit.aromiGroups.map { it.groupId },
                            )
                            .mapNotNull {
                                val customerId = aromiGroupMap[it.groupId]?.aromiCustomerId
                                if (
                                    it.groupId == null || it.groupName == null || customerId == null
                                )
                                    null
                                else
                                    AromiGroupAttendanceData(
                                        groupId = it.groupId,
                                        groupName = it.groupName,
                                        aromiCustomerId = customerId,
                                        attendanceData = it.items,
                                    )
                            },
                )
            }
        return AromiMealOrderData(report = fullAromidata)
    }

    private fun printCsv(writer: Appendable, data: AromiMealOrderData) {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').get()
        CSVPrinter(writer, format).use { printer -> printRecords(printer, data) }
    }

    private fun printRecords(printer: CSVPrinter, data: AromiMealOrderData) {
        data.report.forEach { unitData ->
            unitData.groupAttendanceData.forEach { groupData ->
                groupData.attendanceData
                    .filterNot { it.fullDayAbsence }
                    .forEach {
                        printRecord(
                            printer,
                            it,
                            unitData.daycareName,
                            groupData.groupName,
                            groupData.aromiCustomerId,
                        )
                    }
            }
        }
    }

    private fun printRecord(
        printer: CSVPrinter,
        item: AttendanceReservationReportByChildItem,
        unitName: String,
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
            null, // Toimituspisteen nimi
            reservation.end.toLocalTime().format(endTimeFormatter), // Hoidosta lähdön kellonaika
        )
    }
}

private data class AromiReportingUnit(
    val unitId: DaycareId,
    val name: String,
    val aromiGroups: List<AromiReportingGroup>,
)

private data class AromiReportingGroup(
    val groupId: GroupId,
    val name: String,
    val aromiCustomerId: String,
)

private data class AromiReportingRow(
    val daycareId: DaycareId,
    val daycareName: String,
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

private data class AromiGroupAttendanceData(
    val groupId: GroupId,
    val groupName: String,
    val aromiCustomerId: String,
    val attendanceData: List<AttendanceReservationReportByChildItem>,
)

private data class AromiUnitAttendanceData(
    val daycareId: DaycareId,
    val daycareName: String,
    val groupAttendanceData: List<AromiGroupAttendanceData>,
)

private data class AromiMealOrderData(val report: List<AromiUnitAttendanceData>)

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
