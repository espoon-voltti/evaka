import LocalDate from '@evaka/lib-common/src/local-date'
import { Application, ApplicationForm } from '~applications/types'

export type ServiceNeedFormData = {
  preferredStartDate: LocalDate | null
  urgent: boolean
}

export type UnitPreferenceFormData = {
  siblingBasis: boolean
  siblingName: string
  siblingSsn: string
}

export type ContactInfoFormData = {
  // todo
}

export type FeeFormData = {
  // todo
}

export type AdditionalDetailsFormData = {
  // todo
}

export type ApplicationFormData = {
  serviceNeed: ServiceNeedFormData
  unitPreference: UnitPreferenceFormData
  contactInfo: ContactInfoFormData
  fee: FeeFormData
  additionalDetails: AdditionalDetailsFormData
}

export function apiDataToFormData(
  application: Application
): ApplicationFormData {
  return {
    serviceNeed: {
      preferredStartDate: application.form.preferences.preferredStartDate,
      urgent: application.form.preferences.urgent
    },
    unitPreference: {
      siblingBasis: application.form.preferences.siblingBasis !== null,
      siblingName: application.form.preferences.siblingBasis?.siblingName ?? '',
      siblingSsn: application.form.preferences.siblingBasis?.siblingSsn ?? ''
    },
    contactInfo: {},
    fee: {},
    additionalDetails: {}
  }
}

export function formDataToApiData(form: ApplicationFormData): ApplicationForm {
  return {
    child: {
      person: {
        firstName: 'string',
        lastName: 'string',
        socialSecurityNumber: null
      },
      dateOfBirth: null,
      address: null,
      futureAddress: null,
      nationality: 'string',
      language: 'string',
      allergies: 'string',
      diet: 'string',
      assistanceNeeded: false,
      assistanceDescription: 'string'
    },
    guardian: {
      person: {
        firstName: 'string',
        lastName: 'string',
        socialSecurityNumber: null
      },
      address: null,
      futureAddress: null,
      phoneNumber: 'string',
      email: 'string'
    },
    secondGuardian: null,
    otherPartner: null,
    otherChildren: [],
    preferences: {
      preferredUnits: [],
      preferredStartDate: form.serviceNeed.preferredStartDate,
      serviceNeed: null,
      siblingBasis: form.unitPreference.siblingBasis
        ? {
            siblingName: form.unitPreference.siblingName,
            siblingSsn: form.unitPreference.siblingSsn
          }
        : null,
      preparatory: false,
      urgent: form.serviceNeed.urgent
    },
    maxFeeAccepted: false,
    otherInfo: 'string',
    clubDetails: null
  }
}
