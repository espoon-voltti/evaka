// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import { DocumentStatus } from 'lib-common/generated/api-types/document'

import { FileType } from './molecules/FileUpload'

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
    open: string
    remove: string
    saving: string
    saved: string
    search: string
    yes: string
    openExpandingInfo: string
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
      text: string
    }
    noSelection: string
    documentStates: Record<DocumentStatus, string>
  }
  fileUpload: {
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
    inactiveUserMessage: string
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
  }
  messageEditor: {
    newMessage: string
    to: {
      label: string
      placeholder: string
      noOptions: string
    }
    type: {
      label: string
      message: string
      bulletin: string
    }
    flags: {
      heading: string
      urgent: {
        info: string
        label: string
      }
      sensitive: {
        info: string
        label: string
        whyDisabled: string
      }
    }
    sender: string
    recipientsPlaceholder: string
    title: string
    deleteDraft: string
  }
  messageReplyEditor: {
    messagePlaceholder: string | undefined
    discard: string
    messagePlaceholderSensitiveThread: string | undefined
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
