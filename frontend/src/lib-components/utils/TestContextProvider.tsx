// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ThemeProvider } from 'styled-components'

import espooTheme from 'lib-common/themes/espoo-theme'

import { ComponentLocalizationContextProvider, Translations } from '../i18n'

export const testTranslations: Translations = {
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
    yes: ''
  },
  datePicker: {
    placeholder: '',
    description: '',
    validationErrors: {
      validDate: '',
      dateTooEarly: '',
      dateTooLate: ''
    }
  },
  documentTemplates: {
    templateQuestions: {
      label: '',
      infoText: '',
      withText: '',
      options: '',
      multiline: '',
      text: ''
    },
    noSelection: '',
    documentStates: {
      DRAFT: '',
      PREPARED: '',
      COMPLETED: ''
    }
  },
  fileUpload: {
    deleteFile: '',
    input: {
      title: '',
      text: []
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
  messages: { staffAnnotation: '' },
  messageEditor: {
    newMessage: '',
    to: {
      label: '',
      placeholder: '',
      noOptions: ''
    },
    type: {
      label: '',
      message: '',
      bulletin: ''
    },
    urgent: {
      heading: '',
      info: '',
      label: ''
    },
    sender: '',
    recipients: '',
    recipientsPlaceholder: '',
    title: '',
    message: '',
    deleteDraft: '',
    send: '',
    sending: ''
  },
  messageReplyEditor: {
    discard: ''
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
    timeFormat: '',
    timeRequired: '',
    unitNotSelected: '',
    emailsDoNotMatch: '',
    httpUrl: '',
    unselectableDate: '',
    openAttendance: ''
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
