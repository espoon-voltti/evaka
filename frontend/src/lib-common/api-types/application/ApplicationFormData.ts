// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { throwIfNull } from 'lib-common/form-validation'
import {
  Address,
  ApplicationAttachment,
  ApplicationDetails,
  ApplicationFormUpdate,
  CitizenChildren,
  OtherGuardianAgreementStatus,
  ServiceNeedOption
} from 'lib-common/generated/api-types/application'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'

export type ServiceNeedFormData = {
  preferredStartDate: LocalDate | null
  urgent: boolean
  connectedDaycare: boolean
  connectedDaycarePreferredStartDate: LocalDate | null
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
  placementType: PlacementType | null
  serviceNeedOption: ServiceNeedOption | null
}

export type UnitPreferenceFormData = {
  siblingBasis: boolean
  vtjSiblings: VtjSibling[]
  siblingName: string
  siblingSsn: string
  preferredUnits: { id: string; name: string }[]
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
  noGuardianEmail: boolean
  guardianEmailVerification: string
  guardianFutureAddressExists: boolean
  guardianFutureAddressEqualsChild: boolean
  guardianMoveDate: LocalDate | null
  guardianFutureStreet: string
  guardianFuturePostalCode: string
  guardianFuturePostOffice: string
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null
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

export type AdditionalDetailsFormData = {
  otherInfo: string
  diet: string
  allergies: string
}

export type ApplicationFormData = {
  serviceNeed: ServiceNeedFormData
  unitPreference: UnitPreferenceFormData
  contactInfo: ContactInfoFormData
  additionalDetails: AdditionalDetailsFormData
}

export function apiDataToFormData(
  application: ApplicationDetails,
  children: CitizenChildren[]
): ApplicationFormData {
  const vtjSiblings: VtjSibling[] = children
    .map(
      ({
        firstName,
        lastName,
        socialSecurityNumber
      }): VtjSibling | undefined =>
        socialSecurityNumber
          ? {
              firstName,
              lastName,
              socialSecurityNumber,
              selected: false
            }
          : undefined
    )
    .filter(
      (vtjChild): vtjChild is VtjSibling =>
        !!vtjChild &&
        vtjChild.socialSecurityNumber !==
          application.form.child.person.socialSecurityNumber
    )

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

  const formatAddress = (address: Address | null): string => {
    if (address) {
      const formattedAddress = `${address.street || ''}, ${address.postalCode || ''} ${
        address.postOffice || ''
      }`

      return formattedAddress.trim().length > 1 ? formattedAddress : ''
    } else {
      return ''
    }
  }

  return {
    serviceNeed: {
      preferredStartDate: application.form.preferences.preferredStartDate,
      connectedDaycarePreferredStartDate:
        application.form.preferences.connectedDaycarePreferredStartDate,
      urgent: application.form.preferences.urgent,
      connectedDaycare:
        application.type === 'PRESCHOOL' &&
        application.form.preferences.serviceNeed !== null,
      startTime: application.form.preferences.serviceNeed?.startTime ?? '',
      endTime: application.form.preferences.serviceNeed?.endTime ?? '',
      shiftCare: application.form.preferences.serviceNeed?.shiftCare ?? false,
      partTime: application.form.preferences.serviceNeed?.partTime ?? false,
      placementType:
        application.form.preferences.serviceNeed?.serviceNeedOption
          ?.validPlacementType ?? 'PRESCHOOL_DAYCARE',
      serviceNeedOption:
        application.form.preferences.serviceNeed?.serviceNeedOption ?? null,
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
      childStreet: formatAddress(application.form.child.address),
      childFutureAddressExists: application.form.child.futureAddress !== null,
      childMoveDate: application.form.child.futureAddress?.movingDate ?? null,
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
      guardianEmailVerification: application.form.guardian.email || '',
      noGuardianEmail: false,
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
        application.form.guardian.futureAddress?.movingDate ?? null,
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
    hasOtherGuardian,
    otherGuardianLivesInSameAddress
  }: ApplicationDetails,
  form: ApplicationFormData,
  isDraft = false
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
            movingDate: isDraft
              ? form.contactInfo.childMoveDate
              : throwIfNull(form.contactInfo.childMoveDate)
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
            movingDate: isDraft
              ? form.contactInfo.guardianMoveDate
              : throwIfNull(form.contactInfo.guardianMoveDate)
          }
        : null,
      phoneNumber: form.contactInfo.guardianPhone,
      email: form.contactInfo.guardianEmail
    },
    secondGuardian:
      type !== 'CLUB' && hasOtherGuardian && !otherGuardianLivesInSameAddress
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
      preferredStartDate: form.serviceNeed.preferredStartDate,
      connectedDaycarePreferredStartDate:
        form.serviceNeed.connectedDaycarePreferredStartDate,
      serviceNeed:
        type === 'DAYCARE' ||
        (type === 'PRESCHOOL' && form.serviceNeed.connectedDaycare)
          ? {
              startTime: form.serviceNeed.startTime,
              endTime: form.serviceNeed.endTime,
              shiftCare: form.serviceNeed.shiftCare,
              partTime: type === 'DAYCARE' && form.serviceNeed.partTime,
              serviceNeedOption: form.serviceNeed.serviceNeedOption
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
    maxFeeAccepted: false,
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
