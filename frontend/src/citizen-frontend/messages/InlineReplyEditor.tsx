// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { TextArea } from 'lib-components/atoms/form/InputField'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'
import Button from '../../lib-components/atoms/buttons/Button'
import ButtonContainer from '../../lib-components/layout/ButtonContainer'
import { useTranslation } from '../localization'
import { MessageContainer } from './MessageComponents'
import { MessageContext } from './state'
import { Message, MessageAccount } from './types'

const MultiRowTextArea = styled(TextArea)`
  height: unset;
`

interface Props {
  account: MessageAccount
  message: Message
}

export function InlineReplyEditor({ account, message }: Props) {
  const i18n = useTranslation()
  const { sendReply, replyState } = useContext(MessageContext)

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

  const [content, setContent] = useState('')

  const onSend = useCallback(() => {
    sendReply({
      content,
      messageId: message.id,
      recipientAccountIds: recipients.map((r) => r.id)
    })
  }, [message.id, recipients, content, sendReply])

  useEffect(() => {
    if (replyState?.isSuccess) {
      setContent('')
    }
  }, [replyState])

  return (
    <MessageContainer>
      <div>
        <strong>{i18n.messages.recipients}</strong>:{' '}
        {recipients.map((r) => r.name).join(', ')}
      </div>
      <div>
        <strong>{i18n.messages.types.MESSAGE}</strong>
        <MultiRowTextArea
          rows={4}
          value={content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            setContent(e.target.value)
          }
        />
      </div>
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
    </MessageContainer>
  )
}
