// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ThemeProvider } from 'styled-components'

import espooTheme from 'lib-common/themes/espoo-theme'

import type { Translations } from '../i18n'
import { ComponentLocalizationContextProvider } from '../i18n'

export const testTranslations: Translations = {
  sessionTimeout: {
    cancel: '',
    goToLoginPage: '',
    sessionExpiredMessage: '',
    sessionExpiredTitle: ''
  },
  asyncButton: {
    inProgress: '',
    failure: '',
    success: ''
  },
  common: {
    add: '',
    cancel: '',
    close: '',
    confirm: '',
    no: '',
    noResults: '',
    open: '',
    remove: '',
    saved: '',
    saving: '',
    search: '',
    yes: '',
    openExpandingInfo: ''
  },
  datePicker: {
    placeholder: '',
    description: '',
    validationErrors: {
      validDate: '',
      dateTooEarly: '',
      dateTooLate: ''
    },
    open: '',
    close: ''
  },
  documentTemplates: {
    templateQuestions: {
      label: '',
      infoText: '',
      withText: '',
      options: '',
      multiline: '',
      text: '',
      textFields: '',
      addTextField: '',
      allowMultipleRows: '',
      addRow: ''
    },
    noSelection: '',
    documentStates: {
      DRAFT: '',
      PREPARED: '',
      CITIZEN_DRAFT: '',
      DECISION_PROPOSAL: '',
      COMPLETED: '',
      ACCEPTED: '',
      REJECTED: '',
      ANNULLED: ''
    }
  },
  fileUpload: {
    uploadFile: '',
    deleteFile: '',
    input: {
      title: '',
      text: () => ''
    },
    loading: '',
    loaded: '',
    error: {
      FILE_TOO_LARGE: '',
      EXTENSION_INVALID: '',
      EXTENSION_MISSING: '',
      INVALID_CONTENT_TYPE: '',
      SERVER_ERROR: ''
    }
  },
  loginErrorModal: {
    header: '',
    message: '',
    returnMessage: ''
  },
  messages: {
    staffAnnotation: '',
    recipients: '',
    message: '',
    send: '',
    sending: '',
    types: {
      MESSAGE: '',
      BULLETIN: ''
    },
    thread: {
      type: '',
      urgent: ''
    },
    outOfOffice: {
      singleRecipient: () => '',
      multipleRecipientsHeader: ''
    }
  },
  messageReplyEditor: {
    discard: '',
    messagePlaceholder: undefined,
    messagePlaceholderSensitiveThread: undefined
  },
  notifications: {
    close: ''
  },
  offlineNotification: '',
  reloadNotification: {
    title: '',
    buttonText: ''
  },
  treeDropdown: {
    expand: () => '',
    collapse: () => '',
    expandDropdown: ''
  },
  validationErrors: {
    required: '',
    requiredSelection: '',
    format: '',
    integerFormat: '',
    ssn: '',
    phone: '',
    email: '',
    preferredStartDate: '',
    connectedPreferredStartDate: '',
    timeFormat: '',
    timeRequired: '',
    dateTooEarly: '',
    dateTooLate: '',
    unitNotSelected: '',
    emailsDoNotMatch: '',
    httpUrl: '',
    unselectableDate: '',
    openAttendance: '',
    generic: ''
  }
}

export function TestContextProvider({
  translations = testTranslations,
  children
}: {
  translations?: Translations
  children: React.ReactNode
}) {
  return (
    <ThemeProvider theme={espooTheme}>
      <ComponentLocalizationContextProvider
        useTranslations={() => translations}
      >
        {children}
      </ComponentLocalizationContextProvider>
    </ThemeProvider>
  )
}
