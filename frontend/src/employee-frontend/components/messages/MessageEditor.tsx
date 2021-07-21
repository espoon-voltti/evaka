// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UpdateStateFn } from 'lib-common/form-state'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes, faTrash } from 'lib-icons'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import Select, { SelectOption } from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import {
  deselectAll,
  getReceiverOptions,
  getSelected,
  getSelectedBottomElements,
  getSelectorName,
  getSubTree,
  SelectorNode,
  updateSelector
} from './SelectorNode'
import {
  DraftContent,
  MessageAccount,
  MessageBody,
  UpsertableDraftContent
} from './types'
import { Draft, useDraft } from './useDraft'

type Message = UpsertableDraftContent & {
  sender: SelectOption
  recipientAccountIds: UUID[]
}

const messageToUpsertableDraftWithAccount = ({
  sender: { value: accountId },
  recipientAccountIds: _,
  ...rest
}: Message): Draft => ({ ...rest, accountId })

const emptyMessage = {
  content: '',
  recipientIds: [],
  recipientNames: [],
  recipientAccountIds: [],
  title: '',
  type: 'MESSAGE' as const
}

const getInitialMessage = (
  draft: DraftContent | undefined,
  sender: SelectOption
): Message =>
  draft
    ? { ...draft, sender, recipientAccountIds: [] }
    : { sender, ...emptyMessage }

const areRequiredFieldsFilledForMessage = (msg: Message): boolean =>
  !!(msg.recipientAccountIds?.length && msg.type && msg.content && msg.title)

const createReceiverTree = (tree: SelectorNode, selectedIds: UUID[]) =>
  selectedIds.reduce(
    (acc, next) => updateSelector(acc, { selectorId: next, selected: true }),
    deselectAll(tree)
  )

interface Props {
  defaultSender: SelectOption
  accounts: MessageAccount[]
  selectedUnit: SelectOption
  availableReceivers: SelectorNode
  onSend: (
    accountId: UUID,
    messageBody: MessageBody,
    draftId: string | undefined
  ) => void
  onClose: (didChanges: boolean) => void
  onDiscard: (accountId: UUID, draftId?: UUID) => void
  draftContent?: DraftContent
}

export default React.memo(function MessageEditor({
  defaultSender,
  accounts,
  selectedUnit,
  availableReceivers,
  onSend,
  onDiscard,
  onClose,
  draftContent
}: Props) {
  const { i18n } = useTranslation()

  const [receiverTree, setReceiverTree] = useState<SelectorNode>(
    draftContent
      ? createReceiverTree(availableReceivers, draftContent.recipientIds)
      : availableReceivers
  )
  const selectedReceivers = useMemo(
    () => getSelected(receiverTree),
    [receiverTree]
  )
  const receiverOptions = useMemo(
    () => getReceiverOptions(receiverTree),
    [receiverTree]
  )

  const [message, setMessage] = useState<Message>(
    getInitialMessage(draftContent, defaultSender)
  )
  const [contentTouched, setContentTouched] = useState(false)
  const {
    draftId,
    setDraft,
    saveDraft,
    state: draftState,
    wasModified: draftWasModified
  } = useDraft(draftContent?.id)

  useEffect(
    function syncDraftContentOnMessageChanges() {
      contentTouched && setDraft(messageToUpsertableDraftWithAccount(message))
    },
    [contentTouched, message, setDraft]
  )

  const updateReceiverTree = useCallback((newSelection: SelectOption[]) => {
    setReceiverTree((old) =>
      createReceiverTree(
        old,
        newSelection.map((s) => s.value)
      )
    )
    setContentTouched(true)
  }, [])

  useEffect(
    function updateSelectedReceiversOnReceiverTreeChanges() {
      const selected = getSelected(receiverTree)
      const recipientAccountIds = getSelectedBottomElements(receiverTree)
      setMessage((old) => ({
        ...old,
        recipientAccountIds,
        recipientIds: selected.map((s) => s.value),
        recipientNames: selected.map((s) => s.label)
      }))
    },
    [receiverTree]
  )

  const [saveStatus, setSaveStatus] = useState<string>()
  useEffect(
    function updateTextualSaveStatusOnDraftStateChange() {
      if (draftState === 'saving') {
        setSaveStatus(`${i18n.common.saving}...`)
      } else if (draftState === 'clean' && draftWasModified) {
        setSaveStatus(i18n.common.saved)
      } else {
        return
      }
      const clearStatus = () => setSaveStatus(undefined)
      const timeoutHandle = setTimeout(clearStatus, 1500)
      return () => clearTimeout(timeoutHandle)
    },
    [i18n, draftState, draftWasModified]
  )
  useEffect(
    function updateReceiversOnSenderChange() {
      const account = accounts.find(
        (account) => account.id === message.sender.value
      )
      if (account?.type === 'PERSONAL') {
        setReceiverTree(availableReceivers)
      } else if (account?.type === 'GROUP') {
        const groupId = account.daycareGroup.id
        const selection = getSubTree(availableReceivers, groupId)
        if (selection) {
          setReceiverTree(selection)
        }
      }
    },
    [message.sender, accounts, availableReceivers]
  )

  const debouncedSaveStatus = useDebounce(saveStatus, 250)
  const title =
    debouncedSaveStatus ||
    message.title ||
    i18n.messages.messageEditor.newMessage(selectedUnit.label)

  const updateMessage = useCallback<UpdateStateFn<Message>>((changes) => {
    setMessage((old) => ({ ...old, ...changes }))
    setContentTouched(true)
  }, [])

  const sendEnabled = areRequiredFieldsFilledForMessage(message)
  const sendHandler = useCallback(() => {
    const {
      sender: { value: senderId },
      content,
      title,
      type,
      recipientAccountIds,
      recipientNames
    } = message
    onSend(
      senderId,
      { content, title, type, recipientAccountIds, recipientNames },
      draftId
    )
  }, [onSend, message, draftId])

  const onCloseHandler = useCallback(() => {
    if (draftWasModified && draftState === 'dirty') {
      saveDraft()
    }
    onClose(draftWasModified)
  }, [draftState, draftWasModified, onClose, saveDraft])

  const senderOptions = useMemo(
    () =>
      accounts
        .filter(
          (account: MessageAccount) =>
            account.type === 'PERSONAL' ||
            (account.type === 'GROUP' &&
              !!getSelectorName(account.daycareGroup.id, availableReceivers) &&
              account.daycareGroup.unitId === selectedUnit.value)
        )
        .map((account: MessageAccount) => ({
          value: account.id,
          label: account.name
        })),
    [accounts, availableReceivers, selectedUnit.value]
  )

  return (
    <Container data-qa="message-editor" data-status={draftState}>
      <TopBar>
        <Title>{title}</Title>
        <IconButton
          icon={faTimes}
          onClick={onCloseHandler}
          white
          data-qa="close-message-editor-btn"
        />
      </TopBar>
      <FormArea>
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.sender}</div>
        </div>
        <Select
          items={senderOptions}
          onChange={(sender) =>
            sender ? updateMessage({ sender }) : undefined
          }
          selectedItem={message.sender}
          data-qa="select-sender"
        />
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.receivers}</div>
        </div>
        <MultiSelect
          placeholder={i18n.common.search}
          value={selectedReceivers}
          options={receiverOptions}
          onChange={updateReceiverTree}
          noOptionsMessage={i18n.common.noResults}
          getOptionId={({ value }) => value}
          getOptionLabel={({ label }) => label}
          data-qa="select-receiver"
        />
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.type.label}</div>
        <Gap size={'xs'} />
        <FixedSpaceRow>
          <Radio
            label={i18n.messages.messageEditor.type.message}
            checked={message.type === 'MESSAGE'}
            onChange={() => updateMessage({ type: 'MESSAGE' })}
          />
          <Radio
            label={i18n.messages.messageEditor.type.bulletin}
            checked={message.type === 'BULLETIN'}
            onChange={() => updateMessage({ type: 'BULLETIN' })}
          />
        </FixedSpaceRow>
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.title}</div>
        <Gap size={'xs'} />
        <InputField
          value={message.title ?? ''}
          onChange={(title) => updateMessage({ title })}
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={message.content}
          onChange={(e) => updateMessage({ content: e.target.value })}
          data-qa={'input-content'}
        />
        <Gap size={'s'} />
        <BottomRow>
          <InlineButton
            onClick={() => onDiscard(message.sender.value, draftId)}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
            data-qa="discard-draft-btn"
          />
          <Button
            text={i18n.messages.messageEditor.send}
            primary
            disabled={!sendEnabled}
            onClick={sendHandler}
            data-qa="send-message-btn"
          />
        </BottomRow>
      </FormArea>
    </Container>
  )
})

const Container = styled.div`
  width: 680px;
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
