// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.maxEndDate
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

data class AssistanceFactor(
    val id: AssistanceFactorId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val capacityFactor: Double,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class AssistanceFactorUpdate(val validDuring: FiniteDateRange, val capacityFactor: Double)

@ConstList("daycareAssistanceLevels")
enum class DaycareAssistanceLevel : DatabaseEnum {
    GENERAL_SUPPORT,
    GENERAL_SUPPORT_WITH_DECISION,
    INTENSIFIED_SUPPORT,
    SPECIAL_SUPPORT;

    override val sqlType: String = "daycare_assistance_level"
}

data class DaycareAssistance(
    val id: DaycareAssistanceId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val level: DaycareAssistanceLevel,
    val modified: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class DaycareAssistanceUpdate(
    val validDuring: FiniteDateRange,
    val level: DaycareAssistanceLevel,
)

@ConstList("preschoolAssistanceLevels")
enum class PreschoolAssistanceLevel(
    val minStartDate: LocalDate? = null,
    val maxEndDate: LocalDate? = null,
) : DatabaseEnum {
    // deprecated
    INTENSIFIED_SUPPORT(maxEndDate = LocalDate.of(2025, 7, 31)),
    // deprecated
    SPECIAL_SUPPORT(maxEndDate = LocalDate.of(2025, 7, 31)),
    // deprecated
    SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1(maxEndDate = LocalDate.of(2026, 7, 31)),
    // deprecated
    SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2(maxEndDate = LocalDate.of(2026, 7, 31)),
    CHILD_SUPPORT(minStartDate = LocalDate.of(2025, 8, 1)),
    CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION(minStartDate = LocalDate.of(2026, 8, 1)),
    // deprecated, only available during the transition year
    CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION(
        minStartDate = LocalDate.of(2025, 8, 1),
        maxEndDate = LocalDate.of(2026, 7, 31),
    ),
    GROUP_SUPPORT(minStartDate = LocalDate.of(2025, 8, 1));

    override val sqlType: String = "preschool_assistance_level"
}

data class PreschoolAssistance(
    val id: PreschoolAssistanceId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val level: PreschoolAssistanceLevel,
    val modified: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class PreschoolAssistanceUpdate(
    val validDuring: FiniteDateRange,
    val level: PreschoolAssistanceLevel,
) {
    fun validate() {
        if (level.minStartDate != null && validDuring.start < level.minStartDate)
            throw BadRequest(
                "Preschool assistance level $level cannot start before ${level.minStartDate}"
            )
        if (level.maxEndDate != null && validDuring.end > level.maxEndDate)
            throw BadRequest(
                "Preschool assistance level $level cannot end after ${level.maxEndDate}"
            )
    }
}

@ConstList("otherAssistanceMeasureTypes")
enum class OtherAssistanceMeasureType : DatabaseEnum {
    TRANSPORT_BENEFIT,
    ACCULTURATION_SUPPORT,
    ANOMALOUS_EDUCATION_START,
    CHILD_DISCUSSION_OFFERED,
    CHILD_DISCUSSION_HELD,
    CHILD_DISCUSSION_COUNSELING;

    override val sqlType: String = "other_assistance_measure_type"
}

data class OtherAssistanceMeasure(
    val id: OtherAssistanceMeasureId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val type: OtherAssistanceMeasureType,
    val modified: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class OtherAssistanceMeasureUpdate(
    val validDuring: FiniteDateRange,
    val type: OtherAssistanceMeasureType,
)
