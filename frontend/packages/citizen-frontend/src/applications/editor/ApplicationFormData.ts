import LocalDate from '@evaka/lib-common/src/local-date'
import {
  ApplicationAttachment,
  ApplicationDetails,
  ApplicationFormUpdate
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { ApplicationGuardianAgreementStatus } from '@evaka/lib-common/src/api-types/application/enums'
import { User } from '../../auth/state'

export type ServiceNeedFormData = {
  preferredStartDate: string
  urgent: boolean
  connectedDaycare: boolean
  startTime: string
  endTime: string
  shiftCare: boolean
  partTime: boolean
  preparatory: boolean
  assistanceNeeded: boolean
  assistanceDescription: string
  shiftCareAttachments: ApplicationAttachment[]
  urgencyAttachments: ApplicationAttachment[]
  wasOnClubCare: boolean
  wasOnDaycare: boolean
}

export type UnitPreferenceFormData = {
  siblingBasis: boolean
  vtjSiblings: VtjSibling[]
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
  childMoveDate: string
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
  guardianFutureAddressEqualsChild: boolean
  guardianMoveDate: string
  guardianFutureStreet: string
  guardianFuturePostalCode: string
  guardianFuturePostOffice: string
  otherGuardianAgreementStatus: ApplicationGuardianAgreementStatus | null
  otherGuardianPhone: string
  otherGuardianEmail: string
  otherPartnerExists: boolean
  otherPartnerFirstName: string
  otherPartnerLastName: string
  otherPartnerSSN: string
  vtjSiblings: VtjSibling[]
  otherChildrenExists: boolean
  otherChildren: OtherChildFormData[]
}

export type VtjSibling = {
  firstName: string
  lastName: string
  socialSecurityNumber: string
  selected: boolean
}

export type OtherChildFormData = {
  firstName: string
  lastName: string
  socialSecurityNumber: string
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
  application: ApplicationDetails,
  user: User | undefined
): ApplicationFormData {
  const vtjSiblings: VtjSibling[] = user
    ? user.children
        .filter(
          (vtjChild) =>
            vtjChild.socialSecurityNumber !==
            application.form.child.person.socialSecurityNumber
        )
        .map((vtjChild) => ({
          firstName: vtjChild.firstName,
          lastName: vtjChild.lastName,
          socialSecurityNumber: vtjChild.socialSecurityNumber,
          selected: false
        }))
    : []

  const vtjSiblingsSiblingBasis = vtjSiblings.map((sibling) => ({
    ...sibling,
    selected:
      application.form.preferences.siblingBasis?.siblingSsn ===
      sibling.socialSecurityNumber
  }))

  const siblingBasisFromVtj =
    vtjSiblingsSiblingBasis.find((s) => s.selected) !== undefined

  const vtjSiblingsOtherChildren = vtjSiblings.map((sibling) => ({
    ...sibling,
    selected:
      application.form.otherChildren.find(
        (child) => child.socialSecurityNumber === sibling.socialSecurityNumber
      ) !== undefined
  }))

  const otherChildren: OtherChildFormData[] = application.form.otherChildren
    .filter(
      (child) =>
        !vtjSiblings.find(
          (vtjChild) =>
            vtjChild.socialSecurityNumber === child.socialSecurityNumber
        )
    )
    .map((child) => ({
      firstName: child.firstName,
      lastName: child.lastName,
      socialSecurityNumber: child.socialSecurityNumber ?? ''
    }))

  return {
    serviceNeed: {
      preferredStartDate:
        application.form.preferences.preferredStartDate?.format() ?? '',
      urgent: application.form.preferences.urgent,
      connectedDaycare:
        application.type === 'PRESCHOOL' &&
        application.form.preferences.serviceNeed !== null,
      startTime: application.form.preferences.serviceNeed?.startTime ?? '',
      endTime: application.form.preferences.serviceNeed?.endTime ?? '',
      shiftCare: application.form.preferences.serviceNeed?.shiftCare ?? false,
      partTime: application.form.preferences.serviceNeed?.partTime ?? false,
      preparatory: application.form.preferences.preparatory,
      assistanceNeeded: application.form.child.assistanceNeeded,
      assistanceDescription: application.form.child.assistanceDescription,
      shiftCareAttachments: application.attachments.filter(
        ({ type }: ApplicationAttachment) => type === 'EXTENDED_CARE'
      ),
      urgencyAttachments: application.attachments.filter(
        ({ type }: ApplicationAttachment) => type === 'URGENCY'
      ),
      wasOnClubCare: application.form.clubDetails?.wasOnClubCare ?? false,
      wasOnDaycare: application.form.clubDetails?.wasOnDaycare ?? false
    },
    unitPreference: {
      siblingBasis: application.form.preferences.siblingBasis !== null,
      vtjSiblings: vtjSiblingsSiblingBasis,
      siblingName: siblingBasisFromVtj
        ? ''
        : application.form.preferences.siblingBasis?.siblingName ?? '',
      siblingSsn: siblingBasisFromVtj
        ? ''
        : application.form.preferences.siblingBasis?.siblingSsn ?? '',
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
      childFutureAddressExists: application.form.child.futureAddress !== null,
      childMoveDate:
        application.form.child.futureAddress?.movingDate?.format() ?? '',
      childFutureStreet: application.form.child.futureAddress?.street ?? '',
      childFuturePostalCode:
        application.form.child.futureAddress?.postalCode ?? '',
      childFuturePostOffice:
        application.form.child.futureAddress?.postOffice ?? '',
      guardianFirstName: application.form.guardian.person.firstName,
      guardianLastName: application.form.guardian.person.lastName,
      guardianSSN: application.form.guardian.person.socialSecurityNumber || '',
      guardianHomeAddress: application.form.guardian.address?.street || '',
      guardianPhone: application.form.guardian.phoneNumber || '',
      guardianEmail: application.form.guardian.email || '',
      guardianFutureAddressExists:
        application.form.guardian.futureAddress !== null,
      guardianFutureAddressEqualsChild:
        (application.form.child.futureAddress &&
          application.form.guardian.futureAddress &&
          application.form.child.futureAddress.street ===
            application.form.guardian.futureAddress.street &&
          application.form.child.futureAddress.postalCode ===
            application.form.guardian.futureAddress.postalCode &&
          application.form.child.futureAddress.postOffice ===
            application.form.guardian.futureAddress.postOffice) ??
        false,
      guardianMoveDate:
        application.form.guardian.futureAddress?.movingDate?.format() ?? '',
      guardianFutureStreet:
        application.form.guardian.futureAddress?.street ?? '',
      guardianFuturePostalCode:
        application.form.guardian.futureAddress?.postalCode ?? '',
      guardianFuturePostOffice:
        application.form.guardian.futureAddress?.postOffice ?? '',
      otherGuardianAgreementStatus:
        application.form.secondGuardian?.agreementStatus ?? null,
      otherGuardianPhone: application.form.secondGuardian?.phoneNumber ?? '',
      otherGuardianEmail: application.form.secondGuardian?.email ?? '',
      otherPartnerExists: application.form.otherPartner !== null,
      otherPartnerFirstName: application.form.otherPartner?.firstName ?? '',
      otherPartnerLastName: application.form.otherPartner?.lastName ?? '',
      otherPartnerSSN:
        application.form.otherPartner?.socialSecurityNumber ?? '',
      vtjSiblings: vtjSiblingsOtherChildren,
      otherChildrenExists: otherChildren.length > 0,
      otherChildren
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
  {
    type,
    otherGuardianId,
    otherGuardianLivesInSameAddress
  }: ApplicationDetails,
  form: ApplicationFormData
): ApplicationFormUpdate {
  const fullFamily =
    type === 'DAYCARE' ||
    (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)

  const selectedSibling = form.unitPreference.vtjSiblings.find(
    (s) => s.selected
  )

  return {
    child: {
      futureAddress: form.contactInfo.childFutureAddressExists
        ? {
            street: form.contactInfo.childFutureStreet,
            postalCode: form.contactInfo.childFuturePostalCode,
            postOffice: form.contactInfo.childFuturePostOffice,
            movingDate: LocalDate.parseFi(form.contactInfo.childMoveDate)
          }
        : null,
      allergies: type !== 'CLUB' ? form.additionalDetails.allergies : '',
      diet: type !== 'CLUB' ? form.additionalDetails.diet : '',
      assistanceNeeded: form.serviceNeed.assistanceNeeded,
      assistanceDescription: form.serviceNeed.assistanceDescription
    },
    guardian: {
      futureAddress: form.contactInfo.guardianFutureAddressExists
        ? {
            street: form.contactInfo.guardianFutureStreet,
            postalCode: form.contactInfo.guardianFuturePostalCode,
            postOffice: form.contactInfo.guardianFuturePostOffice,
            movingDate: LocalDate.parseFi(form.contactInfo.guardianMoveDate)
          }
        : null,
      phoneNumber: form.contactInfo.guardianPhone,
      email: form.contactInfo.guardianEmail
    },
    secondGuardian:
      type === 'PRESCHOOL' &&
      otherGuardianId &&
      !otherGuardianLivesInSameAddress
        ? {
            agreementStatus:
              form.contactInfo.otherGuardianAgreementStatus ?? 'NOT_AGREED',
            phoneNumber:
              form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
                ? form.contactInfo.otherGuardianPhone
                : '',
            email:
              form.contactInfo.otherGuardianAgreementStatus === 'NOT_AGREED'
                ? form.contactInfo.otherGuardianEmail
                : ''
          }
        : null,
    otherPartner:
      fullFamily && form.contactInfo.otherPartnerExists
        ? {
            firstName: form.contactInfo.otherPartnerFirstName,
            lastName: form.contactInfo.otherPartnerLastName,
            socialSecurityNumber: form.contactInfo.otherPartnerSSN
          }
        : null,
    otherChildren: fullFamily
      ? [
          ...form.contactInfo.vtjSiblings
            .filter((child) => child.selected)
            .map(({ firstName, lastName, socialSecurityNumber }) => ({
              firstName,
              lastName,
              socialSecurityNumber
            })),
          ...(form.contactInfo.otherChildrenExists
            ? form.contactInfo.otherChildren
            : [])
        ]
      : [],
    preferences: {
      preferredUnits: form.unitPreference.preferredUnits,
      preferredStartDate: LocalDate.parseFiOrNull(
        form.serviceNeed.preferredStartDate
      ),
      serviceNeed:
        type === 'DAYCARE' ||
        (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
          ? {
              startTime: form.serviceNeed.startTime,
              endTime: form.serviceNeed.endTime,
              shiftCare: form.serviceNeed.shiftCare,
              partTime: type === 'DAYCARE' && form.serviceNeed.partTime
            }
          : null,
      siblingBasis: form.unitPreference.siblingBasis
        ? {
            siblingName: selectedSibling
              ? `${selectedSibling.firstName} ${selectedSibling.lastName}`
              : form.unitPreference.siblingName,
            siblingSsn:
              selectedSibling?.socialSecurityNumber ??
              form.unitPreference.siblingSsn
          }
        : null,
      preparatory: type === 'PRESCHOOL' && form.serviceNeed.preparatory,
      urgent: type === 'DAYCARE' && form.serviceNeed.urgent
    },
    maxFeeAccepted: form.fee.maxFeeAccepted,
    otherInfo: form.additionalDetails.otherInfo,
    clubDetails:
      type === 'CLUB'
        ? {
            wasOnClubCare: form.serviceNeed.wasOnClubCare,
            wasOnDaycare: form.serviceNeed.wasOnDaycare
          }
        : null
  }
}
