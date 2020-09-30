DROP VIEW IF EXISTS application_view;

CREATE OR REPLACE VIEW application_view (
  id,
  revision,
  document,
  docVersion,
  created,
  formModified,
  sentDate,
  dueDate,
  status,
  type,
  urgent,
  startDate,
  childName,
  childFirstName,
  childLastName,
  childSSN,
  childStreetAddr,
  preferredUnit,
  preferredUnits,
  allergyType,
  dietType,
  otherInfo,
  daycareAssistanceNeeded,
  siblingBasis,
  childId,
  guardianId,
  otherGuardianId,
  term,
  wasOnDaycare,
  wasOnClubCare,
  clubCareAssistanceNeeded,
  origin,
  extendedCare,
  placementDaycareUnit,
  connectedDaycare,
  preparatoryEducation,
  checkedByAdmin,
  guardianPhoneNumber,
  hideFromGuardian,
  transferApplication,
  additionalDaycareApplication,
  duplicateApplicationIds,
  hasActiveAssistanceNeed
  ) AS
SELECT
  id,
  revision,
  document,
  docVersion,
  created,
  formModified,
  sentDate,
  dueDate,
  status,
  type,
  urgent,
  startDate,
  childFirstName || ' ' || childLastName AS childName,
  childFirstName,
  childLastName,
  childSSN,
  childStreetAddr,
  preferredUnit :: UUID,
  preferredUnits,
  allergyType,
  dietType,
  otherInfo,
  daycareAssistanceNeeded,
  siblingBasis,
  childId :: UUID,
  guardianId :: UUID,
  otherGuardianId :: UUID,
  term :: UUID,
  wasOnDaycare,
  wasOnClubCare,
  clubCareAssistanceNeeded,
  origin,
  extendedCare,
  placementDaycareUnit,
  connectedDaycare,
  preparatoryEducation,
  checkedByAdmin,
  guardianPhoneNumber,
  hideFromGuardian,
  transferApplication,
  additionalDaycareApplication,
  duplicateApplicationIds,
  hasActiveAssistanceNeed,
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
), active_assistance_need AS (
    SELECT
        application.child_id AS child_id,
        count(*) AS active_count
    FROM
        application, assistance_need
    WHERE
        application.child_id = assistance_need.child_id
        AND assistance_need.start_date <= current_date
        AND assistance_need.end_date >= current_date
    GROUP
        BY application.child_id
)
SELECT
  appl.id,
  data.revision,
  data.document,
  data.document -> 'docVersion'                                                               AS docVersion,
  data.created                                                                                AS formModified,
  appl.created,
  appl.sentDate,
  appl.dueDate,
  appl.status                                                                                 AS status,
  appl.origin                                                                                 AS origin,
  appl.child_id                                                                               AS childId,
  appl.guardian_id                                                                            AS guardianId,
  appl.other_guardian_id                                                                      AS otherGuardianId,
  data.document ->> 'type'                                                                    AS type,
  (data.document ->> 'urgent') :: BOOLEAN                                                     AS urgent,
  COALESCE(
      (SELECT min(coalesce(d.requested_start_date, d.start_date)) FROM decision d WHERE d.application_id = appl.id AND d.status != 'REJECTED'),
      placement_plan.start_date,
      (data.document ->> 'preferredStartDate')::DATE
  )                                                                                           AS startDate,
  data.document -> 'apply' -> 'preferredUnits' ->> 0                                          AS preferredUnit,
  (
      SELECT array_agg(e::UUID)
      FROM jsonb_array_elements_text(data.document -> 'apply' -> 'preferredUnits') e
  )                                                                                           AS preferredUnits,
  data.document -> 'child' ->> 'firstName'                                                    AS childFirstName,
  data.document -> 'child' ->> 'lastName'                                                     AS childLastName,
  data.document -> 'child' ->> 'socialSecurityNumber'                                         AS childSSN,
  data.document -> 'child' -> 'address' ->> 'street'                                          AS childStreetAddr,
  data.document -> 'guardian' ->> 'phoneNumber'                                               AS guardianPhoneNumber,
  data.document -> 'additionalDetails' ->> 'allergyType'                                      AS allergyType,
  data.document -> 'additionalDetails' ->> 'dietType'                                         AS dietType,
  data.document -> 'additionalDetails' ->> 'otherInfo'                                        AS otherInfo,
  (data.document -> 'apply' ->> 'siblingBasis') :: BOOLEAN                                    AS siblingBasis,
  data.document ->> 'term'                                                                    AS term,
  (data.document ->> 'wasOnDaycare') :: BOOLEAN                                               AS wasOnDaycare,
  (data.document ->> 'wasOnClubCare') :: BOOLEAN                                              AS wasOnClubCare,
  (data.document -> 'clubCare' ->> 'assistanceNeeded') :: BOOLEAN                             AS clubCareAssistanceNeeded,
  (data.document -> 'careDetails' ->> 'assistanceNeeded') :: BOOLEAN                          AS daycareAssistanceNeeded,
  (data.document ->> 'extendedCare') :: BOOLEAN                                               AS extendedCare,
  placement_plan.unit_id                                                                      AS placementDaycareUnit,
  (data.document ->> 'connectedDaycare') :: BOOLEAN                                           AS connectedDaycare,
  (data.document -> 'careDetails' ->> 'preparatory') :: BOOLEAN                               AS preparatoryEducation,
  appl.checkedByAdmin,
  appl.hideFromGuardian,
  appl.transferApplication,
  appl.additionalDaycareApplication,
  dup_appl.duplicate_application_ids                                                          AS duplicateApplicationIds,
  CASE WHEN active_assistance_need.active_count > 0
       THEN true
       ELSE false
  END                                                                                         AS hasActiveAssistanceNeed,
  (data.document ->> 'otherGuardianAgreementStatus')                                          AS otherGuardianAgreementStatus
FROM
  application appl
  INNER JOIN application_form data
    ON (appl.id = data.application_id)
    LEFT JOIN placement_plan
    ON (placement_plan.application_id = appl.id)
    LEFT JOIN dup_appl
    ON dup_appl.id = appl.id
    LEFT JOIN active_assistance_need
    ON (active_assistance_need.child_id = appl.child_id)
WHERE data.latest IS TRUE
AND document @> '{
"docVersion": 0
}' :: JSONB
) jsonV0;
