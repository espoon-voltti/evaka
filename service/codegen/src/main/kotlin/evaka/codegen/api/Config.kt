// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import evaka.codegen.api.TsProject.LibCommon
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.messaging.MessageReceiver
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.vasu.CurriculumTemplateError
import fi.espoo.evaka.vasu.VasuQuestion
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KType
import kotlin.reflect.typeOf

const val basePackage = "fi.espoo.evaka"

val forceIncludes: Set<KType> =
    setOf(
        typeOf<CurriculumTemplateError>(),
        typeOf<IncomeCoefficient>(),
        typeOf<VoucherValueDecision>()
    )

object Imports {
    val localDate = TsImport.Default(LibCommon / "local-date.ts", "LocalDate")
    val localTime = TsImport.Default(LibCommon / "local-time.ts", "LocalTime")
    val helsinkiDateTime =
        TsImport.Default(LibCommon / "helsinki-date-time.ts", name = "HelsinkiDateTime")
    val finiteDateRange = TsImport.Default(LibCommon / "finite-date-range.ts", "FiniteDateRange")
    val dateRange = TsImport.Default(LibCommon / "date-range.ts", "DateRange")
    val vasuQuestion = TsImport.Named(LibCommon / "api-types/vasu.ts", "VasuQuestion")
    val mapVasuQuestion = TsImport.Named(LibCommon / "api-types/vasu.ts", "mapVasuQuestion")
    val messageReceiver = TsImport.Named(LibCommon / "api-types/messaging.ts", "MessageReceiver")
    val uuid = TsImport.Named(LibCommon / "types.d.ts", "UUID")
    val action = TsImport.Named(LibCommon / "generated/action.ts", "Action")
    val jsonOf = TsImport.Named(LibCommon / "json.d.ts", "JsonOf")
}

val defaultMetadata =
    TypeMetadata(
        Byte::class to TsPlain(type = "number"),
        Int::class to TsPlain(type = "number"),
        Double::class to TsPlain(type = "number"),
        Long::class to TsPlain(type = "number"),
        BigDecimal::class to TsPlain(type = "number"),
        Boolean::class to TsPlain(type = "boolean"),
        String::class to TsPlain(type = "string"),
        ProductKey::class to TsPlain(type = "string"),
        URI::class to TsPlain(type = "string"),
        Any::class to TsPlain(type = "unknown"),
        LocalDate::class to
            TsExternalTypeRef(
                "LocalDate",
                keyRepresentation = TsCode("string"),
                jsonDeserializeExpression = { json: String ->
                    TsCode("LocalDate.parseIso($json)", Imports.localDate)
                },
                Imports.localDate
            ),
        LocalTime::class to
            TsExternalTypeRef(
                "LocalTime",
                keyRepresentation = TsCode("string"),
                jsonDeserializeExpression = { json: String ->
                    TsCode("LocalTime.parseIso($json)", Imports.localTime)
                },
                Imports.localTime
            ),
        HelsinkiDateTime::class to
            TsExternalTypeRef(
                "HelsinkiDateTime",
                keyRepresentation = null,
                jsonDeserializeExpression = { json: String ->
                    TsCode("HelsinkiDateTime.parseIso($json)", Imports.helsinkiDateTime)
                },
                Imports.helsinkiDateTime
            ),
        FiniteDateRange::class to
            TsExternalTypeRef(
                "FiniteDateRange",
                keyRepresentation = null,
                jsonDeserializeExpression = { json: String ->
                    TsCode("FiniteDateRange.parseJson($json)", Imports.finiteDateRange)
                },
                Imports.finiteDateRange
            ),
        DateRange::class to
            TsExternalTypeRef(
                "DateRange",
                keyRepresentation = null,
                jsonDeserializeExpression = { json: String ->
                    TsCode("DateRange.parseJson($json)", Imports.dateRange)
                },
                Imports.dateRange
            ),
        DateSet::class to
            TsExternalTypeRef(
                "FiniteDateRange[]",
                keyRepresentation = null,
                jsonDeserializeExpression = { json: String ->
                    TsCode(
                        "$json.map((x) => FiniteDateRange.parseJson(x))",
                        Imports.finiteDateRange
                    )
                },
                Imports.finiteDateRange
            ),
        VasuQuestion::class to
            TsExternalTypeRef(
                "VasuQuestion",
                keyRepresentation = null,
                jsonDeserializeExpression = { json: String ->
                    TsCode("mapVasuQuestion($json)", Imports.mapVasuQuestion)
                },
                Imports.vasuQuestion
            ),
        MessageReceiver::class to
            TsExternalTypeRef(
                "MessageReceiver",
                keyRepresentation = null,
                jsonDeserializeExpression = null,
                Imports.messageReceiver
            ),
        UUID::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                jsonDeserializeExpression = null,
                Imports.uuid
            ),
        Id::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                jsonDeserializeExpression = null,
                Imports.uuid
            ),
        List::class to TsArray,
        Set::class to TsArray,
        Map::class to TsRecord,
        Void::class to Excluded,
        YearMonth::class to Excluded
    ) +
        TypeMetadata(
            Action::class.nestedClasses.associateWith { action ->
                TsExternalTypeRef(
                    "Action.${action.simpleName}",
                    keyRepresentation = TsCode("string"),
                    jsonDeserializeExpression = null,
                    Imports.action
                )
            } +
                Action.Citizen::class.nestedClasses.associateWith { action ->
                    TsExternalTypeRef(
                        "Action.Citizen.${action.simpleName}",
                        keyRepresentation = TsCode("string"),
                        jsonDeserializeExpression = null,
                        Imports.action
                    )
                }
        )
