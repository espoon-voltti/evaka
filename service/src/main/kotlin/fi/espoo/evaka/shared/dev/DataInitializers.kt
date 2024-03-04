// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.getApplicationType
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistanceaction.insertAssistanceActionOptionRefs
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.incomestatement.IncomeStatementType
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.messaging.createPersonMessageAccount
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.CalendarEventAttendeeId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildAttendanceId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ClubTermId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EmployeePinId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FamilyContactId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.FosterParentId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.StaffAttendancePlanId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.evaka.shared.security.upsertCitizenUser
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.varda.VardaServiceNeed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import mu.KotlinLogging
import org.jdbi.v3.json.Json
import org.springframework.core.io.ClassPathResource

private val logger = KotlinLogging.logger {}

fun Database.Transaction.runDevScript(devScriptName: String) {
    val path = "dev-data/$devScriptName"
    logger.info("Running SQL script: $path")
    ClassPathResource(path).inputStream.use {
        it.bufferedReader().readText().let { content -> execute(content) }
    }
}

fun Database.Transaction.resetDatabase() {
    execute("SELECT reset_database()")
    execute(
        "INSERT INTO evaka_user (id, type, name) VALUES ('00000000-0000-0000-0000-000000000000', 'SYSTEM', 'eVaka')"
    )
}

fun Database.Transaction.ensureDevData() {
    if (createQuery { sql("SELECT count(*) FROM care_area") }.exactlyOne<Int>() == 0) {
        listOf(
                "dev-data.sql",
                "service-need-options.sql",
                "employees.sql",
                "preschool-terms.sql",
                "club-terms.sql",
                "holidays.sql"
            )
            .forEach { runDevScript(it) }
    }
}

fun Database.Transaction.insert(row: DevCareArea): AreaId =
    createUpdate {
            sql(
                """
INSERT INTO care_area (id, name, short_name, area_code, sub_cost_center)
VALUES (${bind(row.id)}, ${bind(row.name)}, ${bind(row.shortName)}, ${bind(row.areaCode)}, ${bind(row.subCostCenter)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(holiday: DevHoliday) {
    createUpdate {
            sql(
                """
INSERT INTO holiday (date, description)
VALUES (${bind(holiday.date)}, ${bind(holiday.description)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevDaycare): DaycareId =
    createUpdate {
            sql(
                """
INSERT INTO daycare (
    id, name, opening_date, closing_date, care_area_id, type, daily_preschool_time, daily_preparatory_time, daycare_apply_period, preschool_apply_period, club_apply_period, provider_type,
    capacity, language, ghost_unit, upload_to_varda, upload_children_to_varda, upload_to_koski, invoiced_by_municipality, cost_center, dw_cost_center,
    additional_info, phone, email, url,
    street_address, postal_code, post_office,
    location, mailing_street_address, mailing_po_box, mailing_postal_code, mailing_post_office,
    unit_manager_name, unit_manager_phone, unit_manager_email,
    decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address,
    oph_unit_oid, oph_organizer_oid, round_the_clock, operation_times, enabled_pilot_features,
    finance_decision_handler, business_id, iban, provider_id
) VALUES (
    ${bind(row.id)}, ${bind(row.name)}, ${bind(row.openingDate)}, ${bind(row.closingDate)}, ${bind(row.areaId)},
    ${bind(row.type)}::care_types[], ${bind(row.dailyPreschoolTime)}, ${bind(row.dailyPreparatoryTime)}, ${bind(row.daycareApplyPeriod)},
    ${bind(row.preschoolApplyPeriod)}, ${bind(row.clubApplyPeriod)}, ${bind(row.providerType)}, ${bind(row.capacity)},
    ${bind(row.language)}, ${bind(row.ghostUnit)}, ${bind(row.uploadToVarda)}, ${bind(row.uploadChildrenToVarda)},
    ${bind(row.uploadToKoski)}, ${bind(row.invoicedByMunicipality)}, ${bind(row.costCenter)}, ${bind(row.dwCostCenter)},
    ${bind(row.additionalInfo)}, ${bind(row.phone)}, ${bind(row.email)}, ${bind(row.url)}, ${bind(row.visitingAddress.streetAddress)},
    ${bind(row.visitingAddress.postalCode)}, ${bind(row.visitingAddress.postOffice)}, ${bind(row.location)},
    ${bind(row.mailingAddress.streetAddress)}, ${bind(row.mailingAddress.poBox)}, ${bind(row.mailingAddress.postalCode)},
    ${bind(row.mailingAddress.postOffice)}, ${bind(row.unitManager.name)}, ${bind(row.unitManager.phone)}, ${bind(row.unitManager.email)},
    ${bind(row.decisionCustomization.daycareName)}, ${bind(row.decisionCustomization.preschoolName)}, ${bind(row.decisionCustomization.handler)},
    ${bind(row.decisionCustomization.handlerAddress)}, ${bind(row.ophUnitOid)}, ${bind(row.ophOrganizerOid)}, ${bind(row.roundTheClock)},
    ${bind(row.operationTimes)}, ${bind(row.enabledPilotFeatures)}::pilot_feature[], ${bind(row.financeDecisionHandler)}, ${bind(row.businessId)},
    ${bind(row.iban)}, ${bind(row.providerId)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.updateDaycareAcl(
    daycareId: DaycareId,
    externalId: ExternalId,
    role: UserRole
) {
    createUpdate {
            sql(
                "INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES ((SELECT id from employee where external_id = ${bind(externalId)}), ${bind(daycareId)}, ${bind(role)})"
            )
        }
        .execute()
}

fun Database.Transaction.updateDaycareAclWithEmployee(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole
) {
    createUpdate {
            sql(
                "INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES (${bind(employeeId)}, ${bind(daycareId)}, ${bind(role)})"
            )
        }
        .execute()
}

fun Database.Transaction.insertEmployeeToDaycareGroupAcl(groupId: GroupId, employeeId: EmployeeId) {
    createUpdate {
            sql(
                "INSERT INTO daycare_group_acl (employee_id, daycare_group_id) VALUES (${bind(employeeId)}, ${bind(groupId)})"
            )
        }
        .execute()
}

fun Database.Transaction.createMobileDeviceToUnit(
    id: MobileDeviceId,
    unitId: DaycareId,
    name: String = "Nimeämätön laite"
) {
    createUpdate {
            sql(
                """
INSERT INTO mobile_device (id, unit_id, name) VALUES (${bind(id)}, ${bind(unitId)}, ${bind(name)});
INSERT INTO evaka_user (id, type, mobile_device_id, name) VALUES (${bind(id)}, 'MOBILE_DEVICE', ${bind(id)}, ${bind(name)});
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevEmployee) =
    createUpdate {
            sql(
                """
INSERT INTO employee (id, preferred_first_name, first_name, last_name, email, external_id, employee_number, roles, last_login, active)
VALUES (${bind(row.id)}, ${bind(row.preferredFirstName)}, ${bind(row.firstName)}, ${bind(row.lastName)}, ${bind(row.email)}, ${bind(row.externalId)}, ${bind(row.employeeNumber)}, ${bind(row.roles)}::user_role[], ${bind(row.lastLogin)}, ${bind(row.active)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<EmployeeId>()
        .also { upsertEmployeeUser(it) }

fun Database.Transaction.insert(
    row: DevEmployee,
    unitRoles: Map<DaycareId, UserRole> = mapOf(),
    groupAcl: Map<DaycareId, Collection<GroupId>> = mapOf()
) =
    createUpdate {
            sql(
                """
INSERT INTO employee (id, preferred_first_name, first_name, last_name, email, external_id, employee_number, roles, last_login, active)
VALUES (${bind(row.id)}, ${bind(row.preferredFirstName)}, ${bind(row.firstName)}, ${bind(row.lastName)}, ${bind(row.email)}, ${bind(row.externalId)}, ${bind(row.employeeNumber)}, ${bind(row.roles)}::user_role[], ${bind(row.lastLogin)}, ${bind(row.active)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<EmployeeId>()
        .also { employeeId ->
            upsertEmployeeUser(employeeId)
            unitRoles.forEach { (daycareId, role) ->
                insertDaycareAclRow(daycareId, employeeId, role)
            }
            groupAcl.forEach { (daycareId, groups) ->
                insertDaycareGroupAcl(daycareId, employeeId, groups)
            }
        }

fun Database.Transaction.insert(row: DevMobileDevice) =
    createUpdate {
            sql(
                """
INSERT INTO mobile_device (id, unit_id, name, long_term_token, push_notification_categories)
VALUES (${bind(row.id)}, ${bind(row.unitId)}, ${bind(row.name)}, ${bind(row.longTermToken)}, ${bind(row.pushNotificationCategories)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<MobileDeviceId>()

fun Database.Transaction.insert(row: DevPersonalMobileDevice) =
    createUpdate {
            sql(
                """
INSERT INTO mobile_device (id, employee_id, name, long_term_token)
VALUES (${bind(row.id)}, ${bind(row.employeeId)}, ${bind(row.name)}, ${bind(row.longTermToken)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<MobileDeviceId>()

enum class DevPersonType {
    CHILD,
    ADULT,
    RAW_ROW,
}

fun Database.Transaction.insert(person: DevPerson, type: DevPersonType): PersonId {
    val p = person.copy(updatedFromVtj = if (person.ssn != null) HelsinkiDateTime.now() else null)
    return createUpdate {
            sql(
                """
INSERT INTO person (
    id, date_of_birth, date_of_death, first_name, last_name, preferred_name, social_security_number, email, phone, language,
    street_address, postal_code, post_office, residence_code, nationalities, restricted_details_enabled, restricted_details_end_date,
    invoicing_street_address, invoicing_postal_code, invoicing_post_office, updated_from_vtj, oph_person_oid, duplicate_of, enabled_email_types
) VALUES (
    ${bind(p.id)}, ${bind(p.dateOfBirth)}, ${bind(p.dateOfDeath)}, ${bind(p.firstName)}, ${bind(p.lastName)}, ${bind(p.preferredName)},
    ${bind(p.ssn)}, ${bind(p.email)}, ${bind(p.phone)}, ${bind(p.language)}, ${bind(p.streetAddress)}, ${bind(p.postalCode)}, ${bind(p.postOffice)},
    ${bind(p.residenceCode)}, ${bind(p.nationalities)}, ${bind(p.restrictedDetailsEnabled)}, ${bind(p.restrictedDetailsEndDate)},
    ${bind(p.invoicingStreetAddress)}, ${bind(p.invoicingPostalCode)}, ${bind(p.invoicingPostOffice)}, ${bind(p.updatedFromVtj)},
    ${bind(p.ophPersonOid)}, ${bind(p.duplicateOf)}, ${bind(p.enabledEmailTypes)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<PersonId>()
        .also { id ->
            when (type) {
                DevPersonType.CHILD -> {
                    insert(DevChild(id))
                }
                DevPersonType.ADULT -> {
                    upsertCitizenUser(id)
                    createPersonMessageAccount(id)
                }
                DevPersonType.RAW_ROW -> {}
            }
        }
}

fun Database.Transaction.insertTestParentship(
    headOfChild: PersonId,
    childId: ChildId,
    id: ParentshipId = ParentshipId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): ParentshipId {
    createUpdate {
            sql(
                """
INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
VALUES (${bind(id)}, ${bind(headOfChild)}, ${bind(childId)}, ${bind(startDate)}, ${bind(endDate)})
"""
            )
        }
        .execute()
    return id
}

fun Database.Transaction.insert(row: DevParentship): ParentshipId =
    createUpdate {
            sql(
                """
INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
VALUES (${bind(row.id)}, ${bind(row.headOfChildId)}, ${bind(row.childId)}, ${bind(row.startDate)}, ${bind(row.endDate)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertTestPartnership(
    adult1: PersonId,
    adult2: PersonId,
    id: PartnershipId = PartnershipId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null,
    createdAt: HelsinkiDateTime =
        HelsinkiDateTime.of(LocalDate.of(2019, 1, 1), LocalTime.of(12, 0, 0))
): PartnershipId {
    insert(
        DevFridgePartner(
            partnershipId = id,
            indx = 1,
            otherIndx = 2,
            personId = adult1,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt
        )
    )
    insert(
        DevFridgePartner(
            partnershipId = id,
            indx = 2,
            otherIndx = 1,
            personId = adult2,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt
        )
    )
    return id
}

fun Database.Transaction.insertTestApplication(
    id: ApplicationId = ApplicationId(UUID.randomUUID()),
    type: ApplicationType,
    sentDate: LocalDate? = LocalDate.of(2019, 1, 1),
    dueDate: LocalDate? = LocalDate.of(2019, 5, 1),
    status: ApplicationStatus = ApplicationStatus.SENT,
    guardianId: PersonId,
    childId: ChildId,
    otherGuardianId: PersonId? = null,
    hideFromGuardian: Boolean = false,
    additionalDaycareApplication: Boolean = false,
    transferApplication: Boolean = false,
    allowOtherGuardianAccess: Boolean = true
): ApplicationId {
    createUpdate {
            sql(
                """
INSERT INTO application (type, id, sentdate, duedate, status, guardian_id, child_id, other_guardian_id, origin, hidefromguardian, additionalDaycareApplication, transferApplication, allow_other_guardian_access)
VALUES (${bind(type)}, ${bind(id)}, ${bind(sentDate)}, ${bind(dueDate)}, ${bind(status)}::application_status_type, ${bind(guardianId)}, ${bind(childId)}, ${bind(otherGuardianId)}, 'ELECTRONIC'::application_origin_type, ${bind(hideFromGuardian)}, ${bind(additionalDaycareApplication)}, ${bind(transferApplication)}, ${bind(allowOtherGuardianAccess)})
"""
            )
        }
        .execute()

    if (otherGuardianId != null) {
        createUpdate {
                sql(
                    """
INSERT INTO application_other_guardian (application_id, guardian_id) 
VALUES (${bind(id)}, ${bind(otherGuardianId)})
"""
                )
            }
            .execute()
    }

    return id
}

fun Database.Transaction.insertTestApplicationForm(
    applicationId: ApplicationId,
    document: DaycareFormV0
) {
    check(getApplicationType(applicationId) == document.type) {
        "Invalid form type for the application"
    }

    createUpdate {
            sql(
                """
UPDATE application SET document = ${bindJson(document)}, form_modified = now()
WHERE id = ${bind(applicationId)}
"""
            )
        }
        .execute()
}

fun Database.Transaction.insertTestClubApplicationForm(
    applicationId: ApplicationId,
    document: ClubFormV0
) {
    check(getApplicationType(applicationId) == document.type) {
        "Invalid form type for the application"
    }

    createUpdate {
            sql(
                """
UPDATE application SET document = ${bindJson(document)}, form_modified = now()
WHERE id = ${bind(applicationId)}
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevChild): ChildId =
    createUpdate {
            sql(
                """
INSERT INTO child (id, allergies, diet, medication, additionalinfo, language_at_home, language_at_home_details)
VALUES (${bind(row.id)}, ${bind(row.allergies)}, ${bind(row.diet)}, ${bind(row.medication)}, ${bind(row.additionalInfo)}, ${bind(row.languageAtHome)}, ${bind(row.languageAtHomeDetails)})
ON CONFLICT(id) DO UPDATE
SET id = ${bind(row.id)}, allergies = ${bind(row.allergies)}, diet = ${bind(row.diet)}, medication = ${bind(row.medication)}, additionalInfo = ${bind(row.additionalInfo)}, language_at_home = ${bind(row.languageAtHome)}, language_at_home_details = ${bind(row.languageAtHomeDetails)}
RETURNING id
    """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevPlacement): PlacementId =
    createUpdate {
            sql(
                """
INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date, termination_requested_date, terminated_by, place_guarantee)
VALUES (${bind(row.id)}, ${bind(row.type)}, ${bind(row.childId)}, ${bind(row.unitId)}, ${bind(row.startDate)}, ${bind(row.endDate)}, ${bind(row.terminationRequestedDate)}, ${bind(row.terminatedBy)}, ${bind(row.placeGuarantee)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertTestPlacement(
    id: PlacementId = PlacementId(UUID.randomUUID()),
    childId: ChildId = ChildId(UUID.randomUUID()),
    unitId: DaycareId = DaycareId(UUID.randomUUID()),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31),
    terminationRequestedDate: LocalDate? = null,
    terminatedBy: EvakaUserId? = null,
    placeGuarantee: Boolean = false
): PlacementId {
    createUpdate {
            sql(
                """
INSERT INTO placement (id, child_id, unit_id, type, start_date, end_date, termination_requested_date, terminated_by, place_guarantee)
VALUES (${bind(id)}, ${bind(childId)}, ${bind(unitId)}, ${bind(type)}::placement_type, ${bind(startDate)}, ${bind(endDate)}, ${bind(terminationRequestedDate)}, ${bind(terminatedBy)}, ${bind(placeGuarantee)})
"""
            )
        }
        .execute()
    return id
}

fun Database.Transaction.insertTestHoliday(date: LocalDate, description: String = "holiday") {
    createUpdate {
            sql(
                """
INSERT INTO holiday (date, description)
VALUES (${bind(date)}, ${bind(description)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.insertTestServiceNeed(
    confirmedBy: EvakaUserId,
    placementId: PlacementId,
    period: FiniteDateRange,
    optionId: ServiceNeedOptionId,
    shiftCare: ShiftCareType = ShiftCareType.NONE,
    confirmedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    id: ServiceNeedId = ServiceNeedId(UUID.randomUUID())
): ServiceNeedId {
    createUpdate {
            sql(
                """
INSERT INTO service_need (id, placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at)
VALUES (${bind(id)}, ${bind(placementId)}, ${bind(period.start)}, ${bind(period.end)}, ${bind(optionId)}, ${bind(shiftCare)}, ${bind(confirmedBy)}, ${bind(confirmedAt)})
"""
            )
        }
        .execute()
    return id
}

fun Database.Transaction.insertServiceNeedOption(option: ServiceNeedOption) {
    createUpdate {
            sql(
                """
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, contract_days_per_month, daycare_hours_per_month, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, active, updated)
VALUES (${bind(option.id)}, ${bind(option.nameFi)}, ${bind(option.nameSv)}, ${bind(option.nameEn)}, ${bind(option.validPlacementType)}, ${bind(option.defaultOption)}, ${bind(option.feeCoefficient)}, ${bind(option.occupancyCoefficient)}, ${bind(option.occupancyCoefficientUnder3y)}, ${bind(option.realizedOccupancyCoefficient)}, ${bind(option.realizedOccupancyCoefficientUnder3y)}, ${bind(option.daycareHoursPerWeek)}, ${bind(option.contractDaysPerMonth)}, ${bind(option.daycareHoursPerMonth)}, ${bind(option.partDay)}, ${bind(option.partWeek)}, ${bind(option.feeDescriptionFi)}, ${bind(option.feeDescriptionSv)}, ${bind(option.voucherValueDescriptionFi)}, ${bind(option.voucherValueDescriptionSv)}, ${bind(option.active)}, ${bind(option.updated)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.upsertServiceNeedOption(option: ServiceNeedOption) {
    createUpdate {
            sql(
                """
ALTER TABLE service_need_option DISABLE TRIGGER set_timestamp;
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, contract_days_per_month, daycare_hours_per_month, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, updated)
VALUES (${bind(option.id)}, ${bind(option.nameFi)}, ${bind(option.nameSv)}, ${bind(option.nameEn)}, ${bind(option.validPlacementType)}, ${bind(option.defaultOption)}, ${bind(option.feeCoefficient)}, ${bind(option.occupancyCoefficient)}, ${bind(option.occupancyCoefficientUnder3y)}, ${bind(option.realizedOccupancyCoefficient)}, ${bind(option.realizedOccupancyCoefficientUnder3y)}, ${bind(option.daycareHoursPerWeek)}, ${bind(option.contractDaysPerMonth)}, ${bind(option.daycareHoursPerMonth)}, ${bind(option.partDay)}, ${bind(option.partWeek)}, ${bind(option.feeDescriptionFi)}, ${bind(option.feeDescriptionSv)}, ${bind(option.voucherValueDescriptionFi)}, ${bind(option.voucherValueDescriptionSv)}, ${bind(option.updated)})
ON CONFLICT (id) DO UPDATE SET updated = ${bind(option.updated)}, daycare_hours_per_week = ${bind(option.daycareHoursPerWeek)};
ALTER TABLE service_need_option ENABLE TRIGGER set_timestamp;
"""
            )
        }
        .execute()
}

data class DevIncome(
    val personId: PersonId,
    val id: IncomeId = IncomeId(UUID.randomUUID()),
    val validFrom: LocalDate = LocalDate.of(2019, 1, 1),
    val validTo: LocalDate? = null,
    @Json val data: Map<String, IncomeValue> = mapOf(),
    val effect: IncomeEffect = IncomeEffect.MAX_FEE_ACCEPTED,
    val isEntrepreneur: Boolean = false,
    val worksAtEcha: Boolean = false,
    val updatedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val updatedBy: EvakaUserId
)

fun Database.Transaction.insert(row: DevIncome): IncomeId =
    createUpdate {
            sql(
                """
INSERT INTO income (id, person_id, valid_from, valid_to, data, effect, is_entrepreneur, works_at_echa, updated_at, updated_by)
VALUES (${bind(row.id)}, ${bind(row.personId)}, ${bind(row.validFrom)}, ${bind(row.validTo)}, ${bindJson(row.data)}, ${bind(row.effect)}::income_effect, ${bind(row.isEntrepreneur)}, ${bind(row.worksAtEcha)}, ${bind(row.updatedAt)}, ${bind(row.updatedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevIncomeStatement(
    val id: IncomeStatementId = IncomeStatementId(UUID.randomUUID()),
    val personId: PersonId,
    val startDate: LocalDate,
    val type: IncomeStatementType,
    val grossEstimatedMonthlyIncome: Int,
    val handlerId: EmployeeId? = null
)

fun Database.Transaction.insert(row: DevIncomeStatement): IncomeStatementId =
    createUpdate {
            sql(
                """
INSERT INTO income_statement (id, person_id, start_date, type, gross_estimated_monthly_income, handler_id)
VALUES (${bind(row.id)}, ${bind(row.personId)}, ${bind(row.startDate)}, ${bind(row.type)}, ${bind(row.grossEstimatedMonthlyIncome)}, ${bind(row.handlerId)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevFeeAlteration(
    val id: FeeAlterationId = FeeAlterationId(UUID.randomUUID()),
    val personId: PersonId,
    val type: FeeAlterationType,
    val amount: Double,
    val isAbsolute: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String = "",
    val updatedBy: EvakaUserId,
    val updatedAt: HelsinkiDateTime = HelsinkiDateTime.now()
)

fun Database.Transaction.insert(row: DevFeeAlteration): FeeAlterationId =
    createUpdate {
            sql(
                """
INSERT INTO fee_alteration (id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, updated_at, updated_by)
VALUES (${bind(row.id)}, ${bind(row.personId)}, ${bind(row.type)}::fee_alteration_type, ${bind(row.amount)}, ${bind(row.isAbsolute)}, ${bind(row.validFrom)}, ${bind(row.validTo)}, ${bind(row.notes)}, ${bind(row.updatedAt)}, ${bind(row.updatedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: FeeThresholds): FeeThresholdsId =
    createUpdate {
            sql(
                """
INSERT INTO fee_thresholds (valid_during, min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6, income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6, max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6, income_threshold_increase_6_plus, sibling_discount_2, sibling_discount_2_plus, max_fee, min_fee, temporary_fee, temporary_fee_part_day, temporary_fee_sibling, temporary_fee_sibling_part_day)
VALUES (${bind(row.validDuring)}, ${bind(row.minIncomeThreshold2)}, ${bind(row.minIncomeThreshold3)}, ${bind(row.minIncomeThreshold4)}, ${bind(row.minIncomeThreshold5)}, ${bind(row.minIncomeThreshold6)}, ${bind(row.incomeMultiplier2)}, ${bind(row.incomeMultiplier3)}, ${bind(row.incomeMultiplier4)}, ${bind(row.incomeMultiplier5)}, ${bind(row.incomeMultiplier6)}, ${bind(row.maxIncomeThreshold2)}, ${bind(row.maxIncomeThreshold3)}, ${bind(row.maxIncomeThreshold4)}, ${bind(row.maxIncomeThreshold5)}, ${bind(row.maxIncomeThreshold6)}, ${bind(row.incomeThresholdIncrease6Plus)}, ${bind(row.siblingDiscount2)}, ${bind(row.siblingDiscount2Plus)}, ${bind(row.maxFee)}, ${bind(row.minFee)}, ${bind(row.temporaryFee)}, ${bind(row.temporaryFeePartDay)}, ${bind(row.temporaryFeeSibling)}, ${bind(row.temporaryFeeSiblingPartDay)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDaycareGroup): GroupId =
    createUpdate {
            sql(
                """
INSERT INTO daycare_group (id, daycare_id, name, start_date, end_date)
VALUES (${bind(row.id)}, ${bind(row.daycareId)}, ${bind(row.name)}, ${bind(row.startDate)}, ${bind(row.endDate)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDaycareGroupPlacement): GroupPlacementId =
    createUpdate {
            sql(
                """
INSERT INTO daycare_group_placement (id, daycare_placement_id, daycare_group_id, start_date, end_date)
VALUES (${bind(row.id)}, ${bind(row.daycarePlacementId)}, ${bind(row.daycareGroupId)}, ${bind(row.startDate)}, ${bind(row.endDate)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertTestDaycareGroupPlacement(
    daycarePlacementId: PlacementId = PlacementId(UUID.randomUUID()),
    groupId: GroupId = GroupId(UUID.randomUUID()),
    id: GroupPlacementId = GroupPlacementId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): GroupPlacementId {
    createUpdate {
            sql(
                """
INSERT INTO daycare_group_placement (id, daycare_placement_id, daycare_group_id, start_date, end_date)
VALUES (${bind(id)}, ${bind(daycarePlacementId)}, ${bind(groupId)}, ${bind(startDate)}, ${bind(endDate)})
"""
            )
        }
        .execute()
    return id
}

fun Database.Transaction.insertTestPlacementPlan(
    applicationId: ApplicationId,
    unitId: DaycareId,
    id: PlacementPlanId = PlacementPlanId(UUID.randomUUID()),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31),
    preschoolDaycareStartDate: LocalDate? = null,
    preschoolDaycareEndDate: LocalDate? = null,
    updated: HelsinkiDateTime = HelsinkiDateTime.now(),
    deleted: Boolean? = false
): PlacementPlanId {
    createUpdate {
            sql(
                """
INSERT INTO placement_plan (id, unit_id, application_id, type, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date, updated, deleted)
VALUES (${bind(id)}, ${bind(unitId)}, ${bind(applicationId)}, ${bind(type)}::placement_type, ${bind(startDate)}, ${bind(endDate)}, ${bind(preschoolDaycareStartDate)}, ${bind(preschoolDaycareEndDate)}, ${bind(updated)}, ${bind(deleted)})
"""
            )
        }
        .execute()
    return id
}

data class TestDecision(
    val id: DecisionId? = DecisionId(UUID.randomUUID()),
    val createdBy: EvakaUserId,
    val sentDate: LocalDate = LocalDate.now(europeHelsinki),
    val unitId: DaycareId,
    val applicationId: ApplicationId,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus = DecisionStatus.PENDING,
    val requestedStartDate: LocalDate? = null,
    val resolvedBy: UUID? = null,
    val resolved: HelsinkiDateTime? =
        resolvedBy?.let {
            HelsinkiDateTime.from(Instant.ofEpochSecond(1546300800))
        }, // 2019-01-01 midnight
    val pendingDecisionEmailsSentCount: Int? = 0,
    val pendingDecisionEmailSent: HelsinkiDateTime? = null
)

fun Database.Transaction.insertTestDecision(decision: TestDecision): DecisionId =
    createUpdate {
            sql(
                """
INSERT INTO decision (id, created_by, sent_date, unit_id, application_id, type, start_date, end_date, status, requested_start_date, resolved, resolved_by, pending_decision_emails_sent_count, pending_decision_email_sent)
VALUES (${bind(decision.id)}, ${bind(decision.createdBy)}, ${bind(decision.sentDate)}, ${bind(decision.unitId)}, ${bind(decision.applicationId)}, ${bind(decision.type)}, ${bind(decision.startDate)}, ${bind(decision.endDate)}, ${bind(decision.status)}, ${bind(decision.requestedStartDate)}, ${bind(decision.resolved)}, ${bind(decision.resolvedBy)}, ${bind(decision.pendingDecisionEmailsSentCount)}, ${bind(decision.pendingDecisionEmailSent)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevAssistanceAction): AssistanceActionId =
    createUpdate {
            sql(
                """
INSERT INTO assistance_action (id, updated_by, child_id, start_date, end_date, other_action)
VALUES (${bind(row.id)}, ${bind(row.updatedBy)}, ${bind(row.childId)}, ${bind(row.startDate)}, ${bind(row.endDate)}, ${bind(row.otherAction)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<AssistanceActionId>()
        .also {
            val counts = insertAssistanceActionOptionRefs(it, row.actions)
            assert(counts.size == row.actions.size)
        }

fun Database.Transaction.insertTestCaretakers(
    groupId: GroupId,
    id: UUID = UUID.randomUUID(),
    amount: Double = 3.0,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null
) {
    createUpdate {
            sql(
                """
INSERT INTO daycare_caretaker (id, group_id, amount, start_date, end_date)
VALUES (${bind(id)}, ${bind(groupId)}, ${bind(amount)}, ${bind(startDate)}, ${bind(endDate)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.insertTestAssistanceNeedPreschoolDecision(
    decision: DevAssistanceNeedPreschoolDecision
): AssistanceNeedPreschoolDecisionId {
    createUpdate {
            sql(
                """
INSERT INTO assistance_need_preschool_decision (
    id, decision_number, child_id, status, annulment_reason, language, type, valid_from, 
    extended_compulsory_education, extended_compulsory_education_info, 
    granted_assistance_service, granted_interpretation_service, 
    granted_assistive_devices, granted_services_basis, selected_unit, primary_group, 
    decision_basis, basis_document_pedagogical_report, basis_document_psychologist_statement, 
    basis_document_social_report, basis_document_doctor_statement, basis_document_other_or_missing, 
    basis_document_other_or_missing_info, basis_documents_info, guardians_heard_on, other_representative_heard, 
    other_representative_details, view_of_guardians, preparer_1_employee_id, preparer_1_title, preparer_1_phone_number, 
    preparer_2_employee_id, preparer_2_title, preparer_2_phone_number, 
    decision_maker_employee_id, decision_maker_title, 
    sent_for_decision, decision_made, unread_guardian_ids
) VALUES (
    ${bind(decision.id)}, ${bind(decision.decisionNumber)}, ${bind(decision.childId)}, ${bind(decision.status)}, ${bind(decision.annulmentReason)},
    ${bind(decision.form.language)}, ${bind(decision.form.type)}, ${bind(decision.form.validFrom)}, ${bind(decision.form.extendedCompulsoryEducation)},
    ${bind(decision.form.extendedCompulsoryEducationInfo)}, ${bind(decision.form.grantedAssistanceService)}, ${bind(decision.form.grantedInterpretationService)}, 
    ${bind(decision.form.grantedAssistiveDevices)}, ${bind(decision.form.grantedServicesBasis)}, ${bind(decision.form.selectedUnit)},
    ${bind(decision.form.primaryGroup)}, ${bind(decision.form.decisionBasis)}, ${bind(decision.form.basisDocumentPedagogicalReport)},
    ${bind(decision.form.basisDocumentPsychologistStatement)}, ${bind(decision.form.basisDocumentSocialReport)}, ${bind(decision.form.basisDocumentDoctorStatement)},
    ${bind(decision.form.basisDocumentOtherOrMissing)}, ${bind(decision.form.basisDocumentOtherOrMissingInfo)}, ${bind(decision.form.basisDocumentsInfo)},
    ${bind(decision.form.guardiansHeardOn)}, ${bind(decision.form.otherRepresentativeHeard)}, ${bind(decision.form.otherRepresentativeDetails)},
    ${bind(decision.form.viewOfGuardians)}, ${bind(decision.form.preparer1EmployeeId)}, ${bind(decision.form.preparer1Title)},
    ${bind(decision.form.preparer1PhoneNumber)}, ${bind(decision.form.preparer2EmployeeId)}, ${bind(decision.form.preparer2Title)},
    ${bind(decision.form.preparer2PhoneNumber)}, ${bind(decision.form.decisionMakerEmployeeId)}, ${bind(decision.form.decisionMakerTitle)}, 
    ${bind(decision.sentForDecision)}, ${bind(decision.decisionMade)}, ${bind(decision.unreadGuardianIds)}
)
"""
            )
        }
        .execute()

    decision.form.guardianInfo.forEach { guardian ->
        createUpdate {
                sql(
                    """
INSERT INTO assistance_need_preschool_decision_guardian (id, assistance_need_decision_id, person_id, is_heard, details)
VALUES (${bind(guardian.id)}, ${bind(decision.id)}, ${bind(guardian.personId)}, ${bind(guardian.isHeard)}, ${bind(guardian.details)})
"""
                )
            }
            .execute()
    }

    return decision.id
}

fun Database.Transaction.insertTestAssistanceNeedDecision(
    childId: ChildId,
    data: DevAssistanceNeedDecision
): AssistanceNeedDecisionId {
    val id =
        createQuery {
                sql(
                    """
INSERT INTO assistance_need_decision (
  id, decision_number, child_id, validity_period, status, language, decision_made, sent_for_decision,
  selected_unit, pedagogical_motivation, structural_motivation_opt_smaller_group,
  structural_motivation_opt_special_group, structural_motivation_opt_small_group,
  structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
  structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
  service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
  service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
  expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
  assistance_levels, motivation_for_decision, decision_maker_employee_id,
  decision_maker_title, preparer_1_employee_id, preparer_1_title, preparer_2_employee_id, preparer_2_title,
  preparer_1_phone_number, preparer_2_phone_number, unread_guardian_ids, annulment_reason
)
VALUES (
    ${bind(data.id)}, ${bind(data.decisionNumber)}, ${bind(childId)}, ${bind(data.validityPeriod)}, ${bind(data.status)},
    ${bind(data.language)}, ${bind(data.decisionMade)}, ${bind(data.sentForDecision)}, ${bind(data.selectedUnit)}, ${bind(data.pedagogicalMotivation)},
    ${bind(data.structuralMotivationOptions.smallerGroup)}, ${bind(data.structuralMotivationOptions.specialGroup)}, ${bind(data.structuralMotivationOptions.smallGroup)},
    ${bind(data.structuralMotivationOptions.groupAssistant)}, ${bind(data.structuralMotivationOptions.childAssistant)}, ${bind(data.structuralMotivationOptions.additionalStaff)},
    ${bind(data.structuralMotivationDescription)}, ${bind(data.careMotivation)}, ${bind(data.serviceOptions.consultationSpecialEd)},
    ${bind(data.serviceOptions.partTimeSpecialEd)}, ${bind(data.serviceOptions.fullTimeSpecialEd)}, ${bind(data.serviceOptions.interpretationAndAssistanceServices)},
    ${bind(data.serviceOptions.specialAides)}, ${bind(data.servicesMotivation)}, ${bind(data.expertResponsibilities)}, ${bind(data.guardiansHeardOn)},
    ${bind(data.viewOfGuardians)}, ${bind(data.otherRepresentativeHeard)}, ${bind(data.otherRepresentativeDetails)}, ${bind(data.assistanceLevels)},
    ${bind(data.motivationForDecision)}, ${bind(data.decisionMaker?.employeeId)}, ${bind(data.decisionMaker?.title)}, ${bind(data.preparedBy1?.employeeId)},
    ${bind(data.preparedBy1?.title)}, ${bind(data.preparedBy2?.employeeId)}, ${bind(data.preparedBy2?.title)}, ${bind(data.preparedBy1?.phoneNumber)},
    ${bind(data.preparedBy2?.phoneNumber)}, ${bind(data.unreadGuardianIds)}, ${bind(data.annulmentReason)}
)
RETURNING id
"""
                )
            }
            .exactlyOne<AssistanceNeedDecisionId>()

    val guardianSql =
        """
        INSERT INTO assistance_need_decision_guardian (
            assistance_need_decision_id,
            person_id,
            is_heard,
            details
        ) VALUES (
            :assistanceNeedDecisionId,
            :personId,
            :isHeard,
            :details
        )
            
        """

    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian ->
        batch.bindKotlin(guardian).bind("assistanceNeedDecisionId", id).add()
    }
    batch.execute()

    return id
}

fun Database.Transaction.insertTestStaffAttendance(
    id: StaffAttendanceId = StaffAttendanceId(UUID.randomUUID()),
    groupId: GroupId,
    date: LocalDate,
    count: Double
) {
    createUpdate {
            sql(
                """
INSERT INTO staff_attendance (id, group_id, date, count)
VALUES (${bind(id)}, ${bind(groupId)}, ${bind(date)}, ${bind(count)})
"""
            )
        }
        .execute()
}

data class DevStaffAttendancePlan(
    val id: StaffAttendancePlanId = StaffAttendancePlanId(UUID.randomUUID()),
    val employeeId: EmployeeId,
    val type: StaffAttendanceType = StaffAttendanceType.PRESENT,
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime,
    val description: String? = null
)

fun Database.Transaction.insert(row: DevStaffAttendancePlan): StaffAttendancePlanId =
    createUpdate {
            sql(
                """
INSERT INTO staff_attendance_plan (id, employee_id, type, start_time, end_time)
VALUES (${bind(row.id)}, ${bind(row.employeeId)}, ${bind(row.type)}, ${bind(row.startTime)}, ${bind(row.endTime)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertTestAbsence(
    id: AbsenceId = AbsenceId(UUID.randomUUID()),
    childId: ChildId,
    date: LocalDate,
    category: AbsenceCategory,
    absenceType: AbsenceType = AbsenceType.SICKLEAVE,
    modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
    modifiedAt: HelsinkiDateTime? = null
) {
    createUpdate {
            sql(
                """
INSERT INTO absence (id, child_id, date, category, absence_type, modified_by, modified_at)
VALUES (${bind(id)}, ${bind(childId)}, ${bind(date)}, ${bind(category)}, ${bind(absenceType)}, ${bind(modifiedBy)}, coalesce(${bind(modifiedAt)}, now()))
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevStaffAttendance): StaffAttendanceRealtimeId =
    createUpdate {
            sql(
                """
INSERT INTO staff_attendance_realtime (id, employee_id, group_id, arrived, departed, occupancy_coefficient, type, departed_automatically)
VALUES (${bind(row.id)}, ${bind(row.employeeId)}, ${bind(row.groupId)}, ${bind(row.arrived)}, ${bind(row.departed)}, ${bind(row.occupancyCoefficient)}, ${bind(row.type)}, ${bind(row.departedAutomatically)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertTestChildAttendance(
    childId: ChildId,
    unitId: DaycareId,
    arrived: HelsinkiDateTime,
    departed: HelsinkiDateTime?
) {
    val attendances: List<Triple<LocalDate, LocalTime, LocalTime?>> =
        if (departed == null) {
            listOf(Triple(arrived.toLocalDate(), arrived.toLocalTime(), null))
        } else {
            generateSequence(arrived.toLocalDate()) { it.plusDays(1) }
                .takeWhile { it <= departed.toLocalDate() }
                .map { date ->
                    Triple(
                        date,
                        if (arrived.toLocalDate() == date) arrived.toLocalTime()
                        else LocalTime.of(0, 0),
                        if (departed.toLocalDate() == date) departed.toLocalTime()
                        else LocalTime.of(23, 59)
                    )
                }
                .toList()
        }

    prepareBatch(
            """
        INSERT INTO child_attendance (id, child_id, unit_id, date, start_time, end_time)
        VALUES (:id, :childId, :unitId, :date, :startTime, :endTime)
        """
        )
        .also { batch ->
            attendances.forEach { (date, startTime, endTime) ->
                batch
                    .bind("id", ChildAttendanceId(UUID.randomUUID()))
                    .bind("childId", childId)
                    .bind("unitId", unitId)
                    .bind("date", date)
                    .bind("startTime", startTime.withSecond(0).withNano(0))
                    .bind("endTime", endTime?.withSecond(0)?.withNano(0))
                    .add()
            }
            batch.execute()
        }
}

fun Database.Transaction.insertTestBackUpCare(
    childId: ChildId,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate,
    groupId: GroupId? = null,
    id: BackupCareId = BackupCareId(UUID.randomUUID())
) {
    createUpdate {
            sql(
                """
INSERT INTO backup_care (id, child_id, unit_id, start_date, end_date, group_id)
VALUES (${bind(id)}, ${bind(childId)}, ${bind(unitId)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(groupId)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevBackupCare): BackupCareId =
    createUpdate {
            sql(
                """
INSERT INTO backup_care (id, child_id, unit_id, group_id, start_date, end_date)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.unitId)}, ${bind(row.groupId)}, ${bind(row.period.start)}, ${bind(row.period.end)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insertApplication(application: DevApplicationWithForm): ApplicationId {
    createUpdate {
            sql(
                """
INSERT INTO application(id, type, sentdate, duedate, status, guardian_id, child_id, origin, checkedbyadmin, hidefromguardian, transferapplication, other_guardian_id, allow_other_guardian_access)
VALUES (
    ${bind(application.id)},
    ${bind(application.type)},
    ${bind(application.sentDate)},
    ${bind(application.dueDate)},
    ${bind(application.status)}::application_status_type,
    ${bind(application.guardianId)},
    ${bind(application.childId)},
    ${bind(application.origin)}::application_origin_type,
    ${bind(application.checkedByAdmin)},
    ${bind(application.hideFromGuardian)},
    ${bind(application.transferApplication)},
    ${bind(application.otherGuardianId)},
    ${bind(application.allowOtherGuardianAccess)}
)
"""
            )
        }
        .execute()

    return application.id
}

fun Database.Transaction.insertApplicationForm(applicationForm: DevApplicationForm): UUID {
    check(getApplicationType(applicationForm.applicationId) == applicationForm.document.type) {
        "Invalid form type for the application"
    }

    createUpdate {
            sql(
                """
UPDATE application SET document = ${bindJson(applicationForm.document)}, form_modified = now()
WHERE id = ${bind(applicationForm.applicationId)}
"""
            )
        }
        .execute()
    return applicationForm.id ?: UUID.randomUUID()
}

data class DevFamilyContact(
    val id: UUID,
    val childId: ChildId,
    val contactPersonId: PersonId,
    val priority: Int
)

fun Database.Transaction.insert(row: DevFamilyContact): FamilyContactId =
    createUpdate {
            sql(
                """
INSERT INTO family_contact (id, child_id, contact_person_id, priority)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.contactPersonId)}, ${bind(row.priority)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevBackupPickup(
    val id: BackupPickupId,
    val childId: ChildId,
    val name: String,
    val phone: String
)

fun Database.Transaction.insert(row: DevBackupPickup): BackupPickupId =
    createUpdate {
            sql(
                """
INSERT INTO backup_pickup (id, child_id, name, phone)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.name)}, ${bind(row.phone)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevFridgeChild(
    val id: ParentshipId = ParentshipId(UUID.randomUUID()),
    val childId: ChildId,
    val headOfChild: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val conflict: Boolean = false
)

fun Database.Transaction.insert(row: DevFridgeChild): ParentshipId =
    createUpdate {
            sql(
                """
INSERT INTO fridge_child (id, child_id, head_of_child, start_date, end_date, conflict)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.headOfChild)}, ${bind(row.startDate)}, ${bind(row.endDate)}, ${bind(row.conflict)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevFridgePartner(
    val partnershipId: PartnershipId,
    val indx: Int,
    val otherIndx: Int,
    val personId: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val createdAt: HelsinkiDateTime,
    val conflict: Boolean = false
)

fun Database.Transaction.insert(row: DevFridgePartner): PartnershipId =
    createUpdate {
            sql(
                """
INSERT INTO fridge_partner (partnership_id, indx, other_indx, person_id, start_date, end_date, created_at, conflict)
VALUES (${bind(row.partnershipId)}, ${bind(row.indx)}, ${bind(row.otherIndx)}, ${bind(row.personId)}, ${bind(row.startDate)}, ${bind(row.endDate)}, ${bind(row.createdAt)}, ${bind(row.conflict)})
RETURNING partnership_id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevFridgePartnership(
    val id: PartnershipId = PartnershipId(UUID.randomUUID()),
    val first: PersonId,
    val second: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val conflict: Boolean = false
)

fun Database.Transaction.insert(partnership: DevFridgePartnership): PartnershipId =
    insert(
            DevFridgePartner(
                partnership.id,
                indx = 1,
                otherIndx = 2,
                personId = partnership.first,
                startDate = partnership.startDate,
                endDate = partnership.endDate,
                createdAt = partnership.createdAt,
                conflict = partnership.conflict
            )
        )
        .also {
            insert(
                DevFridgePartner(
                    partnership.id,
                    indx = 2,
                    otherIndx = 1,
                    personId = partnership.second,
                    startDate = partnership.startDate,
                    endDate = partnership.endDate,
                    createdAt = partnership.createdAt,
                    conflict = partnership.conflict
                )
            )
        }

data class DevEmployeePin(
    val id: UUID = UUID.randomUUID(),
    val userId: EmployeeId? = null,
    val employeeExternalId: String? = null,
    val pin: String,
    val locked: Boolean = false
)

data class DevFosterParent(
    val id: FosterParentId = FosterParentId(UUID.randomUUID()),
    val childId: ChildId,
    val parentId: PersonId,
    val validDuring: DateRange
)

fun Database.Transaction.insert(row: DevFosterParent): FosterParentId =
    createUpdate {
            sql(
                """
INSERT INTO foster_parent (id, child_id, parent_id, valid_during)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.parentId)}, ${bind(row.validDuring)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevEmployeePin): EmployeePinId =
    createUpdate {
            sql(
                """
INSERT INTO employee_pin (id, user_id, pin, locked)
VALUES (${bind(row.id)}, ${bind(row.userId)}, ${bind(row.pin)}, ${bind(row.locked)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.getEmployeeIdByExternalId(externalId: String) =
    createQuery { sql("SELECT id FROM employee WHERE external_id = ${bind(externalId)}") }
        .exactlyOne<EmployeeId>()

fun Database.Transaction.insert(row: DevDaycareGroupAcl) {
    createUpdate {
            sql(
                """
INSERT INTO daycare_group_acl (daycare_group_id, employee_id)
VALUES (${bind(row.groupId)}, ${bind(row.employeeId)})
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevVardaOrganizerChild) {
    createUpdate {
            sql(
                """
INSERT INTO varda_organizer_child (evaka_person_id, varda_person_oid, varda_child_id, organizer_oid, uploaded_at, varda_person_id)
VALUES (${bind(row.evakaPersonId)}, ${bind(row.vardaPersonOid)}, ${bind(row.vardaChildId)}, ${bind(row.organizerOid)}, ${bind(row.uploadedAt)}, ${bind(row.vardaPersonId)})
RETURNING evaka_person_id
"""
            )
        }
        .execute()
}

fun Database.Transaction.insertVardaServiceNeed(row: VardaServiceNeed) {
    createUpdate {
            sql(
                """
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_updated, evaka_child_id, varda_decision_id, varda_placement_id, update_failed, errors)
VALUES (
    ${bind(row.evakaServiceNeedId)},
    ${bind(row.evakaServiceNeedUpdated)},
    ${bind(row.evakaChildId)},
    ${bind(row.vardaDecisionId)},
    ${bind(row.vardaPlacementId)},
    ${bind(row.updateFailed)},
    ${bind(row.errors)}
)
RETURNING evaka_service_need_id
    """
            )
        }
        .execute()
}

data class DevPedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String
)

fun Database.Transaction.insert(row: DevPedagogicalDocument): PedagogicalDocumentId =
    createUpdate {
            sql(
                """
INSERT INTO pedagogical_document (id, child_id, description)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.description)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

data class DevReservation(
    val id: AttendanceReservationId = AttendanceReservationId(UUID.randomUUID()),
    val childId: ChildId,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val created: HelsinkiDateTime = HelsinkiDateTime.now(),
    val createdBy: EvakaUserId
)

fun Database.Transaction.insert(row: DevReservation): AttendanceReservationId =
    createUpdate {
            sql(
                """
INSERT INTO attendance_reservation (id, child_id, date, start_time, end_time, created, created_by)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.date)}, ${bind(row.startTime)}, ${bind(row.endTime)}, ${bind(row.created)}, ${bind(row.createdBy)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevGuardianBlocklistEntry) {
    createUpdate {
            sql(
                """
INSERT INTO guardian_blocklist (guardian_id, child_id)
VALUES (${bind(row.guardianId)}, ${bind(row.childId)})
"""
            )
        }
        .execute()
}

data class DevInvoiceCorrection(
    val id: InvoiceCorrectionId = InvoiceCorrectionId(UUID.randomUUID()),
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String
)

fun Database.Transaction.insert(row: DevInvoiceCorrection): InvoiceCorrectionId =
    createUpdate {
            sql(
                """
INSERT INTO invoice_correction (id, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (${bind(row.id)}, ${bind(row.headOfFamilyId)}, ${bind(row.childId)}, ${bind(row.unitId)}, ${bind(row.product)}, ${bind(row.period)}, ${bind(row.amount)}, ${bind(row.unitPrice)}, ${bind(row.description)}, ${bind(row.note)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevPayment): PaymentId =
    createUpdate {
            sql(
                """
INSERT INTO payment (id, unit_id, unit_name, unit_business_id, unit_iban, unit_provider_id, period, number, amount, status, payment_date, due_date, sent_at, sent_by)
VALUES (${bind(row.id)}, ${bind(row.unitId)}, ${bind(row.unitName)}, ${bind(row.unitBusinessId)}, ${bind(row.unitIban)}, ${bind(row.unitProviderId)}, ${bind(row.period)}, ${bind(row.number)}, ${bind(row.amount)}, ${bind(row.status)}, ${bind(row.paymentDate)}, ${bind(row.dueDate)}, ${bind(row.sentAt)}, ${bind(row.sentBy)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevCalendarEvent): CalendarEventId =
    createUpdate {
            sql(
                """
INSERT INTO calendar_event (id, title, description, period, modified_at, content_modified_at)
VALUES (${bind(row.id)}, ${bind(row.title)}, ${bind(row.description)}, ${bind(row.period)}, ${bind(row.modifiedAt)}, ${bind(row.modifiedAt)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevCalendarEventAttendee): CalendarEventAttendeeId =
    createUpdate {
            sql(
                """
INSERT INTO calendar_event_attendee (id, calendar_event_id, unit_id, group_id, child_id)
VALUES (${bind(row.id)}, ${bind(row.calendarEventId)}, ${bind(row.unitId)}, ${bind(row.groupId)}, ${bind(row.childId)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDailyServiceTimes): DailyServiceTimesId =
    createUpdate {
            sql(
                """
INSERT INTO daily_service_time (id, child_id, type, validity_period, regular_times, monday_times, tuesday_times, wednesday_times, thursday_times, friday_times, saturday_times, sunday_times)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.type)}, ${bind(row.validityPeriod)}, ${bind(row.regularTimes)}, ${bind(row.mondayTimes)}, ${bind(row.tuesdayTimes)}, ${bind(row.wednesdayTimes)}, ${bind(row.thursdayTimes)}, ${bind(row.fridayTimes)}, ${bind(row.saturdayTimes)}, ${bind(row.sundayTimes)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevGuardian) {
    createUpdate {
            sql(
                """
INSERT INTO guardian (guardian_id, child_id)
VALUES (${bind(row.guardianId)}, ${bind(row.childId)})
ON CONFLICT (guardian_id, child_id) DO NOTHING
"""
            )
        }
        .execute()
}

fun Database.Transaction.insert(row: DevAbsence): AbsenceId =
    createUpdate {
            sql(
                """
INSERT INTO absence (id, child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.date)}, ${bind(row.absenceType)}, ${bind(row.modifiedAt)}, ${bind(row.modifiedBy)}, ${bind(row.absenceCategory)}, ${bind(row.questionnaireId)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDaycareCaretaker): DaycareCaretakerId =
    createUpdate {
            sql(
                """
INSERT INTO daycare_caretaker (id, group_id, amount, start_date, end_date)
VALUES (${bind(row.id)}, ${bind(row.groupId)}, ${bind(row.amount)}, ${bind(row.startDate)}, ${bind(row.endDate)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDocumentTemplate): DocumentTemplateId =
    createUpdate {
            sql(
                """
INSERT INTO document_template (id, name, type, language, confidential, legal_basis, validity, published, content) 
VALUES (${bind(row.id)}, ${bind(row.name)}, ${bind(row.type)}, ${bind(row.language)}, ${bind(row.confidential)}, ${bind(row.legalBasis)}, ${bind(row.validity)}, ${bind(row.published)}, ${bind(row.content)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevChildDocument): ChildDocumentId =
    createUpdate {
            sql(
                """
INSERT INTO child_document (id, status, child_id, template_id, content, published_content, modified_at, published_at)
VALUES (${bind(row.id)}, ${bind(row.status)}, ${bind(row.childId)}, ${bind(row.templateId)}, ${bind(row.content)}, ${bind(row.publishedContent)}, ${bind(row.modifiedAt)}, ${bind(row.publishedAt)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.updateDaycareOperationTimes(
    daycareId: DaycareId,
    operationTimes: List<TimeRange>
) =
    createUpdate {
            sql(
                "UPDATE daycare SET operation_times = ${bind(operationTimes)} WHERE id = ${bind(daycareId)}",
            )
        }
        .execute()

fun Database.Transaction.insert(row: DevAssistanceFactor): AssistanceFactorId =
    createUpdate {
            sql(
                """
INSERT INTO assistance_factor (id, child_id, valid_during, capacity_factor, modified, modified_by)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.validDuring)}, ${bind(row.capacityFactor)}, ${bind(row.modified)}, ${bind(row.modifiedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevDaycareAssistance): DaycareAssistanceId =
    createUpdate {
            sql(
                """
INSERT INTO daycare_assistance (id, child_id, valid_during, level, modified, modified_by)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.validDuring)}, ${bind(row.level)}, ${bind(row.modified)}, ${bind(row.modifiedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevPreschoolAssistance): PreschoolAssistanceId =
    createUpdate {
            sql(
                """
INSERT INTO preschool_assistance (id, child_id, valid_during, level, modified, modified_by)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.validDuring)}, ${bind(row.level)}, ${bind(row.modified)}, ${bind(row.modifiedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevOtherAssistanceMeasure): OtherAssistanceMeasureId =
    createUpdate {
            sql(
                """
INSERT INTO other_assistance_measure (id, child_id, valid_during, type, modified, modified_by)
VALUES (${bind(row.id)}, ${bind(row.childId)}, ${bind(row.validDuring)}, ${bind(row.type)}, ${bind(row.modified)}, ${bind(row.modifiedBy)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevServiceNeed): ServiceNeedId =
    createUpdate {
            sql(
                """
INSERT INTO service_need (id, option_id, placement_id, start_date, end_date, shift_care, confirmed_by, confirmed_at)
VALUES (${bind(row.id)}, ${bind(row.optionId)}, ${bind(row.placementId)}, ${bind(row.startDate)}, ${bind(row.endDate)}, ${bind(row.shiftCare)}, ${bind(row.confirmedBy)}, ${bind(row.confirmedAt)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevChildAttendance): ChildAttendanceId =
    createUpdate {
            sql(
                """
INSERT INTO child_attendance (child_id, unit_id, date, start_time, end_time)
VALUES (${bind(row.childId)}, ${bind(row.unitId)}, ${bind(row.date)}, ${bind(row.arrived)}, ${bind(row.departed)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevPreschoolTerm): PreschoolTermId =
    createUpdate {
            sql(
                """
INSERT INTO preschool_term (id, finnish_preschool, swedish_preschool, extended_term, application_period, term_breaks) 
VALUES (${bind(row.id)}, ${bind(row.finnishPreschool)}, ${bind(row.swedishPreschool)}, ${bind(row.extendedTerm)}, ${bind(row.applicationPeriod)}, ${bind(row.termBreaks)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.insert(row: DevClubTerm): ClubTermId =
    createUpdate {
            sql(
                """
INSERT INTO club_term (id, term, application_period, term_breaks) 
VALUES (${bind(row.id)}, ${bind(row.term)}, ${bind(row.applicationPeriod)}, ${bind(row.termBreaks)})
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
