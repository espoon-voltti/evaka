// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import { DocumentStatus } from 'lib-common/generated/api-types/document'

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
    remove: string
    yes: string
  }
  datePicker: {
    placeholder: string
    description: string
    validationErrors: {
      validDate: string
      dateTooEarly: string
      dateTooLate: string
    }
  }
  documentTemplates: {
    templateQuestions: {
      label: string
      infoText: string
      withText: string
      options: string
      multiline: string
    }
    noSelection: string
    documentStates: Record<DocumentStatus, string>
  }
  loginErrorModal: {
    header: string
    message: string
    returnMessage: string
  }
  notifications: {
    close: string
  }
  offlineNotification: string
  reloadNotification: {
    title: string
    buttonText: string
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
    timeFormat: string
    timeRequired: string
    unitNotSelected: string
    emailsDoNotMatch: string
    httpUrl: string
    unselectableDate: string
    openAttendance: string
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
