// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationEditorActor,
  ApplicationFormData
} from 'lib-common/application/ApplicationFormData'
import { requiresFullFamily } from 'lib-common/application/ApplicationFormData'
import type { FeatureFlags } from 'lib-common/feature-flags'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ErrorKey,
  ErrorsOf,
  StandardValidator
} from 'lib-common/form-validation'
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
  validateIf,
  validDuration
} from 'lib-common/form-validation'
import type {
  ApplicationAttachment,
  ApplicationDetails as ApplicationDetailsGen,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import type {
  ClubTerm,
  PreschoolTerm
} from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'

export interface Term {
  term: FiniteDateRange
  extendedTerm: FiniteDateRange
}

/**
 * Adapts the two term shapes the API returns into the single {@link Term} shape
 * the editor and its validation use.
 *
 * `onlyOpenForApplications` is true for citizens, who may only apply to terms
 * whose application period is currently open; employees record paper
 * applications against any term.
 */
export const toApplicationTerms = (
  type: ApplicationType,
  preschoolTerms: PreschoolTerm[],
  clubTerms: ClubTerm[],
  onlyOpenForApplications: boolean
): Term[] | undefined => {
  switch (type) {
    case 'PRESCHOOL':
      return preschoolTerms
        .filter(({ applicationPeriod, extendedTerm }) => {
          if (!onlyOpenForApplications) return true
          const today = LocalDate.todayInSystemTz()
          return (
            applicationPeriod.start.isEqualOrBefore(today) &&
            extendedTerm.end.isEqualOrAfter(today)
          )
        })
        .map((term) => ({
          term: term.finnishPreschool,
          extendedTerm: term.extendedTerm
        }))
    case 'CLUB':
      return clubTerms
        .filter(
          ({ applicationPeriod }) =>
            !onlyOpenForApplications ||
            applicationPeriod.includes(LocalDate.todayInHelsinkiTz())
        )
        .map(({ term }) => ({ term, extendedTerm: term }))
    default:
      return undefined
  }
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

// `checkWindow` is false for employees: they transcribe paper applications that
// may have been filed long ago, so the "not in the past, at most a year ahead"
// window does not apply to them. Term membership still does.
const isValidPreferredStartDate = (
  date: LocalDate,
  originalPreferredStartDate: LocalDate | null,
  terms: Term[] | undefined,
  checkWindow: boolean
): boolean => {
  if (checkWindow) {
    if (date.isBefore(minPreferredStartDate(originalPreferredStartDate)))
      return false

    if (date.isAfter(maxPreferredStartDate())) return false
  }

  if (terms !== undefined) {
    return terms.some((term) => term.extendedTerm.includes(date))
  }

  return true
}

const preferredStartDateValidator =
  (
    originalPreferredStartDate: LocalDate | null,
    terms: Term[] | undefined,
    checkWindow = true
  ) =>
  (
    val: LocalDate | null,
    err: ErrorKey = 'preferredStartDate'
  ): ErrorKey | undefined =>
    val &&
    isValidPreferredStartDate(
      val,
      originalPreferredStartDate,
      terms,
      checkWindow
    )
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

  const requireFullFamily = requiresFullFamily(apiData.type, form)

  const siblingSelected =
    form.unitPreference.vtjSiblings.find((s) => s.selected) !== undefined

  const maxPartTimeDailyMinutes = 300 // 5 hours, max part-time daily duration

  // Whether each conditionally rendered field is actually on screen. Named once
  // so the citizen and employee rules below cannot drift apart from each other.
  const dailyTimesVisible =
    (apiData.type === 'DAYCARE' &&
      featureFlags.daycareApplication.dailyTimes) ||
    (apiData.type === 'PRESCHOOL' &&
      !featureFlags.preschoolApplication.serviceNeedOption &&
      form.serviceNeed.connectedDaycare)
  const childFutureAddressVisible = form.contactInfo.childFutureAddressExists
  const guardianFutureAddressVisible =
    form.contactInfo.guardianFutureAddressExists
  const otherPartnerVisible =
    requireFullFamily && form.contactInfo.otherPartnerExists
  const otherChildrenVisible =
    requireFullFamily && form.contactInfo.otherChildrenExists
  const otherGuardianSeparated =
    apiData.type !== 'CLUB' &&
    apiData.hasOtherGuardian &&
    apiData.otherGuardianLivesInSameAddress === false

  // Some fields are not required of an employee filling a paper application,
  // but anything they did fill in must still be formatted correctly.
  const formatIfPresent = (
    value: string,
    visible: boolean,
    ...format: StandardValidator<string>[]
  ): ErrorKey | undefined =>
    visible && value ? validate(value, ...format) : undefined

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
          : preferredStartDateValidator(null, terms, false)
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
      startTime: dailyTimesVisible
        ? required(form.serviceNeed.startTime, 'timeRequired') ||
          regexp(form.serviceNeed.startTime, TIME_REGEXP, 'timeFormat')
        : undefined,
      endTime: dailyTimesVisible
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
      assistanceDescription: validateIf(
        form.serviceNeed.assistanceNeeded,
        form.serviceNeed.assistanceDescription,
        required
      ),
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
      siblingName: validateIf(
        citizen && form.unitPreference.siblingBasis && !siblingSelected,
        form.unitPreference.siblingName,
        required
      ),
      siblingSsn: citizen
        ? validateIf(
            form.unitPreference.siblingBasis && !siblingSelected,
            form.unitPreference.siblingSsn,
            required,
            ssn
          )
        : formatIfPresent(
            form.unitPreference.siblingSsn,
            form.unitPreference.siblingBasis,
            ssn
          ),
      siblingUnit: validateIf(
        citizen &&
          form.unitPreference.siblingBasis &&
          apiData.type === 'PRESCHOOL',
        form.unitPreference.siblingUnit,
        required
      ),
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
      childMoveDate: validateIf(
        citizen && childFutureAddressVisible,
        form.contactInfo.childMoveDate,
        required
      ),
      childFutureStreet: validateIf(
        citizen && childFutureAddressVisible,
        form.contactInfo.childFutureStreet,
        required
      ),
      childFuturePostalCode: validateIf(
        citizen && childFutureAddressVisible,
        form.contactInfo.childFuturePostalCode,
        required
      ),
      childFuturePostOffice: validateIf(
        citizen && childFutureAddressVisible,
        form.contactInfo.childFuturePostOffice,
        required
      ),
      guardianPhone: citizen
        ? validate(form.contactInfo.guardianPhone, required, phone)
        : formatIfPresent(form.contactInfo.guardianPhone, true, phone),
      guardianEmail: citizen
        ? form.contactInfo.noGuardianEmail &&
          form.contactInfo.guardianEmail.length === 0
          ? undefined
          : validate(form.contactInfo.guardianEmail, email, required)
        : formatIfPresent(form.contactInfo.guardianEmail, true, email),
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
      guardianMoveDate: validateIf(
        citizen && guardianFutureAddressVisible,
        form.contactInfo.guardianMoveDate,
        required
      ),
      guardianFutureStreet: validateIf(
        citizen && guardianFutureAddressVisible,
        form.contactInfo.guardianFutureStreet,
        required
      ),
      guardianFuturePostalCode: validateIf(
        citizen && guardianFutureAddressVisible,
        form.contactInfo.guardianFuturePostalCode,
        required
      ),
      guardianFuturePostOffice: validateIf(
        citizen && guardianFutureAddressVisible,
        form.contactInfo.guardianFuturePostOffice,
        required
      ),
      otherGuardianAgreementStatus:
        citizen && otherGuardianSeparated
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone: citizen
        ? validateIf(
            otherGuardianSeparated &&
              form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED',
            form.contactInfo.otherGuardianPhone,
            phone
          )
        : formatIfPresent(form.contactInfo.otherGuardianPhone, true, phone),
      otherGuardianEmail: citizen
        ? validateIf(
            otherGuardianSeparated &&
              form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED',
            form.contactInfo.otherGuardianEmail,
            email
          )
        : formatIfPresent(form.contactInfo.otherGuardianEmail, true, email),
      otherPartnerFirstName: validateIf(
        citizen && otherPartnerVisible,
        form.contactInfo.otherPartnerFirstName,
        required
      ),
      otherPartnerLastName: validateIf(
        citizen && otherPartnerVisible,
        form.contactInfo.otherPartnerLastName,
        required
      ),
      otherPartnerSSN: citizen
        ? validateIf(
            otherPartnerVisible,
            form.contactInfo.otherPartnerSSN,
            required,
            ssn
          )
        : formatIfPresent(
            form.contactInfo.otherPartnerSSN,
            otherPartnerVisible,
            ssn
          ),
      otherChildren: {
        arrayErrors: undefined,
        itemErrors: form.contactInfo.otherChildren.map((child) =>
          citizen && otherChildrenVisible
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
                socialSecurityNumber: citizen
                  ? undefined
                  : formatIfPresent(
                      child.socialSecurityNumber,
                      otherChildrenVisible,
                      ssn
                    )
              }
        )
      }
    },
    additionalDetails: {}
  }
}
