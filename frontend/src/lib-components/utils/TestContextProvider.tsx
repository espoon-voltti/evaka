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
    remove: '',
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
      multiline: ''
    },
    noSelection: '',
    documentStates: {
      DRAFT: '',
      PREPARED: '',
      COMPLETED: ''
    }
  },
  loginErrorModal: {
    header: '',
    message: '',
    returnMessage: ''
  },
  notifications: {
    close: ''
  },
  offlineNotification: '',
  reloadNotification: {
    title: '',
    buttonText: ''
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
