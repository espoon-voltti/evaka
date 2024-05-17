// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { wrapResult } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { UUID } from 'lib-common/types'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import { H3, H4, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getAttachmentUrl, saveIncomeStatementAttachment } from '../attachments'
import { deleteAttachment } from '../generated/api-clients/attachment'
import { useTranslation } from '../localization'

import { AttachmentType } from './types/common'

const deleteAttachmentResult = wrapResult(deleteAttachment)

export default React.memo(function Attachments({
  incomeStatementId,
  requiredAttachments,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: UUID | undefined
  requiredAttachments: Set<AttachmentType>
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: UUID) => void
}) {
  const t = useTranslation()

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
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn spacing="zero">
        <H3 noMargin>{t.income.attachments.title}</H3>
        <Gap size="s" />
        <P noMargin>{t.income.attachments.description}</P>
        <Gap size="L" />
        {requiredAttachments.size > 0 && (
          <>
            <H4 noMargin>{t.income.attachments.required.title}</H4>
            <Gap size="s" />
            <UnorderedList data-qa="required-attachments">
              {[...requiredAttachments].map((attachmentType) => (
                <li key={attachmentType}>
                  {t.income.attachments.attachmentNames[attachmentType]}
                </li>
              ))}
            </UnorderedList>
            <Gap size="L" />
          </>
        )}
        <FileUpload
          files={attachments}
          onUpload={handleUpload}
          onDelete={handleDelete}
          getDownloadUrl={getAttachmentUrl}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})
