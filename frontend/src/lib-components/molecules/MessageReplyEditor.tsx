// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { faTrash } from 'lib-icons'

import Button from '../atoms/buttons/Button'
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
  onSubmit: () => void
  onDiscard: () => void
  replyState: Result<void> | undefined
  onUpdateContent: (content: string) => void
  replyContent: string
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
  replyState
}: Props) {
  const sendEnabled =
    !!replyContent &&
    !replyState?.isLoading &&
    recipients.some((r) => r.selected)
  return (
    <>
      <EditorRow>
        <Label>{i18n.recipients}:</Label>{' '}
        {recipients.map((recipient) => (
          <ToggleableRecipient
            key={recipient.id}
            onToggleRecipient={onToggleRecipient}
            labelAdd={i18n.add}
            {...recipient}
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
          <Button
            text={replyState?.isLoading ? i18n.sending : i18n.send}
            primary
            data-qa="message-send-btn"
            onClick={onSubmit}
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
