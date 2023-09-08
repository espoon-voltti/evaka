// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

enum class EspooBiTable(val fileName: String, val query: CsvQuery) {
    Absence("absences", EspooBi.getAbsences),
    Application("applications", EspooBi.getApplications),
    Area("areas", EspooBi.getAreas),
    Child("children", EspooBi.getChildren),
    CurriculumDocument("curriculum_documents", EspooBi.getCurriculumDocuments),
    CurriculumTemplate("curriculum_template", EspooBi.getCurriculumTemplates),
    Decision("decisions", EspooBi.getDecisions),
    FeeDecision("fee_decision", EspooBi.getFeeDecisions),
    FeeDecisionChild("fee_decision_children", EspooBi.getFeeDecisionChildren),
    Group("groups", EspooBi.getGroups),
    GroupCaretakerAllocation("group_caretaker_allocations", EspooBi.getGroupCaretakerAllocations),
    GroupPlacement("group_placements", EspooBi.getGroupPlacements),
    PedagogicalDocument("pedagogical_documents", EspooBi.getPedagogicalDocuments),
    Placement("placements", EspooBi.getPlacements),
    ServiceNeed("service_needs", EspooBi.getServiceNeeds),
    ServiceNeedOption("service_need_options", EspooBi.getServiceNeedOptions),
    Unit("units", EspooBi.getUnits),
    VoucherValueDecision("voucher_value_decisions", EspooBi.getVoucherValueDecisions)
}
