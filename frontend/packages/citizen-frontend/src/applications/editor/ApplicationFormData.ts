import LocalDate from '@evaka/lib-common/src/local-date'
import {
  ApplicationAttachment,
  ApplicationDetails,
  ApplicationFormUpdate,
  ApplicationPersonBasics
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'

export type ServiceNeedFormData = {
  preferredStartDate: LocalDate | null
  urgent: boolean
  startTime: string
  endTime: string
  shiftCare: boolean
  partTime: boolean
  assistanceNeeded: boolean
  assistanceDescription: string
  shiftCareAttachments: ApplicationAttachment[]
  urgencyAttachments: ApplicationAttachment[]
}

export type UnitPreferenceFormData = {
  siblingBasis: boolean
  siblingName: string
  siblingSsn: string
  preferredUnits: Array<{ id: string; name: string }>
}

export type ContactInfoFormData = {
  childFirstName: string
  childLastName: string
  childSSN: string
  childStreet: string
  childFutureAddressExists: boolean
  childMoveDate: LocalDate | null
  childFutureStreet: string
  childFuturePostalCode: string
  childFuturePostOffice: string
  guardianFirstName: string
  guardianLastName: string
  guardianSSN: string
  guardianHomeAddress: string
  guardianPhone: string
  guardianEmail: string
  guardianFutureAddressExists: boolean
  guardianMoveDate: LocalDate | null
  guardianFutureStreet: string
  guardianFuturePostalCode: string
  guardianFuturePostOffice: string
  otherPartnerInSameAddress: boolean
  otherPartnerFirstName: string
  otherPartnerLastName: string
  otherPartnerSSN: string
  otherChildren: ApplicationPersonBasics[]
}

export type FeeFormData = {
  maxFeeAccepted: boolean
}

export type AdditionalDetailsFormData = {
  otherInfo: string
  diet: string
  allergies: string
}

export type ApplicationFormData = {
  serviceNeed: ServiceNeedFormData
  unitPreference: UnitPreferenceFormData
  contactInfo: ContactInfoFormData
  fee: FeeFormData
  additionalDetails: AdditionalDetailsFormData
}

export function apiDataToFormData(
  application: ApplicationDetails
): ApplicationFormData {
  return {
    serviceNeed: {
      preferredStartDate: application.form.preferences.preferredStartDate,
      urgent: application.form.preferences.urgent,
      startTime: application.form.preferences.serviceNeed?.startTime ?? '',
      endTime: application.form.preferences.serviceNeed?.endTime ?? '',
      shiftCare: application.form.preferences.serviceNeed?.shiftCare ?? false,
      partTime: application.form.preferences.serviceNeed?.partTime ?? false,
      assistanceNeeded: application.form.child.assistanceNeeded,
      assistanceDescription: application.form.child.assistanceDescription,
      shiftCareAttachments: application.attachments.filter(
        ({ type }: ApplicationAttachment) => type === 'EXTENDED_CARE'
      ),
      urgencyAttachments: application.attachments.filter(
        ({ type }: ApplicationAttachment) => type === 'URGENCY'
      )
    },
    unitPreference: {
      siblingBasis: application.form.preferences.siblingBasis !== null,
      siblingName: application.form.preferences.siblingBasis?.siblingName ?? '',
      siblingSsn: application.form.preferences.siblingBasis?.siblingSsn ?? '',
      preferredUnits: application.form.preferences.preferredUnits
    },
    contactInfo: {
      childFirstName: application.form.child.person.firstName,
      childLastName: application.form.child.person.lastName,
      childSSN: application.form.child.person.socialSecurityNumber || '',
      childStreet:
        `${application.form.child.address?.street || ''}, ${
          application.form.child.address?.postalCode || ''
        } ${application.form.child.address?.postOffice || ''}` || '',
      childFutureAddressExists: false,
      childMoveDate: null,
      childFutureStreet: '',
      childFuturePostalCode: '',
      childFuturePostOffice: '',
      guardianFirstName: application.form.guardian.person.firstName,
      guardianLastName: application.form.guardian.person.lastName,
      guardianSSN: application.form.guardian.person.socialSecurityNumber || '',
      guardianHomeAddress: application.form.guardian.address?.street || '',
      guardianPhone: application.form.guardian.phoneNumber || '',
      guardianEmail: application.form.guardian.email || '',
      guardianFutureAddressExists: false,
      guardianMoveDate: null,
      guardianFutureStreet: '',
      guardianFuturePostalCode: '',
      guardianFuturePostOffice: '',
      otherPartnerInSameAddress: false,
      otherPartnerFirstName: '',
      otherPartnerLastName: '',
      otherPartnerSSN: '',
      otherChildren: application.form.otherChildren
    },
    fee: { maxFeeAccepted: application.form.maxFeeAccepted },
    additionalDetails: {
      otherInfo: application.form.otherInfo,
      diet: application.form.child.diet,
      allergies: application.form.child.allergies
    }
  }
}

export function formDataToApiData(
  form: ApplicationFormData
): ApplicationFormUpdate {
  return {
    child: {
      futureAddress: form.contactInfo.childFutureAddressExists
        ? {
            street: form.contactInfo.childFutureStreet,
            postalCode: form.contactInfo.childFuturePostalCode,
            postOffice: form.contactInfo.childFuturePostOffice,
            movingDate: form.contactInfo.childMoveDate
          }
        : null,
      allergies: form.additionalDetails.allergies,
      diet: form.additionalDetails.diet,
      assistanceNeeded: form.serviceNeed.assistanceNeeded,
      assistanceDescription: form.serviceNeed.assistanceDescription
    },
    guardian: {
      futureAddress: form.contactInfo.guardianFutureAddressExists
        ? {
            street: form.contactInfo.guardianFutureStreet,
            postalCode: form.contactInfo.guardianFuturePostalCode,
            postOffice: form.contactInfo.guardianFuturePostOffice,
            movingDate: form.contactInfo.guardianMoveDate
          }
        : null,
      phoneNumber: form.contactInfo.guardianPhone,
      email: form.contactInfo.guardianEmail
    },
    secondGuardian: null,
    otherPartner: form.contactInfo.otherPartnerInSameAddress
      ? {
          firstName: form.contactInfo.otherPartnerFirstName,
          lastName: form.contactInfo.otherPartnerLastName,
          socialSecurityNumber: form.contactInfo.otherPartnerSSN
        }
      : null,
    otherChildren: form.contactInfo.otherChildren,
    preferences: {
      preferredUnits: form.unitPreference.preferredUnits,
      preferredStartDate: form.serviceNeed.preferredStartDate,
      serviceNeed: {
        startTime: form.serviceNeed.startTime,
        endTime: form.serviceNeed.endTime,
        shiftCare: form.serviceNeed.shiftCare,
        partTime: form.serviceNeed.partTime
      },
      siblingBasis: form.unitPreference.siblingBasis
        ? {
            siblingName: form.unitPreference.siblingName,
            siblingSsn: form.unitPreference.siblingSsn
          }
        : null,
      preparatory: false,
      urgent: form.serviceNeed.urgent
    },
    maxFeeAccepted: form.fee.maxFeeAccepted,
    otherInfo: form.additionalDetails.otherInfo,
    clubDetails: null
  }
}
