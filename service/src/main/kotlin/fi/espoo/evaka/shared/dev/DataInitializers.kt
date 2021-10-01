// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.objectMapper
import fi.espoo.evaka.assistanceaction.insertAssistanceActionOptionRefs
import fi.espoo.evaka.assistanceneed.insertAssistanceBasisOptionRefs
import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.VardaServiceNeed
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json
import org.postgresql.util.PGobject
import org.springframework.core.io.ClassPathResource
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

fun Database.Transaction.runDevScript(devScriptName: String) {
    val path = "dev-data/$devScriptName"
    logger.info("Running SQL script: $path")
    ClassPathResource(path).inputStream.use {
        it.bufferedReader().readText().let { content ->
            execute(content)
        }
    }
}

fun Database.Transaction.resetDatabase() {
    execute("SELECT reset_database()")
}

fun Database.Transaction.ensureDevData() {
    if (createQuery("SELECT count(*) FROM care_area").mapTo<Int>().first() == 0) {
        listOf("espoo-dev-data.sql", "service-need-options.sql", "employees.sql", "preschool-terms.sql", "club-terms.sql").forEach { runDevScript(it) }
    }
}

/**
 * Insert one row of data to the database.
 *
 * * all fields from `row` are bound separately as parameters
 * * SQL must return "id" column of type UUID
 */
private fun Database.Transaction.insertTestDataRow(row: Any, @Language("sql") sql: String): UUID = createUpdate(sql)
    .bindKotlin(row)
    .executeAndReturnGeneratedKeys()
    .mapTo<UUID>()
    .asSequence()
    .single()

fun Database.Transaction.insertTestCareArea(area: DevCareArea): AreaId = insertTestDataRow(
    area,
    """
INSERT INTO care_area (id, name, short_name, area_code, sub_cost_center)
VALUES (:id, :name, :shortName, :areaCode, :subCostCenter)
RETURNING id
"""
).let(::AreaId)

fun Database.Transaction.insertTestDaycare(daycare: DevDaycare): DaycareId = insertTestDataRow(
    daycare,
    """
WITH insert_unit_manager AS (
  INSERT INTO unit_manager (name) VALUES (:unitManager.name)
  RETURNING id
)
INSERT INTO daycare (
  id, name, opening_date, closing_date, care_area_id, type, daycare_apply_period, preschool_apply_period, club_apply_period, provider_type,
  capacity, language, ghost_unit, upload_to_varda, upload_children_to_varda, upload_to_koski, invoiced_by_municipality, cost_center,
  additional_info, phone, email, url,
  street_address, postal_code, post_office,
  location, mailing_street_address, mailing_po_box, mailing_postal_code, mailing_post_office,
  unit_manager_id,
  decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address,
  oph_unit_oid, oph_organizer_oid, oph_organization_oid, round_the_clock, operation_days, enabled_pilot_features
) VALUES (
  :id, :name, :openingDate, :closingDate, :areaId, :type::care_types[], :daycareApplyPeriod, :preschoolApplyPeriod, :clubApplyPeriod, :providerType,
  :capacity, :language, :ghostUnit, :uploadToVarda, :uploadChildrenToVarda, :uploadToKoski, :invoicedByMunicipality, :costCenter,
  :additionalInfo, :phone, :email, :url,
  :visitingAddress.streetAddress, :visitingAddress.postalCode, :visitingAddress.postOffice,
  :location, :mailingAddress.streetAddress, :mailingAddress.poBox, :mailingAddress.postalCode, :mailingAddress.postOffice,
  (SELECT id FROM insert_unit_manager),
  :decisionCustomization.daycareName, :decisionCustomization.preschoolName, :decisionCustomization.handler, :decisionCustomization.handlerAddress,
  :ophUnitOid, :ophOrganizerOid, :ophOrganizationOid, :roundTheClock, :operationDays, :enabledPilotFeatures::pilot_feature[]
)
RETURNING id
"""
).let(::DaycareId)

fun Database.Transaction.updateDaycareAcl(daycareId: DaycareId, externalId: ExternalId, role: UserRole) {
    createUpdate("INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES ((SELECT id from employee where external_id = :external_id), :daycare_id, :role)")
        .bind("daycare_id", daycareId)
        .bind("external_id", externalId)
        .bind("role", role)
        .execute()
}

fun Database.Transaction.updateDaycareAclWithEmployee(daycareId: DaycareId, employeeId: UUID, role: UserRole) {
    createUpdate("INSERT INTO daycare_acl (employee_id, daycare_id, role) VALUES (:employeeId, :daycare_id, :role)")
        .bind("daycare_id", daycareId)
        .bind("employeeId", employeeId)
        .bind("role", role)
        .execute()
}

fun Database.Transaction.updateDaycareGroupAclWithEmployee(daycareId: GroupId, employeeId: UUID) {
    createUpdate("INSERT INTO daycare_group_acl (employee_id, daycare_group_id) VALUES (:employeeId, :daycare_group_id)")
        .bind("daycare_group_id", daycareId)
        .bind("employeeId", employeeId)
        .execute()
}

fun Database.Transaction.createMobileDeviceToUnit(id: UUID, unitId: DaycareId, name: String = "Nimeämätön laite") {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id)
        VALUES (:id, :name, 'Yksikkö', null, null);

        INSERT INTO mobile_device (id, unit_id, name) VALUES (:id, :unitId, :name);
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("unitId", unitId)
        .bind("name", name)
        .execute()
}

fun Database.Transaction.insertTestEmployee(employee: DevEmployee) = insertTestDataRow(
    employee,
    """
INSERT INTO employee (id, first_name, last_name, email, external_id, roles, last_login)
VALUES (:id, :firstName, :lastName, :email, :externalId, :roles::user_role[], :lastLogin)
RETURNING id
"""
)

fun Database.Transaction.insertTestMobileDevice(device: DevMobileDevice) = insertTestDataRow(
    device,
    """
INSERT INTO mobile_device (id, unit_id, name, deleted, long_term_token)
VALUES (:id, :unitId, :name, :deleted, :longTermToken)
RETURNING id
    """
)

fun Database.Transaction.insertTestPerson(person: DevPerson) = insertTestDataRow(
    person.copy(updatedFromVtj = if (person.ssn != null) HelsinkiDateTime.now() else null),
    """
INSERT INTO person (
    id, date_of_birth, date_of_death, first_name, last_name, social_security_number, email, phone, language,
    street_address, postal_code, post_office, residence_code, nationalities, restricted_details_enabled, restricted_details_end_date,
    invoicing_street_address, invoicing_postal_code, invoicing_post_office, updated_from_vtj, oph_person_oid
) VALUES (
    :id, :dateOfBirth, :dateOfDeath, :firstName, :lastName, :ssn, :email, :phone, :language,
    :streetAddress, :postalCode, :postOffice, :residenceCode, :nationalities, :restrictedDetailsEnabled, :restrictedDetailsEndDate,
    :invoicingStreetAddress, :invoicingPostalCode, :invoicingPostOffice, :updatedFromVtj, :ophPersonOid
)
RETURNING id
"""
)

fun Database.Transaction.insertTestParentship(
    headOfChild: UUID,
    childId: UUID,
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
        .bindMap(
            mapOf(
                "id" to id,
                "headOfChild" to headOfChild,
                "childId" to childId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestParentship(parentship: DevParentship): DevParentship {
    val withId = if (parentship.id == null) parentship.copy(id = ParentshipId(UUID.randomUUID())) else parentship
    // language=sql
    val sql =
        """
        INSERT INTO fridge_child (id, head_of_child, child_id, start_date, end_date)
        VALUES (:id, :headOfChildId, :childId, :startDate, :endDate)
        """.trimIndent()
    createUpdate(sql).bindKotlin(withId).execute()
    return withId
}

fun Database.Transaction.insertTestPartnership(
    adult1: UUID,
    adult2: UUID,
    id: PartnershipId = PartnershipId(UUID.randomUUID()),
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate? = null
): PartnershipId {
    createUpdate(
        """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:id, :index, :personId, :startDate, :endDate)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "index" to 1,
                "personId" to adult1,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    createUpdate(
        """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:id, :index, :personId, :startDate, :endDate)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "index" to 2,
                "personId" to adult2,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestApplication(
    id: ApplicationId = ApplicationId(UUID.randomUUID()),
    sentDate: LocalDate? = LocalDate.of(2019, 1, 1),
    dueDate: LocalDate? = LocalDate.of(2019, 5, 1),
    status: ApplicationStatus = ApplicationStatus.SENT,
    guardianId: UUID,
    childId: UUID,
    otherGuardianId: UUID? = null,
    hideFromGuardian: Boolean = false,
    transferApplication: Boolean = false
): ApplicationId {
    createUpdate(
        """
            INSERT INTO application (id, sentdate, duedate, status, guardian_id, child_id, other_guardian_id, origin, hidefromguardian, transferApplication)
            VALUES (:id, :sentDate, :dueDate, :status::application_status_type, :guardianId, :childId, :otherGuardianId, 'ELECTRONIC'::application_origin_type, :hideFromGuardian, :transferApplication)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "sentDate" to sentDate,
                "dueDate" to dueDate,
                "status" to status,
                "guardianId" to guardianId,
                "childId" to childId,
                "otherGuardianId" to otherGuardianId,
                "hideFromGuardian" to hideFromGuardian,
                "transferApplication" to transferApplication
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestApplicationForm(applicationId: ApplicationId, document: DaycareFormV0, revision: Int = 1) {
    createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationId)
        .bind("revision", revision)
        .execute()
    createUpdate(
        // language=SQL
        """
INSERT INTO application_form (application_id, revision, document, latest)
VALUES (:applicationId, :revision, :document, TRUE)
"""
    )
        .bind("applicationId", applicationId)
        .bind("revision", revision)
        .bindByType("document", document, QualifiedType.of(document.javaClass).with(Json::class.java))
        .execute()
}

fun Database.Transaction.insertTestClubApplicationForm(applicationId: ApplicationId, document: ClubFormV0, revision: Int = 1) {
    createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationId)
        .bind("revision", revision)
        .execute()
    createUpdate(
        // language=SQL
        """
INSERT INTO application_form (application_id, revision, document, latest)
VALUES (:applicationId, :revision, :document, TRUE)
"""
    )
        .bind("applicationId", applicationId)
        .bind("revision", revision)
        .bindByType("document", document, QualifiedType.of(document.javaClass).with(Json::class.java))
        .execute()
}

fun Database.Transaction.insertTestChild(child: DevChild) = insertTestDataRow(
    child,
    """
INSERT INTO child (id, allergies, diet, medication, additionalinfo, preferred_Name)
VALUES (:id, :allergies, :diet, :medication, :additionalInfo, :preferredName)
ON CONFLICT(id) DO UPDATE
SET id = :id, allergies = :allergies, diet = :diet, medication = :medication, additionalInfo = :additionalInfo, preferred_name = :preferredName
RETURNING id
    """
)

fun Database.Transaction.insertTestPlacement(placement: DevPlacement) = insertTestDataRow(
    placement,
    """
INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date)
VALUES (:id, :type, :childId, :unitId, :startDate, :endDate)
RETURNING id
"""
).let(::PlacementId)

fun Database.Transaction.insertTestPlacement(
    id: PlacementId = PlacementId(UUID.randomUUID()),
    childId: UUID = UUID.randomUUID(),
    unitId: DaycareId = DaycareId(UUID.randomUUID()),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31)
): PlacementId {
    createUpdate(
        """
            INSERT INTO placement (id, child_id, unit_id, type, start_date, end_date)
            VALUES (:id, :childId, :unitId, :type::placement_type, :startDate, :endDate)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "type" to type.toString(),
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestHoliday(
    date: LocalDate,
    description: String = "holiday"
) {
    createUpdate(
        """
            INSERT INTO holiday (date, description)
            VALUES (:date, :description)
            """
    )
        .bindMap(
            mapOf(
                "date" to date,
                "description" to description
            )
        )
        .execute()
}

fun Database.Transaction.insertTestServiceNeed(
    confirmedBy: UUID,
    placementId: PlacementId,
    period: FiniteDateRange,
    optionId: ServiceNeedOptionId,
    shiftCare: Boolean = false,
    confirmedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    id: ServiceNeedId = ServiceNeedId(UUID.randomUUID()),
    updated: HelsinkiDateTime = HelsinkiDateTime.now()
): ServiceNeedId {
    createUpdate(
        """
ALTER TABLE service_need DISABLE TRIGGER set_timestamp;
INSERT INTO service_need (id, placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at, updated)
VALUES (:id, :placementId, :startDate, :endDate, :optionId, :shiftCare, :confirmedBy, :confirmedAt, :updated);
ALTER TABLE service_need ENABLE TRIGGER set_timestamp;
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
        .bind("updated", updated)
        .execute()
    return id
}

fun Database.Transaction.insertServiceNeedOption(option: ServiceNeedOption) {
    createUpdate(
        // language=sql
        """
ALTER TABLE service_need_option DISABLE TRIGGER set_timestamp;
INSERT INTO service_need_option (id, name, valid_placement_type, default_option, fee_coefficient, voucher_value_coefficient, occupancy_coefficient, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, updated)
VALUES (:id, :name, :validPlacementType, :defaultOption, :feeCoefficient, :voucherValueCoefficient, :occupancyCoefficient, :daycareHoursPerWeek, :partDay, :partWeek, :feeDescriptionFi, :feeDescriptionSv, :voucherValueDescriptionFi, :voucherValueDescriptionSv, :updated)
ON CONFLICT (id) DO UPDATE SET updated = :updated;
ALTER TABLE service_need_option ENABLE TRIGGER set_timestamp;
"""
    )
        .bindKotlin(option)
        .execute()
}

fun Database.Transaction.insertTestIncome(
    objectMapper: ObjectMapper,
    personId: UUID,
    id: UUID = UUID.randomUUID(),
    validFrom: LocalDate = LocalDate.of(2019, 1, 1),
    validTo: LocalDate? = null,
    data: Map<String, IncomeValue> = mapOf(),
    effect: IncomeEffect = IncomeEffect.MAX_FEE_ACCEPTED,
    updatedAt: Instant = Instant.now(),
    updatedBy: UUID = UUID.randomUUID()
): UUID {
    createUpdate(
        """
            INSERT INTO income (id, person_id, valid_from, valid_to, data, effect, updated_at, updated_by)
            VALUES (:id, :personId, :validFrom, :validTo, :data, :effect::income_effect, :updatedAt, :updatedBy)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "personId" to personId,
                "validFrom" to validFrom,
                "validTo" to validTo,
                "data" to PGobject().apply {
                    type = "jsonb"
                    value = objectMapper.writeValueAsString(data)
                },
                "effect" to effect.toString(),
                "updatedAt" to Timestamp(updatedAt.toEpochMilli()),
                "updatedBy" to updatedBy
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestFeeAlteration(
    childId: UUID,
    id: UUID = UUID.randomUUID(),
    type: FeeAlteration.Type = FeeAlteration.Type.DISCOUNT,
    amount: Double = 50.0,
    isAbsolute: Boolean = false,
    validFrom: LocalDate = LocalDate.of(2019, 1, 1),
    validTo: LocalDate? = null,
    notes: String = "",
    updatedAt: Instant = Instant.now(),
    updatedBy: UUID
): UUID {
    createUpdate(
        """
            INSERT INTO fee_alteration (id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, updated_at, updated_by)
            VALUES (:id, :childId, :type::fee_alteration_type, :amount, :isAbsolute, :validFrom, :validTo, :notes, :updatedAt, :updatedBy)
        """.trimIndent()
    )
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "type" to type.toString(),
                "amount" to amount,
                "isAbsolute" to isAbsolute,
                "validFrom" to validFrom,
                "validTo" to validTo,
                "notes" to notes,
                "updatedAt" to updatedAt,
                "updatedBy" to updatedBy
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestFeeThresholds(feeThresholds: FeeThresholds) = insertTestDataRow(
    feeThresholds,
    """
INSERT INTO fee_thresholds (valid_during, min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6, income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6, max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6, income_threshold_increase_6_plus, sibling_discount_2, sibling_discount_2_plus, max_fee, min_fee)
VALUES (:validDuring, :minIncomeThreshold2, :minIncomeThreshold3, :minIncomeThreshold4, :minIncomeThreshold5, :minIncomeThreshold6, :incomeMultiplier2, :incomeMultiplier3, :incomeMultiplier4, :incomeMultiplier5, :incomeMultiplier6, :maxIncomeThreshold2, :maxIncomeThreshold3, :maxIncomeThreshold4, :maxIncomeThreshold5, :maxIncomeThreshold6, :incomeThresholdIncrease6Plus, :siblingDiscount2, :siblingDiscount2Plus, :maxFee, :minFee)
RETURNING id
    """.trimIndent()
)

fun Database.Transaction.insertTestVoucherValue(voucherValue: VoucherValue) = insertTestDataRow(
    voucherValue,
    "INSERT INTO voucher_value (id, validity, base_value, age_under_three_coefficient) VALUES (:id, :validity, :baseValue, :ageUnderThreeCoefficient)"
)

fun Database.Transaction.insertTestDaycareGroup(group: DevDaycareGroup) = insertTestDataRow(
    group,
    """
INSERT INTO daycare_group (id, daycare_id, name, start_date, end_date)
VALUES (:id, :daycareId, :name, :startDate, :endDate)
"""
).let(::GroupId)

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
        .bindMap(
            mapOf(
                "id" to id,
                "placementId" to daycarePlacementId,
                "groupId" to groupId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
    return id
}

fun Database.Transaction.insertTestPlacementPlan(
    applicationId: ApplicationId,
    unitId: DaycareId,
    id: UUID = UUID.randomUUID(),
    type: PlacementType = PlacementType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31),
    preschoolDaycareStartDate: LocalDate? = null,
    preschoolDaycareEndDate: LocalDate? = null,
    updated: Instant = Instant.now(),
    deleted: Boolean? = false
): UUID {
    createUpdate(
        """
            INSERT INTO placement_plan (id, unit_id, application_id, type, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date, updated, deleted)
            VALUES (:id, :unitId, :applicationId, :type::placement_type, :startDate, :endDate, :preschoolDaycareStartDate, :preschoolDaycareEndDate, :updated, :deleted)
            """
    )
        .bindMap(
            mapOf(
                "id" to id,
                "applicationId" to applicationId,
                "unitId" to unitId,
                "type" to type.toString(),
                "startDate" to startDate,
                "endDate" to endDate,
                "preschoolDaycareStartDate" to preschoolDaycareStartDate,
                "preschoolDaycareEndDate" to preschoolDaycareEndDate,
                "updated" to updated,
                "deleted" to deleted
            )
        )
        .execute()
    return id
}

data class TestDecision(
    val createdBy: UUID,
    val sentDate: LocalDate = LocalDate.now(),
    val unitId: DaycareId,
    val applicationId: ApplicationId,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus = DecisionStatus.PENDING,
    val requestedStartDate: LocalDate? = null,
    val resolvedBy: UUID? = null,
    val resolved: Instant? = resolvedBy?.let { Instant.ofEpochSecond(1546300800) }, // 2019-01-01 midnight
    val pendingDecisionEmailsSentCount: Int? = 0,
    val pendingDecisionEmailSent: Instant? = null
)

fun Database.Transaction.insertTestDecision(decision: TestDecision) = insertTestDataRow(
    decision,
    """
INSERT INTO decision (created_by, sent_date, unit_id, application_id, type, start_date, end_date, status, requested_start_date, resolved, resolved_by, pending_decision_emails_sent_count, pending_decision_email_sent)
VALUES (:createdBy, :sentDate, :unitId, :applicationId, :type, :startDate, :endDate, :status, :requestedStartDate, :resolved, :resolvedBy, :pendingDecisionEmailsSentCount, :pendingDecisionEmailSent)
RETURNING id
"""
).let(::DecisionId)

fun Database.Transaction.insertTestAssistanceNeed(assistanceNeed: DevAssistanceNeed): AssistanceNeedId {
    val id = insertTestDataRow(
        assistanceNeed,
        """
INSERT INTO assistance_need (id, updated_by, child_id, start_date, end_date, capacity_factor, description, other_basis)
VALUES (:id, :updatedBy, :childId, :startDate, :endDate, :capacityFactor, :description, :otherBasis)
RETURNING id
"""
    ).let(::AssistanceNeedId)
    val counts = insertAssistanceBasisOptionRefs(id, assistanceNeed.bases)
    assert(counts.size == assistanceNeed.bases.size)
    return id
}

fun Database.Transaction.insertTestAssistanceAction(assistanceAction: DevAssistanceAction): AssistanceActionId {
    val id = insertTestDataRow(
        assistanceAction,
        """
INSERT INTO assistance_action (id, updated_by, child_id, start_date, end_date, other_action, measures)
VALUES (:id, :updatedBy, :childId, :startDate, :endDate, :otherAction, :measures::assistance_measure[])
RETURNING id
"""
    ).let(::AssistanceActionId)
    val counts = insertAssistanceActionOptionRefs(id, assistanceAction.actions)
    assert(counts.size == assistanceAction.actions.size)
    return id
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
        """.trimIndent()
    )
        .bindMap(
            mapOf(
                "id" to id,
                "groupId" to groupId,
                "amount" to amount,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )
        .execute()
}

fun Database.Transaction.insertTestStaffAttendance(
    id: UUID = UUID.randomUUID(),
    groupId: GroupId,
    date: LocalDate,
    count: Double
) {
    //language=sql
    val sql =
        """
        INSERT INTO staff_attendance (id, group_id, date, count)
        VALUES (:id, :groupId, :date, :count)
        """.trimIndent()
    createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "groupId" to groupId,
                "date" to date,
                "count" to count
            )
        )
        .execute()
}

fun Database.Transaction.insertTestAbsence(
    id: UUID = UUID.randomUUID(),
    childId: UUID,
    date: LocalDate,
    careType: AbsenceCareType,
    absenceType: AbsenceType = AbsenceType.SICKLEAVE,
    modifiedBy: String = "Someone"
) {
    //language=sql
    val sql =
        """
        INSERT INTO absence (id, child_id, date, care_type, absence_type, modified_by_deprecated)
        VALUES (:id, :childId, :date, :careType, :absenceType, :modifiedBy)
        """.trimIndent()
    createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "date" to date,
                "careType" to careType,
                "absenceType" to absenceType,
                "modifiedBy" to modifiedBy
            )
        )
        .execute()
}

fun Database.Transaction.insertTestChildAttendance(
    id: UUID = UUID.randomUUID(),
    childId: UUID,
    unitId: DaycareId,
    arrived: HelsinkiDateTime,
    departed: HelsinkiDateTime?
) {
    //language=sql
    val sql =
        """
        INSERT INTO child_attendance (id, child_id, unit_id, arrived, departed)
        VALUES (:id, :childId, :unitId, :arrived, :departed)
        """.trimIndent()
    createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "arrived" to arrived,
                "departed" to departed
            )
        )
        .execute()
}

fun Database.Transaction.insertTestBackUpCare(
    childId: UUID,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate,
    groupId: GroupId? = null,
    id: UUID = UUID.randomUUID()
) {
    //language=sql
    val sql =
        """
        INSERT INTO backup_care (id, child_id, unit_id, start_date, end_date, group_id)
        VALUES (:id, :childId, :unitId, :startDate, :endDate, :groupId)
        """.trimIndent()
    createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to id,
                "childId" to childId,
                "unitId" to unitId,
                "startDate" to startDate,
                "endDate" to endDate,
                "groupId" to groupId
            )
        )
        .execute()
}

fun Database.Transaction.insertTestBackupCare(
    backupCare: DevBackupCare
) = createUpdate(
    // language=SQL
    """
INSERT INTO backup_care (id, child_id, unit_id, group_id, start_date, end_date)
VALUES (:id, :childId, :unitId, :groupId, :startDate, :endDate)
"""
)
    .bind("id", backupCare.id ?: UUID.randomUUID())
    .bind("childId", backupCare.childId)
    .bind("unitId", backupCare.unitId)
    .bind("groupId", backupCare.groupId)
    .bind("startDate", backupCare.period.start)
    .bind("endDate", backupCare.period.end)
    .execute()

fun Database.Transaction.insertApplication(application: ApplicationWithForm): ApplicationId {
    val id = application.id ?: ApplicationId(UUID.randomUUID())

    //language=sql
    val sql =
        """
        INSERT INTO application(id, sentdate, duedate, status, guardian_id, child_id, origin, checkedbyadmin, hidefromguardian, transferapplication, other_guardian_id)
        VALUES(:id, :sentDate, :dueDate, :applicationStatus::application_status_type, :guardianId, :childId, :origin::application_origin_type, :checkedByAdmin, :hideFromGuardian, :transferApplication, :otherGuardianId)
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
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
        .execute()

    return id
}

fun Database.Transaction.insertApplicationForm(applicationForm: ApplicationForm): UUID {
    createUpdate(
        """
UPDATE application_form SET latest = FALSE
WHERE application_id = :applicationId AND revision < :revision
"""
    ).bind("applicationId", applicationForm.applicationId)
        .bind("revision", applicationForm.revision)
        .execute()
    val id = applicationForm.id ?: UUID.randomUUID()

    //language=sql
    val sql =
        """
        INSERT INTO application_form(id, application_id, created, revision, updated, document, latest)
        VALUES(:id, :applicationId, :created, :revision, :updated, :document::JSON, TRUE)
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("applicationId", applicationForm.applicationId)
        .bind("created", applicationForm.createdDate)
        .bind("revision", applicationForm.revision)
        .bind("updated", applicationForm.updated)
        .bind("document", objectMapper().writeValueAsString(applicationForm.document))
        .execute()

    return id
}

data class DevFamilyContact(
    val id: UUID,
    val childId: UUID,
    val contactPersonId: UUID,
    val priority: Int
)

fun Database.Transaction.insertFamilyContact(contact: DevFamilyContact) = insertTestDataRow(
    contact,
    """
INSERT INTO family_contact (id, child_id, contact_person_id, priority)
VALUES (:id, :childId, :contactPersonId, :priority)
RETURNING id
"""
)

data class DevBackupPickup(
    val id: UUID,
    val childId: UUID,
    val name: String,
    val phone: String
)

fun Database.Transaction.insertBackupPickup(pickup: DevBackupPickup) = insertTestDataRow(
    pickup,
    """
INSERT INTO backup_pickup (id, child_id, name, phone)
VALUES (:id, :childId, :name, :phone)
RETURNING id
"""
)

data class DevFridgeChild(
    val id: ParentshipId = ParentshipId(UUID.randomUUID()),
    val childId: UUID,
    val headOfChild: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

fun Database.Transaction.insertFridgeChild(pickup: DevFridgeChild) = insertTestDataRow(
    pickup,
    """
INSERT INTO fridge_child (id, child_id, head_of_child, start_date, end_date)
VALUES (:id, :childId, :headOfChild, :startDate, :endDate)
RETURNING id
"""
)

data class DevFridgePartner(
    val partnershipId: PartnershipId,
    val indx: Int,
    val personId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

fun Database.Transaction.insertFridgePartner(pickup: DevFridgePartner) = insertTestDataRow(
    pickup,
    """
INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
VALUES (:partnershipId, :indx, :personId, :startDate, :endDate)
RETURNING partnership_id
"""
).let(::PartnershipId)

data class DevEmployeePin(
    val id: UUID,
    val userId: UUID? = null,
    val employeeExternalId: String? = null,
    val pin: String,
    val locked: Boolean? = false
)

fun Database.Transaction.insertEmployeePin(employeePin: DevEmployeePin) = insertTestDataRow(
    employeePin,
    """
INSERT INTO employee_pin (id, user_id, pin, locked)
VALUES (:id, :userId, :pin, :locked)
RETURNING id
"""
)

fun Database.Transaction.getEmployeeIdByExternalId(externalId: String) = createQuery("SELECT id FROM employee WHERE external_id = :id")
    .bind("id", externalId)
    .mapTo<UUID>()
    .first()

fun Database.Transaction.insertTestDaycareGroupAcl(aclRow: DevDaycareGroupAcl) = createUpdate(
    """
INSERT INTO daycare_group_acl (daycare_group_id, employee_id)
VALUES (:groupId, :employeeId)
"""
).bindKotlin(aclRow).execute()

fun Database.Transaction.insertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed) = insertTestDataRow(
    vardaServiceNeed,
    """
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_updated, evaka_child_id, varda_decision_id, varda_placement_id, update_failed, errors)
VALUES (:evakaServiceNeedId, :evakaServiceNeedUpdated, :evakaChildId, :vardaDecisionId, :vardaPlacementId, :updateFailed, :errors)
RETURNING evaka_service_need_id
    """
)
