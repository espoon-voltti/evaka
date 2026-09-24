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
import evaka.core.shared.ChildImageId
import evaka.core.shared.async.AsyncJob
import java.time.Period

/**
 * Finance decisions are regenerated for any period, so the family and income data they are based on
 * cannot be deleted before a freeze of old decisions exists
 */
const val FINANCE_FREEZE_IMPLEMENTED = false

/**
 * Once the freeze exists, the finance decisions of a period older than this are never regenerated
 */
val FINANCE_FREEZE: Period = Period.ofYears(5)

private val oneYear = Period.ofYears(1)
private val fiveYears = Period.ofYears(5)
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
fun buildDataRetentionSchema(
    financeFreezeImplemented: Boolean = FINANCE_FREEZE_IMPLEMENTED
): SchemaDefinition {
    val financeFreezeExists = if (financeFreezeImplemented) Always else Never
    return SchemaDefinition(
        listOf(
            HandledTable(
                name = "person",
                handledBy = ADULT,
                references = emptyList(),
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
                expirationRule = After(tenYears, OwnColumn("end_date")).safeFor(KOSKI, VARDA),
            ),
            HandledTable(
                name = "service_need",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("placement_id", referencedTable = "placement")),
                bundledBy = "placement",
                expirationRule = Always.safeFor(VARDA),
            ),
            HandledTable(
                name = "daycare_group_placement",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("daycare_placement_id", referencedTable = "placement")),
                bundledBy = "placement",
                expirationRule = Always,
            ),
            HandledTable(
                name = "backup_care",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                expirationRule =
                    Coalesce(tenYearsAfterLastPlacement, After(tenYears, OwnColumn("end_date"))),
            ),
            HandledTable(
                name = "application",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(fiveYears, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
                        )
                        .safeFor(VARDA),
            ),
            HandledTable(
                name = "application_note",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
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
                expirationRule = Always,
            ),
            HandledTable(
                name = "placement_draft",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                identifiedByCols = listOf("application_id"),
                expirationRule = Always,
            ),
            HandledTable(
                name = "decision",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                bundledBy = "application",
                expirationRule = Always.safeFor(VARDA),
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
                expirationRule = Always,
            ),
            HandledTable(
                name = "koski_upload_error",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                bundledBy = "child",
                expirationRule = Always,
            ),
            HandledTable(
                name = "varda_state",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                bundledBy = "child",
                expirationRule = Always,
            ),
            HandledTable(
                name = "child_images",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                expirationRule =
                    Coalesce(
                        After(oneYear, lastPlacementEnd),
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
                expirationRule =
                    AllOf(
                        Coalesce(
                            After(Period.ZERO, childDocumentRetentionEnd),
                            After(
                                tenYears,
                                OwnColumn("status_modified_at", TIMESTAMP_WITH_TIME_ZONE),
                            ),
                        ),
                        childDocumentArchivedIfRequired,
                    ),
                orphansToDelete =
                    listOf(
                        OutsideGraphReference(
                            referenceColumn = "decision_id",
                            referencedTable = "child_document_decision",
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
                expirationRule = Coalesce(After(oneYear, lastPlacementEnd), Never),
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
                expirationRule =
                    AllOf(
                            financeFreezeExists,
                            After(FINANCE_FREEZE, OwnColumn("valid_to")),
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
                expirationRule =
                    AllOf(
                        financeFreezeExists,
                        AnyOf(
                            After(FINANCE_FREEZE, OwnColumn("end_date")),
                            // Five years after the last placement of the children the parentship
                            // affects, or a year after the row was last modified or created when
                            // they have none
                            Coalesce(
                                After(fiveYears, fridgeChildRelevantPlacementEnd),
                                After(oneYear, OwnColumn("modified_at", TIMESTAMP_WITH_TIME_ZONE)),
                                After(oneYear, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
                            ),
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
                expirationRule =
                    AllOf(
                        financeFreezeExists,
                        AnyOf(
                            Coalesce(After(FINANCE_FREEZE, OwnColumn("end_date")), Never),
                            // Five years after the last placement of the children of either
                            // partner during the partnership, or a year after the row was last
                            // modified or created when they have none
                            Coalesce(
                                After(fiveYears, fridgePartnerRelevantPlacementEnd),
                                After(oneYear, OwnColumn("modified_at", TIMESTAMP_WITH_TIME_ZONE)),
                                After(oneYear, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
                            ),
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
                expirationRule =
                    AllOf(
                        financeFreezeExists,
                        Coalesce(After(tenYears, OwnColumn("valid_to")), Never),
                    ),
            ),
            HandledTable(
                name = "income_statement",
                handledBy = ADULT,
                references = listOf(primaryReference("person_id", referencedTable = "person")),
                independentRows = true,
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
                expirationRule =
                    AllOf(
                            financeFreezeExists,
                            After(FINANCE_FREEZE, feeDecisionValidityEnd),
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
