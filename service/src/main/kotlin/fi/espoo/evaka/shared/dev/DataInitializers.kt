// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.getApplicationType
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistanceaction.insertAssistanceActionOptionRefs
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
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
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.CalendarEventAttendeeId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
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
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.StaffAttendancePlanId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
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
import org.intellij.lang.annotations.Language
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
    if (createQuery("SELECT count(*) FROM care_area").exactlyOne<Int>() == 0) {
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

/**
 * Insert one row of data to the database.
 * * all fields from `row` are bound separately as parameters
 * * SQL must return "id" column of type UUID
 */
private fun Database.Transaction.insertTestDataRow(row: Any, @Language("sql") sql: String): UUID =
    createUpdate(sql).bindKotlin(row).executeAndReturnGeneratedKeys().exactlyOne<UUID>()

fun Database.Transaction.insert(area: DevCareArea): AreaId =
    insertTestDataRow(
            area,
            """
INSERT INTO care_area (id, name, short_name, area_code, sub_cost_center)
VALUES (:id, :name, :shortName, :areaCode, :subCostCenter)
RETURNING id
"""
        )
        .let(::AreaId)

fun Database.Transaction.insert(daycare: DevDaycare): DaycareId =
    insertTestDataRow(
            daycare,
            """
INSERT INTO daycare (
  id, name, opening_date, closing_date, care_area_id, type, daycare_apply_period, preschool_apply_period, club_apply_period, provider_type,
  capacity, language, ghost_unit, upload_to_varda, upload_children_to_varda, upload_to_koski, invoiced_by_municipality, cost_center, dw_cost_center,
  additional_info, phone, email, url,
  street_address, postal_code, post_office,
  location, mailing_street_address, mailing_po_box, mailing_postal_code, mailing_post_office,
  unit_manager_name, unit_manager_phone, unit_manager_email,
  decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address,
  oph_unit_oid, oph_organizer_oid, round_the_clock, operation_times, enabled_pilot_features,
  finance_decision_handler, business_id, iban, provider_id
) VALUES (
  :id, :name, :openingDate, :closingDate, :areaId, :type::care_types[], :daycareApplyPeriod, :preschoolApplyPeriod, :clubApplyPeriod, :providerType,
  :capacity, :language, :ghostUnit, :uploadToVarda, :uploadChildrenToVarda, :uploadToKoski, :invoicedByMunicipality, :costCenter, :dwCostCenter,
  :additionalInfo, :phone, :email, :url,
  :visitingAddress.streetAddress, :visitingAddress.postalCode, :visitingAddress.postOffice,
  :location, :mailingAddress.streetAddress, :mailingAddress.poBox, :mailingAddress.postalCode, :mailingAddress.postOffice,
  :unitManager.name, :unitManager.phone, :unitManager.email,
  :decisionCustomization.daycareName, :decisionCustomization.preschoolName, :decisionCustomization.handler, :decisionCustomization.handlerAddress,
  :ophUnitOid, :ophOrganizerOid, :roundTheClock, :operationTimes, :enabledPilotFeatures::pilot_feature[], :financeDecisionHandler,
  :businessId, :iban, :providerId
)
RETURNING id
"""
        )
        .let(::DaycareId)

fun Database.Transaction.updateDaycareAcl(
    daycareId: DaycareId,
    externalId: ExternalId,
    role: UserRole
) {
    createUpdate(
            "INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES ((SELECT id from employee where external_id = :external_id), :daycare_id, :role)"
        )
        .bind("daycare_id", daycareId)
        .bind("external_id", externalId)
        .bind("role", role)
        .execute()
}

fun Database.Transaction.updateDaycareAclWithEmployee(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole
) {
    createUpdate(
            "INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES (:employeeId, :daycare_id, :role)"
        )
        .bind("daycare_id", daycareId)
        .bind("employeeId", employeeId)
        .bind("role", role)
        .execute()
}

fun Database.Transaction.insertEmployeeToDaycareGroupAcl(groupId: GroupId, employeeId: EmployeeId) {
    createUpdate(
            "INSERT INTO daycare_group_acl (employee_id, daycare_group_id) VALUES (:employeeId, :daycare_group_id)"
        )
        .bind("daycare_group_id", groupId)
        .bind("employeeId", employeeId)
        .execute()
}

fun Database.Transaction.createMobileDeviceToUnit(
    id: MobileDeviceId,
    unitId: DaycareId,
    name: String = "Nimeämätön laite"
) {
    // language=sql
    val sql =
        """
        INSERT INTO mobile_device (id, unit_id, name) VALUES (:id, :unitId, :name);

        INSERT INTO evaka_user (id, type, mobile_device_id, name) VALUES (:id, 'MOBILE_DEVICE', :id, :name);
        """
            .trimIndent()

    createUpdate(sql).bind("id", id).bind("unitId", unitId).bind("name", name).execute()
}

fun Database.Transaction.insert(employee: DevEmployee) =
    insertTestDataRow(
            employee,
            """
INSERT INTO employee (id, preferred_first_name, first_name, last_name, email, external_id, employee_number, roles, last_login, active)
VALUES (:id, :preferredFirstName, :firstName, :lastName, :email, :externalId, :employeeNumber, :roles::user_role[], :lastLogin, :active)
RETURNING id
"""
        )
        .let(::EmployeeId)
        .also { upsertEmployeeUser(it) }

fun Database.Transaction.insert(device: DevMobileDevice) =
    insertTestDataRow(
            device,
            """
INSERT INTO mobile_device (id, unit_id, name, long_term_token, push_notification_categories)
VALUES (:id, :unitId, :name, :longTermToken, :pushNotificationCategories)
RETURNING id
    """
        )
        .let(::MobileDeviceId)

fun Database.Transaction.insert(device: DevPersonalMobileDevice) =
    insertTestDataRow(
            device,
            """
INSERT INTO mobile_device (id, employee_id, name, long_term_token)
VALUES (:id, :employeeId, :name, :longTermToken)
RETURNING id
    """
        )
        .let(::MobileDeviceId)

enum class DevPersonType {
    CHILD,
    ADULT,
    RAW_ROW,
}

fun Database.Transaction.insert(person: DevPerson, type: DevPersonType) =
    insertTestDataRow(
            person.copy(updatedFromVtj = if (person.ssn != null) HelsinkiDateTime.now() else null),
            """
INSERT INTO person (
    id, date_of_birth, date_of_death, first_name, last_name, preferred_name, social_security_number, email, phone, language,
    street_address, postal_code, post_office, residence_code, nationalities, restricted_details_enabled, restricted_details_end_date,
    invoicing_street_address, invoicing_postal_code, invoicing_post_office, updated_from_vtj, oph_person_oid, duplicate_of, enabled_email_types
) VALUES (
    :id, :dateOfBirth, :dateOfDeath, :firstName, :lastName, :preferredName, :ssn, :email, :phone, :language,
    :streetAddress, :postalCode, :postOffice, :residenceCode, :nationalities, :restrictedDetailsEnabled, :restrictedDetailsEndDate,
    :invoicingStreetAddress, :invoicingPostalCode, :invoicingPostOffice, :updatedFromVtj, :ophPersonOid, :duplicateOf, :enabledEmailTypes
)
RETURNING id
"""
        )
        .let(::PersonId)
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

fun Database.Transaction.insertTestParentship(
    headOfChild: PersonId,
    childId: ChildId,
    id: ParentshipId = ParentshipId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): ParentshipId {
    createUpdate(
            """
            INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
            VALUES (:id, :headOfChild, :childId, :startDate, :endDate)
            """
        )
        .bind("id", id)
        .bind("headOfChild", headOfChild)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .execute()
    return id
}

fun Database.Transaction.insert(parentship: DevParentship): ParentshipId =
    insertTestDataRow(
            parentship,
            """
INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
VALUES (:id, :headOfChildId, :childId, :startDate, :endDate)
        """
        )
        .let(::ParentshipId)

fun Database.Transaction.insertTestPartnership(
    adult1: PersonId,
    adult2: PersonId,
    id: PartnershipId = PartnershipId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null
): PartnershipId {
    insert(
        DevFridgePartner(
            partnershipId = id,
            indx = 1,
            otherIndx = 2,
            personId = adult1,
            startDate = startDate,
            endDate = endDate
        )
    )
    insert(
        DevFridgePartner(
            partnershipId = id,
            indx = 2,
            otherIndx = 1,
            personId = adult2,
            startDate = startDate,
            endDate = endDate
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
    createUpdate(
            """
            INSERT INTO application (type, id, sentdate, duedate, status, guardian_id, child_id, other_guardian_id, origin, hidefromguardian, additionalDaycareApplication, transferApplication, allow_other_guardian_access)
            VALUES (:type, :id, :sentDate, :dueDate, :status::application_status_type, :guardianId, :childId, :otherGuardianId, 'ELECTRONIC'::application_origin_type, :hideFromGuardian, :additionalDaycareApplication, :transferApplication, :allowOtherGuardianAccess)
            """
        )
        .bind("id", id)
        .bind("type", type)
        .bind("sentDate", sentDate)
        .bind("dueDate", dueDate)
        .bind("status", status)
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .bind("otherGuardianId", otherGuardianId)
        .bind("hideFromGuardian", hideFromGuardian)
        .bind("additionalDaycareApplication", additionalDaycareApplication)
        .bind("transferApplication", transferApplication)
        .bind("allowOtherGuardianAccess", allowOtherGuardianAccess)
        .execute()
    return id
}

fun Database.Transaction.insertTestApplicationForm(
    applicationId: ApplicationId,
    document: DaycareFormV0
) {
    check(getApplicationType(applicationId) == document.type) {
        "Invalid form type for the application"
    }

    createUpdate(
            """
UPDATE application SET document = :document, form_modified = now()
WHERE id = :applicationId
"""
        )
        .bind("applicationId", applicationId)
        .bindJson("document", document)
        .execute()
}

fun Database.Transaction.insertTestClubApplicationForm(
    applicationId: ApplicationId,
    document: ClubFormV0
) {
    check(getApplicationType(applicationId) == document.type) {
        "Invalid form type for the application"
    }

    createUpdate(
            """
UPDATE application SET document = :document, form_modified = now()
WHERE id = :applicationId
"""
        )
        .bind("applicationId", applicationId)
        .bindJson("document", document)
        .execute()
}

fun Database.Transaction.insert(child: DevChild) =
    insertTestDataRow(
            child,
            """
INSERT INTO child (id, allergies, diet, medication, additionalinfo, language_at_home, language_at_home_details)
VALUES (:id, :allergies, :diet, :medication, :additionalInfo, :languageAtHome, :languageAtHomeDetails)
ON CONFLICT(id) DO UPDATE
SET id = :id, allergies = :allergies, diet = :diet, medication = :medication, additionalInfo = :additionalInfo, language_at_home = :languageAtHome, language_at_home_details = :languageAtHomeDetails
RETURNING id
    """
        )
        .let(::ChildId)

fun Database.Transaction.insert(placement: DevPlacement) =
    insertTestDataRow(
            placement,
            """
INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date, termination_requested_date, terminated_by, place_guarantee)
VALUES (:id, :type, :childId, :unitId, :startDate, :endDate, :terminationRequestedDate, :terminatedBy, :placeGuarantee)
RETURNING id
"""
        )
        .let(::PlacementId)

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
    createUpdate(
            """
            INSERT INTO placement (id, child_id, unit_id, type, start_date, end_date, termination_requested_date, terminated_by, place_guarantee)
            VALUES (:id, :childId, :unitId, :type::placement_type, :startDate, :endDate, :terminationRequestedDate, :terminatedBy, :placeGuarantee)
            """
        )
        .bind("id", id)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("type", type)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("terminationRequestedDate", terminationRequestedDate)
        .bind("terminatedBy", terminatedBy)
        .bind("placeGuarantee", placeGuarantee)
        .execute()
    return id
}

fun Database.Transaction.insertTestHoliday(date: LocalDate, description: String = "holiday") {
    createUpdate(
            """
            INSERT INTO holiday (date, description)
            VALUES (:date, :description)
            """
        )
        .bind("date", date)
        .bind("description", description)
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
    createUpdate(
            """
INSERT INTO service_need (id, placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at)
VALUES (:id, :placementId, :startDate, :endDate, :optionId, :shiftCare, :confirmedBy, :confirmedAt);
"""
        )
        .bind("id", id)
        .bind("placementId", placementId)
        .bind("startDate", period.start)
        .bind("endDate", period.end)
        .bind("optionId", optionId)
        .bind("shiftCare", shiftCare)
        .bind("confirmedBy", confirmedBy)
        .bind("confirmedAt", confirmedAt)
        .execute()
    return id
}

fun Database.Transaction.insertServiceNeedOption(option: ServiceNeedOption) {
    createUpdate(
            // language=sql
            """
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, contract_days_per_month, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, active, updated)
VALUES (:id, :nameFi, :nameSv, :nameEn, :validPlacementType, :defaultOption, :feeCoefficient, :occupancyCoefficient, :occupancyCoefficientUnder3y, :realizedOccupancyCoefficient, :realizedOccupancyCoefficientUnder3y, :daycareHoursPerWeek, :contractDaysPerMonth, :partDay, :partWeek, :feeDescriptionFi, :feeDescriptionSv, :voucherValueDescriptionFi, :voucherValueDescriptionSv, :active, :updated)
"""
        )
        .bindKotlin(option)
        .execute()
}

fun Database.Transaction.upsertServiceNeedOption(option: ServiceNeedOption) {
    createUpdate(
            // language=sql
            """
ALTER TABLE service_need_option DISABLE TRIGGER set_timestamp;
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, updated)
VALUES (:id, :nameFi, :nameSv, :nameEn, :validPlacementType, :defaultOption, :feeCoefficient, :occupancyCoefficient, :occupancyCoefficientUnder3y, :realizedOccupancyCoefficient, :realizedOccupancyCoefficientUnder3y, :daycareHoursPerWeek, :partDay, :partWeek, :feeDescriptionFi, :feeDescriptionSv, :voucherValueDescriptionFi, :voucherValueDescriptionSv, :updated)
ON CONFLICT (id) DO UPDATE SET updated = :updated, daycare_hours_per_week = :daycareHoursPerWeek;
ALTER TABLE service_need_option ENABLE TRIGGER set_timestamp;
"""
        )
        .bindKotlin(option)
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

fun Database.Transaction.insert(income: DevIncome): IncomeId =
    insertTestDataRow(
            income,
            """
            INSERT INTO income (id, person_id, valid_from, valid_to, data, effect, is_entrepreneur, works_at_echa, updated_at, updated_by)
            VALUES (:id, :personId, :validFrom, :validTo, :data, :effect::income_effect, :isEntrepreneur, :worksAtEcha, :updatedAt, :updatedBy)
            """
        )
        .let(::IncomeId)

data class DevIncomeStatement(
    val id: IncomeStatementId,
    val personId: PersonId,
    val startDate: LocalDate,
    val type: IncomeStatementType,
    val grossEstimatedMonthlyIncome: Int,
    val handlerId: EmployeeId? = null
)

fun Database.Transaction.insert(incomeStatement: DevIncomeStatement) =
    insertTestDataRow(
            incomeStatement,
            """
INSERT INTO income_statement (id, person_id, start_date, type, gross_estimated_monthly_income, handler_id)
VALUES (:id, :personId, :startDate, :type, :grossEstimatedMonthlyIncome, :handlerId)
RETURNING id
"""
        )
        .let(::IncomeStatementId)

data class DevFeeAlteration(
    val id: FeeAlterationId,
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

fun Database.Transaction.insert(feeAlteration: DevFeeAlteration) =
    insertTestDataRow(
            feeAlteration,
            """
            INSERT INTO fee_alteration (id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, updated_at, updated_by)
            VALUES (:id, :personId, :type::fee_alteration_type, :amount, :isAbsolute, :validFrom, :validTo, :notes, :updatedAt, :updatedBy)
        """
        )
        .let(::FeeAlterationId)

fun Database.Transaction.insert(feeThresholds: FeeThresholds) =
    insertTestDataRow(
            feeThresholds,
            """
INSERT INTO fee_thresholds (valid_during, min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6, income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6, max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6, income_threshold_increase_6_plus, sibling_discount_2, sibling_discount_2_plus, max_fee, min_fee, temporary_fee, temporary_fee_part_day, temporary_fee_sibling, temporary_fee_sibling_part_day)
VALUES (:validDuring, :minIncomeThreshold2, :minIncomeThreshold3, :minIncomeThreshold4, :minIncomeThreshold5, :minIncomeThreshold6, :incomeMultiplier2, :incomeMultiplier3, :incomeMultiplier4, :incomeMultiplier5, :incomeMultiplier6, :maxIncomeThreshold2, :maxIncomeThreshold3, :maxIncomeThreshold4, :maxIncomeThreshold5, :maxIncomeThreshold6, :incomeThresholdIncrease6Plus, :siblingDiscount2, :siblingDiscount2Plus, :maxFee, :minFee, :temporaryFee, :temporaryFeePartDay, :temporaryFeeSibling, :temporaryFeeSiblingPartDay)
RETURNING id
    """
                .trimIndent()
        )
        .let(::FeeThresholdsId)

fun Database.Transaction.insert(group: DevDaycareGroup) =
    insertTestDataRow(
            group,
            """
INSERT INTO daycare_group (id, daycare_id, name, start_date, end_date)
VALUES (:id, :daycareId, :name, :startDate, :endDate)
"""
        )
        .let(::GroupId)

fun Database.Transaction.insert(groupPlacement: DevDaycareGroupPlacement) =
    insertTestDataRow(
            groupPlacement,
            """
INSERT INTO daycare_group_placement (id, daycare_placement_id, daycare_group_id, start_date, end_date)
VALUES (:id, :daycarePlacementId, :daycareGroupId, :startDate, :endDate)
    """
        )
        .let(::GroupPlacementId)

fun Database.Transaction.insertTestDaycareGroupPlacement(
    daycarePlacementId: PlacementId = PlacementId(UUID.randomUUID()),
    groupId: GroupId = GroupId(UUID.randomUUID()),
    id: GroupPlacementId = GroupPlacementId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): GroupPlacementId {
    createUpdate(
            """
                INSERT INTO daycare_group_placement (id, daycare_placement_id, daycare_group_id, start_date, end_date)
                VALUES (:id, :placementId, :groupId, :startDate, :endDate)
            """
        )
        .bind("id", id)
        .bind("placementId", daycarePlacementId)
        .bind("groupId", groupId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
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
    createUpdate(
            """
            INSERT INTO placement_plan (id, unit_id, application_id, type, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date, updated, deleted)
            VALUES (:id, :unitId, :applicationId, :type::placement_type, :startDate, :endDate, :preschoolDaycareStartDate, :preschoolDaycareEndDate, :updated, :deleted)
            """
        )
        .bind("id", id)
        .bind("applicationId", applicationId)
        .bind("unitId", unitId)
        .bind("type", type)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("preschoolDaycareStartDate", preschoolDaycareStartDate)
        .bind("preschoolDaycareEndDate", preschoolDaycareEndDate)
        .bind("updated", updated)
        .bind("deleted", deleted)
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

fun Database.Transaction.insertTestDecision(decision: TestDecision) =
    insertTestDataRow(
            decision,
            """
INSERT INTO decision (id, created_by, sent_date, unit_id, application_id, type, start_date, end_date, status, requested_start_date, resolved, resolved_by, pending_decision_emails_sent_count, pending_decision_email_sent)
VALUES (:id, :createdBy, :sentDate, :unitId, :applicationId, :type, :startDate, :endDate, :status, :requestedStartDate, :resolved, :resolvedBy, :pendingDecisionEmailsSentCount, :pendingDecisionEmailSent)
RETURNING id
"""
        )
        .let(::DecisionId)

fun Database.Transaction.insert(assistanceAction: DevAssistanceAction): AssistanceActionId =
    insertTestDataRow(
            assistanceAction,
            """
INSERT INTO assistance_action (id, updated_by, child_id, start_date, end_date, other_action)
VALUES (:id, :updatedBy, :childId, :startDate, :endDate, :otherAction)
RETURNING id
"""
        )
        .let(::AssistanceActionId)
        .also {
            val counts = insertAssistanceActionOptionRefs(it, assistanceAction.actions)
            assert(counts.size == assistanceAction.actions.size)
        }

fun Database.Transaction.insertTestCaretakers(
    groupId: GroupId,
    id: UUID = UUID.randomUUID(),
    amount: Double = 3.0,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null
) {
    createUpdate(
            """
            INSERT INTO daycare_caretaker (id, group_id, amount, start_date, end_date)
            VALUES (:id, :groupId, :amount, :startDate, :endDate)
        """
                .trimIndent()
        )
        .bind("id", id)
        .bind("groupId", groupId)
        .bind("amount", amount)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .execute()
}

fun Database.Transaction.insertTestAssistanceNeedPreschoolDecision(
    decision: DevAssistanceNeedPreschoolDecision
): AssistanceNeedPreschoolDecisionId {
    createUpdate(
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
            :id, :decisionNumber, :childId, :status, :annulmentReason, :language, :type, :validFrom, 
            :extendedCompulsoryEducation, :extendedCompulsoryEducationInfo, 
            :grantedAssistanceService, :grantedInterpretationService, 
            :grantedAssistiveDevices, :grantedServicesBasis, :selectedUnit, :primaryGroup, 
            :decisionBasis, :basisDocumentPedagogicalReport, :basisDocumentPsychologistStatement, 
            :basisDocumentSocialReport, :basisDocumentDoctorStatement, :basisDocumentOtherOrMissing, 
            :basisDocumentOtherOrMissingInfo, :basisDocumentsInfo, :guardiansHeardOn, :otherRepresentativeHeard, 
            :otherRepresentativeDetails, :viewOfGuardians, :preparer1EmployeeId, :preparer1Title, :preparer1PhoneNumber, 
            :preparer2EmployeeId, :preparer2Title, :preparer2PhoneNumber, 
            :decisionMakerEmployeeId, :decisionMakerTitle, 
            :sentForDecision, :decisionMade, :unreadGuardianIds
        )
    """
        )
        .bind("id", decision.id)
        .bind("decisionNumber", decision.decisionNumber)
        .bind("childId", decision.childId)
        .bind("status", decision.status)
        .bind("annulmentReason", decision.annulmentReason)
        .bindKotlin(decision.form)
        .bind("sentForDecision", decision.sentForDecision)
        .bind("decisionMade", decision.decisionMade)
        .bind("unreadGuardianIds", decision.unreadGuardianIds)
        .execute()

    decision.form.guardianInfo.forEach { guardian ->
        createUpdate(
                """
            INSERT INTO assistance_need_preschool_decision_guardian (
                id, assistance_need_decision_id, person_id, is_heard, details
            ) VALUES (
                :id, :decisionId, :personId, :isHeard, :details
            )
            """
            )
            .bind("decisionId", decision.id)
            .bindKotlin(guardian)
            .execute()
    }

    return decision.id
}

fun Database.Transaction.insertTestAssistanceNeedDecision(
    childId: ChildId,
    data: DevAssistanceNeedDecision
): AssistanceNeedDecisionId {
    // language=sql
    val sql =
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
            :id,
            :decisionNumber,
            :childId, 
            :validityPeriod, 
            :status,
            :language,
            :decisionMade,
            :sentForDecision,
            :selectedUnit, 
            :pedagogicalMotivation,
            :structuralMotivationOptSmallerGroup,
            :structuralMotivationOptSpecialGroup,
            :structuralMotivationOptSmallGroup,
            :structuralMotivationOptGroupAssistant, 
            :structuralMotivationOptChildAssistant,
            :structuralMotivationOptAdditionalStaff,
            :structuralMotivationDescription,
            :careMotivation,
            :serviceOptConsultationSpecialEd,
            :serviceOptPartTimeSpecialEd,
            :serviceOptFullTimeSpecialEd,
            :serviceOptInterpretationAndAssistanceServices,
            :serviceOptSpecialAides,
            :servicesMotivation,
            :expertResponsibilities,
            :guardiansHeardOn,
            :viewOfGuardians,
            :otherRepresentativeHeard,
            :otherRepresentativeDetails, 
            :assistanceLevels,
            :motivationForDecision,
            :decisionMakerEmployeeId,
            :decisionMakerTitle,
            :preparer1EmployeeId,
            :preparer1Title,
            :preparer2EmployeeId,
            :preparer2Title,
            :preparer1PhoneNumber,
            :preparer2PhoneNumber,
            :unreadGuardianIds,
            :annulmentReason
        )
        RETURNING id
        """
            .trimIndent()

    val id =
        createQuery(sql)
            .bindKotlin(data)
            .bind("childId", childId)
            .bind(
                "structuralMotivationOptSmallerGroup",
                data.structuralMotivationOptions.smallerGroup
            )
            .bind(
                "structuralMotivationOptSpecialGroup",
                data.structuralMotivationOptions.specialGroup
            )
            .bind("structuralMotivationOptSmallGroup", data.structuralMotivationOptions.smallGroup)
            .bind(
                "structuralMotivationOptGroupAssistant",
                data.structuralMotivationOptions.groupAssistant
            )
            .bind(
                "structuralMotivationOptChildAssistant",
                data.structuralMotivationOptions.childAssistant
            )
            .bind(
                "structuralMotivationOptAdditionalStaff",
                data.structuralMotivationOptions.additionalStaff
            )
            .bind("serviceOptConsultationSpecialEd", data.serviceOptions.consultationSpecialEd)
            .bind("serviceOptPartTimeSpecialEd", data.serviceOptions.partTimeSpecialEd)
            .bind("serviceOptFullTimeSpecialEd", data.serviceOptions.fullTimeSpecialEd)
            .bind(
                "serviceOptInterpretationAndAssistanceServices",
                data.serviceOptions.interpretationAndAssistanceServices
            )
            .bind("serviceOptSpecialAides", data.serviceOptions.specialAides)
            .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
            .bind("decisionMakerTitle", data.decisionMaker?.title)
            .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
            .bind("preparer1Title", data.preparedBy1?.title)
            .bind("preparer1PhoneNumber", data.preparedBy1?.phoneNumber)
            .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
            .bind("preparer2Title", data.preparedBy2?.title)
            .bind("preparer2PhoneNumber", data.preparedBy2?.phoneNumber)
            .bind("selectedUnit", data.selectedUnit?.id)
            .exactlyOne<AssistanceNeedDecisionId>()

    // language=sql
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
            .trimIndent()

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
    // language=sql
    val sql =
        """
        INSERT INTO staff_attendance (id, group_id, date, count)
        VALUES (:id, :groupId, :date, :count)
        """
            .trimIndent()
    createUpdate(sql)
        .bind("id", id)
        .bind("groupId", groupId)
        .bind("date", date)
        .bind("count", count)
        .execute()
}

data class DevStaffAttendancePlan(
    val id: StaffAttendancePlanId = StaffAttendancePlanId(UUID.randomUUID()),
    val employeeId: EmployeeId,
    val type: StaffAttendanceType,
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime,
    val description: String?
)

fun Database.Transaction.insert(staffAttendancePlan: DevStaffAttendancePlan) =
    insertTestDataRow(
            staffAttendancePlan,
            """
        INSERT INTO staff_attendance_plan (id, employee_id, type, start_time, end_time)
        VALUES (:id, :employeeId, :type, :startTime, :endTime)
        """
        )
        .let(::StaffAttendancePlanId)

fun Database.Transaction.insertTestAbsence(
    id: AbsenceId = AbsenceId(UUID.randomUUID()),
    childId: ChildId,
    date: LocalDate,
    category: AbsenceCategory,
    absenceType: AbsenceType = AbsenceType.SICKLEAVE,
    modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
    modifiedAt: HelsinkiDateTime? = null
) {
    // language=sql
    val sql =
        """
        INSERT INTO absence (id, child_id, date, category, absence_type, modified_by, modified_at)
        VALUES (:id, :childId, :date, :category, :absenceType, :modifiedBy, coalesce(:modifiedAt, now()))
        """
            .trimIndent()
    createUpdate(sql)
        .bind("id", id)
        .bind("childId", childId)
        .bind("date", date)
        .bind("category", category)
        .bind("absenceType", absenceType)
        .bind("modifiedBy", modifiedBy)
        .bind("modifiedAt", modifiedAt)
        .execute()
}

fun Database.Transaction.insert(
    staffAttendanceRealtime: DevStaffAttendance
): StaffAttendanceRealtimeId {
    val sql =
        """
INSERT INTO staff_attendance_realtime (id, employee_id, group_id, arrived, departed, occupancy_coefficient, type)
VALUES (:id, :employeeId, :groupId, :arrived, :departed, :occupancyCoefficient, :type)
RETURNING id
    """
    return insertTestDataRow(staffAttendanceRealtime, sql).let(::StaffAttendanceRealtimeId)
}

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
                .trimIndent()
        )
        .also { batch ->
            attendances.forEach { (date, startTime, endTime) ->
                batch
                    .bind("id", AttendanceId(UUID.randomUUID()))
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
    // language=sql
    val sql =
        """
        INSERT INTO backup_care (id, child_id, unit_id, start_date, end_date, group_id)
        VALUES (:id, :childId, :unitId, :startDate, :endDate, :groupId)
        """
            .trimIndent()
    createUpdate(sql)
        .bind("id", id)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("groupId", groupId)
        .execute()
}

fun Database.Transaction.insert(backupCare: DevBackupCare) =
    insertTestDataRow(
            backupCare,
            """
INSERT INTO backup_care (id, child_id, unit_id, group_id, start_date, end_date)
VALUES (:id, :childId, :unitId, :groupId, :period.start, :period.end)
RETURNING id
"""
        )
        .let(::BackupCareId)

fun Database.Transaction.insertApplication(application: DevApplicationWithForm): ApplicationId {
    // language=sql
    val sql =
        """
        INSERT INTO application(id, type, sentdate, duedate, status, guardian_id, child_id, origin, checkedbyadmin, hidefromguardian, transferapplication, other_guardian_id, allow_other_guardian_access)
        VALUES(:id, :type, :sentDate, :dueDate, :applicationStatus::application_status_type, :guardianId, :childId, :origin::application_origin_type, :checkedByAdmin, :hideFromGuardian, :transferApplication, :otherGuardianId, :allowOtherGuardianAccess)
        """
            .trimIndent()

    createUpdate(sql)
        .bind("id", application.id)
        .bind("type", application.type)
        .bind("sentDate", application.sentDate)
        .bind("dueDate", application.dueDate)
        .bind("applicationStatus", application.status)
        .bind("guardianId", application.guardianId)
        .bind("childId", application.childId)
        .bind("origin", application.origin)
        .bind("checkedByAdmin", application.checkedByAdmin)
        .bind("hideFromGuardian", application.hideFromGuardian)
        .bind("transferApplication", application.transferApplication)
        .bind("otherGuardianId", application.otherGuardianId)
        .bind("allowOtherGuardianAccess", application.allowOtherGuardianAccess)
        .execute()

    return application.id
}

fun Database.Transaction.insertApplicationForm(applicationForm: DevApplicationForm): UUID {
    check(getApplicationType(applicationForm.applicationId) == applicationForm.document.type) {
        "Invalid form type for the application"
    }

    createUpdate(
            """
UPDATE application SET document = :document, form_modified = now()
WHERE id = :applicationId
"""
        )
        .bind("applicationId", applicationForm.applicationId)
        .bindJson("document", applicationForm.document)
        .execute()
    return applicationForm.id ?: UUID.randomUUID()
}

data class DevFamilyContact(
    val id: UUID,
    val childId: ChildId,
    val contactPersonId: PersonId,
    val priority: Int
)

fun Database.Transaction.insert(contact: DevFamilyContact) =
    insertTestDataRow(
            contact,
            """
INSERT INTO family_contact (id, child_id, contact_person_id, priority)
VALUES (:id, :childId, :contactPersonId, :priority)
RETURNING id
"""
        )
        .let(::FamilyContactId)

data class DevBackupPickup(
    val id: BackupPickupId,
    val childId: ChildId,
    val name: String,
    val phone: String
)

fun Database.Transaction.insert(pickup: DevBackupPickup) =
    insertTestDataRow(
            pickup,
            """
INSERT INTO backup_pickup (id, child_id, name, phone)
VALUES (:id, :childId, :name, :phone)
RETURNING id
"""
        )
        .let(::BackupPickupId)

data class DevFridgeChild(
    val id: ParentshipId = ParentshipId(UUID.randomUUID()),
    val childId: ChildId,
    val headOfChild: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val conflict: Boolean = false
)

fun Database.Transaction.insert(pickup: DevFridgeChild) =
    insertTestDataRow(
            pickup,
            """
INSERT INTO fridge_child (id, child_id, head_of_child, start_date, end_date, conflict)
VALUES (:id, :childId, :headOfChild, :startDate, :endDate, :conflict)
RETURNING id
"""
        )
        .let(::ParentshipId)

data class DevFridgePartner(
    val partnershipId: PartnershipId,
    val indx: Int,
    val otherIndx: Int,
    val personId: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)

fun Database.Transaction.insert(pickup: DevFridgePartner) =
    insertTestDataRow(
            pickup,
            """
INSERT INTO fridge_partner (partnership_id, indx, other_indx, person_id, start_date, end_date)
VALUES (:partnershipId, :indx, :otherIndx, :personId, :startDate, :endDate)
RETURNING partnership_id
"""
        )
        .let(::PartnershipId)

data class DevFridgePartnership(
    val id: PartnershipId = PartnershipId(UUID.randomUUID()),
    val first: PersonId,
    val second: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)

fun Database.Transaction.insert(partnership: DevFridgePartnership): PartnershipId =
    insert(
            DevFridgePartner(
                partnership.id,
                indx = 1,
                otherIndx = 2,
                personId = partnership.first,
                startDate = partnership.startDate,
                endDate = partnership.endDate
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
                    endDate = partnership.endDate
                )
            )
        }

data class DevEmployeePin(
    val id: UUID = UUID.randomUUID(),
    val userId: EmployeeId? = null,
    val employeeExternalId: String? = null,
    val pin: String,
    val locked: Boolean? = false
)

data class DevFosterParent(
    val id: FosterParentId = FosterParentId(UUID.randomUUID()),
    val childId: ChildId,
    val parentId: PersonId,
    val validDuring: DateRange
)

fun Database.Transaction.insert(fixture: DevFosterParent) =
    insertTestDataRow(
            fixture,
            """
INSERT INTO foster_parent (id, child_id, parent_id, valid_during)
VALUES (:id, :childId, :parentId, :validDuring)
RETURNING id
"""
        )
        .let(::FosterParentId)

fun Database.Transaction.insert(employeePin: DevEmployeePin) =
    insertTestDataRow(
            employeePin,
            """
INSERT INTO employee_pin (id, user_id, pin, locked)
VALUES (:id, :userId, :pin, :locked)
RETURNING id
"""
        )
        .let(::EmployeePinId)

fun Database.Transaction.getEmployeeIdByExternalId(externalId: String) =
    createQuery("SELECT id FROM employee WHERE external_id = :id")
        .bind("id", externalId)
        .exactlyOne<EmployeeId>()

fun Database.Transaction.insert(aclRow: DevDaycareGroupAcl) {
    createUpdate(
            """
INSERT INTO daycare_group_acl (daycare_group_id, employee_id)
VALUES (:groupId, :employeeId)
"""
        )
        .bindKotlin(aclRow)
        .execute()
}

fun Database.Transaction.insert(vardaOrganizerChild: DevVardaOrganizerChild) =
    insertTestDataRow(
        vardaOrganizerChild,
        """
INSERT INTO varda_organizer_child (evaka_person_id, varda_person_oid, varda_child_id, organizer_oid, uploaded_at, varda_person_id)
VALUES (:evakaPersonId, :vardaPersonOid, :vardaChildId, :organizerOid, :uploadedAt, :vardaPersonId)
RETURNING evaka_person_id"""
    )

fun Database.Transaction.insertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed) =
    insertTestDataRow(
        vardaServiceNeed,
        """
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_updated, evaka_child_id, varda_decision_id, varda_placement_id, update_failed, errors)
VALUES (:evakaServiceNeedId, :evakaServiceNeedUpdated, :evakaChildId, :vardaDecisionId, :vardaPlacementId, :updateFailed, :errors)
RETURNING evaka_service_need_id
    """
    )

data class DevPedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String
)

fun Database.Transaction.insert(pedagogicalDocument: DevPedagogicalDocument) =
    insertTestDataRow(
            pedagogicalDocument,
            """
INSERT INTO pedagogical_document (id, child_id, description)
VALUES (:id, :childId, :description)
RETURNING id
    """
        )
        .let(::PedagogicalDocumentId)

data class DevReservation(
    val id: AttendanceReservationId = AttendanceReservationId(UUID.randomUUID()),
    val childId: ChildId,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val created: HelsinkiDateTime = HelsinkiDateTime.now(),
    val createdBy: EvakaUserId
)

fun Database.Transaction.insert(reservation: DevReservation) =
    insertTestDataRow(
            reservation,
            """
INSERT INTO attendance_reservation (id, child_id, date, start_time, end_time, created, created_by)
VALUES (:id, :childId, :date, :startTime, :endTime, :created, :createdBy)
RETURNING id
"""
        )
        .let(::AttendanceReservationId)

fun Database.Transaction.insert(entry: DevGuardianBlocklistEntry) {
    createUpdate(
            """
INSERT INTO guardian_blocklist (guardian_id, child_id)
VALUES (:guardianId, :childId)
        """
        )
        .bindKotlin(entry)
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

fun Database.Transaction.insert(invoiceCorrection: DevInvoiceCorrection) =
    insertTestDataRow(
            invoiceCorrection,
            """
INSERT INTO invoice_correction (id, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (:id, :headOfFamilyId, :childId, :unitId, :product, :period, :amount, :unitPrice, :description, :note)
RETURNING id
"""
        )
        .let(::InvoiceCorrectionId)

fun Database.Transaction.insert(payment: DevPayment) =
    insertTestDataRow(
            payment,
            """
INSERT INTO payment (id, unit_id, unit_name, unit_business_id, unit_iban, unit_provider_id, period, number, amount, status, payment_date, due_date, sent_at, sent_by)
VALUES (:id, :unitId, :unitName, :unitBusinessId, :unitIban, :unitProviderId, :period, :number, :amount, :status, :paymentDate, :dueDate, :sentAt, :sentBy)
RETURNING id
"""
        )
        .let(::PaymentId)

fun Database.Transaction.insert(calendarEvent: DevCalendarEvent) =
    insertTestDataRow(
            calendarEvent,
            """
INSERT INTO calendar_event (id, title, description, period)
VALUES (:id, :title, :description, :period)
RETURNING id
"""
        )
        .let(::CalendarEventId)

fun Database.Transaction.insert(attendee: DevCalendarEventAttendee) =
    insertTestDataRow(
            attendee,
            """
INSERT INTO calendar_event_attendee (id, calendar_event_id, unit_id, group_id, child_id)
VALUES (:id, :calendarEventId, :unitId, :groupId, :childId)
RETURNING id
"""
        )
        .let(::CalendarEventAttendeeId)

fun Database.Transaction.insert(dailyServiceTimes: DevDailyServiceTimes) =
    insertTestDataRow(
            dailyServiceTimes,
            """
INSERT INTO daily_service_time (id, child_id, type, validity_period, regular_times, monday_times, tuesday_times, wednesday_times, thursday_times, friday_times, saturday_times, sunday_times)
VALUES (:id, :childId, :type, :validityPeriod, :regularTimes, :mondayTimes, :tuesdayTimes, :wednesdayTimes, :thursdayTimes, :fridayTimes, :saturdayTimes, :sundayTimes)
"""
        )
        .let(::DailyServiceTimesId)

fun Database.Transaction.insert(guardian: DevGuardian) {
    createUpdate(
            """
INSERT INTO guardian (guardian_id, child_id)
VALUES (:guardianId, :childId)
ON CONFLICT (guardian_id, child_id) DO NOTHING
"""
        )
        .bindKotlin(guardian)
        .execute()
}

fun Database.Transaction.insert(absence: DevAbsence) =
    insertTestDataRow(
            absence,
            """
INSERT INTO absence (id, child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id)
VALUES (:id, :childId, :date, :absenceType, :modifiedAt, :modifiedBy, :absenceCategory, :questionnaireId)
"""
        )
        .let(::AbsenceId)

fun Database.Transaction.insert(row: DevDaycareCaretaker) =
    insertTestDataRow(
            row,
            """
INSERT INTO daycare_caretaker (id, group_id, amount, start_date, end_date)
VALUES (:id, :groupId, :amount, :startDate, :endDate)
"""
        )
        .let(::DaycareCaretakerId)

fun Database.Transaction.insert(row: DevDocumentTemplate) =
    insertTestDataRow(
            row,
            """
INSERT INTO document_template (id, name, type, language, confidential, legal_basis, validity, published, content) 
VALUES (:id, :name, :type, :language, :confidential, :legalBasis, :validity, :published, :content)
"""
        )
        .let(::DocumentTemplateId)

fun Database.Transaction.insert(row: DevChildDocument) =
    insertTestDataRow(
            row,
            """
INSERT INTO child_document (id, status, child_id, template_id, content, published_content, modified_at, published_at)
VALUES (:id, :status, :childId, :templateId, :content, :publishedContent, :modifiedAt, :publishedAt)
"""
        )
        .let(::ChildDocumentId)

fun Database.Transaction.updateDaycareOperationTimes(
    daycareId: DaycareId,
    opTimes: List<TimeRange>
) =
    createUpdate(
            "UPDATE daycare SET operation_times = :operationTimes WHERE id = :daycareId",
        )
        .bind("daycareId", daycareId)
        .bind("operationTimes", opTimes)
        .execute()

fun Database.Transaction.insert(assistanceFactor: DevAssistanceFactor) =
    insertTestDataRow(
            assistanceFactor,
            """
INSERT INTO assistance_factor (id, child_id, valid_during, capacity_factor, modified, modified_by)
VALUES (:id, :childId, :validDuring, :capacityFactor, :modified, :modifiedBy)
"""
        )
        .let(::AssistanceFactorId)

fun Database.Transaction.insert(daycareAssistance: DevDaycareAssistance) =
    insertTestDataRow(
            daycareAssistance,
            """
INSERT INTO daycare_assistance (id, child_id, valid_during, level, modified, modified_by)
VALUES (:id, :childId, :validDuring, :level, :modified, :modifiedBy)
"""
        )
        .let(::DaycareAssistanceId)

fun Database.Transaction.insert(preschoolAssistance: DevPreschoolAssistance) =
    insertTestDataRow(
            preschoolAssistance,
            """
INSERT INTO preschool_assistance (id, child_id, valid_during, level, modified, modified_by)
VALUES (:id, :childId, :validDuring, :level, :modified, :modifiedBy)
"""
        )
        .let(::PreschoolAssistanceId)

fun Database.Transaction.insert(otherAssistanceMeasure: DevOtherAssistanceMeasure) =
    insertTestDataRow(
            otherAssistanceMeasure,
            """
INSERT INTO other_assistance_measure (id, child_id, valid_during, type, modified, modified_by)
VALUES (:id, :childId, :validDuring, :type, :modified, :modifiedBy)
"""
        )
        .let(::OtherAssistanceMeasureId)
