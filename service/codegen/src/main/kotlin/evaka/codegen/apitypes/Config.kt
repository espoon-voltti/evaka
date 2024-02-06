// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.vasu.CurriculumTemplateError
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

const val basePackage = "fi.espoo.evaka"

val forceIncludes: Set<KClass<*>> =
    setOf(CurriculumTemplateError::class, IncomeCoefficient::class, VoucherValueDecision::class)

private val standardTsMapping: Map<String, String> =
    mapOf(
        "kotlin.String" to "string",
        "kotlin.Byte" to "number",
        "kotlin.Int" to "number",
        "kotlin.Long" to "number",
        "kotlin.Double" to "number",
        "java.math.BigDecimal" to "number",
        "kotlin.Boolean" to "boolean",
        "java.time.OffsetDateTime" to "Date",
        "java.net.URI" to "string",
        "kotlin.Any" to "unknown",
        "java.lang.Void" to "never"
    )

private val customClassesMapping: Map<String, TSMapping> =
    mapOf(
        "java.util.UUID" to TSMapping("UUID", "import { UUID } from '../../types'"),
        "fi.espoo.evaka.shared.Id" to TSMapping("UUID", "import { UUID } from '../../types'"),
        "java.time.LocalDate" to TSMapping("LocalDate", "import LocalDate from '../../local-date'"),
        "java.time.LocalTime" to TSMapping("LocalTime", "import LocalTime from '../../local-time'"),
        "fi.espoo.evaka.shared.domain.HelsinkiDateTime" to
            TSMapping(
                "HelsinkiDateTime",
                "import HelsinkiDateTime from '../../helsinki-date-time'"
            ),
        "fi.espoo.evaka.shared.domain.FiniteDateRange" to
            TSMapping("FiniteDateRange", "import FiniteDateRange from '../../finite-date-range'"),
        "fi.espoo.evaka.shared.domain.DateRange" to
            TSMapping("DateRange", "import DateRange from '../../date-range'"),
        "fi.espoo.evaka.shared.data.DateSet" to
            TSMapping("FiniteDateRange[]", "import FiniteDateRange from '../../finite-date-range'"),
        "fi.espoo.evaka.shared.domain.LocalHm" to
            TSMapping("LocalHm", "import LocalHm from '../../local-hm'"),
        "fi.espoo.evaka.shared.domain.LocalHmRange" to
            TSMapping("LocalHmRange", "import LocalHmRange from '../../local-hm-range'"),
        "fi.espoo.evaka.vasu.VasuQuestion" to
            TSMapping("VasuQuestion", "import { VasuQuestion } from '../../api-types/vasu'"),
        "fi.espoo.evaka.messaging.MessageReceiver" to
            TSMapping(
                "MessageReceiver",
                "import { MessageReceiver } from '../../api-types/messaging'"
            ),
        "fi.espoo.evaka.invoicing.service.ProductKey" to TSMapping("string")
    )

private val actionsMapping: Map<String, TSMapping> =
    Action::class.nestedClasses.associate { action ->
        action.qualifiedName!! to
            TSMapping("Action.${action.simpleName}", "import { Action } from '../action'")
    } +
        Action.Citizen::class.nestedClasses.associate { action ->
            action.qualifiedName!! to
                TSMapping(
                    "Action.Citizen.${action.simpleName}",
                    "import { Action } from '../action'"
                )
        }

val tsMapping: Map<String, TSMapping> =
    standardTsMapping.mapValues { TSMapping(it.value) } + customClassesMapping + actionsMapping

data class TSMapping(val type: String, val import: String? = null)

val kotlinCollectionClasses =
    listOf(
        Collection::class,
        Array::class,
        IntArray::class,
        DoubleArray::class,
        BooleanArray::class
    )

val validMapKeyTypes = listOf(String::class, UUID::class, Id::class, LocalDate::class)
