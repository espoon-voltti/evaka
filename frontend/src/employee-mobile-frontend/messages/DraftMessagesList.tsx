// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { DraftContent } from 'lib-common/generated/api-types/messaging'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MessageTypeChip } from 'lib-components/messages/MessageCharacteristics'
import {
  Container,
  Header,
  TitleAndDate,
  Truncated
} from 'lib-components/messages/ThreadListItem'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'

import { draftMessagesQuery } from './queries'
import { MessageContext } from './state'

interface Props {
  onSelectDraft: (draft: DraftContent) => void
}

export default React.memo(function DraftMessagesList({ onSelectDraft }: Props) {
  const { selectedAccount } = useContext(MessageContext)
  const draftMessages = useQueryResult(
    queryOrDefault(
      draftMessagesQuery,
      []
    )(selectedAccount ? selectedAccount.account.id : undefined)
  )

  return renderResult(draftMessages, (messages) => (
    <div>
      {messages.map((message) => (
        <DraftMessagePreview
          key={message.id}
          message={message}
          onClick={() => onSelectDraft(message)}
        />
      ))}
    </div>
  ))
})

const DraftMessagePreview = React.memo(function DraftMessagePreview({
  message,
  onClick
}: {
  message: DraftContent
  onClick: () => void
}) {
  const { i18n } = useTranslation()
  return (
    <Container
      isRead={true}
      active={false}
      data-qa="sent-message-preview"
      onClick={onClick}
    >
      <FixedSpaceColumn>
        <Header isRead={true}>
          <Truncated data-qa="message-recipients">
            {message.recipientNames.join(', ')}
          </Truncated>
          <MessageTypeChip color={colors.accents.a5orangeLight}>
            {i18n.messages.draft}
          </MessageTypeChip>
        </Header>
        <TitleAndDate isRead={true}>
          <Truncated data-qa="message-preview-title">{message.title}</Truncated>
        </TitleAndDate>
        <Truncated>
          {message.content
            .substring(0, 200)
            .replace(new RegExp('\\n', 'g'), ' ')}
        </Truncated>
      </FixedSpaceColumn>
    </Container>
  )
})
