// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { IncomeStatementAttachments } from 'lib-common/income-statements'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'

import { getAttachmentUrl, incomeStatementAttachment } from '../attachments'

import { SetStateCallback } from './IncomeStatementComponents'

export default React.memo(function Attachments({
  incomeStatementId,
  attachments,
  onChange
}: {
  incomeStatementId: IncomeStatementId | undefined
  attachments: IncomeStatementAttachments
  onChange: SetStateCallback<IncomeStatementAttachments>
}) {
  const onUploaded = useCallback(
    (attachment: Attachment) =>
      onChange((prev) =>
        prev.typed
          ? prev
          : { ...prev, attachments: [...prev.untypedAttachments, attachment] }
      ),
    [onChange]
  )

  const onDeleted = useCallback(
    (id: AttachmentId) =>
      onChange((prev) =>
        prev.typed
          ? prev
          : {
              ...prev,
              attachments: prev.untypedAttachments.filter((a) => a.id !== id)
            }
      ),
    [onChange]
  )

  if (attachments.typed) return null

  return (
    <FixedSpaceColumn spacing="zero">
      <FileUpload
        files={attachments.untypedAttachments}
        uploadHandler={incomeStatementAttachment(incomeStatementId, null)}
        onUploaded={onUploaded}
        onDeleted={onDeleted}
        getDownloadUrl={getAttachmentUrl}
      />
    </FixedSpaceColumn>
  )
})
