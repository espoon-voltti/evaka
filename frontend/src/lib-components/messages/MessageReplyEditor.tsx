// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { AccountType } from 'lib-common/generated/api-types/messaging'
import type { MessageAccountId } from 'lib-common/generated/api-types/shared'
import type { MutationDescription } from 'lib-common/query'
import { type cancelMutation } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { faTrash } from 'lib-icons'

import { MutateButton } from '../atoms/buttons/MutateButton'
import TextArea from '../atoms/form/TextArea'
import { useTranslations } from '../i18n'
import ButtonContainer from '../layout/ButtonContainer'
import { Label } from '../typography'
import { defaultMargins } from '../white-space'

import OutOfOfficeInfo from './OutOfOfficeInfo'
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
  id: MessageAccountId
  name: string
  selected: boolean
  toggleable: boolean
  type: AccountType
  outOfOffice: FiniteDateRange | null
}

interface Props<T, R> {
  recipients: SelectableAccount[]
  onToggleRecipient: (id: MessageAccountId, selected: boolean) => void
  mutation: MutationDescription<T, R>
  onSubmit: () => T | typeof cancelMutation
  onSuccess: (response: R) => void
  onDiscard: () => void
  onUpdateContent: (content: string) => void
  replyContent: string
  sendEnabled: boolean
  messageThreadSensitive?: boolean
}

function MessageReplyEditor<T, R>({
  mutation,
  onSubmit,
  onSuccess,
  onDiscard,
  onUpdateContent,
  onToggleRecipient,
  recipients,
  replyContent,
  sendEnabled,
  messageThreadSensitive = false
}: Props<T, R>) {
  const i18n = useTranslations()

  const handleSuccess = useCallback(
    (response: R) => {
      onUpdateContent('')
      onSuccess(response)
    },
    [onUpdateContent, onSuccess]
  )

  return (
    <>
      <EditorRow>
        <Label>{i18n.messages.recipients}:</Label>{' '}
        {recipients.map((recipient) => (
          <ToggleableRecipient
            key={recipient.id}
            recipient={recipient}
            onToggleRecipient={onToggleRecipient}
            labelAdd={i18n.common.add}
          />
        ))}
        <OutOfOfficeInfo
          selectedAccountIds={recipients
            .filter((r) => r.selected)
            .map((r) => r.id)}
          accounts={recipients.map((r) => ({
            account: { id: r.id, name: r.name, type: r.type },
            outOfOffice: r.outOfOffice
          }))}
        />
      </EditorRow>
      <EditorRow>
        <Label>{i18n.messages.message}</Label>
        <MultiRowTextArea
          rows={4}
          placeholder={
            messageThreadSensitive
              ? i18n.messageReplyEditor.messagePlaceholderSensitiveThread
              : i18n.messageReplyEditor.messagePlaceholder
          }
          value={replyContent}
          onChange={(value) => onUpdateContent(value)}
          data-qa="message-reply-content"
          autoFocus
          preventAutoFocusScroll={true}
        />
      </EditorRow>
      <EditorRow>
        <ButtonContainer justify="space-between">
          <MutateButton
            mutation={mutation}
            text={i18n.messages.send}
            textInProgress={i18n.messages.sending}
            primary
            data-qa="message-send-btn"
            onClick={onSubmit}
            onSuccess={handleSuccess}
            disabled={!sendEnabled}
          />
          <Button
            appearance="inline"
            text={i18n.messageReplyEditor.discard}
            icon={faTrash}
            data-qa="message-discard-btn"
            onClick={onDiscard}
          />
        </ButtonContainer>
      </EditorRow>
    </>
  )
}

export default React.memo(MessageReplyEditor) as typeof MessageReplyEditor
