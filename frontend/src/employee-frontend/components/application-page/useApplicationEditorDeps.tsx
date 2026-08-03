// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconProp } from '@fortawesome/fontawesome-svg-core'
import React, { useMemo, useState } from 'react'

import type { ApplicationEditorDeps } from 'lib-components/application-editor/types'
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

interface InfoDialogMessage {
  title: string
  text: string
  type: 'warning'
  icon: IconProp
  resolve: { action: () => void; label: string }
}

export function useApplicationEditorDeps(): {
  deps: ApplicationEditorDeps
  infoDialog: React.ReactNode
} {
  const { lang } = useTranslation()
  const [infoDialogMessage, setInfoDialogMessage] =
    useState<InfoDialogMessage | null>(null)

  const deps = useMemo<ApplicationEditorDeps>(
    () => ({
      actor: 'employee',
      lang,
      translations: citizenTranslations[lang],
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
    [lang]
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
