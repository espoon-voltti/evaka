import LocalDate from '@evaka/lib-common/src/local-date'
import { Application, ApplicationForm } from '~applications/types'

export type ServiceNeedFormData = {
  preferredStartDate: LocalDate | null
  urgent: boolean
  startTime: string
  endTime: string
  shiftCare: boolean
  partTime: boolean
  assistanceNeeded: boolean
  assistanceDescription: string
}

export type UnitPreferenceFormData = {
  siblingBasis: boolean
  siblingName: string
  siblingSsn: string
  preferredUnits: Array<{ id: string; name: string }>
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
      urgent: application.form.preferences.urgent,
      startTime: application.form.preferences.serviceNeed?.startTime ?? '',
      endTime: application.form.preferences.serviceNeed?.endTime ?? '',
      shiftCare: application.form.preferences.serviceNeed?.shiftCare ?? false,
      partTime: application.form.preferences.serviceNeed?.partTime ?? false,
      assistanceNeeded: application.form.child.assistanceNeeded,
      assistanceDescription: application.form.child.assistanceDescription
    },
    unitPreference: {
      siblingBasis: application.form.preferences.siblingBasis !== null,
      siblingName: application.form.preferences.siblingBasis?.siblingName ?? '',
      siblingSsn: application.form.preferences.siblingBasis?.siblingSsn ?? '',
      preferredUnits: application.form.preferences.preferredUnits
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
      assistanceNeeded: form.serviceNeed.assistanceNeeded,
      assistanceDescription: form.serviceNeed.assistanceDescription
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
    maxFeeAccepted: false,
    otherInfo: 'string',
    clubDetails: null
  }
}
