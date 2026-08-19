// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
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

import { applicationDetailsQuery, applicationUnitsQuery } from './queries'

export function useApplicationEditorDeps(): {
  deps: ApplicationEditorDeps
  infoDialog: React.ReactNode
} {
  const { i18n, lang } = useTranslation()
  const queryClient = useQueryClient()
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
      applicationAttachmentUploadHandler: (
        applicationId,
        type,
        deleteAttachment
      ) => {
        const handler = applicationAttachment(
          applicationId,
          type,
          deleteAttachment
        )
        // The upload endpoint is not a query framework mutation and
        // deleteAttachmentMutation invalidates nothing, so without this the
        // application query keeps serving the attachment list from before the
        // upload -- most visibly in the read view after editing is cancelled.
        const invalidate = () =>
          void queryClient.invalidateQueries({
            queryKey: applicationDetailsQuery({ applicationId }).queryKey
          })
        return {
          upload: async (file, onUploadProgress) => {
            const result = await handler.upload(file, onUploadProgress)
            if (result.isSuccess) invalidate()
            return result
          },
          delete: async (arg) => {
            const result = await handler.delete(arg)
            if (result.isSuccess) invalidate()
            return result
          }
        }
      },
      getAttachmentUrl,
      emailVerificationStatusQuery: null,
      infoDialog: {
        show: setInfoDialogMessage,
        close: () => setInfoDialogMessage(null)
      }
    }),
    [i18n, lang, queryClient]
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
