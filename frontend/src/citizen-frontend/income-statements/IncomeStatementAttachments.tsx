// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Attachment } from 'lib-common/generated/api-types/attachment'
import {
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import { H3, H4, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getAttachmentUrl, incomeStatementAttachment } from '../attachments'
import { useTranslation } from '../localization'

import { AttachmentType } from './types/common'

export default React.memo(function Attachments({
  incomeStatementId,
  requiredAttachments,
  attachments,
  onUploaded,
  onDeleted
}: {
  incomeStatementId: IncomeStatementId | undefined
  requiredAttachments: Set<AttachmentType>
  attachments: Attachment[]
  onUploaded: (attachment: Attachment) => void
  onDeleted: (attachmentId: AttachmentId) => void
}) {
  const t = useTranslation()

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
          uploadHandler={incomeStatementAttachment(incomeStatementId)}
          onUploaded={onUploaded}
          onDeleted={onDeleted}
          getDownloadUrl={getAttachmentUrl}
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})
