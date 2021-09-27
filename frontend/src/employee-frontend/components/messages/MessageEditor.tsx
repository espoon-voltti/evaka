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
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes, faTrash } from 'lib-icons'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import FileUpload from 'lib-components/molecules/FileUpload'
import { Attachment } from 'lib-common/api-types/attachment'
import { Failure } from 'lib-common/api'
import { featureFlags } from 'lib-customizations/employee'
import {
  DraftContent,
  NestedMessageAccount,
  PostMessageBody,
  UpsertableDraftContent
} from 'lib-common/generated/api-types/messaging'
import { fontWeights } from 'lib-components/typography'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveMessageAttachment
} from '../../api/attachments'
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
import { isNestedGroupMessageAccount } from './types'
import { Draft, useDraft } from './useDraft'

type Message = UpsertableDraftContent & {
  sender: SelectOption
  recipientAccountIds: UUID[]
  attachments: Attachment[]
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
  attachments: [],
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

const areRequiredFieldsFilled = (msg: Message): boolean =>
  !!(msg.recipientAccountIds?.length && msg.type && msg.content && msg.title)

const createReceiverTree = (tree: SelectorNode, selectedIds: UUID[]) =>
  selectedIds.reduce(
    (acc, next) => updateSelector(acc, { selectorId: next, selected: true }),
    deselectAll(tree)
  )

interface Props {
  defaultSender: SelectOption
  nestedAccounts: NestedMessageAccount[]
  selectedUnit: SelectOption
  availableReceivers: SelectorNode
  onSend: (accountId: UUID, msg: PostMessageBody) => void
  onClose: (didChanges: boolean) => void
  onDiscard: (accountId: UUID, draftId: UUID) => void
  draftContent?: DraftContent
  sending: boolean
}

export default React.memo(function MessageEditor({
  defaultSender,
  nestedAccounts,
  selectedUnit,
  availableReceivers,
  onSend,
  onDiscard,
  onClose,
  draftContent,
  sending
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
  } = useDraft(draftContent?.id ?? null)

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
      const nestedAccount = nestedAccounts.find(
        (account) => account.account.id === message.sender.value
      )
      if (nestedAccount && !isNestedGroupMessageAccount(nestedAccount)) {
        setReceiverTree(availableReceivers)
      } else if (nestedAccount && isNestedGroupMessageAccount(nestedAccount)) {
        const groupId = nestedAccount.daycareGroup.id
        const selection = getSubTree(availableReceivers, groupId)
        if (selection) {
          setReceiverTree(selection)
        }
      }
    },
    [message.sender, nestedAccounts, availableReceivers]
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

  const sendHandler = useCallback(() => {
    const {
      attachments,
      sender: { value: senderId },
      ...rest
    } = message
    const attachmentIds = attachments.map(({ id }) => id)
    onSend(senderId, { ...rest, attachmentIds, draftId })
  }, [onSend, message, draftId])

  const handleAttachmentUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) =>
      draftId
        ? (await saveMessageAttachment(draftId, file, onUploadProgress)).map(
            (id) => {
              setMessage(({ attachments, ...rest }) => ({
                ...rest,
                attachments: [
                  ...attachments,
                  { id, name: file.name, contentType: file.type }
                ]
              }))
              setContentTouched(true)
              return id
            }
          )
        : Failure.of<UUID>({ message: 'Should not happen' }),
    [draftId]
  )

  const handleAttachmentDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachment(id)).map(() =>
        setMessage(({ attachments, ...rest }) => ({
          ...rest,
          attachments: attachments.filter((a) => a.id !== id)
        }))
      ),
    []
  )

  const onCloseHandler = useCallback(() => {
    if (draftWasModified && draftState === 'dirty') {
      saveDraft()
    }
    onClose(draftWasModified)
  }, [draftState, draftWasModified, onClose, saveDraft])

  const senderOptions = useMemo(
    () =>
      nestedAccounts
        .filter(
          (nestedAccount: NestedMessageAccount) =>
            !isNestedGroupMessageAccount(nestedAccount) ||
            (isNestedGroupMessageAccount(nestedAccount) &&
              !!getSelectorName(
                nestedAccount.daycareGroup.id,
                availableReceivers
              ) &&
              nestedAccount.daycareGroup.unitId === selectedUnit.value)
        )
        .map((nestedAccount: NestedMessageAccount) => ({
          value: nestedAccount.account.id,
          label: nestedAccount.account.name
        })),
    [nestedAccounts, availableReceivers, selectedUnit.value]
  )

  const sendEnabled =
    !sending && draftState === 'clean' && areRequiredFieldsFilled(message)

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
      <ScrollableFormArea>
        <Bold>{i18n.messages.messageEditor.sender}</Bold>
        <Gap size={'xs'} />
        <Select
          items={senderOptions}
          onChange={(sender) =>
            sender ? updateMessage({ sender }) : undefined
          }
          selectedItem={message.sender}
          data-qa="select-sender"
          fullWidth
        />
        <div>
          <Gap size={'s'} />
          <Bold>{i18n.messages.messageEditor.receivers}</Bold>
          <Gap size={'xs'} />
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
        <Gap size={'s'} />
        <Bold>{i18n.messages.messageEditor.type.label}</Bold>
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
        <Gap size={'s'} />
        <Bold>{i18n.messages.messageEditor.title}</Bold>
        <InputField
          value={message.title ?? ''}
          onChange={(title) => updateMessage({ title })}
          data-qa={'input-title'}
        />
        <Gap size="m" />
        <Bold>{i18n.messages.messageEditor.message}</Bold>
        <Gap size="xs" />
        <StyledTextArea
          value={message.content}
          onChange={(e) => updateMessage({ content: e.target.value })}
          data-qa="input-content"
        />
        {featureFlags.experimental?.messageAttachments && (
          <FileUpload
            slim
            disabled={!draftId}
            data-qa="upload-message-attachment"
            files={message.attachments}
            i18n={i18n.fileUpload}
            onDownloadFile={getAttachmentBlob}
            onUpload={handleAttachmentUpload}
            onDelete={handleAttachmentDelete}
          />
        )}
        <Gap size={'L'} />
      </ScrollableFormArea>
      <BottomBar>
        {draftId ? (
          <InlineButton
            onClick={() => onDiscard(message.sender.value, draftId)}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
            data-qa="discard-draft-btn"
          />
        ) : (
          <Gap horizontal />
        )}
        <Button
          text={
            sending
              ? i18n.messages.messageEditor.sending
              : i18n.messages.messageEditor.send
          }
          primary
          disabled={!sendEnabled}
          onClick={sendHandler}
          data-qa="send-message-btn"
        />
      </BottomBar>
    </Container>
  )
})

const Container = styled.div`
  width: 680px;
  max-height: 900px;
  height: 105%;
  position: absolute;
  z-index: ${modalZIndex - 1};
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.greyscale.white};
  overflow: scroll;
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

const ScrollableFormArea = styled.div`
  width: 100%;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;

  flex-grow: 1;
  overflow: auto;
  min-height: 0; // for Firefox
`

const StyledTextArea = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
  min-height: 280px;
`

const BottomBar = styled.div`
  width: 100%;
  border-top: 1px solid ${colors.greyscale.medium};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const Bold = styled.span`
  font-weight: ${fontWeights.semibold};
`
