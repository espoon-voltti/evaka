// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import FocusLock from 'react-focus-lock'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import type {
  CitizenMessageBody,
  GetRecipientsResponse,
  MessageAccount
} from 'lib-common/generated/api-types/messaging'
import type {
  MessageAccountId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useMutationResult } from 'lib-common/query'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { desktopMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import OutOfOfficeInfo from 'lib-components/messages/OutOfOfficeInfo'
import { ToggleableRecipient } from 'lib-components/messages/ToggleableRecipient'
import type { UploadStatus } from 'lib-components/molecules/FileUpload'
import FileUpload, {
  initialUploadStatus
} from 'lib-components/molecules/FileUpload'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { Bold, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { getAttachmentUrl, messageAttachment } from '../attachments/attachments'
import { deleteAttachmentMutation } from '../attachments/queries'
import { useUser } from '../auth/state'
import { ErrorMessageBox } from '../calendar/ChildSelector'
import { useTranslation } from '../localization'
import { getDuplicateChildInfo } from '../utils/duplicated-child-utils'

import { isPrimaryRecipient } from './utils'

const emptyMessage: CitizenMessageBody = {
  title: '',
  content: '',
  recipients: [],
  children: [],
  attachmentIds: []
}

interface Props {
  children_: ChildAndPermittedActions[]
  recipientOptions: GetRecipientsResponse
  messageAttachmentsAllowed: boolean
  onSend: (messageBody: CitizenMessageBody) => Promise<Result<unknown>>
  onSuccess: () => void
  onFailure: () => void
  onClose: () => void
  displaySendError: boolean
}

export default React.memo(function MessageEditor({
  children_,
  recipientOptions,
  messageAttachmentsAllowed,
  onSend,
  onSuccess,
  onFailure,
  onClose,
  displaySendError
}: Props) {
  const i18n = useTranslation()
  const user = useUser()

  const childIds = useMemo(
    () =>
      recipientOptions.childrenToMessageAccounts
        .filter((c) => c.childId !== null)
        .map((c) => c.childId!),
    [recipientOptions]
  )

  const [message, setMessage] = useState<CitizenMessageBody>(
    childIds.length === 1
      ? {
          ...emptyMessage,
          children: [childIds[0]]
        }
      : emptyMessage
  )
  const title = message.title || i18n.messages.messageEditor.newMessage

  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [uploadStatus, setUploadStatus] =
    useState<UploadStatus>(initialUploadStatus)

  const [isChildSelectionTouched, setChildSelectionTouched] = useBoolean(false)

  useEffect(
    () =>
      setMessage((prev) => ({
        ...prev,
        attachmentIds: attachments.map((a) => a.id)
      })),
    [attachments]
  )

  const { mutateAsync: deleteAttachment } = useMutationResult(
    deleteAttachmentMutation
  )

  const send = useCallback(() => onSend(message), [message, onSend])

  const selectedChildren = useMemo(
    () => children_.filter((c) => message.children.includes(c.id)),
    [children_, message.children]
  )
  const selectedChildrenInSameUnit = useMemo(
    () =>
      selectedChildren.length === 1 ||
      selectedChildren.every(
        (c) => c.unit !== null && c.unit.id === selectedChildren[0].unit?.id
      ),
    [selectedChildren]
  )
  const duplicateChildInfo = useMemo(
    () => getDuplicateChildInfo(children_, i18n),
    [children_, i18n]
  )

  const validAccounts = useMemo(() => {
    const newMessageAuthorized =
      (accountId: MessageAccountId) =>
      (childId: PersonId): boolean =>
        recipientOptions.childrenToMessageAccounts
          .find((value) => value.childId === childId)
          ?.newMessage.includes(accountId) ?? false

    // Can send to groups which are recipients for at least one of the selected children,
    // as long as all the children and thus groups are in the same unit.
    // For other recipient types, they must be valid recipient for ALL selected children.
    const accounts = recipientOptions.messageAccounts
      .filter(
        (account) =>
          selectedChildrenInSameUnit &&
          message.children.length > 0 &&
          (account.account.type === 'GROUP'
            ? message.children.some(newMessageAuthorized(account.account.id))
            : message.children.every(newMessageAuthorized(account.account.id)))
      )
      .map((withPresence) => withPresence.account)
    const [primary, secondary] = partition(accounts, isPrimaryRecipient)
    return { primary, secondary }
  }, [selectedChildrenInSameUnit, message.children, recipientOptions])

  const recipients = useMemo(() => {
    const isMessageRecipient = (account: MessageAccount) =>
      message.recipients.includes(account.id)
    return {
      primary: validAccounts.primary.filter(isMessageRecipient),
      secondary: validAccounts.secondary.filter(isMessageRecipient)
    }
  }, [message.recipients, validAccounts])

  const showSecondaryRecipientSelection = validAccounts.secondary.length > 0
  const sendEnabled =
    !!(recipients.primary.length > 0 && message.content && message.title) &&
    uploadStatus.inProgress === 0

  const required = (text: string) => `${text}*`

  const chipGroupContainerRef = React.useRef<HTMLDivElement>(null)

  const ChipContainer = styled(FixedSpaceFlexWrap)`
    margin-bottom: -${defaultMargins.xxs};

    > * {
      margin-bottom: ${defaultMargins.xxs};
    }
  `

  return (
    <ModalAccessibilityWrapper>
      <FocusLock>
        <Container data-qa="message-editor">
          <TopBar>
            <Title>{title}</Title>
            <IconOnlyButton
              icon={faTimes}
              onClick={() => onClose()}
              color="white"
              data-qa="close-message-editor-btn"
              aria-label={i18n.common.close}
            />
          </TopBar>
          <FormArea>
            {user && (
              <>
                <Bold>{i18n.messages.sender}</Bold>
                <Gap size="xs" />
                <P noMargin>
                  <PersonName person={user} format="First Last" />
                </P>
              </>
            )}
            <Gap size="s" />
            {childIds && childIds.length > 1 && (
              <>
                <label>
                  <Bold>{required(i18n.messages.messageEditor.children)}</Bold>
                  <FixedSpaceColumn>
                    <ChipContainer
                      horizontalSpacing="xs"
                      ref={chipGroupContainerRef}
                    >
                      {children_
                        .filter((child) => childIds.includes(child.id))
                        .map((child) => (
                          <div key={child.id} data-qa="relevant-child">
                            <SelectionChip
                              key={child.id}
                              text={`${formatPersonName(child, 'FirstFirst')}${
                                duplicateChildInfo[child.id] !== undefined
                                  ? ` ${duplicateChildInfo[child.id]}`
                                  : ''
                              }`}
                              translate="no"
                              selected={message.children.includes(child.id)}
                              onChange={(selected) => {
                                setChildSelectionTouched.on()
                                const children = selected
                                  ? [...message.children, child.id]
                                  : message.children.filter(
                                      (id) => id !== child.id
                                    )
                                const recipients = message.recipients.filter(
                                  (accountId) =>
                                    children.every(
                                      (childId) =>
                                        recipientOptions.childrenToMessageAccounts
                                          .find(
                                            (value) => value.childId === childId
                                          )
                                          ?.newMessage.includes(accountId) ??
                                        false
                                    )
                                )
                                setMessage((message) => ({
                                  ...message,
                                  children,
                                  recipients
                                }))
                              }}
                              onBlur={(e) => {
                                const focusTargetOutsideThisSelector =
                                  chipGroupContainerRef.current &&
                                  !chipGroupContainerRef.current.contains(
                                    e.relatedTarget
                                  )

                                if (focusTargetOutsideThisSelector) {
                                  setChildSelectionTouched.on()
                                }
                              }}
                              data-qa={`child-${child.id}`}
                            />
                          </div>
                        ))}
                    </ChipContainer>
                    {isChildSelectionTouched &&
                      message.children.length === 0 && (
                        <ErrorMessageBox
                          text={i18n.calendar.childSelectionMissingError}
                        />
                      )}
                  </FixedSpaceColumn>
                </label>
                <Gap size="s" />
              </>
            )}

            <label>
              <Bold>{required(i18n.messages.messageEditor.recipients)}</Bold>
              <Gap size="xs" />
              <MultiSelect
                placeholder={i18n.messages.messageEditor.search}
                value={recipients.primary}
                options={validAccounts.primary}
                onChange={(primary) =>
                  setMessage((message) => ({
                    ...message,
                    recipients: [...primary, ...recipients.secondary].map(
                      ({ id }) => id
                    )
                  }))
                }
                noOptionsMessage={i18n.messages.messageEditor.noResults}
                getOptionId={({ id }) => id}
                getOptionLabel={({ name, type }) =>
                  type === 'GROUP'
                    ? `${name} (${i18n.messages.staffAnnotation})`
                    : name
                }
                data-qa="select-recipient"
                required
              />
            </label>

            <OutOfOfficeInfo
              selectedAccountIds={recipients.primary.map((a) => a.id)}
              accounts={recipientOptions.messageAccounts}
            />

            {showSecondaryRecipientSelection && (
              <>
                <Gap size="xs" />
                <div>
                  <label>
                    <Bold>
                      {i18n.messages.messageEditor.secondaryRecipients}
                    </Bold>
                  </label>
                  <Gap size="xs" horizontal={true} />
                  <SecondaryRecipients>
                    {validAccounts.secondary.map((recipient) => (
                      <ToggleableRecipient
                        key={recipient.id}
                        recipient={{
                          ...recipient,
                          toggleable: true,
                          selected: recipients.secondary.some(
                            (acc) => acc.id === recipient.id
                          ),
                          outOfOffice: null
                        }}
                        data-qa="secondary-recipient"
                        onToggleRecipient={(_, selected) =>
                          setMessage((message) => ({
                            ...message,
                            recipients: selected
                              ? [...message.recipients, recipient.id]
                              : message.recipients.filter(
                                  (accountId) => accountId !== recipient.id
                                )
                          }))
                        }
                        labelAdd={i18n.common.add}
                      />
                    ))}
                  </SecondaryRecipients>
                </div>
              </>
            )}

            {!selectedChildrenInSameUnit && (
              <InfoBox
                message={i18n.messages.messageEditor.singleUnitRequired}
              />
            )}

            <Gap size="s" />

            <label>
              <Bold>{required(i18n.messages.messageEditor.subject)}</Bold>
              <InputField
                value={message.title ?? ''}
                onChange={(updated) =>
                  setMessage((message) => ({ ...message, title: updated }))
                }
                data-qa="input-title"
                required
              />
            </label>

            <Gap size="s" />

            <TextAreaLabel>
              <Bold>{required(i18n.messages.messageEditor.message)}</Bold>
              <Gap size="s" />
              <StyledTextArea
                value={message.content}
                onChange={(updated) =>
                  setMessage((message) => ({
                    ...message,
                    content: updated.target.value
                  }))
                }
                data-qa="input-content"
                required
              />
            </TextAreaLabel>

            {messageAttachmentsAllowed && (
              <>
                <Gap size="xs" />
                <FileUpload
                  slimSingleFile
                  files={attachments}
                  uploadHandler={messageAttachment(deleteAttachment)}
                  onUploaded={(attachment) =>
                    setAttachments((prev) => [...prev, attachment])
                  }
                  onDeleted={(id) =>
                    setAttachments((prev) => prev.filter((a) => a.id !== id))
                  }
                  onStateChange={setUploadStatus}
                  getDownloadUrl={getAttachmentUrl}
                  data-qa="upload-message-attachment"
                  buttonText={
                    i18n.messages.messageEditor.addShiftCareAttachment
                  }
                />
                <Gap size="m" />
              </>
            )}

            <Gap size="s" />
            {displaySendError && (
              <ErrorMessage>
                {i18n.messages.messageEditor.messageSendError}
              </ErrorMessage>
            )}
          </FormArea>
          <BottomRow>
            <LegacyButton
              text={i18n.messages.messageEditor.discard}
              onClick={onClose}
            />
            <span />
            <AsyncButton
              primary
              text={i18n.messages.messageEditor.send}
              disabled={!sendEnabled}
              onClick={send}
              onSuccess={onSuccess}
              onFailure={onFailure}
              data-qa="send-message-btn"
            />
          </BottomRow>
        </Container>
      </FocusLock>
    </ModalAccessibilityWrapper>
  )
})

const Container = styled.div`
  width: 100%;
  max-width: 680px;
  height: calc(100% - 32px);
  margin: ${defaultMargins.s};
  position: fixed;
  z-index: 100;
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.grayscale.g0};
  @media (max-width: ${desktopMin}) {
    width: 100vw;
    height: 100%;
    max-width: 100vw;
    max-height: 100%;
    position: fixed;
    top: 0;
    left: 0;
    overflow-y: scroll;
    margin: 0;
  }
`

const ErrorMessage = styled.div`
  color: ${colors.status.danger};
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${colors.main.m2};
  color: ${colors.grayscale.g0};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const Title = styled(H2)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: ${defaultMargins.s};
  color: ${colors.grayscale.g0};
`

const FormArea = styled.div`
  width: 100%;
  flex-grow: 1;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
  overflow-y: auto;
`

const TextAreaLabel = styled.label`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`

const StyledTextArea = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
  min-height: 200px;
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  bottom: 0;
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.m};
  margin-top: auto;
  border-top: 1px solid ${colors.grayscale.g35};
`

const SecondaryRecipients = styled.span`
  display: inline-flex;
  align-items: center;
`
