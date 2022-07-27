// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import type { Attachment } from 'lib-common/api-types/attachment'
import type { UUID } from 'lib-common/types'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'

import {
  deleteAttachment,
  getAttachmentUrl,
  saveIncomeStatementAttachment
} from '../attachments'
import { useTranslation } from '../localization'

export default React.memo(function Attachments({
  incomeStatementId,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: UUID | undefined
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: UUID) => void
}) {
  const t = useTranslation()

  const handleUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => {
      return (
        await saveIncomeStatementAttachment(
          incomeStatementId,
          file,
          onUploadProgress
        )
      ).map((id) => {
        onUploaded({
          id,
          name: file.name,
          contentType: file.type
        })
        return id
      })
    },
    [incomeStatementId, onUploaded]
  )

  const handleDelete = useCallback(
    async (id: UUID) => {
      return (await deleteAttachment(id)).map(() => {
        onDeleted(id)
      })
    },
    [onDeleted]
  )

  return (
    <FixedSpaceColumn spacing="zero">
      <FileUpload
        files={attachments}
        onUpload={handleUpload}
        onDelete={handleDelete}
        getDownloadUrl={getAttachmentUrl}
        i18n={{ upload: t.fileUpload }}
      />
    </FixedSpaceColumn>
  )
})
