// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.AromiEnv
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.reports.AttendanceReservationReportByChildGroup
import fi.espoo.evaka.reports.AttendanceReservationReportByChildItem
import fi.espoo.evaka.reports.getAttendanceReservationReportByChild
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.sftp.SftpClient
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.lang.Appendable
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
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
        val data = getAllMealOrdersCsv(range, today)
        data.inputStream().use { sftpClient.put(it, today.format(formatter)) }
    }

    fun getAllMealOrdersCsv(range: FiniteDateRange, today: LocalDate): ByteArray =
        ByteArrayOutputStream().use { stream ->
            PrintWriter(stream, false, StandardCharsets.ISO_8859_1).use { writer ->
                printCsv(
                    writer,
                    AromiMealOrderData(
                        report =
                            range
                                .dates()
                                .map { date ->
                                    AttendanceReservationReportByChildGroup(
                                        groupId = GroupId(UUID.randomUUID()),
                                        groupName = "test group",
                                        items =
                                            listOf(
                                                AttendanceReservationReportByChildItem(
                                                    date = date,
                                                    childId = ChildId(UUID.randomUUID()),
                                                    childLastName = "test last",
                                                    childFirstName = "test first",
                                                    childDateOfBirth = today.minusWeeks(30),
                                                    reservation =
                                                        TimeRange(
                                                            LocalTime.of(8, 0),
                                                            LocalTime.of(16, 0),
                                                        ),
                                                    fullDayAbsence = false,
                                                    backupCare = false,
                                                )
                                            ),
                                    )
                                }
                                .toList(),
                        "test unit",
                    ),
                )
                writer.flush()
            }
            stream.toByteArray()
        }

    fun getMealOrdersCsv(
        dbc: Database.Connection,
        range: FiniteDateRange,
        groupIds: List<GroupId>,
    ): ByteArray {
        val data = dbc.read { tx -> getData(tx, range, groupIds) }
        return ByteArrayOutputStream().use { stream ->
            PrintWriter(stream, false, StandardCharsets.ISO_8859_1).use { writer ->
                printCsv(writer, data)
                writer.flush()
            }
            stream.toByteArray()
        }
    }

    private fun getData(
        tx: Database.Read,
        range: FiniteDateRange,
        groupIds: List<GroupId>,
    ): AromiMealOrderData {
        val units =
            groupIds
                .map { tx.getDaycareIdByGroup(it).let { unitId -> tx.getDaycare(unitId) } }
                .distinct()
        if (units.size != 1) {
            throw Error("Too many units")
        }
        val unit = units[0]!!
        val report = getAttendanceReservationReportByChild(tx, range, unit.id, groupIds)
        return AromiMealOrderData(report = report, unitName = unit.name)
    }

    private fun printCsv(writer: Appendable, data: AromiMealOrderData) {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').get()
        CSVPrinter(writer, format).use { printer -> printRecords(printer, data) }
    }

    private fun printRecords(printer: CSVPrinter, data: AromiMealOrderData) {
        data.report.forEach { reportByGroup ->
            reportByGroup.items
                .filterNot { it.fullDayAbsence }
                .forEach { printRecord(printer, it, data.unitName, reportByGroup.groupName) }
        }
    }

    private fun printRecord(
        printer: CSVPrinter,
        item: AttendanceReservationReportByChildItem,
        unitName: String,
        groupName: String?,
    ) {
        val age = Period.between(item.childDateOfBirth, item.date)
        val reservation =
            item.reservation?.asHelsinkiDateTimeRange(item.date)
                ?: HelsinkiDateTimeRange.of(item.date, LocalTime.MIN, LocalTime.MAX)
        printer.printRecord(
            "PKPOT", // Tietuetunnus
            "EVAKA", // Ympäristötunnus
            unitName, // Vastuuyksikkökoodi
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

private data class AromiMealOrderData(
    val report: List<AttendanceReservationReportByChildGroup>,
    val unitName: String,
)

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
