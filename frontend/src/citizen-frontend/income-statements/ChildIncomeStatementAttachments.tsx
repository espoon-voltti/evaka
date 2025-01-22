// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'

import { getAttachmentUrl, incomeStatementAttachment } from '../attachments'

export default React.memo(function Attachments({
  incomeStatementId,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: IncomeStatementId | undefined
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: AttachmentId) => void
}) {
  return (
    <FixedSpaceColumn spacing="zero">
      <FileUpload
        files={attachments}
        uploadHandler={incomeStatementAttachment(incomeStatementId)}
        onUploaded={onUploaded}
        onDeleted={onDeleted}
        getDownloadUrl={getAttachmentUrl}
      />
    </FixedSpaceColumn>
  )
})
