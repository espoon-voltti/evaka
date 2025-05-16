// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
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
  validate
} from 'lib-common/form-validation'
import type {
  ApplicationAttachment,
  ApplicationDetails as ApplicationDetailsGen
} from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'
import { featureFlags } from 'lib-customizations/citizen'

import type { Term } from './ApplicationEditor'

export type ApplicationFormDataErrors = {
  [section in keyof ApplicationFormData]: ErrorsOf<ApplicationFormData[section]>
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
  urgencyAttachments: ApplicationAttachment[]
) =>
  urgent && urgencyAttachments.length === 0 && featureFlags.urgencyAttachments
    ? featureFlags.requireAttachments
      ? 'require'
      : 'notify'
    : undefined

export const getShiftCareAttachmentsValidStatus = (
  shiftCare: boolean,
  shiftCareAttachments: ApplicationAttachment[]
) =>
  shiftCare && shiftCareAttachments.length === 0
    ? featureFlags.requireAttachments
      ? 'require'
      : 'notify'
    : undefined

export const validateApplication = (
  apiData: ApplicationDetailsGen,
  form: ApplicationFormData,
  terms?: Term[]
): ApplicationFormDataErrors => {
  const requireFullFamily =
    apiData.type === 'DAYCARE' ||
    (apiData.type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)

  const siblingSelected =
    form.unitPreference.vtjSiblings.find((s) => s.selected) !== undefined

  return {
    serviceNeed: {
      preferredStartDate: validate(
        form.serviceNeed.preferredStartDate,
        required,
        preferredStartDateValidator(
          apiData.status !== 'CREATED'
            ? apiData.form.preferences.preferredStartDate
            : null,
          terms
        )
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
      assistanceDescription: form.serviceNeed.assistanceNeeded
        ? required(form.serviceNeed.assistanceDescription)
        : undefined,
      urgencyAttachments:
        getUrgencyAttachmentValidStatus(
          form.serviceNeed.urgent,
          form.serviceNeed.urgencyAttachments
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
        getShiftCareAttachmentsValidStatus(
          form.serviceNeed.shiftCare,
          form.serviceNeed.shiftCareAttachments
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
        form.unitPreference.siblingBasis && !siblingSelected
          ? validate(form.unitPreference.siblingName, required)
          : undefined,
      siblingSsn:
        form.unitPreference.siblingBasis && !siblingSelected
          ? validate(form.unitPreference.siblingSsn, required, ssn)
          : undefined,
      siblingUnit:
        form.unitPreference.siblingBasis && apiData.type === 'PRESCHOOL'
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
      childMoveDate: form.contactInfo.childFutureAddressExists
        ? validate(form.contactInfo.childMoveDate, required)
        : undefined,
      childFutureStreet: form.contactInfo.childFutureAddressExists
        ? validate(form.contactInfo.childFutureStreet, required)
        : undefined,
      childFuturePostalCode: form.contactInfo.childFutureAddressExists
        ? validate(form.contactInfo.childFuturePostalCode, required)
        : undefined,
      childFuturePostOffice: form.contactInfo.childFutureAddressExists
        ? validate(form.contactInfo.childFuturePostOffice, required)
        : undefined,
      guardianPhone: validate(form.contactInfo.guardianPhone, required, phone),
      guardianEmail:
        form.contactInfo.noGuardianEmail &&
        form.contactInfo.guardianEmail.length === 0
          ? undefined
          : validate(form.contactInfo.guardianEmail, email, required),
      guardianEmailVerification:
        form.contactInfo.noGuardianEmail &&
        form.contactInfo.guardianEmailVerification.length === 0
          ? undefined
          : validate(
              form.contactInfo.guardianEmailVerification,
              email,
              emailVerificationCheck(form.contactInfo.guardianEmail)
            ),
      guardianMoveDate: form.contactInfo.guardianFutureAddressExists
        ? validate(form.contactInfo.guardianMoveDate, required)
        : undefined,
      guardianFutureStreet: form.contactInfo.guardianFutureAddressExists
        ? validate(form.contactInfo.guardianFutureStreet, required)
        : undefined,
      guardianFuturePostalCode: form.contactInfo.guardianFutureAddressExists
        ? validate(form.contactInfo.guardianFuturePostalCode, required)
        : undefined,
      guardianFuturePostOffice: form.contactInfo.guardianFutureAddressExists
        ? validate(form.contactInfo.guardianFuturePostOffice, required)
        : undefined,
      otherGuardianAgreementStatus:
        apiData.type !== 'CLUB' &&
        apiData.hasOtherGuardian &&
        apiData.otherGuardianLivesInSameAddress === false
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone:
        apiData.type !== 'CLUB' &&
        apiData.hasOtherGuardian &&
        apiData.otherGuardianLivesInSameAddress === false &&
        form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined,
      otherGuardianEmail:
        apiData.type !== 'CLUB' &&
        apiData.hasOtherGuardian &&
        apiData.otherGuardianLivesInSameAddress === false &&
        form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? email(form.contactInfo.otherGuardianEmail)
          : undefined,
      otherPartnerFirstName:
        requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerFirstName, required)
          : undefined,
      otherPartnerLastName:
        requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerLastName, required)
          : undefined,
      otherPartnerSSN:
        requireFullFamily && form.contactInfo.otherPartnerExists
          ? validate(form.contactInfo.otherPartnerSSN, required, ssn)
          : undefined,
      otherChildren: {
        arrayErrors: undefined,
        itemErrors: form.contactInfo.otherChildren.map((child) =>
          requireFullFamily && form.contactInfo.otherChildrenExists
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
                socialSecurityNumber: undefined
              }
        )
      }
    },
    additionalDetails: {}
  }
}
