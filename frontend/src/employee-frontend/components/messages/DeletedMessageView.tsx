// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type {
  MessageAccountId,
  MessageContentId
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Linkify from 'lib-components/atoms/Linkify'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { defaultMargins } from 'lib-components/white-space'

import { getAttachmentUrl } from '../../api/attachments'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { deletedMessageContentQuery } from './queries'

const OriginalBody = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
  word-break: break-word;
`

interface Props {
  accountId: MessageAccountId
  contentId: MessageContentId
  isJustDeleted: boolean
  revealed: boolean
  onSetRevealed: (revealed: boolean) => void
}

export const DeletedMessageView = React.memo(function DeletedMessageView({
  accountId,
  contentId,
  isJustDeleted,
  revealed,
  onSetRevealed
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.messages.deletion.afterDeletion

  const contentResult = useQueryResult(
    deletedMessageContentQuery({ accountId, contentId }),
    { enabled: revealed }
  )

  if (revealed) {
    return renderResult(contentResult, (original) => (
      <FixedSpaceColumn $spacing="s" data-qa="deleted-message-original">
        <OriginalBody>
          <Linkify text={original.content} />
        </OriginalBody>
        {original.attachments.length > 0 && (
          <FixedSpaceColumn $spacing="xs">
            {original.attachments.map((attachment) => (
              <FileDownloadButton
                key={attachment.id}
                file={attachment}
                getFileUrl={getAttachmentUrl}
                icon
                data-qa="deleted-message-attachment"
              />
            ))}
          </FixedSpaceColumn>
        )}
        <FixedSpaceRow>
          <Button
            text={t.hideButton}
            onClick={() => onSetRevealed(false)}
            data-qa="hide-deleted-message-btn"
          />
        </FixedSpaceRow>
      </FixedSpaceColumn>
    ))
  }

  const prompt = (
    <FixedSpaceColumn $spacing="s">
      <span data-qa="deleted-message-warning">{t.viewLogWarning}</span>
      <FixedSpaceRow>
        <Button
          text={t.viewButton}
          onClick={() => onSetRevealed(true)}
          data-qa="view-deleted-message-btn"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )

  return isJustDeleted ? (
    prompt
  ) : (
    <AlertBox noMargin message={prompt} data-qa="deleted-message-alert" />
  )
})
