// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosProgressEvent } from 'axios'

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import type { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import type {
  ApplicationId,
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import type { UploadHandler } from 'lib-components/molecules/FileUpload'

import {
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
  ) => Promise<AttachmentId>,
  deleteAttachmentResult: (arg: {
    attachmentId: AttachmentId
  }) => Promise<Result<void>>
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

export function incomeStatementAttachment(
  incomeStatementId: IncomeStatementId | undefined,
  attachmentType: IncomeStatementAttachmentType | null,
  deleteAttachmentResult: (arg: {
    attachmentId: AttachmentId
  }) => Promise<Result<void>>
): UploadHandler {
  return uploadHandler(
    (file, onUploadProgress) =>
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
          ),
    deleteAttachmentResult
  )
}

export function messageAttachment(
  deleteAttachment: (arg: {
    attachmentId: AttachmentId
  }) => Promise<Result<void>>
): UploadHandler {
  return uploadHandler(
    (file, onUploadProgress) =>
      uploadMessageAttachmentCitizen({ file }, { onUploadProgress }),
    deleteAttachment
  )
}

export function applicationAttachment(
  applicationId: ApplicationId,
  attachmentType: ApplicationAttachmentType,
  deleteAttachmentResult: (arg: {
    attachmentId: AttachmentId
  }) => Promise<Result<void>>
): UploadHandler {
  return uploadHandler(
    (file, onUploadProgress) =>
      uploadApplicationAttachmentCitizen(
        { applicationId, type: attachmentType, file },
        { onUploadProgress }
      ),
    deleteAttachmentResult
  )
}

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  return getAttachment({ attachmentId, requestedFilename }).url.toString()
}
