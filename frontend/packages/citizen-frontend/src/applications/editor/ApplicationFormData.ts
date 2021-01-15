import { Application, ApplicationForm } from '~applications/types'
import LocalDate from '@evaka/lib-common/src/local-date'

export type ServiceNeedFormData = {
  preferredStartDate: LocalDate | null
  urgent: boolean
}

export type UnitPreferenceFormData = {
  // todo
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
    unitPreference: {},
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
      siblingBasis: null,
      preparatory: false,
      urgent: form.serviceNeed.urgent
    },
    maxFeeAccepted: false,
    otherInfo: 'string',
    clubDetails: null
  }
}
