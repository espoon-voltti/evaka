// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { wrapResult } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { UUID } from 'lib-common/types'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'

import { getAttachmentUrl, saveIncomeStatementAttachment } from '../attachments'
import { deleteAttachment } from '../generated/api-clients/attachment'

const deleteAttachmentResult = wrapResult(deleteAttachment)

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
  const handleUpload = useCallback(
    async (file: File, onUploadProgress: (percentage: number) => void) =>
      (
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
      }),
    [incomeStatementId, onUploaded]
  )

  const handleDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachmentResult({ attachmentId: id })).map(() => {
        onDeleted(id)
      }),
    [onDeleted]
  )

  return (
    <FixedSpaceColumn spacing="zero">
      <FileUpload
        files={attachments}
        onUpload={handleUpload}
        onDelete={handleDelete}
        getDownloadUrl={getAttachmentUrl}
      />
    </FixedSpaceColumn>
  )
})
