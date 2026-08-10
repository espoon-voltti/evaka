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
  actions: {
    verify: string
    hasVerified: string
    allowOtherGuardianAccess: ReactNode
    returnToEdit: string
    returnToEditBtn: string
    cancel: string
    send: string
    update: string
    sendError: string
    saveDraft: string
    updateError: string
  }
  verification: {
    title: {
      DAYCARE: string
      PRESCHOOL: string
      CLUB: string
    }
    notYetSent: ReactNode
    notYetSaved: ReactNode
    no: string
    basics: {
      created: string
      modified: string
    }
    attachmentBox: {
      nb: string
      headline: string
      urgency: string
      shiftCare: string
      goBackLinkText: string
      goBackRestText: string
    }
    serviceNeed: {
      title: string
      wasOnDaycare: string
      wasOnDaycareYes: string
      wasOnClubCare: string
      wasOnClubCareYes: string
      connectedDaycare: {
        label: string
        withConnectedDaycare: string
        withoutConnectedDaycare: string
        startDate: string
        serviceNeed: string
      }
      attachments: {
        label: string
        withoutAttachments: string
      }
      startDate: {
        title: {
          DAYCARE: string
          PRESCHOOL: string
          CLUB: string
        }
        preferredStartDate: string
        urgency: string
        withUrgency: string
        withoutUrgency: string
      }
      dailyTime: {
        title: string
        partTime: string
        withPartTime: string
        withoutPartTime: string
        dailyTime: string
        shiftCare: string
        withShiftCare: string
        withoutShiftCare: string
      }
      assistanceNeed: {
        title: string
        assistanceNeed: string
        withAssistanceNeed: string
        withoutAssistanceNeed: string
        description: string
      }
      preparatoryEducation: {
        label: string
        withPreparatory: string
        withoutPreparatory: string
      }
    }
    unitPreference: {
      title: string
      siblingBasis: {
        title: string
        siblingBasisLabel: string
        siblingBasisYes: string
        name: string
        ssn: string
        unit: string
      }
      units: {
        title: string
        label: string
      }
    }
    contactInfo: {
      title: string
      child: {
        title: string
        name: string
        ssn: string
        streetAddress: string
        isAddressChanging: string
        hasFutureAddress: string
        addressChangesAt: string
        newAddress: string
      }
      guardian: {
        title: string
        name: string
        ssn: string
        streetAddress: string
        tel: string
        email: string
        isAddressChanging: string
        hasFutureAddress: string
        addressChangesAt: string
        newAddress: string
      }
      secondGuardian: {
        title: string
        email: string
        tel: string
        info: string
        agreed: string
        notAgreed: string
        rightToGetNotified: string
        noAgreementStatus: string
      }
      fridgePartner: {
        title: string
        fridgePartner: string
        name: string
        ssn: string
      }
      fridgeChildren: {
        title: string
        name: string
        ssn: string
        noOtherChildren: string
      }
    }
    additionalDetails: {
      title: string
      otherInfoLabel: string
      dietLabel: string
      allergiesLabel: string
    }
    otherGuardianAgreement: {
      title: string
      text: string
    }
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
    childInformationLink: string
    childDateOfBirth: string
    nationality: string
    language: string
    homeAddress: string
    moveDate: string
    street: string
    postalCode: string
    postOffice: string
    addressRestricted: string
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
    secondGuardianExists: string
    secondGuardianInfoTitle: string
    secondGuardianInfo: string
    secondGuardianNotFound: string
    secondGuardianInfoPreschoolSeparated: string
    secondGuardianAgreementStatus: {
      label: string
      AGREED: string
      NOT_AGREED: string
      RIGHT_TO_GET_NOTIFIED: string
      NOT_SET: string
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
  draftPolicyInfo: {
    title: string
    text: string
    ok: string
  }
  sentInfo: {
    title: string
    text: string
    ok: string
  }
  updateInfo: {
    title: string
    text: string
    ok: string
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
