DROP VIEW IF EXISTS application_view;

CREATE OR REPLACE VIEW application_view (
  id,
  document,
  docVersion,
  created,
  formModified,
  sentDate,
  dueDate,
  status,
  type,
  urgent,
  preferredStartDate,
  startDate,
  childName,
  childFirstName,
  childLastName,
  childSSN,
  childStreetAddr,
  childPostalCode,
  preferredUnit,
  preferredUnits,
  allergyType,
  dietType,
  otherInfo,
  daycareAssistanceNeeded,
  siblingBasis,
  childId,
  guardianId,
  term,
  wasOnDaycare,
  wasOnClubCare,
  clubCareAssistanceNeeded,
  origin,
  extendedCare,
  placementDaycareUnit,
  connectedDaycare,
  serviceNeedOption,
  preparatoryEducation,
  checkedByAdmin,
  guardianPhoneNumber,
  hideFromGuardian,
  transferApplication,
  additionalDaycareApplication,
  duplicateApplicationIds
  ) AS
SELECT
  id,
  document,
  docVersion,
  created,
  formModified,
  sentDate,
  dueDate,
  status,
  type,
  urgent,
  preferredStartDate,
  startDate,
  childLastName || ' ' || childFirstName AS childName,
  childFirstName,
  childLastName,
  childSSN,
  childStreetAddr,
  childPostalCode,
  preferredUnit :: UUID,
  preferredUnits,
  allergyType,
  dietType,
  otherInfo,
  daycareAssistanceNeeded,
  siblingBasis,
  childId :: UUID,
  guardianId :: UUID,
  term :: UUID,
  wasOnDaycare,
  wasOnClubCare,
  clubCareAssistanceNeeded,
  origin,
  extendedCare,
  placementDaycareUnit,
  connectedDaycare,
  serviceNeedOption,
  preparatoryEducation,
  checkedByAdmin,
  guardianPhoneNumber,
  hideFromGuardian,
  transferApplication,
  additionalDaycareApplication,
  duplicateApplicationIds,
  otherGuardianAgreementStatus
FROM (
WITH dup_appl AS (
    SELECT
        l.id, array_agg(r.id) AS duplicate_application_ids
    FROM
        application l, application r
    WHERE
      l.child_id = r.child_id
      AND l.id <> r.id
      AND l.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
      AND r.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
    GROUP by
        l.id
)
SELECT
  appl.id,
  appl.document,
  appl.document -> 'docVersion'                                                               AS docVersion,
  appl.form_modified                                                                          AS formModified,
  appl.created,
  appl.sentDate,
  appl.dueDate,
  appl.status                                                                                 AS status,
  appl.origin                                                                                 AS origin,
  appl.child_id                                                                               AS childId,
  appl.guardian_id                                                                            AS guardianId,
  appl.type                                                                                   AS type,
  (appl.document ->> 'urgent') :: BOOLEAN                                                     AS urgent,
  (appl.document ->> 'preferredStartDate')::DATE                                              AS preferredStartDate,
  COALESCE(
      (SELECT min(coalesce(d.requested_start_date, d.start_date)) FROM decision d WHERE d.application_id = appl.id AND d.status != 'REJECTED'),
      placement_plan.start_date,
      (appl.document ->> 'preferredStartDate')::DATE
  )                                                                                           AS startDate,
  appl.document -> 'apply' -> 'preferredUnits' ->> 0                                          AS preferredUnit,
  (
      SELECT array_agg(e::UUID)
      FROM jsonb_array_elements_text(appl.document -> 'apply' -> 'preferredUnits') e
  )                                                                                           AS preferredUnits,
  appl.document -> 'child' ->> 'firstName'                                                    AS childFirstName,
  appl.document -> 'child' ->> 'lastName'                                                     AS childLastName,
  appl.document -> 'child' ->> 'socialSecurityNumber'                                         AS childSSN,
  appl.document -> 'child' -> 'address' ->> 'street'                                          AS childStreetAddr,
  appl.document -> 'child' -> 'address' ->> 'postalCode'                                      AS childPostalCode,
  appl.document -> 'guardian' ->> 'phoneNumber'                                               AS guardianPhoneNumber,
  appl.document -> 'additionalDetails' ->> 'allergyType'                                      AS allergyType,
  appl.document -> 'additionalDetails' ->> 'dietType'                                         AS dietType,
  appl.document -> 'additionalDetails' ->> 'otherInfo'                                        AS otherInfo,
  (appl.document -> 'apply' ->> 'siblingBasis') :: BOOLEAN                                    AS siblingBasis,
  appl.document ->> 'term'                                                                    AS term,
  (appl.document ->> 'wasOnDaycare') :: BOOLEAN                                               AS wasOnDaycare,
  (appl.document ->> 'wasOnClubCare') :: BOOLEAN                                              AS wasOnClubCare,
  (appl.document -> 'clubCare' ->> 'assistanceNeeded') :: BOOLEAN                             AS clubCareAssistanceNeeded,
  (appl.document -> 'careDetails' ->> 'assistanceNeeded') :: BOOLEAN                          AS daycareAssistanceNeeded,
  (appl.document ->> 'extendedCare') :: BOOLEAN                                               AS extendedCare,
  placement_plan.unit_id                                                                      AS placementDaycareUnit,
  (appl.document ->> 'connectedDaycare') :: BOOLEAN                                           AS connectedDaycare,
  appl.document ->> 'serviceNeedOption'                                                       AS serviceNeedOption,
  (appl.document -> 'careDetails' ->> 'preparatory') :: BOOLEAN                               AS preparatoryEducation,
  appl.checkedByAdmin,
  appl.hideFromGuardian,
  appl.transferApplication,
  appl.additionalDaycareApplication,
  dup_appl.duplicate_application_ids                                                          AS duplicateApplicationIds,
  (appl.document ->> 'otherGuardianAgreementStatus')                                          AS otherGuardianAgreementStatus
FROM
  application appl
    LEFT JOIN placement_plan
    ON (placement_plan.application_id = appl.id)
    LEFT JOIN dup_appl
    ON dup_appl.id = appl.id
WHERE document @> '{
"docVersion": 0
}' :: JSONB
) jsonV0;
