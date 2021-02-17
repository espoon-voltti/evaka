import {
  email,
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
} from '~form-validation'
import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import {
  ApplicationStatus,
  ApplicationType
} from '@evaka/lib-common/src/api-types/application/enums'
import LocalDate from '@evaka/lib-common/src/local-date'
import { DecisionType } from '~decisions/types'

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
  originalPreferredStartDate: LocalDate | null
): LocalDate => {
  if (status !== 'CREATED') {
    return originalPreferredStartDate
      ? originalPreferredStartDate
      : LocalDate.today()
  } else {
    return type === 'DAYCARE'
      ? LocalDate.today().addDays(14)
      : LocalDate.today()
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
  type: ApplicationType
): boolean => {
  if (date === null) return false

  if (
    date.isBefore(
      minPreferredStartDate(status, type, originalPreferredStartDate)
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
  return date !== null
    ? date.isEqualOrAfter(LocalDate.parseFi(startDate)) &&
        date.isEqualOrBefore(
          maxDecisionStartDate(LocalDate.parseFi(startDate), type)
        )
    : false
}

const preferredStartDateValidator = (
  originalPreferredStartDate: LocalDate | null,
  status: ApplicationStatus,
  type: ApplicationType
) => (
  val: string,
  err: ErrorKey = 'preferredStartDate'
): ErrorKey | undefined => {
  const date = LocalDate.parseFiOrNull(val)
  return date &&
    isValidPreferredStartDate(date, originalPreferredStartDate, status, type)
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
          apiData.type
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
      guardianEmail: validate(form.contactInfo.guardianEmail, email),
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
          requireFullFamily
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
