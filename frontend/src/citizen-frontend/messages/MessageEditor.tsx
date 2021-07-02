// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SendMessageParams } from 'citizen-frontend/messages/api'
import { MessageAccount } from 'lib-common/api-types/messaging/message'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes, faTrash } from 'lib-icons'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'

const emptyMessage: SendMessageParams = {
  title: '',
  content: '',
  recipients: []
}

const areRequiredFieldsFilledForMessage = (msg: SendMessageParams): boolean =>
  !!(msg.recipients.length && msg.content && msg.title)

interface Props {
  receiverOptions: MessageAccount[]
  onSend: (messageBody: SendMessageParams) => Promise<void>
  onClose: () => void
  onDiscard: () => void
  displaySendError: boolean
}

export default React.memo(function MessageEditor({
  receiverOptions,
  onSend,
  onDiscard,
  onClose,
  displaySendError
}: Props) {
  const i18n = useTranslation()

  const [message, setMessage] = useState<SendMessageParams>(emptyMessage)

  const title = message.title || i18n.messages.messageEditor.newMessage

  const sendEnabled = areRequiredFieldsFilledForMessage(message)

  return (
    <Container>
      <TopBar>
        <Title>{title}</Title>
        <IconButton
          icon={faTimes}
          onClick={() => onClose()}
          white
          data-qa="close-message-editor-btn"
        />
      </TopBar>
      <FormArea>
        <div>
          <div>{i18n.messages.messageEditor.receivers}</div>
        </div>
        <MultiSelect
          placeholder={i18n.messages.messageEditor.search}
          value={message.recipients}
          options={receiverOptions}
          onChange={(change) =>
            setMessage((message) => ({
              ...message,
              recipients: change
            }))
          }
          noOptionsMessage={i18n.messages.messageEditor.noResults}
          getOptionId={({ id }) => id}
          getOptionLabel={({ name }) => name}
          data-qa="select-receiver"
        />
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.title}</div>
        <Gap size={'xs'} />
        <InputField
          value={message.title ?? ''}
          onChange={(updated) =>
            setMessage((message) => ({ ...message, title: updated }))
          }
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={message.content}
          onChange={(updated) =>
            setMessage((message) => ({
              ...message,
              content: updated.target.value
            }))
          }
          data-qa={'input-content'}
        />
        <Gap size={'s'} />
        {displaySendError && (
          <ErrorMessage>
            {i18n.messages.messageEditor.messageSendError}
          </ErrorMessage>
        )}
        <BottomRow>
          <InlineButton
            onClick={() => onDiscard()}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
            data-qa="discard-draft-btn"
          />
          <AsyncButton
            primary
            text={i18n.messages.messageEditor.send}
            disabled={!sendEnabled}
            onClick={() => onSend(message)}
            onSuccess={() => undefined}
            data-qa="send-message-btn"
          />
        </BottomRow>
      </FormArea>
    </Container>
  )
})

const Container = styled.div`
  width: 100%;
  max-width: 680px;
  height: 100%;
  max-height: 700px;
  position: absolute;
  z-index: 100;
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.greyscale.white};
`

const ErrorMessage = styled.div`
  color: ${colors.accents.red};
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${colors.primary};
  color: ${colors.greyscale.white};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const Title = styled.span`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: ${defaultMargins.s};
`

const FormArea = styled.div`
  width: 100%;
  flex-grow: 1;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
`

const StyledTextArea = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`
