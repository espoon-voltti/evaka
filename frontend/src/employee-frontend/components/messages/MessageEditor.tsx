// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import isEqual from 'lodash/isEqual'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure, Result } from 'lib-common/api'
import { Attachment } from 'lib-common/api-types/attachment'
import { useBoolean } from 'lib-common/form/hooks'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  AuthorizedMessageAccount,
  DraftContent,
  MessageReceiversResponse,
  PostMessageBody,
  PostMessageFilters,
  UpdatableDraftContent
} from 'lib-common/generated/api-types/messaging'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import TreeDropdown, {
  TreeNode
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import Checkbox from 'lib-components/atoms/form/Checkbox'
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
import {
  ExpandingInfoBox,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import FileUpload from 'lib-components/molecules/FileUpload'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { SelectOption } from 'lib-components/molecules/Select'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Bold } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import {
  faChevronDown,
  faChevronUp,
  faDownLeftAndUpRightToCenter,
  faQuestion,
  faTimes,
  faTrash,
  faUpRightAndDownLeftFromCenter
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { createMessagePreflightCheckQuery } from './queries'

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

type Filters = PostMessageFilters

const getEmptyFilters = (): Filters => ({
  yearsOfBirth: [],
  shiftCare: false,
  intermittentShiftCare: false,
  familyDaycare: false
})

interface FlagProps {
  urgent: boolean
  sensitive: boolean
}

const FlagsInfoContent = React.memo(function FlagsInfoContent({
  urgent,
  sensitive
}: FlagProps) {
  const { i18n } = useTranslation()
  // If only one of the flags is present
  if (urgent !== sensitive) {
    if (urgent) return <>{i18n.messages.messageEditor.flags.urgent.info}</>
    if (sensitive)
      return <>{i18n.messages.messageEditor.flags.sensitive.info}</>
    return null
  }

  // If both flags are present
  return (
    <UlNoMargin>
      <li>{i18n.messages.messageEditor.flags.urgent.info}</li>
      <li>{i18n.messages.messageEditor.flags.sensitive.info}</li>
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
  const { i18n } = useTranslation()

  const [receiverTree, setReceiverTree] = useState<SelectorNode[]>(
    receiversAsSelectorNode(
      defaultSender.value,
      availableReceivers,
      draftContent?.recipientIds
    )
  )
  const [filtersVisible, useFiltersVisible] = useBoolean(false)
  const [yearOfBirthTree, setYearOfBirthTree] = useState<TreeNode[]>(
    [...Array(8).keys()].map<TreeNode>((n) => ({
      text: LocalDate.todayInHelsinkiTz().year - n + '',
      key: LocalDate.todayInHelsinkiTz().year - n + '',
      checked: false,
      children: []
    }))
  )
  const [message, setMessage] = useState<Message>(() =>
    getInitialMessage(draftContent, defaultSender, defaultTitle)
  )
  const [filters, setFilters] = useState<Filters>(() => getEmptyFilters())
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
  const debouncedReceivers = useDebounce(selectedReceivers, 500)
  const preflightResult = useQueryResult(
    createMessagePreflightCheckQuery({
      accountId: message.sender.value,
      body: {
        recipients: debouncedReceivers.map((r) => r.messageRecipient),
        filters: filters
      }
    })
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
  const handleBirthYearChange = useCallback(
    (birthYears: TreeNode[]) => {
      setYearOfBirthTree(birthYears)
      const selected = birthYears.filter((year) => year.checked)

      const updatedFilters = {
        ...filters,
        yearsOfBirth: selected.map((y) => +y.key)
      }
      setFilters(updatedFilters)
    },
    [filters, setFilters]
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
    debouncedSaveStatus ||
    message.title ||
    i18n.messages.messageEditor.newMessage

  const [confirmLargeSend, setConfirmLargeSend] = useState(false)

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
      relatedApplicationId: null,
      filters: filters
    })
  }, [onSend, message, selectedReceivers, draftId, filters])

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
    areRequiredFieldsFilled(message, selectedReceivers) &&
    isEqual(debouncedReceivers, selectedReceivers) &&
    preflightResult.isSuccess

  const shiftCareCheckBox = (
    <Checkbox
      data-qa="checkbox-shiftcare"
      label={i18n.messages.messageEditor.filters.shiftCare.label}
      checked={filters.shiftCare}
      onChange={(shiftCare) => setFilters({ ...filters, shiftCare: shiftCare })}
    />
  )

  const intermittentShiftCareCheckBox = (
    <Checkbox
      data-qa="checkbox-intermittent-shiftcare"
      label={i18n.messages.messageEditor.filters.shiftCare.intermittent}
      checked={filters.intermittentShiftCare}
      onChange={(intermittentShiftCare) =>
        setFilters({ ...filters, intermittentShiftCare: intermittentShiftCare })
      }
    />
  )

  const familyDaycareCheckBox = (
    <Checkbox
      data-qa="checkbox-family-daycare"
      label={i18n.messages.messageEditor.filters.familyDaycare.label}
      checked={filters.familyDaycare}
      onChange={(familyDaycare) =>
        setFilters({ ...filters, familyDaycare: familyDaycare })
      }
    />
  )

  const urgent = (
    <Checkbox
      data-qa="checkbox-urgent"
      label={i18n.messages.messageEditor.flags.urgent.label}
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
        label={i18n.messages.messageEditor.flags.sensitive.label}
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
        label={i18n.messages.messageEditor.type.bulletin}
        checked={message.type === 'BULLETIN'}
        onChange={() => updateMessage({ type: 'BULLETIN' })}
        data-qa="radio-message-type-bulletin"
      />
    ) : (
      <>
        <Radio
          label={i18n.messages.messageEditor.type.message}
          checked={message.type === 'MESSAGE'}
          onChange={() => updateMessage({ type: 'MESSAGE' })}
          data-qa="radio-message-type-message"
        />
        <Radio
          label={i18n.messages.messageEditor.type.bulletin}
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
              <IconOnlyButton
                icon={faDownLeftAndUpRightToCenter}
                onClick={toggleExpandedView}
                color="white"
                size="s"
                data-qa="collapse-view-btn"
                aria-label={i18n.common.open}
              />
            ) : (
              <IconOnlyButton
                icon={faUpRightAndDownLeftFromCenter}
                onClick={toggleExpandedView}
                color="white"
                size="s"
                data-qa="expand-view-btn"
                aria-label={i18n.common.close}
              />
            )}
            <IconOnlyButton
              icon={faTimes}
              onClick={onCloseHandler}
              color="white"
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
                <Bold>{i18n.messages.messageEditor.sender}</Bold>
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
                <Bold>{i18n.messages.messageEditor.recipients}</Bold>
                <TreeDropdown
                  tree={receiverTree}
                  onChange={handleRecipientChange}
                  placeholder={i18n.messages.messageEditor.selectPlaceholder}
                  data-qa="select-receiver"
                />
              </HorizontalField>
            </Dropdowns>
            {expandedView && !simpleMode && (
              <ExpandedRightPane>
                <ExpandedHorizontalField>
                  <Bold>{i18n.messages.messageEditor.type.label}</Bold>
                  {messageType}
                </ExpandedHorizontalField>
                <Gap size="s" />
                <ExpandedHorizontalField>
                  <Bold>{i18n.messages.messageEditor.flags.heading}</Bold>
                  {urgent}
                  {sensitiveCheckbox}
                </ExpandedHorizontalField>
                {sensitiveInfoOpen && (
                  <FixedSpaceRow fullWidth>
                    <ExpandingInfoBox
                      width="auto"
                      info={
                        i18n.messages.messageEditor.flags.sensitive.whyDisabled
                      }
                      close={onSensitiveInfoClick}
                    />
                  </FixedSpaceRow>
                )}
                {flagsInfo}
              </ExpandedRightPane>
            )}
          </ExpandableLayout>
          {(senderAccountType === 'PERSONAL' ||
            senderAccountType === 'MUNICIPAL') && (
            <>
              <Gap size="s" />
              <RightAlignedRow>
                <Button
                  appearance="inline"
                  order="text-icon"
                  data-qa="filters-btn"
                  onClick={useFiltersVisible.toggle}
                  text={
                    filtersVisible
                      ? i18n.messages.messageEditor.filters.hideFilters
                      : i18n.messages.messageEditor.filters.showFilters
                  }
                  icon={filtersVisible ? faChevronUp : faChevronDown}
                />
              </RightAlignedRow>
              {filtersVisible && (
                <>
                  <Gap size="s" />
                  <ExpandableLayout expandedView={expandedView}>
                    <Dropdowns expandedView={expandedView}>
                      <HorizontalField>
                        <Bold>
                          {i18n.messages.messageEditor.filters.yearOfBirth}
                        </Bold>
                        <TreeDropdown
                          tree={yearOfBirthTree}
                          onChange={handleBirthYearChange}
                          placeholder={
                            i18n.messages.messageEditor.selectPlaceholder
                          }
                          data-qa="select-years-of-birth"
                        />
                      </HorizontalField>
                    </Dropdowns>
                    {expandedView && !simpleMode && (
                      <ExpandedRightPane>
                        <ExpandedHorizontalField>
                          <Bold>
                            {
                              i18n.messages.messageEditor.filters.shiftCare
                                .heading
                            }
                          </Bold>
                          {shiftCareCheckBox}
                          {featureFlags.intermittentShiftCare
                            ? intermittentShiftCareCheckBox
                            : null}
                        </ExpandedHorizontalField>
                        <Gap size="s" />
                        <ExpandedHorizontalField>
                          <Bold>
                            {
                              i18n.messages.messageEditor.filters.familyDaycare
                                .heading
                            }
                          </Bold>
                          {familyDaycareCheckBox}
                        </ExpandedHorizontalField>
                      </ExpandedRightPane>
                    )}
                    {!expandedView && !simpleMode && (
                      <>
                        <Gap size="s" />
                        <HorizontalField>
                          <Bold>
                            {
                              i18n.messages.messageEditor.filters.shiftCare
                                .heading
                            }
                          </Bold>
                          {shiftCareCheckBox}
                          {featureFlags.intermittentShiftCare
                            ? intermittentShiftCareCheckBox
                            : null}
                        </HorizontalField>
                        <Gap size="s" />
                        <HorizontalField>
                          <Bold>
                            {
                              i18n.messages.messageEditor.filters.familyDaycare
                                .heading
                            }
                          </Bold>
                          {familyDaycareCheckBox}
                        </HorizontalField>
                      </>
                    )}
                  </ExpandableLayout>
                  <HorizontalLine slim />
                </>
              )}
            </>
          )}
          <Gap size="s" />
          <HorizontalField>
            <Bold>{i18n.messages.messageEditor.title}</Bold>
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
                  <Bold>{i18n.messages.messageEditor.type.label}</Bold>
                  {messageType}
                </HalfWidthColumn>
                <HalfWidthColumn>
                  <Bold>{i18n.messages.messageEditor.flags.heading}</Bold>
                  {urgent}
                  {sensitiveCheckbox}
                </HalfWidthColumn>
              </FixedSpaceRow>
              {sensitiveInfoOpen && !sensitiveCheckboxEnabled && (
                <InfoBoxContainer>
                  <ExpandingInfoBox
                    width="full"
                    info={
                      i18n.messages.messageEditor.flags.sensitive.whyDisabled
                    }
                    close={onSensitiveInfoClick}
                  />
                </InfoBoxContainer>
              )}
              {flagsInfo}
            </>
          )}
          <Gap size="m" />
          <Bold>{i18n.messages.messageEditor.message}</Bold>
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
            <Button
              appearance="inline"
              onClick={() => onDiscard(message.sender.value, draftId)}
              text={i18n.messages.messageEditor.deleteDraft}
              icon={faTrash}
              data-qa="discard-draft-btn"
            />
          ) : (
            <Gap horizontal />
          )}
          <FixedSpaceRow alignItems="center">
            <div>
              {i18n.messages.messageEditor.recipientCount}:{' '}
              {preflightResult
                .map((r) => r.numberOfRecipientAccounts)
                .getOrElse('')}
            </div>
            <LegacyButton
              text={
                sending
                  ? i18n.messages.messageEditor.sending
                  : i18n.messages.messageEditor.send
              }
              primary
              disabled={!sendEnabled}
              onClick={
                preflightResult
                  .map((r) => r.numberOfRecipientAccounts > 2)
                  .getOrElse(false)
                  ? () => setConfirmLargeSend(true)
                  : sendHandler
              }
              data-qa="send-message-btn"
            />
          </FixedSpaceRow>
        </BottomBar>

        {confirmLargeSend && preflightResult.isSuccess && (
          <InfoModal
            type="warning"
            icon={faQuestion}
            title={i18n.messages.messageEditor.manyRecipientsWarning.title}
            text={i18n.messages.messageEditor.manyRecipientsWarning.text(
              preflightResult.value.numberOfRecipientAccounts
            )}
            close={() => setConfirmLargeSend(false)}
            closeLabel={i18n.common.cancel}
            resolve={{
              label: i18n.messages.messageEditor.send,
              action: () => {
                sendHandler()
                setConfirmLargeSend(false)
              }
            }}
            reject={{
              label: i18n.common.cancel,
              action: () => setConfirmLargeSend(false)
            }}
          />
        )}
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
  ${(props) => (props.expandedView ? 'width: 66%;' : '')}
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
const RightAlignedRow = styled.div`
  display: flex;
  justify-content: end;
`
