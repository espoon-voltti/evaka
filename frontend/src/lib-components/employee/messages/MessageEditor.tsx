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
  UpdatableDraftContent,
  MessageReceiversResponse
} from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TreeDropdown from 'lib-components/atoms/dropdowns/TreeDropdown'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import {
  getSelected,
  receiversAsSelectorNode,
  SelectorNode
} from 'lib-components/employee/messages/SelectorNode'
import { SaveDraftParams } from 'lib-components/employee/messages/types'
import { Draft, useDraft } from 'lib-components/employee/messages/useDraft'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import FileUpload, { FileUploadI18n } from 'lib-components/molecules/FileUpload'
import { SelectOption } from 'lib-components/molecules/Select'
import { Bold } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faDownLeftAndUpRightToCenter,
  faTimes,
  faTrash,
  faUpRightAndDownLeftFromCenter
} from 'lib-icons'

import Combobox from '../../atoms/dropdowns/Combobox'
import Checkbox from '../../atoms/form/Checkbox'
import { InfoBox } from '../../molecules/MessageBoxes'

type Message = Omit<
  UpdatableDraftContent,
  'recipientIds' | 'recipientNames'
> & {
  sender: SelectOption
  attachments: Attachment[]
}

const messageToUpdatableDraftWithAccount = (
  m: Message,
  recipients: { key: string; text: string }[]
): Draft => ({
  content: m.content,
  urgent: m.urgent,
  recipientIds: recipients.map(({ key }) => key),
  recipientNames: recipients.map(({ text }) => text),
  title: m.title,
  type: m.type,
  accountId: m.sender.value
})

const getEmptyMessage = (sender: SelectOption, title: string): Message => ({
  sender,
  title,
  content: '',
  urgent: false,
  attachments: [],
  type: 'MESSAGE' as const
})

const getInitialMessage = (
  draft: DraftContent | undefined,
  sender: SelectOption,
  title: string
): Message => (draft ? { ...draft, sender } : getEmptyMessage(sender, title))

const areRequiredFieldsFilled = (
  msg: Message,
  recipients: { key: UUID }[]
): boolean => !!(recipients.length > 0 && msg.type && msg.content && msg.title)

export interface MessageEditorI18n {
  newMessage: string
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
  urgent: {
    heading: string
    info: string
    label: string
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
  close: string
  open: string
  treeDropdown: {
    expandDropdown: string
    expand: (opt: string) => string
    collapse: (opt: string) => string
    placeholder: string
  }
}

interface Props {
  availableReceivers: MessageReceiversResponse[]
  defaultSender: SelectOption
  deleteAttachment: (id: UUID) => Promise<Result<void>>
  draftContent?: DraftContent
  getAttachmentUrl: (attachmentId: UUID, fileName: string) => string
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
  sending: boolean
  defaultTitle?: string
  simpleMode?: boolean
}

export default React.memo(function MessageEditor({
  availableReceivers,
  defaultSender,
  deleteAttachment,
  draftContent,
  getAttachmentUrl,
  i18n,
  initDraftRaw,
  mobileVersion = false,
  accounts,
  onClose,
  onDiscard,
  onSend,
  saveDraftRaw,
  saveMessageAttachment,
  sending,
  defaultTitle = ''
}: Props) {
  const [receiverTree, setReceiverTree] = useState<SelectorNode[]>(
    receiversAsSelectorNode(defaultSender.value, availableReceivers)
  )
  const [message, setMessage] = useState<Message>(() =>
    getInitialMessage(draftContent, defaultSender, defaultTitle)
  )
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
  const updateMessage = useCallback<UpdateStateFn<Message>>(
    (changes) => {
      const updatedMessage = { ...message, ...changes }
      setMessage(updatedMessage)
      const selectedReceivers = getSelected(receiverTree)
      setDraft(
        messageToUpdatableDraftWithAccount(updatedMessage, selectedReceivers)
      )
    },
    [message, receiverTree, setDraft]
  )
  const getSenderAccount = useCallback(
    (senderId: string) =>
      accounts.find(({ account }) => account.id === senderId),
    [accounts]
  )
  const senderAccountType = useMemo(
    () => getSenderAccount(message.sender.value)?.account.type,
    [getSenderAccount, message]
  )
  const simpleMode = useMemo(
    () => senderAccountType === 'SERVICE_WORKER',
    [senderAccountType]
  )

  const setSender = useCallback(
    (sender: SelectOption | null) => {
      if (!sender) return
      updateMessage({ sender })

      const accountReceivers = receiversAsSelectorNode(
        sender.value,
        availableReceivers
      )
      if (accountReceivers) {
        setReceiverTree(accountReceivers)
      }
    },
    [availableReceivers, updateMessage]
  )
  const selectedReceivers = useMemo(
    () => (receiverTree ? getSelected(receiverTree) : []),
    [receiverTree]
  )
  const updateReceivers = useCallback((receivers: SelectorNode[]) => {
    setReceiverTree(receivers)
    const selected = getSelected(receivers)
    setMessage((old) => ({
      ...old,
      recipientIds: selected.map((s) => s.key),
      recipientNames: selected.map((s) => s.text)
    }))
  }, [])

  const [expandedView, setExpandedView] = useState(false)
  const toggleExpandedView = useCallback(
    () => setExpandedView((prev) => !prev),
    []
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

  const debouncedSaveStatus = useDebounce(saveStatus, 250)
  const title = debouncedSaveStatus || message.title || i18n.newMessage

  const sendHandler = useCallback(() => {
    const {
      attachments,
      sender: { value: senderId },
      ...rest
    } = message
    const attachmentIds = attachments.map(({ id }) => id)
    onSend(senderId, {
      ...rest,
      attachmentIds,
      draftId,
      recipients: selectedReceivers.map(
        ({ messageRecipient }) => messageRecipient
      ),
      recipientNames: selectedReceivers.map(({ text: name }) => name),
      relatedApplicationId: null
    })
  }, [onSend, message, selectedReceivers, draftId])

  const handleAttachmentUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) =>
      draftId
        ? (await saveMessageAttachment(draftId, file, onUploadProgress)).map(
            (id) => {
              updateMessage({
                attachments: [
                  ...message.attachments,
                  { id, name: file.name, contentType: file.type }
                ]
              })
              return id
            }
          )
        : Failure.of<UUID>({ message: 'Should not happen' }),
    [draftId, message.attachments, saveMessageAttachment, updateMessage]
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
      accounts.map(({ account: { id, name } }: AuthorizedMessageAccount) => ({
        value: id,
        label: name
      })),
    [accounts]
  )

  useEffect(() => {
    if (senderAccountType === 'MUNICIPAL' && message.type !== 'BULLETIN') {
      updateMessage({ type: 'BULLETIN' })
    }
  }, [senderAccountType, message.type, updateMessage])

  const sendEnabled =
    !sending &&
    draftState === 'clean' &&
    areRequiredFieldsFilled(message, selectedReceivers)

  const urgent = (
    <Checkbox
      data-qa="checkbox-urgent"
      label={i18n.urgent.label}
      checked={message.urgent}
      onChange={(urgent) => updateMessage({ urgent })}
    />
  )

  function desktopEditor() {
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
    const messageType =
      senderAccountType === 'MUNICIPAL' ? (
        <FixedSpaceRow>
          <Radio
            label={i18n.type.bulletin}
            checked={message.type === 'BULLETIN'}
            onChange={() => updateMessage({ type: 'BULLETIN' })}
            data-qa="radio-message-type-bulletin"
          />
        </FixedSpaceRow>
      ) : (
        <FixedSpaceRow>
          <Radio
            label={i18n.type.message}
            checked={message.type === 'MESSAGE'}
            onChange={() => updateMessage({ type: 'MESSAGE' })}
            data-qa="radio-message-type-message"
          />
          <Radio
            label={i18n.type.bulletin}
            checked={message.type === 'BULLETIN'}
            onChange={() => updateMessage({ type: 'BULLETIN' })}
            data-qa="radio-message-type-bulletin"
          />
        </FixedSpaceRow>
      )

    return (
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
                  aria-label={i18n.open}
                />
              ) : (
                <IconButton
                  icon={faUpRightAndDownLeftFromCenter}
                  onClick={toggleExpandedView}
                  white
                  size="s"
                  data-qa="expand-view-btn"
                  aria-label={i18n.close}
                />
              )}
              <IconButton
                icon={faTimes}
                onClick={onCloseHandler}
                white
                size="m"
                data-qa="close-message-editor-btn"
                aria-label={i18n.close}
              />
            </HeaderButtonContainer>
          </TopBar>
          <ScrollableFormArea>
            <ExpandableLayout expandedView={expandedView}>
              <Dropdowns expandedView={expandedView}>
                <HorizontalField>
                  <Bold>{i18n.sender}</Bold>
                  <Combobox
                    items={senderOptions}
                    onChange={setSender}
                    selectedItem={message.sender}
                    getItemLabel={(sender) => sender.label}
                    data-qa="select-sender"
                    fullWidth
                  />
                </HorizontalField>
                <Gap size="s" />
                <HorizontalField>
                  <Bold>{i18n.receivers}</Bold>
                  <TreeDropdown
                    tree={receiverTree}
                    onChange={updateReceivers}
                    labels={i18n.treeDropdown}
                    placeholder={i18n.treeDropdown.placeholder}
                    data-qa="select-receiver"
                  />
                </HorizontalField>
              </Dropdowns>
              {expandedView && !simpleMode && (
                <ExpandedRightPane>
                  <HorizontalField long={true}>
                    <Bold>{i18n.type.label}</Bold>
                    {messageType}
                  </HorizontalField>
                  <Gap size="s" />
                  <HorizontalField long={true}>
                    <Bold>{i18n.urgent.heading}</Bold>
                    {urgent}
                  </HorizontalField>
                  {message.urgent && (
                    <>
                      <Gap size="s" />
                      <InfoBox message={i18n.urgent.info} noMargin={true} />
                    </>
                  )}
                </ExpandedRightPane>
              )}
            </ExpandableLayout>
            <Gap size="s" />
            <HorizontalField>
              <Bold>{i18n.title}</Bold>
              <InputField
                value={message.title ?? ''}
                onChange={(title) => updateMessage({ title })}
                data-qa="input-title"
              />
            </HorizontalField>
            {!expandedView && !simpleMode && (
              <>
                <Gap size="s" />
                <FixedSpaceRow justifyContent="space-between">
                  <div>
                    <Bold>{i18n.type.label}</Bold>
                    <Gap size="xs" />
                    {messageType}
                  </div>
                  <div>
                    <Bold>{i18n.urgent.heading}</Bold>
                    <Gap size="xs" />
                    {urgent}
                  </div>
                </FixedSpaceRow>
                {message.urgent && (
                  <>
                    <Gap size="s" />
                    <InfoBox message={i18n.urgent.info} noMargin={true} />
                  </>
                )}
              </>
            )}
            <Gap size="m" />
            <Bold>{i18n.message}</Bold>
            <Gap size="xs" />
            <StyledTextArea
              value={message.content}
              onChange={(e) => updateMessage({ content: e.target.value })}
              data-qa="input-content"
            />
            {!simpleMode && (
              <FileUpload
                slim
                disabled={!draftId}
                data-qa="upload-message-attachment"
                files={message.attachments}
                i18n={i18nWithReplacedTitle}
                getDownloadUrl={getAttachmentUrl}
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
  }

  return mobileVersion ? (
    <ContainerMobile data-qa="message-editor" data-status={draftState}>
      <TopBarMobile>
        <Title>{title}</Title>
        <IconButton
          icon={faTimes}
          onClick={onCloseHandler}
          data-qa="close-message-editor-btn"
          aria-label={i18n.close}
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
        <TreeDropdown
          tree={receiverTree}
          onChange={updateReceivers}
          data-qa="attendees"
          labels={i18n.treeDropdown}
          placeholder={i18n.treeDropdown.placeholder}
        />
        <Gap size="s" />
        <Bold>{i18n.urgent.heading}</Bold>
        {urgent}
        {message.urgent && (
          <>
            <Gap size="s" />
            <InfoBox message={i18n.urgent.info} noMargin={true} />
          </>
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
    desktopEditor()
  )
})

const FullScreenContainer = styled.div`
  position: fixed;
  top: ${defaultMargins.s};
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  z-index: 39;

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

const HorizontalField = styled.div<{ long?: boolean }>`
  display: flex;
  align-items: center;

  & > * {
    flex: 1 1 auto;
  }

  & > :nth-child(1) {
    flex: 0 0 auto;
    width: ${(props) => (props.long ? '200px' : '130px')};
  }
`

const ExpandableLayout = styled.div<{ expandedView: boolean }>`
  display: ${(props) => (props.expandedView ? 'flex' : 'block')};
`

const Dropdowns = styled.div<{ expandedView: boolean }>`
  ${(props) => (props.expandedView ? 'width: 66%' : '')}
  flex: 1 1 auto;
`

const ExpandedRightPane = styled.div`
  margin-left: ${defaultMargins.XL};
  width: 33%;
  flex: 1 1 auto;
`
