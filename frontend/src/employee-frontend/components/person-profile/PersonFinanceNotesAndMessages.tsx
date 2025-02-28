// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled, { useTheme } from 'styled-components'

import { combine, wrapResult } from 'lib-common/api'
import {
  MessageThread,
  PostMessageBody
} from 'lib-common/generated/api-types/messaging'
import {
  FinanceNoteId,
  MessageAccountId,
  // MessageId,
  MessageThreadId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import {
  cancelMutation,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label, Light } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faEnvelope, faPen, faQuestion, faReply, faTrash } from 'lib-icons'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { getAttachmentUrl, messageAttachment } from '../../api/attachments'
import {
  deleteDraftMessage,
  initDraftMessage,
  updateDraftMessage
} from '../../generated/api-clients/messaging'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatPersonName } from '../../utils'
import { formatParagraphs } from '../../utils/html-utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { MessageContext } from '../messages/MessageContext'
import MessageEditor from '../messages/MessageEditor'
import {
  createFinanceThreadMutation,
  deleteFinanceThreadMutation,
  financeThreadsQuery,
  replyToFinanceThreadMutation
} from '../messages/queries'

import {
  createFinanceNoteMutation,
  deleteFinanceNoteMutation,
  financeNotesQuery,
  updateFinanceNoteMutation
} from './queries'
import { PersonContext } from './state'

const initDraftMessageResult = wrapResult(initDraftMessage)
const updateDraftMessageResult = wrapResult(updateDraftMessage)
const deleteDraftMessageResult = wrapResult(deleteDraftMessage)

interface Props {
  id: PersonId
  open: boolean
}

export default React.memo(function PersonFinanceNotesAndMessages({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const theme = useTheme()
  const { person, permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const { selectedDraft, refreshMessages, financeAccount } =
    useContext(MessageContext)
  const [open, setIsOpen] = useState(startOpen)
  const financeNotes = useQueryResult(financeNotesQuery({ personId: id }))
  const [text, setText] = useState<string>('')
  const [confirmDeleteNote, setConfirmDeleteNote] = useState<FinanceNoteId>()
  const financeMessages = useQueryResult(financeThreadsQuery({ personId: id }))
  const [sending, setSending] = useState(false)
  const [thread, setThread] = useState<MessageThread>()
  const [threadsOpen, setThreadsOpen] = useState<Record<string, boolean>>({})
  const [confirmDeleteThread, setConfirmDeleteThread] =
    useState<MessageThreadId>()

  const personName = useMemo((): string | undefined => {
    if (person.isSuccess) {
      const p = person.getOrElse(null)
      if (p !== null) {
        return formatPersonName(p, i18n, true)
      }
    }
    return undefined
  }, [person, i18n])

  const { mutateAsync: replyToThread } = useMutationResult(
    replyToFinanceThreadMutation
  )

  const { mutateAsync: createThread } = useMutationResult(
    createFinanceThreadMutation
  )

  const onSend = useCallback(
    async (accountId: MessageAccountId, messageBody: PostMessageBody) => {
      const afterSend = (isSuccess: boolean, accountId: MessageAccountId) => {
        if (isSuccess) {
          refreshMessages(accountId)
          clearUiMode()
        } else {
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
        }
      }

      setSending(true)

      if (thread) {
        await replyToThread({
          id,
          accountId,
          messageId: thread.messages[0].id,
          body: {
            content: messageBody.content,
            recipientAccountIds: thread.messages[0].recipients.map((r) => r.id)
          }
        }).then((res) => afterSend(res.isSuccess, accountId))
      } else {
        await createThread({
          id,
          accountId,
          body: {
            ...messageBody,
            sensitive: true
          }
        }).then((res) => afterSend(res.isSuccess, accountId))
      }

      setSending(false)
    },
    [
      clearUiMode,
      i18n.common.error.unknown,
      i18n.common.ok,
      refreshMessages,
      setErrorMessage,
      thread
    ]
  )

  return (
    <>
      {uiMode.startsWith('delete-finance-note') && (
        <MutateFormModal
          type="warning"
          title={i18n.personProfile.financeNotesAndMessages.confirmDeleteNote}
          icon={faQuestion}
          resolveMutation={deleteFinanceNoteMutation}
          resolveAction={() =>
            confirmDeleteNote !== undefined
              ? {
                  id,
                  noteId: confirmDeleteNote
                }
              : cancelMutation
          }
          resolveLabel={i18n.common.remove}
          resolveDisabled={false}
          rejectAction={() => {
            setConfirmDeleteNote(undefined)
            clearUiMode()
          }}
          rejectLabel={i18n.common.cancel}
          onSuccess={() => {
            setConfirmDeleteNote(undefined)
            clearUiMode()
          }}
          data-qa="delete-finance-note-modal"
        />
      )}

      {uiMode.startsWith('delete-finance-thread') && (
        <MutateFormModal
          type="warning"
          title={i18n.personProfile.financeNotesAndMessages.confirmDeleteThread}
          icon={faQuestion}
          resolveMutation={deleteFinanceThreadMutation}
          resolveAction={() =>
            confirmDeleteThread !== undefined && financeAccount !== undefined
              ? {
                  id,
                  accountId: financeAccount.account.id,
                  threadId: confirmDeleteThread
                }
              : cancelMutation
          }
          resolveLabel={i18n.common.remove}
          resolveDisabled={false}
          rejectAction={() => {
            setConfirmDeleteThread(undefined)
            clearUiMode()
          }}
          rejectLabel={i18n.common.cancel}
          onSuccess={() => {
            setConfirmDeleteThread(undefined)
            clearUiMode()
          }}
          data-qa="delete-finance-message-modal"
        />
      )}

      {uiMode === 'finance-message-editor' &&
        financeAccount &&
        personName !== undefined && (
          <StyledMessageEditor
            availableReceivers={[
              {
                accountId: financeAccount.account.id,
                receivers: [
                  {
                    id: id,
                    name: personName,
                    type: 'CITIZEN'
                  }
                ]
              }
            ]}
            defaultSender={{
              value: financeAccount.account.id,
              label: financeAccount.account.name
            }}
            draftContent={selectedDraft}
            getAttachmentUrl={getAttachmentUrl}
            initDraftRaw={(accountId) => initDraftMessageResult({ accountId })}
            accounts={[financeAccount]}
            folders={[]}
            onClose={() => clearUiMode()}
            onDiscard={(accountId, draftId) => {
              clearUiMode()
              void deleteDraftMessageResult({ accountId, draftId }).then(() => {
                refreshMessages(accountId)
              })
            }}
            onSend={onSend}
            saveDraftRaw={(params) =>
              updateDraftMessageResult({
                accountId: params.accountId,
                draftId: params.draftId,
                body: params.content
              })
            }
            saveMessageAttachment={messageAttachment}
            sending={sending}
            defaultTitle={thread ? thread.title : ''}
          />
        )}

      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.financeNotesAndMessages.title}</H2>}
        open={open}
        toggleOpen={() => setIsOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-finance-notes-and-messages-collapsible"
      >
        <BorderedContentArea
          opaque
          paddingHorizontal="0"
          paddingVertical="s"
          data-qa="add-finance-note"
        >
          <FlexRow>
            <AddButton
              text={i18n.personProfile.financeNotesAndMessages.addNote}
              onClick={() => {
                setText('')
                toggleUiMode('add-finance-note')
              }}
              data-qa="add-finance-note-button"
              disabled={
                !permittedActions.has('CREATE_FINANCE_NOTE') ||
                uiMode === 'add-finance-note' ||
                uiMode.startsWith('edit-finance-note')
              }
            />
            <Gap horizontal size="m" />
            <AddButton
              icon={faEnvelope}
              text={i18n.personProfile.financeNotesAndMessages.sendMessage}
              onClick={() => {
                setText('')
                setThread(undefined)
                toggleUiMode('finance-message-editor')
              }}
              data-qa="send-finance-message"
              disabled={false}
            />
          </FlexRow>
        </BorderedContentArea>

        {uiMode === 'add-finance-note' && (
          <BorderedContentArea
            opaque
            paddingHorizontal="0"
            paddingVertical="s"
            data-qa="add-finance-note"
          >
            <StyledTextArea
              autoFocus
              value={text}
              rows={3}
              onChange={setText}
              data-qa="finance-note-text-area"
            />
            <Gap size="xs" />
            <FixedSpaceRow justifyContent="flex-start">
              <Button
                appearance="inline"
                onClick={() => clearUiMode()}
                text={i18n.common.cancel}
              />
              <Gap horizontal size="s" />
              <MutateButton
                data-qa="save-finance-note"
                appearance="inline"
                text={i18n.common.save}
                mutation={createFinanceNoteMutation}
                onClick={() => ({ body: { content: text, personId: id } })}
                onSuccess={() => clearUiMode()}
                disabled={!text}
              />
            </FixedSpaceRow>
          </BorderedContentArea>
        )}

        {renderResult(
          combine(financeMessages, financeNotes),
          ([threads, notes]) => (
            // list note items eiter edit or view mode
            <>
              {threads.length > 0
                ? threads.map((thread) => (
                    <BorderedContentArea
                      key={thread.id}
                      opaque
                      paddingHorizontal="0"
                      paddingVertical="s"
                      data-qa="finance-message-thread"
                    >
                      <FlexRow justifyContent="space-between">
                        <FixedSpaceColumn spacing="xxs">
                          <Label>
                            <FontAwesomeIcon icon={faEnvelope} />
                            <Gap horizontal size="xxs" />
                            {thread.title} ({thread.messages.length}) (
                            <UnderlinedLink
                              data-qa="finance-message-thread-link"
                              to={`/messages?accountId=${financeAccount?.account.id}&messageBox=thread&threadId=${thread.id}`}
                              target="_blank"
                            >
                              {i18n.personProfile.financeNotesAndMessages.link}
                            </UnderlinedLink>
                            )
                          </Label>
                          <Light style={{ fontSize: '14px' }}>
                            {thread.messages[0].sentAt.format()},{' '}
                            {thread.messages[0].sender.name}
                          </Light>
                        </FixedSpaceColumn>
                        <FixedSpaceRow spacing="xs">
                          <IconOnlyButton
                            icon={faReply}
                            onClick={() => {
                              setThread(thread)
                              toggleUiMode(`finance-message-editor`)
                            }}
                            size="s"
                            data-qa="reply-finance-thread"
                            aria-label={i18n.common.edit}
                          />
                          <IconOnlyButton
                            icon={faTrash}
                            onClick={() => {
                              setConfirmDeleteThread(thread.id)
                              toggleUiMode(`delete-finance-thread`)
                            }}
                            size="s"
                            data-qa={`delete-finance-thread-${thread.id}`}
                            aria-label={i18n.common.remove}
                          />
                        </FixedSpaceRow>
                      </FlexRow>
                      <div>{formatParagraphs(thread.messages[0].content)}</div>

                      {thread.messages.length > 1 && (
                        <>
                          {threadsOpen[thread.id] &&
                            thread.messages
                              .filter((_, i) => i !== 0)
                              .map((m) => (
                                <BorderedMessageArea
                                  key={m.id}
                                  opaque
                                  paddingHorizontal="s"
                                  paddingVertical="s"
                                  data-qa="add-finance-note"
                                >
                                  <Light style={{ fontSize: '14px' }}>
                                    {m.sentAt.format()}, {m.sender.name}
                                  </Light>
                                  <div>{formatParagraphs(m.content)}</div>
                                </BorderedMessageArea>
                              ))}

                          <div
                            onClick={() =>
                              setThreadsOpen({
                                ...threadsOpen,
                                [thread.id]: !(threadsOpen[thread.id] ?? false)
                              })
                            }
                          >
                            <AccordionToggle>
                              {threadsOpen[thread.id]
                                ? i18n.personProfile.financeNotesAndMessages
                                    .hideMessages
                                : i18n.personProfile.financeNotesAndMessages
                                    .showMessages}
                              <Gap horizontal size="xs" />
                              <FontAwesomeIcon
                                icon={
                                  threadsOpen[thread.id]
                                    ? faChevronUp
                                    : faChevronDown
                                }
                                color={theme.colors.main.m2}
                              />
                            </AccordionToggle>
                          </div>
                        </>
                      )}
                    </BorderedContentArea>
                  ))
                : null}

              {notes.length > 0
                ? notes.map(({ note, permittedActions }) => (
                    <BorderedContentArea
                      key={note.id}
                      opaque
                      paddingHorizontal="0"
                      paddingVertical="s"
                      data-qa="finance-note"
                    >
                      <FlexRow justifyContent="space-between">
                        <FixedSpaceColumn spacing="xxs">
                          <Label>
                            {i18n.personProfile.financeNotesAndMessages.note}
                          </Label>
                          <Light style={{ fontSize: '14px' }}>
                            {i18n.personProfile.financeNotesAndMessages.created}{' '}
                            <span data-qa="finance-note-created-at">
                              {note.createdAt.format()}
                            </span>
                            , {note.createdByName}
                          </Light>
                          {uiMode === `edit-finance-note_${note.id}` && (
                            <Light style={{ fontSize: '14px' }}>
                              {
                                i18n.personProfile.financeNotesAndMessages
                                  .inEdit
                              }
                            </Light>
                          )}
                        </FixedSpaceColumn>

                        {uiMode !== `edit-finance-note_${note.id}` && (
                          <FixedSpaceRow spacing="xs">
                            <IconOnlyButton
                              icon={faPen}
                              onClick={() => {
                                setText(note.content)
                                toggleUiMode(`edit-finance-note_${note.id}`)
                              }}
                              disabled={!permittedActions.includes('UPDATE')}
                              size="s"
                              data-qa="edit-finance-note"
                              aria-label={i18n.common.edit}
                            />
                            <IconOnlyButton
                              icon={faTrash}
                              onClick={() => {
                                setConfirmDeleteNote(note.id)
                                toggleUiMode(`delete-finance-note-${note.id}`)
                              }}
                              disabled={!permittedActions.includes('DELETE')}
                              size="s"
                              data-qa="delete-finance-note"
                              aria-label={i18n.common.remove}
                            />
                          </FixedSpaceRow>
                        )}
                      </FlexRow>

                      {uiMode === `edit-finance-note_${note.id}` ? (
                        <div key={note.id} data-qa="edit-finance-note">
                          <StyledTextArea
                            autoFocus
                            value={text}
                            rows={3}
                            onChange={setText}
                            data-qa="finance-note-text-area"
                          />
                          <Gap size="xs" />
                          <FixedSpaceRow justifyContent="flex-start">
                            <Button
                              appearance="inline"
                              onClick={() => clearUiMode()}
                              text={i18n.common.cancel}
                            />
                            <MutateButton
                              data-qa="update-finance-note"
                              appearance="inline"
                              text={i18n.common.save}
                              mutation={updateFinanceNoteMutation}
                              onClick={() => ({
                                id,
                                noteId: note.id,
                                body: { content: text, personId: id }
                              })}
                              onSuccess={() => clearUiMode()}
                              disabled={!text}
                            />
                          </FixedSpaceRow>
                        </div>
                      ) : (
                        <div>{formatParagraphs(note.content)}</div>
                      )}
                    </BorderedContentArea>
                  ))
                : null}
            </>
          )
        )}
      </CollapsibleContentArea>
    </>
  )
})

const BorderedContentArea = styled(ContentArea)`
  border-bottom: solid 1px ${(p) => p.theme.colors.grayscale.g35};
`
const BorderedMessageArea = styled(ContentArea)`
  border-top: solid 1px ${(p) => p.theme.colors.grayscale.g15};
`
const StyledTextArea = styled(TextArea)`
  width: 100%;
  max-width: 851px;
  resize: none;
  flex-grow: 1;
  min-height: 100px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
`
const AccordionToggle = styled.span`
  color: ${(p) => p.theme.colors.main.m2};
  cursor: pointer;
`

const UnderlinedLink = styled(Link)`
  font-weight: normal;
  text-decoration: underline;
`
const StyledMessageEditor = styled(MessageEditor)`
  bottom: 0;
  right: 0;
`
