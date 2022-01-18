// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { faTimes, faTrash } from 'lib-icons'
import Button from '../atoms/buttons/Button'
import InlineButton from '../atoms/buttons/InlineButton'
import TextArea from '../atoms/form/TextArea'
import ButtonContainer from '../layout/ButtonContainer'
import { fontWeights, Label } from '../typography'
import { defaultMargins } from '../white-space'

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

const Recipient = styled.span<{ selected: boolean; toggleable: boolean }>`
  cursor: ${(p) => (p.toggleable ? 'pointer' : 'default')};;
  padding: 0 ${(p) => (p.selected ? '12px' : defaultMargins.xs)};
  background-color: ${(p) =>
    p.selected ? p.theme.colors.grayscale.g15 : 'unset'};
  border-radius: 1000px;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => (p.selected ? 'unset' : p.theme.colors.main.m2)};

  & > :last-child {
    margin-left: ${defaultMargins.xs};
  }

  :not(:last-child) {
    margin-right: ${defaultMargins.xs};
`

export interface SelectableAccount {
  id: UUID
  name: string
  selected: boolean
  toggleable: boolean
}

interface ToggleableRecipientProps extends SelectableAccount {
  onToggleRecipient: (id: UUID, selected: boolean) => void
  labelAdd: string
}

function ToggleableRecipient({
  id,
  labelAdd,
  name,
  onToggleRecipient,
  selected,
  toggleable
}: ToggleableRecipientProps) {
  const onClick = toggleable
    ? () => onToggleRecipient(id, !selected)
    : undefined

  return (
    <Recipient onClick={onClick} selected={selected} toggleable={toggleable}>
      {selected ? (
        <>
          {name}
          {toggleable && <FontAwesomeIcon icon={faTimes} />}
        </>
      ) : (
        `+ ${labelAdd} ${name}`
      )}
    </Recipient>
  )
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
