// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ChildDocumentDecisionStatus,
  DocumentStatus
} from 'lib-common/generated/api-types/document'

import type { FileType } from './molecules/FileUpload'

export interface Translations {
  asyncButton: {
    inProgress: string
    failure: string
    success: string
  }
  common: {
    add: string
    cancel: string
    close: string
    confirm: string
    no: string
    noResults: string
    noFirstName: string
    noLastName: string
    open: string
    remove: string
    saving: string
    saved: string
    search: string
    yes: string
    openExpandingInfo: string
    showPassword: string
    hidePassword: string
    userTypes: {
      SYSTEM: string
      CITIZEN: string
      EMPLOYEE: string
      MOBILE_DEVICE: string
      UNKNOWN: string
    }
  }
  datePicker: {
    placeholder: string
    description: string
    validationErrors: {
      validDate: string
      dateTooEarly: string
      dateTooLate: string
    }
    open: string
    close: string
  }
  documentTemplates: {
    templateQuestions: {
      label: string
      infoText: string
      withText: string
      options: string
      multiline: string
      text: string
      textFields: string
      addTextField: string
      allowMultipleRows: string
      addRow: string
    }
    noSelection: string
    documentStates: Record<DocumentStatus | ChildDocumentDecisionStatus, string>
  }
  fileUpload: {
    uploadFile: string
    deleteFile: string
    input: {
      title: string
      text: (fileTypes: FileType[]) => string
    }
    loading: string
    loaded: string
    error: {
      FILE_TOO_LARGE: string
      EXTENSION_INVALID: string
      EXTENSION_MISSING: string
      INVALID_CONTENT_TYPE: string
      SERVER_ERROR: string
    }
  }
  loginErrorModal: {
    header: string
    message: string
    returnMessage: string
  }
  messages: {
    staffAnnotation: string
    message: string
    recipients: string
    send: string
    sending: string
    types: {
      MESSAGE: string
      BULLETIN: string
    }
    thread: {
      type: string
      urgent: string
    }
    outOfOffice: {
      singleRecipient: (name: string, period: FiniteDateRange) => string
      multipleRecipientsHeader: string
    }
  }
  messageReplyEditor: {
    messagePlaceholder: string | undefined
    discard: string
    messagePlaceholderSensitiveThread: string | undefined
  }
  sessionTimeout: {
    sessionExpiredTitle: string
    sessionExpiredMessage: string
    goToLoginPage: string
    cancel: string
  }
  notifications: {
    close: string
  }
  offlineNotification: string
  reloadNotification: {
    title: string
    buttonText: string
  }
  treeDropdown: {
    expand: (opt: string) => string
    collapse: (opt: string) => string
    expandDropdown: string
  }
  validationErrors: {
    required: string
    requiredSelection: string
    format: string
    integerFormat: string
    ssn: string
    phone: string
    email: string
    preferredStartDate: string
    connectedPreferredStartDate: string
    timeFormat: string
    timeRequired: string
    dateTooEarly: string
    dateTooLate: string
    unitNotSelected: string
    emailsDoNotMatch: string
    httpUrl: string
    unselectableDate: string
    openAttendance: string
    generic: string
  }
  metadata: {
    title: string
    notFound: string
    processNumber: string
    processName: string
    organization: string
    archiveDurationMonths: string
    primaryDocument: string
    secondaryDocuments: string
    documentId: string
    name: string
    createdAt: string
    createdBy: string
    monthsUnit: string
    confidentiality: string
    confidential: string
    public: string
    notSet: string
    confidentialityDuration: string
    confidentialityBasis: string
    years: string
    receivedBy: {
      label: string
      PAPER: string
      ELECTRONIC: string
    }
    sfiDelivery: {
      label: string
      method: {
        ELECTRONIC: string
        PAPER_MAIL: string
        PENDING: string
      }
    }
    history: string
    downloadPdf: string
    states: {
      INITIAL: string
      PREPARATION: string
      DECIDING: string
      COMPLETED: string
    }
    section: {
      show: string
      hide: string
    }
  }
}

interface ComponentLocalizationState {
  translations: Translations | undefined
}

const ComponentLocalizationContext =
  React.createContext<ComponentLocalizationState>({
    translations: undefined
  })

export const ComponentLocalizationContextProvider = React.memo(
  function ComponentLocalizationContextProvider({
    useTranslations,
    children
  }: {
    useTranslations: () => Translations
    children: React.ReactNode
  }) {
    const translations = useTranslations()
    return (
      <ComponentLocalizationContext.Provider
        value={useMemo(() => ({ translations }), [translations])}
      >
        {children}
      </ComponentLocalizationContext.Provider>
    )
  }
)

export function useTranslations(): Translations {
  const { translations } = useContext(ComponentLocalizationContext)
  if (!translations) {
    throw new Error(
      'ComponentLocalizationContextProvider needs to be added in the component tree!'
    )
  }
  return translations
}
