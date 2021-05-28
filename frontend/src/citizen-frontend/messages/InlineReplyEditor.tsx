// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import { TextArea } from 'lib-components/atoms/form/InputField'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { defaultMargins } from 'lib-components/white-space'
import React, { useCallback, useContext, useMemo } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import { MessageContainer } from './MessageComponents'
import { MessageContext } from './state'
import { Message, MessageAccount } from './types'

const MultiRowTextArea = styled(TextArea)`
  height: unset;
  margin-top: ${defaultMargins.xs};
  border: 1px solid ${({ theme: { colors } }) => colors.greyscale.medium};

  &:focus {
    border: 1px solid ${({ theme: { colors } }) => colors.greyscale.medium};
  }
`

const EditorRow = styled.div`
  & + & {
    margin-top: ${defaultMargins.s};
  }
`

const Label = styled.span`
  font-weight: 600;
`

interface Props {
  account: MessageAccount
  message: Message
  threadId: UUID
}

export function InlineReplyEditor({ threadId, account, message }: Props) {
  const i18n = useTranslation()
  const {
    sendReply,
    replyState,
    setReplyContent,
    getReplyContent
  } = useContext(MessageContext)

  // TODO toggleable recipients when UX is ready
  const recipients = useMemo(
    () => [
      ...message.recipients.filter((r) => r.id !== account?.id),
      ...(message.senderId !== account?.id
        ? [{ id: message.senderId, name: message.senderName }]
        : [])
    ],
    [account, message]
  )

  const replyContent = getReplyContent(threadId)

  const onSend = useCallback(() => {
    sendReply({
      content: replyContent,
      messageId: message.id,
      recipientAccountIds: recipients.map((r) => r.id)
    })
  }, [message.id, recipients, replyContent, sendReply])

  return (
    <MessageContainer>
      <EditorRow>
        <Label>{i18n.messages.recipients}:</Label>{' '}
        {recipients.map((r) => r.name).join(', ')}
      </EditorRow>
      <EditorRow>
        <Label>{i18n.messages.types.MESSAGE}</Label>
        <MultiRowTextArea
          rows={4}
          value={replyContent}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            setReplyContent(threadId, e.target.value)
          }
        />
      </EditorRow>
      <EditorRow>
        <ButtonContainer justify="flex-start">
          <Button
            text={
              replyState?.isLoading
                ? `${i18n.messages.sending}...`
                : i18n.messages.send
            }
            primary
            data-qa="message-send-btn"
            onClick={onSend}
            disabled={replyState && !replyState.isSuccess}
          />
        </ButtonContainer>
      </EditorRow>
    </MessageContainer>
  )
}
