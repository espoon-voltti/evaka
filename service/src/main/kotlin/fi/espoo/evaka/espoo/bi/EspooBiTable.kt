// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

enum class EspooBiTable(val fileName: String, val query: CsvQuery) {
    Absence("absences", EspooBi.getAbsences),
    Application("applications", EspooBi.getApplications),
    Area("areas", EspooBi.getAreas),
    AssistanceAction("assistance_actions", EspooBi.getAssistanceActions),
    AssistanceActionOptionRef(
        "assistance_action_option_refs",
        EspooBi.getAssistanceActionOptionRefs,
    ),
    AssistanceFactor("assistance_factors", EspooBi.getAssistanceFactors),
    AssistanceNeedDaycareDecision(
        "assistance_need_daycare_decisions",
        EspooBi.getAssistanceNeedDaycareDecisions,
    ),
    AssistanceNeedPreschoolDecision(
        "assistance_need_preschool_decisions",
        EspooBi.getAssistanceNeedPreschoolDecisions,
    ),
    AssistanceNeedVoucherCoefficient(
        "assistance_need_voucher_coefficients",
        EspooBi.getAssistanceNeedVoucherCoefficients,
    ),
    Child("children", EspooBi.getChildren),
    CurriculumDocument("curriculum_documents", EspooBi.getCurriculumDocuments),
    CurriculumTemplate("curriculum_template", EspooBi.getCurriculumTemplates),
    DaycareAssistanceEntry("daycare_assistance_entries", EspooBi.getDaycareAssistanceEntries),
    Decision("decisions", EspooBi.getDecisions),
    FeeDecisionChild("fee_decision_children", EspooBi.getFeeDecisionChildren),
    FeeDecision("fee_decision", EspooBi.getFeeDecisions),
    GroupCaretakerAllocation("group_caretaker_allocations", EspooBi.getGroupCaretakerAllocations),
    Group("groups", EspooBi.getGroups),
    GroupPlacement("group_placements", EspooBi.getGroupPlacements),
    OtherAssistanceMeasureEntry(
        "other_assistance_measure_entries",
        EspooBi.getOtherAssistanceMeasureEntries,
    ),
    PedagogicalDocument("pedagogical_documents", EspooBi.getPedagogicalDocuments),
    Placement("placements", EspooBi.getPlacements),
    PreschoolAssistanceEntry("preschool_assistance_entries", EspooBi.getPreschoolAssistanceEntries),
    ServiceNeedOption("service_need_options", EspooBi.getServiceNeedOptions),
    ServiceNeed("service_needs", EspooBi.getServiceNeeds),
    Unit("units", EspooBi.getUnits),
    VoucherValueDecision("voucher_value_decisions", EspooBi.getVoucherValueDecisions),
}
