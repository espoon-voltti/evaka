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
  AuthorizedMessageAccount,
  DraftContent,
  MessageReceiversResponse,
  PostMessageBody,
  UpdatableDraftContent
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
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import {
  getSelected,
  receiversAsSelectorNode,
  SelectedNode,
  SelectorNode
} from 'lib-components/messages/SelectorNode'
import { SaveDraftParams } from 'lib-components/messages/types'
import { Draft, useDraft } from 'lib-components/messages/useDraft'
import FileUpload from 'lib-components/molecules/FileUpload'
import { SelectOption } from 'lib-components/molecules/Select'
import { Bold } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faDownLeftAndUpRightToCenter,
  faTimes,
  faTrash,
  faUpRightAndDownLeftFromCenter
} from 'lib-icons'

import Combobox from '../atoms/dropdowns/Combobox'
import Checkbox from '../atoms/form/Checkbox'
import { useTranslations } from '../i18n'
import { ExpandingInfoBox, InlineInfoButton } from '../molecules/ExpandingInfo'
import { InfoBox } from '../molecules/MessageBoxes'

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
  sensitive: false,
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
  sensitive: false,
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

const shouldSensitiveCheckboxBeEnabled = (
  selectedReceivers: SelectedNode[],
  messageType: string,
  senderAccountType: string | undefined
) => {
  const recipientValid =
    selectedReceivers.length == 1 &&
    selectedReceivers[0].messageRecipient.type === 'CHILD'
  if (!recipientValid) {
    return false
  }
  if (messageType === 'BULLETIN') {
    return false
  }
  return senderAccountType === 'PERSONAL'
}

interface FlagProps {
  urgent: boolean
  sensitive: boolean
}

const FlagsInfoContent = React.memo(function FlagsInfoContent({
  urgent,
  sensitive
}: FlagProps) {
  const i18n = useTranslations()
  // If only one of the flags is present
  if (urgent !== sensitive) {
    if (urgent) return <>{i18n.messageEditor.flags.urgent.info}</>
    if (sensitive) return <>{i18n.messageEditor.flags.sensitive.info}</>
    return null
  }

  // If both flags are present
  return (
    <UlNoMargin>
      <li>{i18n.messageEditor.flags.urgent.info}</li>
      <li>{i18n.messageEditor.flags.sensitive.info}</li>
    </UlNoMargin>
  )
})

interface Props {
  availableReceivers: MessageReceiversResponse[]
  defaultSender: SelectOption
  deleteAttachment: (arg: { attachmentId: UUID }) => Promise<Result<void>>
  draftContent?: DraftContent
  getAttachmentUrl: (attachmentId: UUID, fileName: string) => string
  initDraftRaw: (accountId: string) => Promise<Result<string>>
  accounts: AuthorizedMessageAccount[]
  onClose: (didChanges: boolean) => void
  onDiscard: (accountId: UUID, draftId: UUID) => void
  onSend: (accountId: UUID, msg: PostMessageBody) => void
  saveDraftRaw: (params: SaveDraftParams) => Promise<Result<void>>
  saveMessageAttachment: (
    draftId: UUID,
    file: File,
    onUploadProgress: (percentage: number) => void
  ) => Promise<Result<UUID>>
  sending: boolean
  defaultTitle?: string
}

export default React.memo(function MessageEditor({
  availableReceivers,
  defaultSender,
  deleteAttachment,
  draftContent,
  getAttachmentUrl,
  initDraftRaw,
  accounts,
  onClose,
  onDiscard,
  onSend,
  saveDraftRaw,
  saveMessageAttachment,
  sending,
  defaultTitle = ''
}: Props) {
  const i18n = useTranslations()

  const [receiverTree, setReceiverTree] = useState<SelectorNode[]>(
    receiversAsSelectorNode(
      defaultSender.value,
      availableReceivers,
      draftContent?.recipientIds
    )
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

  const selectedReceivers = useMemo(
    () => (receiverTree ? getSelected(receiverTree) : []),
    [receiverTree]
  )

  const sensitiveCheckboxEnabled = shouldSensitiveCheckboxBeEnabled(
    selectedReceivers,
    message.type,
    senderAccountType
  )

  const handleSenderChange = useCallback(
    (sender: SelectOption | null) => {
      const shouldResetSensitivity = !shouldSensitiveCheckboxBeEnabled(
        selectedReceivers,
        message.type,
        sender ? sender.value : undefined
      )
      if (!sender) {
        if (shouldResetSensitivity) {
          updateMessage({ sensitive: false })
        }
        return
      }
      if (shouldResetSensitivity) {
        updateMessage({ sender, sensitive: false })
      } else {
        updateMessage({ sender })
      }

      const accountReceivers = receiversAsSelectorNode(
        sender.value,
        availableReceivers
      )
      if (accountReceivers) {
        setReceiverTree(accountReceivers)
      }
    },
    [availableReceivers, message.type, selectedReceivers, updateMessage]
  )
  const handleRecipientChange = useCallback(
    (recipients: SelectorNode[]) => {
      setReceiverTree(recipients)
      const selected = getSelected(recipients)

      const shouldResetSensitivity = !shouldSensitiveCheckboxBeEnabled(
        selected,
        message.type,
        senderAccountType
      )

      const updatedMessage = {
        ...message,
        recipientIds: selected.map((s) => s.key),
        recipientNames: selected.map((s) => s.text),
        sensitive: shouldResetSensitivity ? false : message.sensitive
      }
      setMessage(updatedMessage)
      setDraft(messageToUpdatableDraftWithAccount(updatedMessage, selected))
    },
    [message, senderAccountType, setDraft]
  )

  const [expandedView, setExpandedView] = useState(false)
  const toggleExpandedView = useCallback(
    () => setExpandedView((prev) => !prev),
    []
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

  const debouncedSaveStatus = useDebounce(saveStatus, 250)
  const title =
    debouncedSaveStatus || message.title || i18n.messageEditor.newMessage

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
    async (file: File, onUploadProgress: (percentage: number) => void) =>
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
      (await deleteAttachment({ attachmentId: id })).map(() =>
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
      label={i18n.messageEditor.flags.urgent.label}
      checked={message.urgent}
      onChange={(urgent) => updateMessage({ urgent })}
    />
  )

  const [sensitiveInfoOpen, setSensitiveInfoOpen] = useState(false)
  const onSensitiveInfoClick = useCallback(
    () => setSensitiveInfoOpen((prev) => !prev),
    []
  )

  const sensitiveCheckbox = (
    <FixedSpaceRow spacing="xs" alignItems="center">
      <Checkbox
        data-qa="checkbox-sensitive"
        label={i18n.messageEditor.flags.sensitive.label}
        checked={message.sensitive}
        disabled={!sensitiveCheckboxEnabled}
        onChange={(sensitive) => updateMessage({ sensitive })}
      />
      {!sensitiveCheckboxEnabled && (
        <InlineInfoButton
          onClick={onSensitiveInfoClick}
          aria-label={i18n.common.openExpandingInfo}
          margin="zero"
          data-qa="sensitive-flag-info-button"
          open={sensitiveInfoOpen}
        />
      )}
    </FixedSpaceRow>
  )

  const messageType =
    senderAccountType === 'MUNICIPAL' ? (
      <Radio
        label={i18n.messageEditor.type.bulletin}
        checked={message.type === 'BULLETIN'}
        onChange={() => updateMessage({ type: 'BULLETIN' })}
        data-qa="radio-message-type-bulletin"
      />
    ) : (
      <>
        <Radio
          label={i18n.messageEditor.type.message}
          checked={message.type === 'MESSAGE'}
          onChange={() => updateMessage({ type: 'MESSAGE' })}
          data-qa="radio-message-type-message"
        />
        <Radio
          label={i18n.messageEditor.type.bulletin}
          checked={message.type === 'BULLETIN'}
          onChange={() => updateMessage({ type: 'BULLETIN', sensitive: false })}
          data-qa="radio-message-type-bulletin"
        />
      </>
    )
  const flagsInfo = (message.urgent || message.sensitive) && (
    <>
      <Gap size="s" />
      <InfoBox
        noMargin={true}
        message={
          <FlagsInfoContent
            urgent={message.urgent}
            sensitive={message.sensitive}
          />
        }
      />
    </>
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
                aria-label={i18n.common.open}
              />
            ) : (
              <IconButton
                icon={faUpRightAndDownLeftFromCenter}
                onClick={toggleExpandedView}
                white
                size="s"
                data-qa="expand-view-btn"
                aria-label={i18n.common.close}
              />
            )}
            <IconButton
              icon={faTimes}
              onClick={onCloseHandler}
              white
              size="m"
              data-qa="close-message-editor-btn"
              aria-label={i18n.common.close}
            />
          </HeaderButtonContainer>
        </TopBar>
        <ScrollableFormArea>
          <ExpandableLayout expandedView={expandedView}>
            <Dropdowns expandedView={expandedView}>
              <HorizontalField>
                <Bold>{i18n.messageEditor.sender}</Bold>
                <Combobox
                  items={senderOptions}
                  onChange={handleSenderChange}
                  selectedItem={message.sender}
                  getItemLabel={(sender) => sender.label}
                  data-qa="select-sender"
                  fullWidth
                />
              </HorizontalField>
              <Gap size="s" />
              <HorizontalField>
                <Bold>{i18n.messages.recipients}</Bold>
                <TreeDropdown
                  tree={receiverTree}
                  onChange={handleRecipientChange}
                  placeholder={i18n.messageEditor.recipientsPlaceholder}
                  data-qa="select-receiver"
                />
              </HorizontalField>
            </Dropdowns>
            {expandedView && !simpleMode && (
              <ExpandedRightPane>
                <ExpandedHorizontalField>
                  <Bold>{i18n.messageEditor.type.label}</Bold>
                  {messageType}
                </ExpandedHorizontalField>
                <Gap size="s" />
                <ExpandedHorizontalField>
                  <Bold>{i18n.messageEditor.flags.heading}</Bold>
                  {urgent}
                  {sensitiveCheckbox}
                </ExpandedHorizontalField>
                {sensitiveInfoOpen && (
                  <FixedSpaceRow fullWidth>
                    <ExpandingInfoBox
                      width="auto"
                      info={i18n.messageEditor.flags.sensitive.whyDisabled}
                      close={onSensitiveInfoClick}
                    />
                  </FixedSpaceRow>
                )}
                {flagsInfo}
              </ExpandedRightPane>
            )}
          </ExpandableLayout>
          <Gap size="s" />
          <HorizontalField>
            <Bold>{i18n.messageEditor.title}</Bold>
            <InputField
              value={message.title ?? ''}
              onChange={(title) => updateMessage({ title })}
              data-qa="input-title"
            />
          </HorizontalField>
          {!expandedView && !simpleMode && (
            <>
              <Gap size="s" />
              <FixedSpaceRow>
                <HalfWidthColumn>
                  <Bold>{i18n.messageEditor.type.label}</Bold>
                  {messageType}
                </HalfWidthColumn>
                <HalfWidthColumn>
                  <Bold>{i18n.messageEditor.flags.heading}</Bold>
                  {urgent}
                  {sensitiveCheckbox}
                </HalfWidthColumn>
              </FixedSpaceRow>
              {sensitiveInfoOpen && !sensitiveCheckboxEnabled && (
                <InfoBoxContainer>
                  <ExpandingInfoBox
                    width="full"
                    info={i18n.messageEditor.flags.sensitive.whyDisabled}
                    close={onSensitiveInfoClick}
                  />
                </InfoBoxContainer>
              )}
              {flagsInfo}
            </>
          )}
          <Gap size="m" />
          <Bold>{i18n.messages.message}</Bold>
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
              text={i18n.messageEditor.deleteDraft}
              icon={faTrash}
              data-qa="discard-draft-btn"
            />
          ) : (
            <Gap horizontal />
          )}
          <Button
            text={sending ? i18n.messages.sending : i18n.messages.send}
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

const HeaderButtonContainer = styled.div`
  width: 70px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
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
const ExpandedHorizontalField = styled.div`
  display: flex;
  align-items: center;

  & > * {
    width: 150px;
  }

  & > :first-child {
    width: 190px;
  }

  & > :last-child {
    width: unset;
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

const HalfWidthColumn = styled(FixedSpaceColumn)`
  width: 50%;
`
const UlNoMargin = styled.ul`
  margin-block: 0;
  padding-inline-start: 16px;
`
const InfoBoxContainer = styled.div`
  overflow: none;
`
