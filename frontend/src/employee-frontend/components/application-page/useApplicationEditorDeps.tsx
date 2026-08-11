// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'

import type {
  ApplicationEditorDeps,
  InfoDialogMessage
} from 'lib-components/application-editor/types'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import {
  getMaxPreferredUnits,
  translations as citizenTranslations
} from 'lib-customizations/citizen'
import { featureFlags, placementTypes } from 'lib-customizations/employee'

import { applicationAttachment, getAttachmentUrl } from '../../api/attachments'
import { deleteAttachmentMutation } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { serviceNeedPublicInfosQuery } from '../applications/queries'
import { renderResult } from '../async-rendering'

import { applicationUnitsQuery } from './queries'

export function useApplicationEditorDeps(): {
  deps: ApplicationEditorDeps
  infoDialog: React.ReactNode
} {
  const { i18n, lang } = useTranslation()
  const [infoDialogMessage, setInfoDialogMessage] =
    useState<InfoDialogMessage | null>(null)

  const deps = useMemo<ApplicationEditorDeps>(
    () => ({
      actor: 'employee',
      lang,
      // The service worker sees the application substantially as the guardian
      // does, so the shared form text — including municipality customizations —
      // comes from the citizen bundle. Only employee-only labels are overridden.
      translations: citizenTranslations[lang],
      employeeTexts: {
        childInformationLink: i18n.application.child.title,
        childDateOfBirth: i18n.application.person.dob,
        nationality: i18n.application.person.nationality,
        language: i18n.application.person.language,
        addressRestricted: i18n.application.person.restricted,
        secondGuardianExists:
          i18n.application.guardians.secondGuardian.checkboxLabel,
        secondGuardianAgreementStatusNotSet:
          i18n.application.guardians.secondGuardian.agreementStatusNotSet
      },
      featureFlags,
      getMaxPreferredUnits,
      placementTypes,
      renderResult,
      applicationUnitsQuery,
      serviceNeedOptionPublicInfosQuery: serviceNeedPublicInfosQuery,
      deleteAttachmentMutation,
      applicationAttachmentUploadHandler: applicationAttachment,
      getAttachmentUrl,
      emailVerificationStatusQuery: null,
      infoDialog: {
        show: setInfoDialogMessage,
        close: () => setInfoDialogMessage(null)
      }
    }),
    [i18n, lang]
  )

  const infoDialog = infoDialogMessage ? (
    <InfoModal
      type={infoDialogMessage.type}
      title={infoDialogMessage.title}
      text={infoDialogMessage.text}
      icon={infoDialogMessage.icon}
      resolve={infoDialogMessage.resolve}
    />
  ) : null

  return { deps, infoDialog }
}
