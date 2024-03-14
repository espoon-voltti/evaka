// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  email,
  emailVerificationCheck,
  ErrorKey,
  ErrorsOf,
  getErrorCount,
  phone,
  regexp,
  required,
  requiredSelection,
  ssn,
  TIME_REGEXP,
  validate
} from 'lib-common/form-validation'
import {
  ApplicationDetails as ApplicationDetailsGen,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'
import { featureFlags } from 'lib-customizations/citizen'

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
  type: ApplicationType,
  terms?: FiniteDateRange[]
): boolean => {
  if (date.isBefore(minPreferredStartDate(originalPreferredStartDate)))
    return false

  if (date.isAfter(maxPreferredStartDate())) return false

  if (terms !== undefined) {
    return terms.some((term) => term.includes(date))
  }

  return true
}

const preferredStartDateValidator =
  (
    originalPreferredStartDate: LocalDate | null,
    type: ApplicationType,
    terms?: FiniteDateRange[]
  ) =>
  (
    val: LocalDate | null,
    err: ErrorKey = 'preferredStartDate'
  ): ErrorKey | undefined =>
    val &&
    isValidPreferredStartDate(val, originalPreferredStartDate, type, terms)
      ? undefined
      : err

export const validateApplication = (
  apiData: ApplicationDetailsGen,
  form: ApplicationFormData,
  terms?: FiniteDateRange[]
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
          apiData.type,
          terms
        )
      ),
      connectedDaycarePreferredStartDate:
        apiData.type === 'PRESCHOOL' &&
        featureFlags.preschoolApplication.connectedDaycarePreferredStartDate &&
        form.serviceNeed.connectedDaycare
          ? required(form.serviceNeed.connectedDaycarePreferredStartDate)
          : undefined,
      serviceNeedOption:
        apiData.type === 'PRESCHOOL' &&
        featureFlags.preschoolApplication.serviceNeedOption &&
        form.serviceNeed.connectedDaycare
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
        apiData.otherGuardianId &&
        apiData.otherGuardianLivesInSameAddress === false
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone:
        apiData.type !== 'CLUB' &&
        apiData.otherGuardianId &&
        apiData.otherGuardianLivesInSameAddress === false &&
        form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined,
      otherGuardianEmail:
        apiData.type !== 'CLUB' &&
        apiData.otherGuardianId &&
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
