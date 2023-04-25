// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type { AccountType } from 'lib-common/generated/api-types/messaging'
import type { UUID } from 'lib-common/types'
import { faTrash } from 'lib-icons'

import AsyncButton from '../atoms/buttons/AsyncButton'
import InlineButton from '../atoms/buttons/InlineButton'
import TextArea from '../atoms/form/TextArea'
import ButtonContainer from '../layout/ButtonContainer'
import { Label } from '../typography'
import { defaultMargins } from '../white-space'

import { ToggleableRecipient } from './ToggleableRecipient'

const MultiRowTextArea = styled(TextArea)`
  height: unset;
  margin-top: ${defaultMargins.xs};
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};

  &:focus {
    border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  }
`
const EditorRow = styled.div`
  & + & {
    margin-top: ${defaultMargins.s};
  }
`

export interface SelectableAccount {
  id: UUID
  name: string
  selected: boolean
  toggleable: boolean
  type: AccountType
}

interface Labels {
  add: string
  recipients: string
  message: string
  messagePlaceholder?: string
  send: string
  sending: string
  discard: string
}

interface Props {
  recipients: SelectableAccount[]
  onToggleRecipient: (id: UUID, selected: boolean) => void
  onSubmit: () => Promise<Result<unknown>>
  onDiscard: () => void
  onUpdateContent: (content: string) => void
  replyContent: string
  sendEnabled: boolean
  i18n: Labels
}

export const MessageReplyEditor = React.memo(function MessageReplyEditor({
  i18n,
  onSubmit,
  onDiscard,
  onUpdateContent,
  onToggleRecipient,
  recipients,
  replyContent,
  sendEnabled
}: Props) {
  return (
    <>
      <EditorRow>
        <Label>{i18n.recipients}:</Label>{' '}
        {recipients.map((recipient) => (
          <ToggleableRecipient
            key={recipient.id}
            recipient={recipient}
            onToggleRecipient={onToggleRecipient}
            labelAdd={i18n.add}
          />
        ))}
      </EditorRow>
      <EditorRow>
        <Label>{i18n.message}</Label>
        <MultiRowTextArea
          rows={4}
          placeholder={i18n.messagePlaceholder}
          value={replyContent}
          onChange={(value) => onUpdateContent(value)}
          data-qa="message-reply-content"
          autoFocus
        />
      </EditorRow>
      <EditorRow>
        <ButtonContainer justify="space-between">
          <AsyncButton
            text={i18n.send}
            textInProgress={i18n.sending}
            primary
            data-qa="message-send-btn"
            onClick={onSubmit}
            onSuccess={() => undefined}
            disabled={!sendEnabled}
          />
          <InlineButton
            text={i18n.discard}
            icon={faTrash}
            data-qa="message-discard-btn"
            onClick={onDiscard}
          />
        </ButtonContainer>
      </EditorRow>
    </>
  )
})
