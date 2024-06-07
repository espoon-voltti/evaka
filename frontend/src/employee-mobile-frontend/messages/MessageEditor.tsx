// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { boolean, string } from 'lib-common/form/fields'
import { array, mapped, object, recursive, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import {
  DraftContent,
  MessageAccount,
  MessageReceiversResponse,
  MessageRecipient,
  PostMessageBody,
  UpdatableDraftContent
} from 'lib-common/generated/api-types/messaging'
import { cancelMutation, useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import TreeDropdown from 'lib-components/atoms/dropdowns/TreeDropdown'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import {
  getSelected,
  receiversAsSelectorNode,
  SelectorNode
} from 'lib-components/messages/SelectorNode'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Bold, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'

import {
  createMessagePreflightCheckQuery,
  deleteDraftMutation,
  initDraftMutation,
  saveDraftMutation,
  sendMessageMutation
} from './queries'

const treeNode = (): Form<SelectorNode, never, SelectorNode, unknown> =>
  object({
    text: string(),
    key: string(),
    checked: boolean(),
    messageRecipient: value<MessageRecipient>(),
    children: array(recursive(treeNode))
  })

const messageForm = mapped(
  object({
    recipients: array(treeNode()),
    urgent: boolean(),
    title: string(),
    content: string()
  }),
  (output) => {
    const selectedRecipients = getSelected(output.recipients)
    const commonContent = {
      title: output.title,
      content: output.content,
      type: 'MESSAGE' as const,
      urgent: output.urgent,
      sensitive: false,
      yearsOfBirth: [],
      serviceNeedIds: [],
      shiftCare: false,
      intermittentShiftCare: false,
      familyDaycare: false
    }
    return {
      messageContent: (draftId: UUID | null): PostMessageBody | undefined =>
        selectedRecipients.length === 0 ||
        output.title === '' ||
        output.content === ''
          ? undefined // required fields missing
          : {
              ...commonContent,
              recipients: selectedRecipients.map((r) => r.messageRecipient),
              recipientNames: selectedRecipients.map((r) => r.text),
              attachmentIds: [],
              draftId,
              relatedApplicationId: null,
              filters: null
            },
      draftContent: (): UpdatableDraftContent => ({
        ...commonContent,
        recipientIds: selectedRecipients.map((r) => r.messageRecipient.id),
        recipientNames: selectedRecipients.map((r) => r.text)
      })
    }
  }
)

interface Props {
  unitId: UUID
  account: MessageAccount
  availableRecipients: MessageReceiversResponse[]
  draft: DraftContent | undefined
  onClose: () => void
}

export default React.memo(function MessageEditor({
  unitId,
  account,
  availableRecipients,
  draft,
  onClose
}: Props) {
  const { i18n } = useTranslation()

  const [draftId, setDraftId] = useState<UUID | null>(draft?.id ?? null)
  const { mutateAsync: init } = useMutation(initDraftMutation)
  const { mutate: saveDraft, isPending: isSavingDraft } =
    useMutation(saveDraftMutation)

  const [debouncedSaveDraft, cancelSaveDraft, saveDraftImmediately] =
    useDebouncedCallback(saveDraft, isAutomatedTest ? 200 : 2000)

  useEffect(() => {
    if (!draftId) {
      void init({ accountId: account.id }).then(setDraftId)
    }
  }, [account.id, draftId, init])

  const form = useForm(
    messageForm,
    () =>
      draft
        ? {
            recipients: receiversAsSelectorNode(
              account.id,
              availableRecipients,
              draft.recipientIds
            ),
            urgent: draft.urgent,
            title: draft.title,
            content: draft.content
          }
        : {
            recipients: receiversAsSelectorNode(
              account.id,
              availableRecipients
            ),
            urgent: false,
            title: '',
            content: ''
          },
    {},
    {
      onUpdate(_prevState, nextState, form) {
        if (draftId && !isSavingDraft) {
          const result = form.validate(nextState)
          if (result.isValid) {
            debouncedSaveDraft({
              accountId: account.id,
              draftId,
              body: result.value.draftContent()
            })
          }
        }
        return nextState
      }
    }
  )
  const { recipients, urgent, title, content } = useFormFields(form)

  const selectedRecipients = useMemo(
    () => getSelected(recipients.value()).map((r) => r.messageRecipient),
    [recipients]
  )
  const debouncedRecipients = useDebounce(selectedRecipients, 500)
  const preflightResult = useQueryResult(
    createMessagePreflightCheckQuery({
      accountId: account.id,
      body: {
        filters: null, // not supported on mobile
        recipients: debouncedRecipients
      }
    })
  )

  const sendDisabled =
    !form.isValid() ||
    form.value().messageContent(draftId) === undefined ||
    !isEqual(debouncedRecipients, selectedRecipients) ||
    !preflightResult.isSuccess

  const sendMutationOnClick = () => {
    cancelSaveDraft()
    const messageContent = form.value().messageContent(draftId)
    return messageContent !== undefined
      ? { accountId: account.id, body: messageContent }
      : cancelMutation
  }

  return (
    <div data-qa="message-editor">
      <TopBar
        invertedColors
        title={i18n.messages.messageEditor.newMessage}
        unitId={unitId}
        closeDisabled={isSavingDraft}
        onClose={() => {
          saveDraftImmediately()
          onClose()
        }}
      />
      <ContentArea opaque>
        <Bold>{i18n.messages.messageEditor.sender}</Bold>
        <Gap size="xs" />
        <P noMargin data-qa="sender-name">
          {account.name}
        </P>
        <Gap size="s" />

        <div>
          <Bold>{i18n.messages.messageEditor.to.label}</Bold>
          <Gap size="xs" />
          <TreeDropdown
            tree={recipients.state}
            onChange={recipients.set}
            placeholder={i18n.messages.messageEditor.recipientsPlaceholder}
            data-qa="recipients"
          />
        </div>

        <Gap size="s" />

        <Bold>{i18n.messages.messageEditor.urgent.heading}</Bold>
        <CheckboxF
          bind={urgent}
          label={i18n.messages.messageEditor.urgent.label}
          data-qa="checkbox-urgent"
        />
        {urgent.state && (
          <>
            <Gap size="s" />
            <InfoBox
              message={i18n.messages.messageEditor.urgent.info}
              noMargin
            />
          </>
        )}

        <Gap size="s" />

        <label>
          <Bold>{i18n.messages.messageEditor.subject.heading}</Bold>
          <InputFieldF
            bind={title}
            placeholder={i18n.messages.messageEditor.subject.placeholder}
            data-qa="input-title"
          />
        </label>

        <Gap size="s" />

        <label>
          <Bold>{i18n.messages.messageEditor.message.heading}</Bold>
          <Gap size="s" />
          <StyledTextArea
            value={content.state}
            onChange={(e) => content.set(e.target.value)}
            placeholder={i18n.messages.messageEditor.message.placeholder}
            data-qa="input-content"
          />
        </label>

        <Gap size="s" />

        <div>
          <span>{i18n.messages.messageEditor.recipientCount}: </span>
          {preflightResult.isSuccess && (
            <span>{preflightResult.value.numberOfRecipientAccounts}</span>
          )}
        </div>
        <BottomRow>
          <MutateButton
            appearance="inline"
            text={i18n.messages.messageEditor.deleteDraft}
            mutation={deleteDraftMutation}
            disabled={!draftId}
            onClick={() => {
              cancelSaveDraft()
              if (draftId) {
                return { accountId: account.id, draftId }
              }
              return cancelMutation
            }}
            onSuccess={onClose}
            data-qa="discard-message-btn"
          />
          <span />
          {preflightResult.isSuccess &&
          preflightResult.value.numberOfRecipientAccounts > 2 ? (
            <ConfirmedMutation
              mutation={sendMessageMutation}
              buttonStyle="BUTTON"
              primary
              buttonText={i18n.messages.messageEditor.send}
              disabled={sendDisabled}
              onClick={sendMutationOnClick}
              onSuccess={onClose}
              data-qa="send-message-btn"
              data-qa-modal="many-recipients-warning"
              confirmationTitle={
                i18n.messages.messageEditor.manyRecipientsWarning.title
              }
              confirmationText={i18n.messages.messageEditor.manyRecipientsWarning.text(
                preflightResult.value.numberOfRecipientAccounts
              )}
              confirmLabel={i18n.messages.messageEditor.send}
              cancelLabel={i18n.common.cancel}
            />
          ) : (
            <MutateButton
              mutation={sendMessageMutation}
              primary
              text={i18n.messages.messageEditor.send}
              disabled={sendDisabled}
              onClick={sendMutationOnClick}
              onSuccess={onClose}
              data-qa="send-message-btn"
            />
          )}
        </BottomRow>
      </ContentArea>
    </div>
  )
})

const StyledTextArea = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
  min-height: 100px;
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`
