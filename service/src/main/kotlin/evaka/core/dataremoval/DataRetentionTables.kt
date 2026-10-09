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

private val oneMonth = Period.ofMonths(1)
private val twoMonths = Period.ofMonths(2)
private val sixMonths = Period.ofMonths(6)
private val oneYear = Period.ofYears(1)
private val fiveYears = Period.ofYears(5)
private val tenYears = Period.ofYears(10)

private val lastPlacementEnd = GraphTableColumn("placement", "end_date")

private val oneYearAfterLastPlacement = After(oneYear, lastPlacementEnd)
private val fiveYearsAfterLastPlacement = After(fiveYears, lastPlacementEnd)
private val tenYearsAfterLastPlacement = After(tenYears, lastPlacementEnd)

/**
 * Ten years after the child's last placement, or after the end of the rows' period when the child
 * has no placements, and never before the finance freeze has passed the end of the rows' period
 */
private fun childFinanceRule(periodEnd: DateSource) =
    AllOf(
        After(FINANCE_FREEZE_WITH_MARGIN, periodEnd),
        Coalesce(After(tenYears, lastPlacementEnd), After(tenYears, periodEnd)),
    )

/**
 * Ten years after the last placement of the children of each row, or after the end of the row's
 * period when none of them has placements, and never before the finance freeze has passed the end
 * of the rows' period
 */
private fun headOfFamilyFinanceRule(
    periodEnd: DateSource,
    childrenPlacementOrPeriodEnd: DateSource,
) =
    AllOf(
        After(FINANCE_FREEZE_WITH_MARGIN, periodEnd),
        After(tenYears, childrenPlacementOrPeriodEnd),
    )

/** The tables whose rows the algorithm deletes or waits for, and their expiration rules */
fun buildDataRetentionSchema(valueDecisionCapacityFactorEnabled: Boolean): SchemaDefinition {
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
                expirationRule =
                    After(
                            sixMonths,
                            GraphTableColumn("person", "created", TIMESTAMP_WITH_TIME_ZONE),
                        )
                        .safeFor(KOSKI, VARDA),
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
                        optionalReference(
                            "source_service_application_id",
                            referencedTable = "service_application",
                            alsoNull = listOf("source"),
                        ),
                    ),
                auditIdType = DatabaseTable.Placement::class,
                expirationRule =
                    AllOf(
                            After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("end_date")),
                            After(tenYears, OwnColumn("end_date")),
                        )
                        .safeFor(KOSKI, VARDA),
                bundledBy = "child",
            ),
            HandledTable(
                name = "service_need",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("placement_id", referencedTable = "placement")),
                auditIdType = DatabaseTable.ServiceNeed::class,
                expirationRule = Always.safeFor(VARDA),
                bundledBy = "placement",
            ),
            HandledTable(
                name = "daycare_group_placement",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("daycare_placement_id", referencedTable = "placement")),
                auditIdType = DatabaseTable.GroupPlacement::class,
                expirationRule = Always,
                bundledBy = "placement",
            ),
            HandledTable(
                name = "backup_care",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.BackupCare::class,
                expirationRule = Always,
                bundledBy = "child",
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
                            Coalesce(tenYearsAfterLastPlacement, Always),
                            Coalesce(
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
                auditIdType = DatabaseTable.ApplicationNote::class,
                expirationRule = Always,
                bundledBy = "application",
            ),
            HandledTable(
                name = "application_other_guardian",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("application_id", referencedTable = "application"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                identifiedByCols = listOf("application_id", "guardian_id"),
                expirationRule = Always,
                bundledBy = "application",
            ),
            HandledTable(
                name = "placement_plan",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                auditIdType = DatabaseTable.PlacementPlan::class,
                expirationRule = Always,
                bundledBy = "application",
            ),
            HandledTable(
                name = "placement_draft",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                identifiedByCols = listOf("application_id"),
                auditIdType = DatabaseTable.PlacementDraft::class,
                expirationRule = Always,
                bundledBy = "application",
            ),
            HandledTable(
                name = "decision",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("application_id", referencedTable = "application")),
                auditIdType = DatabaseTable.Decision::class,
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("sent_date")),
                            Always,
                        )
                        .safeFor(VARDA),
                bundledBy = "application",
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
                auditIdType = DatabaseTable.KoskiStudyRight::class,
                expirationRule = Always,
                bundledBy = "child",
            ),
            HandledTable(
                name = "koski_upload_error",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.KoskiUploadError::class,
                expirationRule = Always,
                bundledBy = "child",
            ),
            HandledTable(
                name = "varda_state",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.VardaState::class,
                expirationRule = Always,
                bundledBy = "child",
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
                identifiedByCols = listOf("document_id", "person_id"),
                independentRows = true,
                expirationRule = Always,
                bundledBy = "child_document",
            ),
            HandledTable(
                name = "child_document_published_version",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_document_id", referencedTable = "child_document")
                    ),
                independentRows = true,
                auditIdType = DatabaseTable.ChildDocumentPublishedVersion::class,
                expirationRule = Always,
                bundledBy = "child_document",
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
                expirationRule = Always.safeFor(VARDA),
                bundledBy = "child",
            ),
            HandledTable(
                name = "foster_parent",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("parent_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.FosterParent::class,
                expirationRule = Always,
                bundledBy = "child",
            ),
            HandledTable(
                name = "guardian_blocklist",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("guardian_id", referencedTable = "person"),
                    ),
                identifiedByCols = listOf("guardian_id", "child_id"),
                expirationRule = Always,
                bundledBy = "child",
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
                expirationRule = Coalesce(oneYearAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "absence",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.Absence::class,
                expirationRule =
                    AllOf(
                            After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("date")),
                            Coalesce(
                                fiveYearsAfterLastPlacement,
                                After(fiveYears, OwnColumn("date")),
                            ),
                        )
                        .safeFor(KOSKI),
            ),
            HandledTable(
                name = "absence_application",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.AbsenceApplication::class,
                expirationRule =
                    Coalesce(fiveYearsAfterLastPlacement, After(oneYear, OwnColumn("start_date"))),
            ),
            HandledTable(
                name = "child_attendance",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.ChildAttendance::class,
                expirationRule =
                    Coalesce(tenYearsAfterLastPlacement, After(tenYears, OwnColumn("date"))),
            ),
            HandledTable(
                name = "attendance_reservation",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.AttendanceReservation::class,
                expirationRule =
                    Coalesce(tenYearsAfterLastPlacement, After(tenYears, OwnColumn("date"))),
            ),
            HandledTable(
                name = "daily_service_time",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.DailyServiceTime::class,
                expirationRule = Coalesce(tenYearsAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "holiday_questionnaire_answer",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.HolidayQuestionnaireAnswer::class,
                expirationRule = Coalesce(tenYearsAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "backup_pickup",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.BackupPickup::class,
                expirationRule = Coalesce(oneYearAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "child_daily_note",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.ChildDailyNote::class,
                // In practice the scheduled job removeExpiredNotes deletes a note 14 hours after
                // its last change.
                expirationRule =
                    After(Period.ofDays(1), OwnColumn("modified_at", TIMESTAMP_WITH_TIME_ZONE)),
            ),
            HandledTable(
                name = "child_sticky_note",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.ChildStickyNote::class,
                expirationRule = Coalesce(oneYearAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "nekku_special_diet_choices",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.NekkuSpecialDietChoice::class,
                expirationRule = Coalesce(oneYearAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "calendar_event_time",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.CalendarEventTime::class,
                expirationRule = Coalesce(oneYearAfterLastPlacement, Always),
            ),
            HandledTable(
                name = "assistance_factor",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.AssistanceFactor::class,
                expirationRule =
                    if (valueDecisionCapacityFactorEnabled)
                        childFinanceRule(OwnColumn("valid_during", FINITE_DATE_RANGE_END))
                    else
                        Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("valid_during", FINITE_DATE_RANGE_END)),
                        ),
            ),
            HandledTable(
                name = "daycare_assistance",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.DaycareAssistance::class,
                expirationRule =
                    Coalesce(
                        tenYearsAfterLastPlacement,
                        After(tenYears, OwnColumn("valid_during", FINITE_DATE_RANGE_END)),
                    ),
            ),
            HandledTable(
                name = "preschool_assistance",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.PreschoolAssistance::class,
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("valid_during", FINITE_DATE_RANGE_END)),
                        )
                        .safeFor(KOSKI),
            ),
            HandledTable(
                name = "other_assistance_measure",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.OtherAssistanceMeasure::class,
                expirationRule =
                    Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("valid_during", FINITE_DATE_RANGE_END)),
                        )
                        .safeFor(KOSKI),
            ),
            HandledTable(
                name = "assistance_action",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.AssistanceAction::class,
                expirationRule =
                    Coalesce(tenYearsAfterLastPlacement, After(tenYears, OwnColumn("end_date"))),
            ),
            HandledTable(
                name = "assistance_action_option_ref",
                handledBy = CHILD,
                references =
                    listOf(primaryReference("action_id", referencedTable = "assistance_action")),
                identifiedByCols = listOf("action_id", "option_id"),
                expirationRule = Always,
                bundledBy = "assistance_action",
            ),
            HandledTable(
                name = "assistance_need_voucher_coefficient",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.AssistanceNeedVoucherCoefficient::class,
                expirationRule =
                    if (valueDecisionCapacityFactorEnabled)
                        Coalesce(
                            tenYearsAfterLastPlacement,
                            After(tenYears, OwnColumn("validity_period", FINITE_DATE_RANGE_END)),
                        )
                    else childFinanceRule(OwnColumn("validity_period", FINITE_DATE_RANGE_END)),
            ),
            HandledTable(
                name = "pedagogical_document",
                handledBy = CHILD,
                references = listOf(primaryReference("child_id", referencedTable = "child")),
                auditIdType = DatabaseTable.PedagogicalDocument::class,
                expirationRule =
                    Coalesce(
                        tenYearsAfterLastPlacement,
                        After(tenYears, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
                    ),
            ),
            HandledTable(
                name = "pedagogical_document_read",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference(
                            "pedagogical_document_id",
                            referencedTable = "pedagogical_document",
                        ),
                        secondaryReference("person_id", referencedTable = "person"),
                    ),
                identifiedByCols = listOf("pedagogical_document_id", "person_id"),
                expirationRule = Always,
                bundledBy = "pedagogical_document",
            ),
            HandledTable(
                name = "service_application",
                handledBy = CHILD,
                references =
                    listOf(
                        primaryReference("child_id", referencedTable = "child"),
                        secondaryReference("person_id", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.ServiceApplication::class,
                expirationRule =
                    Coalesce(fiveYearsAfterLastPlacement, After(oneYear, OwnColumn("start_date"))),
            ),
            HandledTable(
                name = "fee_alteration",
                handledBy = CHILD,
                references = listOf(primaryReference("person_id", referencedTable = "child")),
                auditIdType = DatabaseTable.FeeAlteration::class,
                expirationRule =
                    AllOf(
                        Coalesce(
                            After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("valid_to")),
                            After(FINANCE_FREEZE_WITH_MARGIN, lastPlacementEnd),
                            Always,
                        ),
                        Coalesce(
                            After(tenYears, OwnColumn("valid_to")),
                            After(tenYears, OwnColumn("valid_from")),
                        ),
                        Coalesce(tenYearsAfterLastPlacement, Always),
                    ),
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
                expirationRule = childFinanceRule(OwnColumn("valid_to")).safeFor(VARDA),
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
                expirationRule =
                    AllOf(
                        Coalesce(
                            After(FINANCE_FREEZE_WITH_MARGIN, OwnColumn("valid_to")),
                            After(FINANCE_FREEZE_WITH_MARGIN, incomePersonLastFinanceDecisionEnd),
                            Always,
                        ),
                        Coalesce(
                            After(tenYears, OwnColumn("valid_to")),
                            AllOf(
                                After(tenYears, OwnColumn("valid_from")),
                                Coalesce(
                                    After(tenYears, incomePersonLastFinanceDecisionEnd),
                                    Always,
                                ),
                            ),
                        ),
                    ),
            ),
            HandledTable(
                name = "income_statement",
                handledBy = ADULT,
                references = listOf(primaryReference("person_id", referencedTable = "person")),
                independentRows = true,
                auditIdType = DatabaseTable.IncomeStatement::class,
                expirationRule =
                    AllOf(
                        After(Period.ZERO, incomeStatementRetentionEnd),
                        notWhileIncomeStatementAwaitingHandling,
                    ),
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
                    headOfFamilyFinanceRule(
                            OwnColumn("valid_during", FINITE_DATE_RANGE_END),
                            feeDecisionChildrenPlacementOrValidityEnd,
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
                auditIdType = DatabaseTable.FeeDecisionChild::class,
                expirationRule = Always.safeFor(VARDA),
                bundledBy = "fee_decision",
            ),
            HandledTable(
                name = "finance_note",
                handledBy = ADULT,
                references = listOf(primaryReference("person_id", referencedTable = "person")),
                auditIdType = DatabaseTable.FinanceNote::class,
                expirationRule = Always,
                bundledBy = "person",
            ),
            HandledTable(
                name = "income_notification",
                handledBy = ADULT,
                references = listOf(primaryReference("receiver_id", referencedTable = "person")),
                independentRows = true,
                auditIdType = DatabaseTable.IncomeNotification::class,
                expirationRule = After(tenYears, OwnColumn("created", TIMESTAMP_WITH_TIME_ZONE)),
            ),
            HandledTable(
                name = "daily_service_time_notification",
                handledBy = ADULT,
                references = listOf(primaryReference("guardian_id", referencedTable = "person")),
                independentRows = true,
                auditIdType = DatabaseTable.DailyServiceTimeNotification::class,
                expirationRule =
                    After(twoMonths, OwnColumn("created_at", TIMESTAMP_WITH_TIME_ZONE)),
            ),
            HandledTable(
                name = "invoice",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("head_of_family", referencedTable = "person"),
                        secondaryReference("codebtor", referencedTable = "person"),
                    ),
                auditIdType = DatabaseTable.Invoice::class,
                expirationRule =
                    headOfFamilyFinanceRule(
                        OwnColumn("period_end"),
                        invoiceChildrenPlacementOrPeriodEnd,
                    ),
            ),
            HandledTable(
                name = "invoice_row",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("invoice_id", referencedTable = "invoice"),
                        secondaryReference("child", referencedTable = "child"),
                        secondaryReference("correction_id", referencedTable = "invoice_correction"),
                    ),
                auditIdType = DatabaseTable.InvoiceRow::class,
                expirationRule = Always,
                bundledBy = "invoice",
            ),
            HandledTable(
                name = "invoice_correction",
                handledBy = ADULT,
                references =
                    listOf(
                        primaryReference("head_of_family_id", referencedTable = "person"),
                        secondaryReference("child_id", referencedTable = "child"),
                    ),
                auditIdType = DatabaseTable.InvoiceCorrection::class,
                expirationRule =
                    headOfFamilyFinanceRule(
                        OwnColumn("period", FINITE_DATE_RANGE_END),
                        invoiceCorrectionChildPlacementOrPeriodEnd,
                    ),
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
                name = "message_thread_children",
                references = listOf(secondaryReference("child_id", referencedTable = "child")),
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
