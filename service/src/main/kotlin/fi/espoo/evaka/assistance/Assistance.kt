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
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class AssistanceFactor(
    val id: AssistanceFactorId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val capacityFactor: Double,
    val modified: HelsinkiDateTime,
    val modifiedBy: String,
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
    val modifiedBy: String,
)

data class DaycareAssistanceUpdate(
    val validDuring: FiniteDateRange,
    val level: DaycareAssistanceLevel,
)

@ConstList("preschoolAssistanceLevels")
enum class PreschoolAssistanceLevel : DatabaseEnum {
    INTENSIFIED_SUPPORT,
    SPECIAL_SUPPORT,
    SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1,
    SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2;

    override val sqlType: String = "preschool_assistance_level"
}

data class PreschoolAssistance(
    val id: PreschoolAssistanceId,
    val childId: ChildId,
    val validDuring: FiniteDateRange,
    val level: PreschoolAssistanceLevel,
    val modified: HelsinkiDateTime,
    val modifiedBy: String,
)

data class PreschoolAssistanceUpdate(
    val validDuring: FiniteDateRange,
    val level: PreschoolAssistanceLevel,
)

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
    val modifiedBy: String,
)

data class OtherAssistanceMeasureUpdate(
    val validDuring: FiniteDateRange,
    val type: OtherAssistanceMeasureType,
)
