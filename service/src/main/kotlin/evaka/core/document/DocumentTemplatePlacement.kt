// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document

import evaka.core.daycare.domain.Language
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate

data class DocumentPlacementInfo(
    val startDate: LocalDate,
    val type: PlacementType,
    val unitLanguage: Language,
    val enabledPilotFeatures: Set<PilotFeature>,
)

fun Database.Read.getCurrentOrNextPlacement(
    childId: ChildId,
    date: LocalDate,
    daysAllowedBeforePlacementStart: Int,
): DocumentPlacementInfo? =
    createQuery {
            sql(
                """
SELECT pl.start_date, pl.type, d.language AS unit_language, d.enabled_pilot_features AS enabled_pilot_features
FROM placement pl
JOIN daycare d on d.id = pl.unit_id
WHERE pl.child_id = ${bind(childId)}
    AND pl.start_date <= ${bind(date.plusDays(daysAllowedBeforePlacementStart.toLong()))}
    AND pl.end_date >= ${bind(date)}
ORDER BY pl.start_date
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull<DocumentPlacementInfo>()

private val PEDAGOGICAL_DOCUMENT_TYPES =
    setOf(
        ChildDocumentType.HOJKS,
        ChildDocumentType.LEOPS,
        ChildDocumentType.MIGRATED_LEOPS,
        ChildDocumentType.MIGRATED_VASU,
        ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
        ChildDocumentType.VASU,
        ChildDocumentType.OTHER,
    )

private fun isPedagogicalDocument(type: ChildDocumentType) = type in PEDAGOGICAL_DOCUMENT_TYPES

fun isAllowedByPilotFeatures(
    type: ChildDocumentType,
    enabledPilotFeatures: Collection<PilotFeature>,
): Boolean =
    (PilotFeature.VASU_AND_PEDADOC in enabledPilotFeatures || !isPedagogicalDocument(type)) &&
        (PilotFeature.OTHER_DECISION in enabledPilotFeatures ||
            type != ChildDocumentType.OTHER_DECISION) &&
        (PilotFeature.CITIZEN_BASIC_DOCUMENT in enabledPilotFeatures ||
            type != ChildDocumentType.CITIZEN_BASIC)

// Allowed templates: same-language, EN CITIZEN_BASIC for any unit, FI templates for EN units.
fun languageMatches(
    templateLanguage: UiLanguage,
    type: ChildDocumentType,
    unitLanguage: Language,
): Boolean =
    templateLanguage.name.equals(unitLanguage.name, ignoreCase = true) ||
        (templateLanguage == UiLanguage.EN && type == ChildDocumentType.CITIZEN_BASIC) ||
        (unitLanguage == Language.en && templateLanguage == UiLanguage.FI)

fun isTemplateApplicableToPlacement(
    type: ChildDocumentType,
    language: UiLanguage,
    placementTypes: Set<PlacementType>,
    placement: DocumentPlacementInfo,
    today: LocalDate,
): Boolean {
    val placementHasStarted = !placement.startDate.isAfter(today)
    return placementTypes.contains(placement.type) &&
        languageMatches(language, type, placement.unitLanguage) &&
        isAllowedByPilotFeatures(type, placement.enabledPilotFeatures) &&
        // Not-yet-started placements qualify only for CITIZEN_BASIC documents
        (type == ChildDocumentType.CITIZEN_BASIC || placementHasStarted)
}
