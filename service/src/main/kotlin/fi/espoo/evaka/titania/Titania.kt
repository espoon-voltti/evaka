// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import org.springframework.http.HttpStatus

internal const val TITANIA_DATE_FORMAT = "yyyyMMdd"
internal const val TITANIA_TIME_FORMAT = "HHmm"

// from Titania_Vakiotyöaikatapahtumakoodit_Julkinen.pdf v1.3

internal val IGNORED_EVENT_CODES =
    listOf(
        // Työrupeamat
        "(",
        "£",
        "&",
        "€",
        // Vapaapäivät
        "V",
        "L",
        ":",
        "/",
        ")",
        "%",
        "T",
        "!",
        "k",
        "Q",
        "q",
        "v",
        "+",
        // Koulutukset
        "N",
        "n",
        // Varallaolo ja hälytystyö
        "1",
        "2",
        "3",
        "4",
        "5",
        "9",
        "8",
        // Vuosilomat
        "H",
        "h",
        // Sairauslomat
        "S",
        "s",
        "Z",
        // Vanhempainvapaat
        "7",
        "G",
        "g",
        // Virkavapaat
        "M",
        "m",
        // Muut poissaolot
        "t",
        "#",
        "l",
        "{",
        "\\",
        "[",
        // Muut tapahtumat
        ".",
        "*",
        "@",
        "<"
    )

// from updateWorkingTimeEvents.wsdl, version 1.2 25.8.2020 & getStampedWorkingTimeEvents.wsdl,
// version 1.1 14.8.2020

data class TitaniaCode(val code: String, val name: String? = null)

data class TitaniaCodeName(val code: String, val name: String)

data class TitaniaPeriod(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TITANIA_DATE_FORMAT)
    val beginDate: LocalDate,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TITANIA_DATE_FORMAT)
    val endDate: LocalDate
) {
    fun toDateRange() = FiniteDateRange(beginDate, endDate)

    companion object {
        fun from(range: FiniteDateRange) = TitaniaPeriod(range.start, range.end)

        fun from(date: LocalDate) = TitaniaPeriod(date, date)
    }
}

enum class TitaniaPayrollItemType {
    /** Edelliseltä jaksolta siirtyvää */
    PREVIOUS,

    /** Kuluva jakso */
    THIS,

    /** Seuraavalle siirtyvää */
    NEXT,

    /** Aikanakorvattavat (aikahyvitys) */
    TIME,

    /** Rahana korvattavat */
    MONEY
}

enum class TitaniaPayrollItemUnit {
    /** Minuutit */
    MINUTE,

    /** Kappaleet */
    QUANTITY
}

data class UpdateWorkingTimeEventsRequest(
    val organisation: TitaniaCode? = null,
    val period: TitaniaPeriod,
    val schedulingUnit: List<TitaniaSchedulingUnit>
)

data class TitaniaSchedulingUnit(
    val code: String,
    val name: String? = null,
    val occupation: List<TitaniaOccupation>
)

data class TitaniaOccupation(val code: String, val name: String, val person: List<TitaniaPerson>)

// also includes ssn, but we cannot use it so just drop it
data class TitaniaPerson(
    val employeeId: String, // optional in the schema, but required for us
    val name: String,
    val actualWorkingTimeEvents: TitaniaWorkingTimeEvents,
    val payrollItems: TitaniaPayrollItems? = null
) {
    fun firstName() = name.indexOf(' ').let { if (it == -1) "" else name.substring(it + 1) }

    fun lastName() = name.indexOf(' ').let { if (it == -1) name else name.substring(0, it) }
}

data class TitaniaWorkingTimeEvents(val event: List<TitaniaWorkingTimeEvent>)

data class TitaniaWorkingTimeEvent(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TITANIA_DATE_FORMAT) val date: LocalDate,
    val code: String? = null,
    val description: String? = null,
    val beginTime: String? = null,
    val endTime: String? = null,
    val administrativeUnit: TitaniaCodeName? = null,
    val placement: TitaniaCodeName? = null,
    val operativeUnit: TitaniaCodeName? = null,
    val project: TitaniaCodeName? = null,
    val eventType: TitaniaCodeName? = null,
    val eventKind: TitaniaCodeName? = null
)

data class TitaniaPayrollItems(val item: List<TitaniaPayrollItem>)

data class TitaniaPayrollItem(
    val code: String,
    val type: TitaniaPayrollItemType,
    val name: String? = null,
    val value: String,
    val unit: TitaniaPayrollItemUnit
)

data class UpdateWorkingTimeEventsResponse(val message: String) {
    companion object {
        fun ok() = UpdateWorkingTimeEventsResponse("OK")
    }
}

data class UpdateWorkingTimeEventsServiceResponse(
    val updateWorkingTimeEventsResponse: UpdateWorkingTimeEventsResponse,
    val createdEmployees: List<EmployeeId>
)

data class GetStampedWorkingTimeEventsRequest(
    val organisation: TitaniaCode? = null,
    val period: TitaniaPeriod,
    val schedulingUnit: List<TitaniaStampedUnitRequest>
)

data class TitaniaStampedUnitRequest(
    val code: String,
    val name: String? = null,
    val person: List<TitaniaStampedPersonRequest>
)

// also includes ssn, but we cannot use it so just drop it
data class TitaniaStampedPersonRequest(
    val employeeId: String, // optional in the schema, but required for us
    val name: String? = null
)

data class GetStampedWorkingTimeEventsResponse(
    val schedulingUnit: List<TitaniaStampedUnitResponse>
)

data class TitaniaStampedUnitResponse(
    val code: String,
    val name: String? = null,
    val person: List<TitaniaStampedPersonResponse>
)

// also includes ssn, but we cannot use it so just drop it
data class TitaniaStampedPersonResponse(
    val employeeId: String, // optional in the schema, but required for us
    val name: String,
    val stampedWorkingTimeEvents: TitaniaStampedWorkingTimeEvents
)

data class TitaniaStampedWorkingTimeEvents(val event: List<TitaniaStampedWorkingTimeEvent>)

data class TitaniaStampedWorkingTimeEvent(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TITANIA_DATE_FORMAT) val date: LocalDate,
    val beginTime: String? = null,
    val beginReasonCode: String? = null,
    val endTime: String? = null,
    val endReasonCode: String? = null
)

data class TitaniaException(val status: HttpStatus, val detail: List<TitaniaErrorDetail>) :
    RuntimeException() {

    constructor(detail: TitaniaErrorDetail) : this(detail.errorcode.status, listOf(detail))

    override val message: String?
        get() = detail.joinToString { it.message }
}

data class TitaniaErrorDetail(val errorcode: TitaniaError, val message: String)

data class TitaniaErrorResponse(
    val faultcode: String = "Server",
    val faultstring: String = "multiple",
    val faultactor: String,
    val detail: List<TitaniaErrorDetail>
)

enum class TitaniaError(val status: HttpStatus) {
    @JsonProperty("102") EVENT_DATE_OUT_OF_PERIOD(HttpStatus.BAD_REQUEST)
}
