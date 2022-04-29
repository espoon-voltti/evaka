// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure, Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  DraftContent,
  AuthorizedMessageAccount,
  PostMessageBody,
  UpsertableDraftContent
} from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import {
  deselectAll,
  getReceiverOptions,
  getSelected,
  getSelectedBottomElements,
  getSelectorName,
  getSubTree,
  ReactSelectOption,
  SelectorNode,
  updateSelector
} from 'lib-components/employee/messages/SelectorNode'
import {
  isGroupMessageAccount,
  SaveDraftParams
} from 'lib-components/employee/messages/types'
import { Draft, useDraft } from 'lib-components/employee/messages/useDraft'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import FileUpload, { FileUploadI18n } from 'lib-components/molecules/FileUpload'
import { Bold } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faDownLeftAndUpRightToCenter,
  faTimes,
  faTrash,
  faUpRightAndDownLeftFromCenter
} from 'lib-icons'

import Combobox from '../../atoms/dropdowns/Combobox'

type Message = UpsertableDraftContent & {
  sender: ReactSelectOption
  recipientAccountIds: UUID[]
  attachments: Attachment[]
}

const messageToUpsertableDraftWithAccount = (m: Message): Draft => ({
  content: m.content,
  recipientIds: m.recipientIds,
  recipientNames: m.recipientNames,
  title: m.title,
  type: m.type,
  accountId: m.sender.value
})

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
  sender: ReactSelectOption
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

export interface MessageEditorI18n {
  newMessage: (unitName: string) => string
  to: {
    label: string
    placeholder: string
    noOptions: string
  }
  type: {
    label: string
    message: string
    bulletin: string
  }
  sender: string
  receivers: string
  title: string
  message: string
  deleteDraft: string
  send: string
  sending: string
  saving: string
  saved: string
  search: string
  noResults: string
  addAttachmentInfo: string
}

interface Props {
  availableReceivers: SelectorNode
  attachmentsEnabled: boolean
  defaultSender: ReactSelectOption
  deleteAttachment: (id: UUID) => Promise<Result<void>>
  draftContent?: DraftContent
  getAttachmentBlob: (attachmentId: UUID) => Promise<Result<BlobPart>>
  i18n: MessageEditorI18n & FileUploadI18n
  initDraftRaw: (accountId: string) => Promise<Result<string>>
  mobileVersion?: boolean
  accounts: AuthorizedMessageAccount[]
  onClose: (didChanges: boolean) => void
  onDiscard: (accountId: UUID, draftId: UUID) => void
  onSend: (accountId: UUID, msg: PostMessageBody) => void
  saveDraftRaw: (params: SaveDraftParams) => Promise<Result<void>>
  saveMessageAttachment: (
    draftId: UUID,
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ) => Promise<Result<UUID>>
  selectedUnit: ReactSelectOption
  sending: boolean
}

export default React.memo(function MessageEditor({
  availableReceivers,
  attachmentsEnabled,
  defaultSender,
  deleteAttachment,
  draftContent,
  getAttachmentBlob,
  i18n,
  initDraftRaw,
  mobileVersion = false,
  accounts,
  onClose,
  onDiscard,
  onSend,
  saveDraftRaw,
  saveMessageAttachment,
  selectedUnit,
  sending
}: Props) {
  const [receiverTree, setReceiverTree] = useState<SelectorNode>(() =>
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

  const [message, setMessage] = useState<Message>(() =>
    getInitialMessage(draftContent, defaultSender)
  )
  const [contentTouched, setContentTouched] = useState(false)
  const {
    draftId,
    setDraft,
    saveDraft,
    state: draftState,
    wasModified: draftWasModified
  } = useDraft({
    initialId: draftContent?.id ?? null,
    saveDraftRaw,
    initDraftRaw
  })

  const [expandedView, setExpandedView] = useState(false)

  const toggleExpandedView = useCallback(
    () => setExpandedView((prev) => !prev),
    []
  )

  useEffect(
    function syncDraftContentOnMessageChanges() {
      contentTouched && setDraft(messageToUpsertableDraftWithAccount(message))
    },
    [contentTouched, message, setDraft]
  )

  const updateReceiverTree = useCallback(
    (newSelection: ReactSelectOption[]) => {
      setReceiverTree((old) =>
        createReceiverTree(
          old,
          newSelection.map((s) => s.value)
        )
      )
    },
    []
  )

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
        setSaveStatus(`${i18n.saving}...`)
      } else if (draftState === 'clean' && draftWasModified) {
        setSaveStatus(i18n.saved)
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
      const acc = accounts.find(
        (account) => account.account.id === message.sender.value
      )
      if (!acc) {
        throw new Error('Selected sender was not found in accounts')
      }
      if (!isGroupMessageAccount(acc)) {
        setReceiverTree((previousReceivers) =>
          getSelectedBottomElements(previousReceivers).reduce(
            (acc, id) =>
              updateSelector(acc, { selectorId: id, selected: true }),
            availableReceivers
          )
        )
      } else {
        const groupId = acc.daycareGroup.id
        const selection = getSubTree(availableReceivers, groupId)
        if (selection) {
          setReceiverTree((previousReceivers) =>
            getSelectedBottomElements(previousReceivers).reduce(
              (acc, id) =>
                updateSelector(acc, { selectorId: id, selected: true }),
              selection
            )
          )
        }
      }
    },
    [message.sender, accounts, availableReceivers]
  )

  const debouncedSaveStatus = useDebounce(saveStatus, 250)
  const title =
    debouncedSaveStatus || message.title || i18n.newMessage(selectedUnit.label)

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
    [draftId, saveMessageAttachment]
  )

  const handleAttachmentDelete = useCallback(
    async (id: UUID) =>
      (await deleteAttachment(id)).map(() =>
        setMessage(({ attachments, ...rest }) => ({
          ...rest,
          attachments: attachments.filter((a) => a.id !== id)
        }))
      ),
    [deleteAttachment]
  )

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
          (acc: AuthorizedMessageAccount) =>
            !isGroupMessageAccount(acc) ||
            (isGroupMessageAccount(acc) &&
              !!getSelectorName(acc.daycareGroup.id, availableReceivers) &&
              acc.daycareGroup.unitId === selectedUnit.value)
        )
        .map(({ account: { id, name } }: AuthorizedMessageAccount) => ({
          value: id,
          label: name
        })),
    [accounts, availableReceivers, selectedUnit.value]
  )

  const sendEnabled =
    !sending && draftState === 'clean' && areRequiredFieldsFilled(message)

  const i18nWithReplacedTitle = {
    ...i18n,
    upload: {
      ...i18n.upload,
      input: {
        ...i18n.upload.input,
        title: i18n.addAttachmentInfo
      }
    }
  }

  return mobileVersion ? (
    <ContainerMobile data-qa="message-editor" data-status={draftState}>
      <TopBarMobile>
        <Title>{title}</Title>
        <IconButton
          icon={faTimes}
          onClick={onCloseHandler}
          data-qa="close-message-editor-btn"
        />
      </TopBarMobile>
      <ScrollableFormArea>
        <Bold>{i18n.sender}</Bold>
        <Gap size="xs" />
        {message.sender.label}
        <div>
          <Gap size="s" />
          <Bold>{i18n.receivers}</Bold>
          <Gap size="xs" />
        </div>
        {receiverOptions.length > 1 ? (
          <MultiSelect
            placeholder={i18n.search}
            value={selectedReceivers}
            options={receiverOptions}
            onChange={updateReceiverTree}
            noOptionsMessage={i18n.noResults}
            getOptionId={({ value }) => value}
            getOptionLabel={({ label }) => label}
            data-qa="select-receiver"
          />
        ) : (
          message.recipientNames.join(', ')
        )}
        <Gap size="s" />
        <Bold>{i18n.title}</Bold>
        <InputField
          value={message.title ?? ''}
          onChange={(title) => updateMessage({ title })}
          data-qa="input-title"
        />
        <Gap size="m" />
        <Bold>{i18n.message}</Bold>
        <Gap size="xs" />
        <MessageAreaMobile
          value={message.content}
          onChange={(e) => updateMessage({ content: e.target.value })}
          data-qa="input-content"
        />
        <Gap size="L" />
      </ScrollableFormArea>
      <BottomBarMobile>
        {draftId ? (
          <InlineButton
            onClick={() => onDiscard(message.sender.value, draftId)}
            text={i18n.deleteDraft}
            icon={faTrash}
            data-qa="discard-draft-btn"
          />
        ) : (
          <Gap horizontal />
        )}
        <Button
          text={sending ? i18n.sending : i18n.send}
          primary
          disabled={!sendEnabled}
          onClick={sendHandler}
          data-qa="send-message-btn"
        />
      </BottomBarMobile>
    </ContainerMobile>
  ) : (
    <FullScreenContainer
      data-qa="fullscreen-container"
      className={classNames({ fullscreen: expandedView })}
    >
      <Container
        data-qa="message-editor"
        data-status={draftState}
        className={classNames({ fullscreen: expandedView })}
      >
        <TopBar>
          <Title>{title}</Title>
          <HeaderButtonContainer>
            {expandedView ? (
              <IconButton
                icon={faDownLeftAndUpRightToCenter}
                onClick={toggleExpandedView}
                white
                size="s"
                data-qa="collapse-view-btn"
              />
            ) : (
              <IconButton
                icon={faUpRightAndDownLeftFromCenter}
                onClick={toggleExpandedView}
                white
                size="s"
                data-qa="expand-view-btn"
              />
            )}
            <IconButton
              icon={faTimes}
              onClick={onCloseHandler}
              white
              size="m"
              data-qa="close-message-editor-btn"
            />
          </HeaderButtonContainer>
        </TopBar>
        <ScrollableFormArea>
          <HorizontalField>
            <Bold>{i18n.sender}</Bold>
            <Combobox
              items={senderOptions}
              onChange={(sender) =>
                sender ? updateMessage({ sender }) : undefined
              }
              selectedItem={message.sender}
              getItemLabel={(sender) => sender.label}
              data-qa="select-sender"
              fullWidth
            />
          </HorizontalField>
          <Gap size="s" />
          <HorizontalField>
            <Bold>{i18n.receivers}</Bold>
            <MultiSelect
              placeholder={i18n.search}
              value={selectedReceivers}
              options={receiverOptions}
              onChange={updateReceiverTree}
              noOptionsMessage={i18n.noResults}
              getOptionId={({ value }) => value}
              getOptionLabel={({ label }) => label}
              data-qa="select-receiver"
            />
          </HorizontalField>
          <Gap size="s" />
          <HorizontalField>
            <Bold>{i18n.title}</Bold>
            <InputField
              value={message.title ?? ''}
              onChange={(title) => updateMessage({ title })}
              data-qa="input-title"
            />
          </HorizontalField>
          <Gap size="s" />
          <Bold>{i18n.type.label}</Bold>
          <Gap size="xs" />
          <FixedSpaceRow>
            <Radio
              label={i18n.type.message}
              checked={message.type === 'MESSAGE'}
              onChange={() => updateMessage({ type: 'MESSAGE' })}
            />
            <Radio
              label={i18n.type.bulletin}
              checked={message.type === 'BULLETIN'}
              onChange={() => updateMessage({ type: 'BULLETIN' })}
            />
          </FixedSpaceRow>
          <Gap size="m" />
          <Bold>{i18n.message}</Bold>
          <Gap size="xs" />
          <StyledTextArea
            value={message.content}
            onChange={(e) => updateMessage({ content: e.target.value })}
            data-qa="input-content"
          />
          {attachmentsEnabled && (
            <FileUpload
              slim
              disabled={!draftId}
              data-qa="upload-message-attachment"
              files={message.attachments}
              i18n={i18nWithReplacedTitle}
              onDownloadFile={getAttachmentBlob}
              onUpload={handleAttachmentUpload}
              onDelete={handleAttachmentDelete}
            />
          )}
          <Gap size="L" />
        </ScrollableFormArea>
        <BottomBar>
          {draftId ? (
            <InlineButton
              onClick={() => onDiscard(message.sender.value, draftId)}
              text={i18n.deleteDraft}
              icon={faTrash}
              data-qa="discard-draft-btn"
            />
          ) : (
            <Gap horizontal />
          )}
          <Button
            text={sending ? i18n.sending : i18n.send}
            primary
            disabled={!sendEnabled}
            onClick={sendHandler}
            data-qa="send-message-btn"
          />
        </BottomBar>
      </Container>
    </FullScreenContainer>
  )
})

const FullScreenContainer = styled.div`
  position: fixed;
  top: ${defaultMargins.s};
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  z-index: 9999;

  &.fullscreen {
    top: 0;
    bottom: 0;
    right: 0;
    left: 0;
  }
`

const Container = styled.div`
  width: 680px;
  height: 100%;
  position: absolute;
  z-index: ${modalZIndex - 1};
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  overflow: auto;

  &.fullscreen {
    width: 100%;
  }
`

const ContainerMobile = styled.div`
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  overflow: auto;
  height: 100vh;
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${(p) => p.theme.colors.main.m2};
  color: ${(p) => p.theme.colors.grayscale.g0};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const TopBarMobile = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  color: ${(p) => p.theme.colors.main.m2};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const HeaderButtonContainer = styled.div`
  width: 70px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const MessageAreaMobile = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  min-height: 100px;
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
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.xs};
`

const BottomBarMobile = styled.div`
  width: 100%;
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const HorizontalField = styled.div`
  display: flex;
  align-items: center;

  & > * {
    flex: 1 1 auto;
  }

  & > :nth-child(1) {
    flex: 0 0 auto;
    width: 130px;
  }
`
