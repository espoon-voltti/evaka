// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
  validate,
  validDate
} from '../../form-validation'
import { ApplicationFormData } from '../../applications/editor/ApplicationFormData'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import {
  ApplicationStatus,
  ApplicationType
} from 'lib-common/api-types/application/enums'
import LocalDate from 'lib-common/local-date'
import { DecisionType } from '../../decisions/types'

export type ApplicationFormDataErrors = {
  [section in keyof ApplicationFormData]: ErrorsOf<ApplicationFormData[section]>
}

export const applicationHasErrors = (errors: ApplicationFormDataErrors) => {
  const totalErrors = Object.keys(errors).reduce((acc, section) => {
    return acc + getErrorCount(errors[section])
  }, 0)
  return totalErrors > 0
}

const minPreferredStartDate = (
  status: ApplicationStatus,
  type: ApplicationType,
  originalPreferredStartDate: LocalDate | null,
  isUrgent: boolean
): LocalDate => {
  if (status !== 'CREATED') {
    return originalPreferredStartDate
      ? originalPreferredStartDate
      : LocalDate.today()
  } else {
    const today = LocalDate.today()
    return type === 'DAYCARE'
      ? isUrgent
        ? today.addWeeks(2)
        : today.addMonths(4)
      : today
  }
}

const maxPreferredStartDate = (): LocalDate => {
  return LocalDate.today().addYears(1)
}

const maxDecisionStartDate = (
  startDate: LocalDate,
  type: DecisionType
): LocalDate => {
  return ['PRESCHOOL', 'PREPARATORY_EDUCATION'].includes(type)
    ? startDate
    : startDate.addDays(14)
}

export const isValidPreferredStartDate = (
  date: LocalDate,
  originalPreferredStartDate: LocalDate | null,
  status: ApplicationStatus,
  type: ApplicationType,
  isUrgent: boolean
): boolean => {
  if (
    date.isBefore(
      minPreferredStartDate(status, type, originalPreferredStartDate, isUrgent)
    )
  )
    return false

  if (date.isAfter(maxPreferredStartDate())) return false

  if (type === 'PRESCHOOL') {
    // cannot apply for summer time between extended preschool terms
    if (date.isBetween(LocalDate.of(2021, 6, 5), LocalDate.of(2021, 7, 31)))
      return false
  }

  return true
}

export const isValidDecisionStartDate = (
  date: LocalDate,
  startDate: string,
  type: DecisionType
): boolean => {
  let parsedDate: LocalDate
  try {
    parsedDate = LocalDate.parseFiOrThrow(startDate)
  } catch (e) {
    if (e instanceof RangeError) {
      return false
    }
    // Unexpected
    throw e
  }
  return (
    date.isEqualOrAfter(parsedDate) &&
    date.isEqualOrBefore(maxDecisionStartDate(parsedDate, type))
  )
}

const preferredStartDateValidator = (
  originalPreferredStartDate: LocalDate | null,
  status: ApplicationStatus,
  type: ApplicationType,
  isUrgent: boolean
) => (
  val: string,
  err: ErrorKey = 'preferredStartDate'
): ErrorKey | undefined => {
  const date = LocalDate.parseFiOrNull(val)
  return date &&
    isValidPreferredStartDate(
      date,
      originalPreferredStartDate,
      status,
      type,
      isUrgent
    )
    ? undefined
    : err
}

export const validateApplication = (
  apiData: ApplicationDetails,
  form: ApplicationFormData
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
        validDate,
        preferredStartDateValidator(
          apiData.form.preferences.preferredStartDate,
          apiData.status,
          apiData.type,
          form.serviceNeed.urgent
        )
      ),
      startTime:
        apiData.type === 'DAYCARE' ||
        (apiData.type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
          ? required(form.serviceNeed.startTime) ||
            regexp(form.serviceNeed.startTime, TIME_REGEXP, 'timeFormat')
          : undefined,
      endTime:
        apiData.type === 'DAYCARE' ||
        (apiData.type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
          ? required(form.serviceNeed.endTime) ||
            regexp(form.serviceNeed.endTime, TIME_REGEXP, 'timeFormat')
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
        ? validate(form.contactInfo.childMoveDate, required, validDate)
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
        ? validate(form.contactInfo.guardianMoveDate, required, validDate)
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
        apiData.type === 'PRESCHOOL' &&
        apiData.otherGuardianId &&
        apiData.otherGuardianLivesInSameAddress === false
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone:
        apiData.type === 'PRESCHOOL' &&
        apiData.otherGuardianId &&
        apiData.otherGuardianLivesInSameAddress === false &&
        form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined,
      otherGuardianEmail:
        apiData.type === 'PRESCHOOL' &&
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
    fee: {},
    additionalDetails: {}
  }
}
