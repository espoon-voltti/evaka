import {
  email,
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

export type ApplicationFormDataErrors = {
  [section in keyof ApplicationFormData]: ErrorsOf<ApplicationFormData[section]>
}

export const applicationHasErrors = (errors: ApplicationFormDataErrors) => {
  const totalErrors = Object.keys(errors).reduce((acc, section) => {
    return acc + getErrorCount(errors[section])
  }, 0)
  return totalErrors > 0
}

export const validateApplication = (
  {
    type,
    otherGuardianId,
    otherGuardianLivesInSameAddress
  }: ApplicationDetails,
  form: ApplicationFormData
): ApplicationFormDataErrors => {
  const requireFullFamily =
    type === 'DAYCARE' ||
    (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)

  const siblingSelected =
    form.unitPreference.vtjSiblings.find((s) => s.selected) !== undefined

  return {
    serviceNeed: {
      preferredStartDate: validate(
        form.serviceNeed.preferredStartDate,
        required,
        validDate
      ),
      startTime:
        type === 'DAYCARE' ||
        (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
          ? required(form.serviceNeed.startTime) ||
            regexp(form.serviceNeed.startTime, TIME_REGEXP, 'timeFormat')
          : undefined,
      endTime:
        type === 'DAYCARE' ||
        (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
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
        type === 'PRESCHOOL' &&
        otherGuardianId &&
        otherGuardianLivesInSameAddress === false
          ? requiredSelection(form.contactInfo.otherGuardianAgreementStatus)
          : undefined,
      otherGuardianPhone:
        type === 'PRESCHOOL' &&
        otherGuardianId &&
        otherGuardianLivesInSameAddress === false &&
        form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
          ? phone(form.contactInfo.otherGuardianPhone)
          : undefined,
      otherGuardianEmail:
        type === 'PRESCHOOL' &&
        otherGuardianId &&
        otherGuardianLivesInSameAddress === false &&
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
