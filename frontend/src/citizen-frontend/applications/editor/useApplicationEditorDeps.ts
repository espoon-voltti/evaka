// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useContext, useMemo } from 'react'

import type { ApplicationEditorDeps } from 'lib-components/application-editor/types'
import { featureFlags, getMaxPreferredUnits } from 'lib-customizations/citizen'
import { placementTypes } from 'lib-customizations/employee'

import { renderResult } from '../../async-rendering'
import {
  applicationAttachment,
  getAttachmentUrl
} from '../../attachments/attachments'
import { deleteAttachmentMutation } from '../../attachments/queries'
import { useLang, useTranslation } from '../../localization'
import { OverlayContext } from '../../overlay/state'
import { emailVerificationStatusQuery } from '../../personal-details/queries'
import {
  applicationUnitsQuery,
  serviceNeedOptionPublicInfosQuery
} from '../queries'

export function useApplicationEditorDeps(): ApplicationEditorDeps {
  const t = useTranslation()
  const [lang] = useLang()
  const { setInfoMessage, clearInfoMessage } = useContext(OverlayContext)

  return useMemo(
    () => ({
      actor: 'citizen',
      lang,
      translations: t,
      employeeTexts: null,
      featureFlags,
      getMaxPreferredUnits,
      placementTypes,
      renderResult,
      applicationUnitsQuery,
      serviceNeedOptionPublicInfosQuery,
      deleteAttachmentMutation,
      applicationAttachmentUploadHandler: applicationAttachment,
      getAttachmentUrl,
      emailVerificationStatusQuery,
      infoDialog: { show: setInfoMessage, close: clearInfoMessage }
    }),
    [lang, t, setInfoMessage, clearInfoMessage]
  )
}
