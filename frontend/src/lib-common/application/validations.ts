// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationEditorActor,
  ApplicationFormData
} from 'lib-common/application/ApplicationFormData'
import type { FeatureFlags } from 'lib-common/feature-flags'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type { ErrorKey, ErrorsOf } from 'lib-common/form-validation'
import {
  email,
  emailVerificationCheck,
  getErrorCount,
  phone,
  regexp,
  required,
  requiredSelection,
  ssn,
  TIME_REGEXP,
  validate,
  validDuration
} from 'lib-common/form-validation'
import type {
  ApplicationAttachment,
  ApplicationDetails as ApplicationDetailsGen
} from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'

export interface Term {
  term: FiniteDateRange
  extendedTerm: FiniteDateRange
}

export type ApplicationFormDataErrors = {
  [section in keyof ApplicationFormData]: ErrorsOf<ApplicationFormData[section]>
} & {
  serviceNeed: ErrorsOf<ApplicationFormData['serviceNeed']> & {
    partTimeLimit?: ErrorKey
  }
}

export const applicationHasErrors = (errors: ApplicationFormDataErrors) => {
  const totalErrors = (
    Object.keys(errors) as (keyof ApplicationFormData)[]
  ).reduce((acc, section) => acc + getErrorCount(errors[section]), 0)
  return totalErrors > 0
}

export const minPreferredStartDate = (
  originalPreferredStartDate: LocalDate | null
): LocalDate => originalPreferredStartDate ?? LocalDate.todayInSystemTz()

export const maxPreferredStartDate = (): LocalDate =>
  LocalDate.todayInSystemTz().addYears(1)

export const isValidPreferredStartDate = (
  date: LocalDate,
  originalPreferredStartDate: LocalDate | null,
  terms?: Term[]
): boolean => {
  if (date.isBefore(minPreferredStartDate(originalPreferredStartDate)))
    return false

  if (date.isAfter(maxPreferredStartDate())) return false

  if (terms !== undefined) {
    return terms.some((term) => term.extendedTerm.includes(date))
  }

  return true
}

const preferredStartDateValidator =
  (originalPreferredStartDate: LocalDate | null, terms?: Term[]) =>
  (
    val: LocalDate | null,
    err: ErrorKey = 'preferredStartDate'
  ): ErrorKey | undefined =>
    val && isValidPreferredStartDate(val, originalPreferredStartDate, terms)
      ? undefined
      : err

const termMembershipValidator =
  (terms?: Term[]) =>
  (
    val: LocalDate | null,
    err: ErrorKey = 'preferredStartDate'
  ): ErrorKey | undefined =>
    val &&
    (terms === undefined ||
      terms.some((term) => term.extendedTerm.includes(val)))
      ? undefined
      : err

const connectedDaycarePreferredStartDateValidator =
  (preferredStartDate: LocalDate | null, terms?: Term[]) =>
  (
    val: LocalDate | null,
    err: ErrorKey = 'connectedPreferredStartDate'
  ): ErrorKey | undefined => {
    if (val === null || preferredStartDate === null || terms === undefined) {
      return undefined
    }
    if (val.isEqual(preferredStartDate)) {
      return undefined
    }
    const preschoolTerm = terms.find((term) =>
      term.extendedTerm.includes(preferredStartDate)
    )
    if (preschoolTerm === undefined) {
      return undefined
    }
    const daycareTerm = terms.find((term) => term.extendedTerm.includes(val))
    if (
      daycareTerm?.extendedTerm.isEqual(preschoolTerm.extendedTerm) &&
      (val.isAfter(preferredStartDate) ||
        // special case when extended term starts before actual preschool term
        // e.g. term.start = 2025-08-06 & extendedTerm.start = 2025-08-01
        // -> preferredStartDate = 2025-08-06 & connectedDaycarePreferredStartDate
        // from 2025-08-01 to 2025-08-05 is also valid
        (preschoolTerm.extendedTerm.start.isBefore(preschoolTerm.term.start) &&
          preschoolTerm.term.start.isEqual(preferredStartDate) &&
          preschoolTerm.extendedTerm.includes(val)))
    ) {
      return undefined
    }
    return err
  }

export const getUrgencyAttachmentValidStatus = (
  urgent: boolean,
  urgencyAttachments: ApplicationAttachment[],
  featureFlags: FeatureFlags
) =>
  urgent && urgencyAttachments.length === 0 && featureFlags.urgencyAttachments
    ? featureFlags.requireAttachments
      ? 'require'
      : 'notify'
    : undefined

export const getShiftCareAttachmentsValidStatus = (
  shiftCare: boolean,
  shiftCareAttachments: ApplicationAttachment[],
  featureFlags: FeatureFlags
) =>
  shiftCare && shiftCareAttachments.length === 0
    ? featureFlags.requireAttachments
      ? 'require'
      : 'notify'
    : undefined

export const validateApplication = (
  apiData: ApplicationDetailsGen,
  form: ApplicationFormData,
  featureFlags: FeatureFlags,
  actor: ApplicationEditorActor,
  terms?: Term[]
): ApplicationFormDataErrors => {
  const citizen = actor === 'citizen'

  const requireFullFamily =
    apiData.type === 'DAYCARE' ||
    (apiData.type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)

  const siblingSelected =
    form.unitPreference.vtjSiblings.find((s) => s.selected) !== undefined

  const maxPartTimeDailyMinutes = 300 // 5 hours, max part-time daily duration

  return {
    serviceNeed: {
      preferredStartDate: validate(
        form.serviceNeed.preferredStartDate,
        required,
        citizen
          ? preferredStartDateValidator(
              apiData.status !== 'CREATED'
                ? apiData.form.preferences.preferredStartDate
                : null,
              terms
            )
          : termMembershipValidator(terms)
      ),
      connectedDaycarePreferredStartDate:
        apiData.type === 'PRESCHOOL' &&
        featureFlags.preschoolApplication.connectedDaycarePreferredStartDate &&
        form.serviceNeed.connectedDaycare
          ? validate(
              form.serviceNeed.connectedDaycarePreferredStartDate,
              required,
              connectedDaycarePreferredStartDateValidator(
                form.serviceNeed.preferredStartDate,
                terms
              )
            )
          : undefined,
      serviceNeedOption:
        (apiData.type === 'PRESCHOOL' &&
          featureFlags.preschoolApplication.serviceNeedOption &&
          form.serviceNeed.connectedDaycare) ||
        (apiData.type === 'DAYCARE' &&
          featureFlags.daycareApplication.serviceNeedOption)
          ? required(form.serviceNeed.serviceNeedOption)
          : undefined,
      startTime:
        (apiData.type === 'DAYCARE' &&
          featureFlags.daycareApplication.dailyTimes) ||
        (apiData.type === 'PRESCHOOL' &&
          !featureFlags.preschoolApplication.serviceNeedOption &&
          form.serviceNeed.connectedDaycare)
          ? required(form.serviceNeed.startTime, 'timeRequired') ||
            regexp(form.serviceNeed.startTime, TIME_REGEXP, 'timeFormat')
          : undefined,
      endTime:
        (apiData.type === 'DAYCARE' &&
          featureFlags.daycareApplication.dailyTimes) ||
        (apiData.type === 'PRESCHOOL' &&
          !featureFlags.preschoolApplication.serviceNeedOption &&
          form.serviceNeed.connectedDaycare)
          ? required(form.serviceNeed.endTime, 'timeRequired') ||
            regexp(form.serviceNeed.endTime, TIME_REGEXP, 'timeFormat')
          : undefined,
      partTimeLimit:
        citizen &&
        apiData.type === 'DAYCARE' &&
        featureFlags.daycareApplication.dailyTimes &&
        form.serviceNeed.partTime
          ? validDuration(
              maxPartTimeDailyMinutes,
              form.serviceNeed.startTime,
              form.serviceNeed.endTime,
              'exceedsMaxDuration'
            )
          : undefined,
      assistanceDescription: form.serviceNeed.assistanceNeeded
        ? required(form.serviceNeed.assistanceDescription)
        : undefined,
      urgencyAttachments:
        citizen &&
        getUrgencyAttachmentValidStatus(
          form.serviceNeed.urgent,
          form.serviceNeed.urgencyAttachments,
          featureFlags
        ) === 'require'
          ? {
              arrayErrors: 'required',
              itemErrors: form.serviceNeed.urgencyAttachments.map(() => ({
                id: undefined,
                name: undefined
              }))
            }
          : undefined,
      shiftCareAttachments:
        citizen &&
        getShiftCareAttachmentsValidStatus(
          form.serviceNeed.shiftCare,
          form.serviceNeed.shiftCareAttachments,
          featureFlags
        ) === 'require'
          ? {
              arrayErrors:
                form.serviceNeed.shiftCareAttachments.length === 0
                  ? 'required'
                  : undefined,
              itemErrors: form.serviceNeed.shiftCareAttachments.map(() => ({
                id: undefined,
                name: undefined
              }))
            }
          : undefined
    },
    unitPreference: {
      siblingName:
        citizen && form.unitPreference.siblingBasis && !siblingSelected
          ? validate(form.unitPreference.siblingName, required)
          : undefined,
      siblingSsn: citizen
        ? form.unitPreference.siblingBasis && !siblingSelected
          ? validate(form.unitPreference.siblingSsn, required, ssn)
          : undefined
        : form.unitPreference.siblingBasis && form.unitPreference.siblingSsn
          ? ssn(form.unitPreference.siblingSsn)
          : undefined,
      siblingUnit:
        citizen &&
        form.unitPreference.siblingBasis &&
        apiData.type === 'PRESCHOOL'
          ? validate(form.unitPreference.siblingUnit, required)
          : undefined,
      preferredUnits: {
        arrayErrors:
          form.unitPreference.preferredUnits.length === 0
            ? 'unitNotSelected'
            : undefined,
        itemErrors: form.unitPreference.preferredUnits.map(() => ({
          id: undefined,
          name: undefined
        }))
      }
    },
    contactInfo: {
      childMoveDate:
        citizen && form.contactInfo.childFutureAddressExists
          ? validate(form.contactInfo.childMoveDate, required)
          : undefined,
      childFutureStreet:
        citizen && form.contactInfo.childFutureAddressExists
          ? validate(form.contactInfo.childFutureStreet, required)
          : undefined,
      childFuturePostalCode:
        citizen && form.contactInfo.childFutureAddressExists
          ? validate(form.contactInfo.childFuturePostalCode, required)
          : undefined,
      childFuturePostOffice:
        citizen && form.contactInfo.childFutureAddressExists
          ? validate(form.contactInfo.childFuturePostOffice, required)
          : undefined,
      guardianPhone: citizen
        ? validate(form.contactInfo.guardianPhone, required, phone)
        : form.contactInfo.guardianPhone
          ? phone(form.contactInfo.guardianPhone)
          : undefined,
      guardianEmail: citizen
        ? form.contactInfo.noGuardianEmail &&
          form.contactInfo.guardianEmail.length === 0
          ? undefined
          : validate(form.contactInfo.guardianEmail, email, required)
        : form.contactInfo.guardianEmail
          ? email(form.contactInfo.guardianEmail)
          : undefined,
      guardianEmailVerification: citizen
        ? form.contactInfo.noGuardianEmail &&
          form.contactInfo.guardianEmailVerification.length === 0
          ? undefined
          : validate(
              form.contactInfo.guardianEmailVerification,
              email,
              emailVerificationCheck(form.contactInfo.guardianEmail)
            )
        : undefined,
      guardianMoveDate:
        citizen && form.contactInfo.guardianFutureAddressExists
          ? validate(form.contactInfo.guardianMoveDate, required)
          : undefined,
      guardianFutureStreet:
        citizen && form.contactInfo.guardianFutureAddressExists
          ? validate(form.contactInfo.guardianFutureStreet, required)
          : undefined,
      guardianFuturePostalCode:
        citizen && form.contactInfo.guardianFutureAddressExists
          ? validate(form.contactInfo.guardianFuturePostalCode, required)
          : undefined,
      guardianFuturePostOffice:
        citizen && form.contactInfo.guardianFutureAddressExists
          ? validate(form.contactInfo.guardianFuturePostOffice, required)
          : undefined,
      otherGuardianAgreementStatus:
        citizen &&
        apiData.type !== 'CLUB' &&
        apiData.hasOtherGuardian &&
        apiData.otherGuardianLivesInSameAddress === false
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone: citizen
        ? apiData.type !== 'CLUB' &&
          apiData.hasOtherGuardian &&
          apiData.otherGuardianLivesInSameAddress === false &&
          form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined
        : form.contactInfo.otherGuardianPhone
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined,
      otherGuardianEmail: citizen
        ? apiData.type !== 'CLUB' &&
          apiData.hasOtherGuardian &&
          apiData.otherGuardianLivesInSameAddress === false &&
          form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? email(form.contactInfo.otherGuardianEmail)
          : undefined
        : form.contactInfo.otherGuardianEmail
          ? email(form.contactInfo.otherGuardianEmail)
          : undefined,
      otherPartnerFirstName:
        citizen && requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerFirstName, required)
          : undefined,
      otherPartnerLastName:
        citizen && requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerLastName, required)
          : undefined,
      otherPartnerSSN: citizen
        ? requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerSSN, required, ssn)
          : undefined
        : requireFullFamily &&
            form.contactInfo.otherPartnerExists &&
            form.contactInfo.otherPartnerSSN
          ? ssn(form.contactInfo.otherPartnerSSN)
          : undefined,
      otherChildren: {
        arrayErrors: undefined,
        itemErrors: form.contactInfo.otherChildren.map((child) =>
          citizen && requireFullFamily && form.contactInfo.otherChildrenExists
            ? {
                firstName: validate(child.firstName, required),
                lastName: validate(child.lastName, required),
                socialSecurityNumber: validate(
                  child.socialSecurityNumber,
                  required,
                  ssn
                )
              }
            : {
                firstName: undefined,
                lastName: undefined,
                socialSecurityNumber:
                  !citizen &&
                  requireFullFamily &&
                  form.contactInfo.otherChildrenExists &&
                  child.socialSecurityNumber
                    ? ssn(child.socialSecurityNumber)
                    : undefined
              }
        )
      }
    },
    additionalDetails: {}
  }
}
