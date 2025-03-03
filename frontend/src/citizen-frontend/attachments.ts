// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AxiosProgressEvent } from 'axios'

import { Failure, Success, wrapResult } from 'lib-common/api'
import { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import {
  ApplicationId,
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { UploadHandler } from 'lib-components/molecules/FileUpload'

import {
  deleteAttachment,
  getAttachment,
  uploadApplicationAttachmentCitizen,
  uploadIncomeStatementAttachmentCitizen,
  uploadMessageAttachmentCitizen,
  uploadOrphanIncomeStatementAttachmentCitizen
} from './generated/api-clients/attachment'

function uploadHandler(
  upload: (
    file: File,
    onUploadProgress: (event: AxiosProgressEvent) => void
  ) => Promise<AttachmentId>
): UploadHandler {
  return {
    upload: async (file, onUploadProgress) => {
      try {
        const data = await upload(file, ({ loaded, total }) =>
          onUploadProgress(
            total !== undefined && total !== 0
              ? Math.round((loaded * 100) / total)
              : 0
          )
        )
        return Success.of(data)
      } catch (e) {
        return Failure.fromError(e)
      }
    },
    delete: deleteAttachmentResult
  }
}

const deleteAttachmentResult = wrapResult(deleteAttachment)

export function incomeStatementAttachment(
  incomeStatementId: IncomeStatementId | undefined,
  attachmentType: IncomeStatementAttachmentType | null
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    incomeStatementId
      ? uploadIncomeStatementAttachmentCitizen(
          {
            incomeStatementId,
            attachmentType,
            file
          },
          { onUploadProgress }
        )
      : uploadOrphanIncomeStatementAttachmentCitizen(
          { attachmentType, file },
          { onUploadProgress }
        )
  )
}

export const messageAttachment = uploadHandler((file, onUploadProgress) =>
  uploadMessageAttachmentCitizen({ file }, { onUploadProgress })
)

export function applicationAttachment(
  applicationId: ApplicationId,
  attachmentType: ApplicationAttachmentType
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadApplicationAttachmentCitizen(
      { applicationId, type: attachmentType, file },
      { onUploadProgress }
    )
  )
}

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  return getAttachment({ attachmentId, requestedFilename }).url.toString()
}
