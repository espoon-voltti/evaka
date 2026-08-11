// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'

import type { ErrorKey } from 'lib-common/form-validation'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type { ProviderType } from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'

export interface ApplicationEditorTexts {
  heading: {
    title: {
      DAYCARE: string
      PRESCHOOL: string
      CLUB: string
    }
    info: {
      DAYCARE: ReactNode
      PRESCHOOL: ReactNode
      CLUB: ReactNode
    }
    errors: (count: number) => string
    hasErrors: string
    invalidFields: (count: number) => string
  }
  serviceNeed: {
    title: string
    startDate: {
      header: {
        DAYCARE: string
        PRESCHOOL: string
        CLUB: string
      }
      missing: string
      info: {
        DAYCARE: ReactNode[]
        PRESCHOOL: ReactNode[]
        CLUB: ReactNode[]
      }
      clubTerm: string
      clubTerms: string
      preschoolTerm: string
      preschoolTerms: string
      label: {
        DAYCARE: string
        PRESCHOOL: string
        CLUB: string
      }
      noteOnDelay: string
      instructions: {
        DAYCARE: ReactNode
        PRESCHOOL: ReactNode
        CLUB: ReactNode
      }
      placeholder: string
      validationText: string
    }
    clubDetails: {
      wasOnDaycare: string
      wasOnDaycareInfo: string
      wasOnClubCare: string
      wasOnClubCareInfo: string
    }
    urgent: {
      label: string
      attachmentsMessage: {
        text: ReactNode
        subtitle: string
      }
    }
    partTime: {
      label: string
      true: string
      false: string
    }
    dailyTime: {
      label: {
        DAYCARE: string
        PRESCHOOL: string
      }
      connectedDaycareInfo: ReactNode
      connectedDaycare: string
      instructions: {
        DAYCARE: string
        PRESCHOOL: string
      }
      usualArrivalAndDeparture: {
        DAYCARE: string
        PRESCHOOL: string
      }
      starts: string
      ends: string
    }
    shiftCare: {
      label: string
      instructions: string
      attachmentsMessage: {
        DAYCARE: ReactNode
        PRESCHOOL: ReactNode
      }
      attachmentsSubtitle: string
    }
    assistanceNeed: string
    assistanceNeeded: {
      DAYCARE: string
      PRESCHOOL: string
      CLUB: string
    }
    assistanceNeedLabel: string
    assistanceNeedPlaceholder: string
    assistanceNeedInstructions: {
      DAYCARE: ReactNode
      CLUB: ReactNode
      PRESCHOOL: ReactNode
    }
    assistanceNeedExtraInstructions: {
      DAYCARE: ReactNode
      PRESCHOOL: ReactNode
      CLUB: ReactNode
    }
    preparatory: string
    preparatoryInfo: ReactNode
    preparatoryExtraInstructions: ReactNode
  }
  unitPreference: {
    title: string
    siblingBasis: {
      title: string
      info: {
        DAYCARE: ReactNode
        PRESCHOOL: ReactNode
        CLUB: ReactNode
      }
      checkbox: {
        DAYCARE: string
        PRESCHOOL: string
        CLUB: string
      }
      radioLabel: {
        DAYCARE: string
        PRESCHOOL: string
        CLUB: string
      }
      otherSibling: string
      names: string
      namesPlaceholder: string
      ssn: string
      ssnPlaceholder: string
      unit: string
      unitPlaceholder: string
    }
    units: {
      title: (maxUnits: number) => string
      startDateMissing: string
      info: {
        DAYCARE: ReactNode
        PRESCHOOL: ReactNode
        CLUB: ReactNode
      }
      mapLink: string
      serviceVoucherLink: string
      languageFilter: {
        label: string
        fi: string
        sv: string
        en: string
      }
      select: {
        label: (maxUnits: number) => string
        placeholder: string
        maxSelected: string
        noOptions: string
      }
      preferences: {
        label: (maxUnits: number) => string
        noSelections: string
        infoByApplicationType: Partial<
          Record<ApplicationType, (maxUnits: number) => string>
        >
        info: (maxUnits: number) => string
        fi: string
        sv: string
        en: string
        moveUp: string
        moveDown: string
        remove: string
        preferenceNameAndNumber: (n: number, unitName: string) => string
      }
    }
    movePreferredUnitScreenReaderMessage: (
      unitName: string,
      position: number
    ) => string
    removePreferredUnitScreenReaderMessage: (unitName: string) => string
  }
  additionalDetails: {
    title: string
    otherInfoLabel: string
    otherInfoPlaceholder: string
    dietLabel: string
    dietPlaceholder: string
    dietInfo: ReactNode
    allergiesLabel: string
    allergiesPlaceholder: string
  }
  contactInfo: {
    title: string
    familyInfo: ReactNode
    info: ReactNode
    emailInfoText: string
    childInfoTitle: string
    childFirstName: string
    childLastName: string
    childSSN: string
    homeAddress: string
    moveDate: string
    street: string
    postalCode: string
    postOffice: string
    guardianInfoTitle: string
    guardianFirstName: string
    guardianLastName: string
    guardianSSN: string
    phone: string
    verifyEmail: string
    email: string
    emailChangeTip: string
    emailChangeTipLink: string
    noEmail: string
    secondGuardianInfoTitle: string
    secondGuardianInfo: string
    secondGuardianNotFound: string
    secondGuardianInfoPreschoolSeparated: string
    secondGuardianAgreementStatus: {
      label: string
      AGREED: string
      NOT_AGREED: string
      RIGHT_TO_GET_NOTIFIED: string
    }
    secondGuardianPhone: string
    secondGuardianEmail: string
    otherPartnerTitle: string
    otherPartnerCheckboxLabel: string
    personFirstName: string
    personLastName: string
    personSSN: string
    otherChildrenTitle: string
    otherChildrenInfo: string
    otherChildrenChoiceInfo: string
    hasFutureAddress: string
    futureAddressInfo: string
    guardianFutureAddressEqualsChildFutureAddress: string
    firstNamePlaceholder: string
    lastNamePlaceholder: string
    ssnPlaceholder: string
    streetPlaceholder: string
    postalCodePlaceholder: string
    municipalityPlaceholder: string
    addChild: string
    remove: string
    removed: string
    areExtraChildren: string
    choosePlaceholder: string
  }
  unitChangeWarning: {
    title: string
    text: string
    ok: string
  }
}

export interface ApplicationEditorTranslations {
  applications: {
    editor: ApplicationEditorTexts
    creation: { daycare4monthWarning: ReactNode }
  }
  applicationsList: { transferApplication: string }
  common: { unit: { providerTypes: Record<ProviderType, string> } }
  placement: { type: Record<PlacementType, string> }
  validationErrors: Record<ErrorKey, string>
}

/**
 * Labels for the parts of the editor only a service worker sees: the read-only
 * VTJ data about the child, and the manually entered second guardian.
 *
 * These deliberately live outside {@link ApplicationEditorTranslations}, which
 * is satisfied by the citizen translation bundle. Municipalities customize
 * citizen and employee wording separately, so employee-only labels are supplied
 * from the employee bundle instead of being added to the citizen one.
 */
export interface EmployeeApplicationEditorTexts {
  childInformationLink: string
  childDateOfBirth: string
  nationality: string
  language: string
  addressRestricted: string
  secondGuardianExists: string
  secondGuardianAgreementStatusNotSet: string
}
