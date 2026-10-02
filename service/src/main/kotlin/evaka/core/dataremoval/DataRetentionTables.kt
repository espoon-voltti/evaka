// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.DateColumnType.FINITE_DATE_RANGE_END
import evaka.core.dataremoval.DateColumnType.TIMESTAMP_WITH_TIME_ZONE
import evaka.core.dataremoval.DateSource.GraphTableColumn
import evaka.core.dataremoval.DateSource.OwnColumn
import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations.Companion.safeFor
import evaka.core.dataremoval.Handler.ADULT
import evaka.core.dataremoval.Handler.CHILD
import evaka.core.dataremoval.Integration.KOSKI
import evaka.core.dataremoval.Integration.VARDA
import evaka.core.invoicing.domain.FINANCE_FREEZE_PERIOD
import evaka.core.shared.ChildImageId
import evaka.core.shared.DatabaseTable
import evaka.core.shared.async.AsyncJob
import java.time.Period

/** A month past the finance freeze, so that nothing the generator still reads is deleted */
val FINANCE_FREEZE_WITH_MARGIN: Period = FINANCE_FREEZE_PERIOD.plusMonths(1)

/**
 * Some foreign keys into `person` or `child` still cascade without being declared, so a real run
 * would delete their rows unseen. Until they are declared, only dry runs are allowed.
 * `DataRetentionSchemaTest` checks this against its list of those keys.
 */
const val ALL_CASCADING_FOREIGN_KEYS_DECLARED = false

private val oneMonth = Period.ofMonths(1)
private val oneYear = Period.ofYears(1)
private val tenYears = Period.ofYears(10)

fun ageAtLeast(years: Int) =
    After(Period.ofYears(years), GraphTableColumn("person", "date_of_birth"))

private val lastPlacementEnd = GraphTableColumn("placement", "end_date")

private val tenYearsAfterLastPlacement = After(tenYears, lastPlacementEnd)

private val feeDecisionValidityEnd = OwnColumn("valid_during", FINITE_DATE_RANGE_END)

/**
 * The tables handled so far and their expiration rules. The rules are placeholders until the
 * retention periods are decided, and the tables not yet declared are listed in
 * `DataRetentionSchemaTest`.
 */
fun buildDataRetentionSchema(): SchemaDefinition {
    return SchemaDefinition(
        listOf(
            HandledTable(
                name = "person",
                handledBy = ADULT,
                references = emptyList(),
                auditIdType = DatabaseTable.Person::class,
                expirationRule =
                    Coalesce(
                            After(oneYear, OwnColumn("last_login", TIMESTAMP_WITH_TIME_ZONE)),
                            After(oneYear, OwnColumn("created", TIMESTAMP_WITH_TIME_ZONE)),
                        )
                        .safeFor(KOSKI, VARDA),
            ),
            HandledTable(
                name = "child",
                handledBy = CHILD,
                references = listOf(primaryReference("id", referencedTable = "person")),
                auditIdType = DatabaseTable.Person::class,
                expirationRule = ageAtLeast(SAFE_DATA_REMOVAL_AGE.toInt()),
            ),
            HandledTable(
                name = "placement",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        optionalReference(
                            "source_application_id",
                            referencedTable = "application",
                            alsoNull = listOf("source"),
                        ),
                    ),
                bundledBy = "child",
                auditIdType = DatabaseTable.Placement::class,
                expirationRule = After(tenYears, OwnColumn("end_date")).safeFor(KOSKI, VARDA),
            ),
            HandledTable(
                name = "service_need",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("placement_id", referencedTable = "placement")),
                bundledBy = "placement",
                auditIdType = DatabaseTable.ServiceNeed::class,
                expirationRule = Always.safeFor(VARDA),
            ),
            HandledTable(
                name = "daycare_group_placement",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("daycare_placement_id", referencedTable = "placement")),
                bundledBy = "placement",
                auditIdType = DatabaseTable.GroupPlacement::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "backup_care",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.BackupCare::class,
                expirationRule =
                    Coalesce(tenYearsAfterLastPlacement, After(oneYear, OwnColumn("end_date"))),
            ),
            HandledTable(
                name = "application",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.Application::class,
                expirationRule =
                    AllOf(
                            Coalesce(
                                tenYearsAfterLastPlacement,
                                After(tenYears, OwnColumn("sentdate")),
                                After(tenYears, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
                            ),
                            notWhileApplicationPending,
                        )
                        .safeFor(VARDA),
            ),
            HandledTable(
                name = "application_note",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                auditIdType = DatabaseTable.ApplicationNote::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "application_other_guardian",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("application_id", referencedTable = "application"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                bundledBy = "application",
                identifiedByCols = listOf("application_id", "guardian_id"),
                expirationRule = Always,
            ),
            HandledTable(
                name = "placement_plan",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                auditIdType = DatabaseTable.PlacementPlan::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "placement_draft",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                identifiedByCols = listOf("application_id"),
                auditIdType = DatabaseTable.PlacementDraft::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "decision",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                auditIdType = DatabaseTable.Decision::class,
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("sent_date")),
                            After(tenYears, OwnColumn("created", TIMESTAMP_WITH_TIME_ZONE)),
                        )
                        .safeFor(VARDA),
                asyncJobsPlannedOnDelete =
                    AsyncJobsOnDelete(listOf("document_key", "other_guardian_document_key")) { row
                        ->
                        row.valuesByColumn.values.filterNotNull().map {
                            AsyncJob.DeleteDecisionPdf(it)
                        }
                    },
            ),
            HandledTable(
                name = "koski_study_right",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                bundledBy = "child",
                auditIdType = DatabaseTable.KoskiStudyRight::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "koski_upload_error",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                bundledBy = "child",
                auditIdType = DatabaseTable.KoskiUploadError::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "varda_state",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                bundledBy = "child",
                auditIdType = DatabaseTable.VardaState::class,
                expirationRule = Always,
            ),
            HandledTable(
                name = "child_images",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.ChildImage::class,
                expirationRule =
                    Coalesce(
                        After(oneMonth, lastPlacementEnd),
                        After(oneYear, OwnColumn("updated", TIMESTAMP_WITH_TIME_ZONE)),
                    ),
                asyncJobsPlannedOnDelete =
                    AsyncJobsOnDelete(emptyList()) { row ->
                        listOf(AsyncJob.DeleteChildImage(ChildImageId(row.id.single())))
                    },
            ),
            HandledTable(
                name = "child_document",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                independentRows = true,
                auditIdType = DatabaseTable.ChildDocument::class,
                expirationRule =
                    AllOf(
                        Coalesce(
                            After(Period.ZERO, childDocumentRetentionEnd),
                            After(
                                tenYears,
                                OwnColumn("status_modified_at", TIMESTAMP_WITH_TIME_ZONE),
                            ),
                        ),
                        notWhileChildDocumentAwaitingArchival,
                    ),
                orphansToDelete =
                    listOf(
                        OutsideGraphReference(
                            referenceColumn = "decision_id",
                            referencedTable = "child_document_decision",
                            auditIdType = DatabaseTable.ChildDocumentDecision::class,
                        )
                    ),
            ),
            HandledTable(
                name = "child_document_read",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("document_id", referencedTable = "child_document"),
                        secondaryReference("person_id", referencedTable = "person"),
                    ),
                bundledBy = "child_document",
                identifiedByCols = listOf("document_id", "person_id"),
                independentRows = true,
                expirationRule = Always,
            ),
            HandledTable(
                name = "child_document_published_version",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_document_id", referencedTable = "child_document")
                    ),
                bundledBy = "child_document",
                independentRows = true,
                auditIdType = DatabaseTable.ChildDocumentPublishedVersion::class,
                expirationRule = Always,
                asyncJobsPlannedOnDelete =
                    AsyncJobsOnDelete(listOf("document_key")) { row ->
                        listOfNotNull(row.valuesByColumn["document_key"]).map {
                            AsyncJob.DeleteChildDocumentPdf(it)
                        }
                    },
            ),
            HandledTable(
                name = "guardian",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                identifiedByCols = listOf("guardian_id", "child_id"),
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("created", TIMESTAMP_WITH_TIME_ZONE)),
                        )
                        .safeFor(VARDA),
            ),
            HandledTable(
                name = "family_contact",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("contact_person_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.FamilyContact::class,
                expirationRule = Coalesce(After(oneYear, lastPlacementEnd), Always),
            ),
            HandledTable(
                name = "voucher_value_decision",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("head_of_family_id", referencedTable = "person"),
                        secondaryReference("partner_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.VoucherValueDecision::class,
                expirationRule =
                    AllOf(
                            After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("valid_to")),
                            Coalesce(
                                tenYearsAfterLastPlacement,
                                After(tenYears, OwnColumn("valid_to")),
                            ),
                        )
                        .safeFor(VARDA),
                asyncJobsPlannedOnDelete =
                    AsyncJobsOnDelete(listOf("document_key")) { row ->
                        listOfNotNull(row.valuesByColumn["document_key"]).map {
                            AsyncJob.DeleteVoucherValueDecisionPdf(it)
                        }
                    },
            ),
            HandledTable(
                name = "fridge_child",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("head_of_child", referencedTable = "person"),
                        secondaryReference("child_id", referencedTable = "child"),
                        optionalReference(
                            "created_by_application",
                            referencedTable = "application",
                            alsoNull = listOf("create_source"),
                        ),
                    ),
                independentRows = true,
                // Parentship expires once no finance decision it affects
                // can be regenerated: its own period or the placements of the children it affects
                // ended longer ago than the finance freeze, and none of them has a pending
                // application.
                auditIdType = DatabaseTable.Parentship::class,
                expirationRule =
                    AnyOf(
                        After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("end_date")),
                        AllOf(
                            Coalesce(
                                After(FINANCE_FREEZE_WITH_MARGIN, fridgeChildRelevantPlacementEnd),
                                Always,
                            ),
                            notWhileFridgeChildRelevantApplicationPending,
                        ),
                    ),
            ),
            HandledTable(
                name = "fridge_partner",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("person_id", referencedTable = "person"),
                        optionalReference(
                            "created_from_application",
                            referencedTable = "application",
                            alsoNull = listOf("create_source"),
                        ),
                    ),
                identifiedByCols = listOf("partnership_id"),
                independentRows = true,
                // Partnership expires once no finance decision it affects
                // can be regenerated: its own period or the placements of the children it affects
                // ended longer ago than the finance freeze, and none of them has a pending
                // application.
                auditIdType = DatabaseTable.Partnership::class,
                expirationRule =
                    AnyOf(
                        Coalesce(After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("end_date")), Never),
                        AllOf(
                            Coalesce(
                                After(
                                    FINANCE_FREEZE_WITH_MARGIN,
                                    fridgePartnerRelevantPlacementEnd,
                                ),
                                Always,
                            ),
                            notWhileFridgePartnerRelevantApplicationPending,
                        ),
                    ),
            ),
            HandledTable(
                name = "income",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("person_id", referencedTable = "person"),
                        optionalReference("application_id", referencedTable = "application"),
                    ),
                independentRows = true,
                auditIdType = DatabaseTable.Income::class,
                expirationRule = Coalesce(After(tenYears, OwnColumn("valid_to")), Never),
            ),
            HandledTable(
                name = "income_statement",
                handledBy = ADULT,
                references = listOf(primaryReference("person_id", referencedTable = "person")),
                independentRows = true,
                auditIdType = DatabaseTable.IncomeStatement::class,
                expirationRule = After(Period.ZERO, incomeStatementRetentionEnd),
            ),
            HandledTable(
                name = "fee_decision",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("head_of_family_id", referencedTable = "person"),
                        secondaryReference("partner_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.FeeDecision::class,
                expirationRule =
                    AllOf(
                            After(FINANCE_FREEZE_WITH_MARGIN, feeDecisionValidityEnd),
                            Coalesce(
                                After(tenYears, feeDecisionChildrenPlacementEnd),
                                After(tenYears, feeDecisionValidityEnd),
                            ),
                        )
                        .safeFor(VARDA),
                asyncJobsPlannedOnDelete =
                    AsyncJobsOnDelete(listOf("document_key")) { row ->
                        listOfNotNull(row.valuesByColumn["document_key"]).map {
                            AsyncJob.DeleteFeeDecisionPdf(it)
                        }
                    },
            ),
            HandledTable(
                name = "fee_decision_child",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("fee_decision_id", referencedTable = "fee_decision"),
                        secondaryReference("child_id", referencedTable = "child"),
                    ),
                bundledBy = "fee_decision",
                auditIdType = DatabaseTable.FeeDecisionChild::class,
                expirationRule = Always.safeFor(VARDA),
            ),
            ExternalTable(
                name = "message_account",
                references = listOf(secondaryReference("person_id", referencedTable = "person")),
            ),
            ExternalTable(
                name = "message_thread",
                references =
                    listOf(optionalReference("application_id", referencedTable = "application")),
            ),
            ExternalTable(
                name = "voucher_value_report_decision",
                references =
                    listOf(
                        secondaryReference(
                            "decision_id",
                            referencedTable = "voucher_value_decision",
                        )
                    ),
            ),
        )
    )
}

/** Built once: the service uses it for every job */
val dataRetentionSchema: SchemaDefinition = buildDataRetentionSchema()
