package evaka.codegen.apitypes

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.varda.integration.VardaClient
import org.springframework.http.ResponseEntity

const val basePackage = "fi.espoo.evaka"

const val header = """// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */
"""

private val standardTsMapping: Map<String, String> = mapOf(
    "kotlin.String" to "string",
    "kotlin.Int" to "number",
    "kotlin.Long" to "number",
    "kotlin.Double" to "number",
    "java.math.BigDecimal" to "number",
    "kotlin.Boolean" to "boolean",
    "java.time.LocalTime" to "string",
    "java.time.LocalDateTime" to "Date",
    "fi.espoo.evaka.shared.domain.HelsinkiDateTime" to "Date",
    "java.time.Instant" to "Date",
    "java.time.OffsetDateTime" to "Date",
)

private val customClassesMapping: Map<String, TSMapping> = mapOf(
    "java.util.UUID" to TSMapping("UUID", "import { UUID } from '../../types'"),
    "fi.espoo.evaka.shared.Id" to TSMapping("UUID", "import { UUID } from '../../types'"),
    "java.time.LocalDate" to TSMapping("LocalDate", "import LocalDate from '../../local-date'"),
    "fi.espoo.evaka.shared.domain.FiniteDateRange" to TSMapping("FiniteDateRange", "import FiniteDateRange from '../../finite-date-range'"),
    "fi.espoo.evaka.shared.domain.DateRange" to TSMapping("DateRange", "import DateRange from '../../date-range'"),
    "fi.espoo.evaka.dailyservicetimes.DailyServiceTimes" to TSMapping("DailyServiceTimes", "import { DailyServiceTimes } from '../../api-types/child/common'"),
    "fi.espoo.evaka.vasu.VasuQuestion" to TSMapping("VasuQuestion", "import { VasuQuestion } from '../../api-types/vasu'"),
    "fi.espoo.evaka.invoicing.domain.DecisionIncome" to TSMapping("DecisionIncome", "import { DecisionIncome } from '../../api-types/income'"),
    "fi.espoo.evaka.invoicing.service.ProductKey" to TSMapping("string")
)

private val actionsMapping: Map<String, TSMapping> = Action::class.nestedClasses.associate { action ->
    action.qualifiedName!! to TSMapping("Action.${action.simpleName}", "import { Action } from '../action'")
}

val tsMapping: Map<String, TSMapping> = standardTsMapping.mapValues { TSMapping(it.value) } + customClassesMapping + actionsMapping

data class TSMapping(
    val type: String,
    val import: String? = null
)

val kotlinCollectionClasses = listOf(
    Collection::class, Array::class, IntArray::class, DoubleArray::class, BooleanArray::class, Map::class
)

val classesToUnwrap = kotlinCollectionClasses + listOf(
    ResponseEntity::class,
    Paged::class,
    VardaClient.PaginatedResponse::class,
    Wrapper::class
)
