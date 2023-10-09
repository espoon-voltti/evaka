// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { boolean, string } from 'lib-common/form/fields'
import { array, mapped, object, recursive, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import {
  MessageAccount,
  MessageReceiversResponse,
  MessageRecipient,
  PostMessageBody
} from 'lib-common/generated/api-types/messaging'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import TreeDropdown from 'lib-components/atoms/dropdowns/TreeDropdown'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import {
  getSelected,
  receiversAsSelectorNode,
  SelectorNode
} from 'lib-components/messages/SelectorNode'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Bold, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'

import { sendMessageMutation } from './queries'

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
  (output): PostMessageBody => {
    const selectedRecipients = getSelected(output.recipients)
    return {
      title: output.title,
      content: output.content,
      type: 'MESSAGE',
      urgent: output.urgent,
      sensitive: false,
      recipients: selectedRecipients.map((r) => r.messageRecipient),
      recipientNames: selectedRecipients.map((r) => r.text),
      attachmentIds: [],
      draftId: null,
      relatedApplicationId: null
    }
  }
)

interface Props {
  account: MessageAccount
  availableRecipients: MessageReceiversResponse[]
  onClose: () => void
}

export default React.memo(function MessageEditor({
  account,
  availableRecipients,
  onClose
}: Props) {
  const { i18n } = useTranslation()

  const form = useForm(
    messageForm,
    () => ({
      recipients: receiversAsSelectorNode(account.id, availableRecipients),
      urgent: false,
      title: '',
      content: ''
    }),
    {}
  )
  const { recipients, urgent, title, content } = useFormFields(form)

  return (
    <div data-qa="message-editor">
      <TopBar
        invertedColors
        title={i18n.messages.messageEditor.newMessage}
        onClose={onClose}
      />
      <ContentArea opaque>
        <Bold>{i18n.messages.messageEditor.sender}</Bold>
        <Gap size="xs" />
        <P noMargin>{account.name}</P>
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

        <BottomRow>
          <Button
            text={i18n.messages.messageEditor.discard}
            onClick={onClose}
          />
          <span />
          <MutateButton
            mutation={sendMessageMutation}
            primary
            text={i18n.messages.messageEditor.send}
            disabled={!form.isValid()}
            onClick={() => ({ accountId: account.id, body: form.value() })}
            onSuccess={onClose}
            data-qa="send-message-btn"
          />
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
