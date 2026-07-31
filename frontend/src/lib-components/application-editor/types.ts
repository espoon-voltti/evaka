// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconProp } from '@fortawesome/fontawesome-svg-core'
import type { ReactElement } from 'react'

import type { Result } from 'lib-common/api'
import type { ApplicationFormData } from 'lib-common/application/ApplicationFormData'
import type {
  ApplicationFormDataErrors,
  Term
} from 'lib-common/application/validations'
import type { FeatureFlags } from 'lib-common/feature-flags'
import type {
  ApplicationAttachmentType,
  ApplicationDetails,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import type {
  ApplicationUnitType,
  Language,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type {
  ApplicationId,
  AttachmentId
} from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import type { MutationDescription, QueriesQuery } from 'lib-common/query'
import type { RenderResultFn } from 'lib-components/async-rendering'
import type { UploadHandler } from 'lib-components/molecules/FileUpload'

import type { ApplicationEditorTranslations } from './translations'

export interface ApplicationEditorDeps {
  lang: Language
  translations: ApplicationEditorTranslations
  featureFlags: FeatureFlags
  getMaxPreferredUnits: (type: ApplicationType) => number
  placementTypes: readonly PlacementType[]
  renderResult: <T>(
    result: Result<T>,
    renderer: RenderResultFn<T>
  ) => ReactElement
  applicationUnitsQuery: QueriesQuery<
    [
      {
        type: ApplicationUnitType
        date: LocalDate
        shiftCare?: boolean | null
      }
    ],
    PublicUnit[]
  >
  serviceNeedOptionPublicInfosQuery: QueriesQuery<
    [{ placementTypes?: PlacementType[] | null }],
    ServiceNeedOptionPublicInfo[]
  >
  deleteAttachmentMutation: MutationDescription<
    { attachmentId: AttachmentId },
    void
  >
  applicationAttachmentUploadHandler: (
    applicationId: ApplicationId,
    type: ApplicationAttachmentType,
    deleteAttachmentResult: (arg: {
      attachmentId: AttachmentId
    }) => Promise<Result<void>>
  ) => UploadHandler
  getAttachmentUrl: (
    attachmentId: AttachmentId,
    requestedFilename: string
  ) => string
  emailVerificationStatusQuery: QueriesQuery<
    [],
    EmailVerificationStatusResponse
  > | null
  infoDialog: {
    show: (message: {
      title: string
      text: string
      type: 'warning'
      icon: IconProp
      resolve: { action: () => void; label: string }
    }) => void
    close: () => void
  }
}

export type ApplicationFormProps = {
  application: ApplicationDetails
  formData: ApplicationFormData
  setFormData: (
    update: (old: ApplicationFormData) => ApplicationFormData
  ) => void
  errors: ApplicationFormDataErrors
  verificationRequested: boolean
  alertTrigger: number
  isInvalidDate: ((localDate: LocalDate) => string | null) | undefined
  minDate: LocalDate
  maxDate: LocalDate
  terms?: Term[]
  deps: ApplicationEditorDeps
}
