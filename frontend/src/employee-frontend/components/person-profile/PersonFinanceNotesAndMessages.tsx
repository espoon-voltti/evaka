// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled, { useTheme } from 'styled-components'

import { combine, wrapResult } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { FinanceNote } from 'lib-common/generated/api-types/finance'
import {
  MessageThread,
  PostMessageBody
} from 'lib-common/generated/api-types/messaging'
import {
  FinanceNoteId,
  MessageAccountId,
  MessageThreadId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import {
  cancelMutation,
  invalidateDependencies,
  useQueryResult
} from 'lib-common/query'
import { isAutomatedTest } from 'lib-common/utils/helpers'
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
import MessageReplyEditor from 'lib-components/messages/MessageReplyEditor'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label, Light } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
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
  const [confirmDeleteThread, setConfirmDeleteThread] =
    useState<MessageThreadId>()

  const onSuccessTimeout = isAutomatedTest ? 10 : 800 // same as used in async-button-behaviour

  const personName = useMemo((): string | undefined => {
    if (person.isSuccess) {
      const p = person.getOrElse(null)
      if (p !== null) {
        return formatPersonName(p, i18n, true)
      }
    }
    return undefined
  }, [person, i18n])

  const queryClient = useQueryClient()
  const { mutateAsync: createThread } = useMutation({
    mutationFn: createFinanceThreadMutation.api
  })

  const onSend = useCallback(
    async (accountId: MessageAccountId, messageBody: PostMessageBody) => {
      setSending(true)

      const arg = {
        id,
        accountId,
        body: {
          ...messageBody,
          sensitive: true
        }
      }
      await createThread(arg)
        .then(() => {
          setTimeout(() => {
            void invalidateDependencies(
              queryClient,
              createFinanceThreadMutation,
              arg
            )
            refreshMessages(accountId)
            clearUiMode()
          }, onSuccessTimeout)
        })
        .catch(() =>
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
        )

      setSending(false)
    },
    [
      clearUiMode,
      createThread,
      i18n.common.error.unknown,
      i18n.common.ok,
      id,
      onSuccessTimeout,
      queryClient,
      refreshMessages,
      setErrorMessage
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
            refreshMessages(financeAccount?.account.id)
            setConfirmDeleteThread(undefined)
            clearUiMode()
          }}
          data-qa="delete-finance-thread-modal"
        />
      )}

      {uiMode === 'new-finance-message-editor' &&
        financeAccount &&
        personName !== undefined && (
          <MessageEditor
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
                toggleUiMode('new-finance-message-editor')
              }}
              data-qa="send-finance-message-button"
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
              {financeAccount &&
                [...threads, ...notes]
                  .sort((a, b) => {
                    const dateA =
                      'messages' in a ? a.messages[0].sentAt : a.note.createdAt
                    const dateB =
                      'messages' in b ? b.messages[0].sentAt : b.note.createdAt
                    return dateB.formatIso().localeCompare(dateA.formatIso())
                  })
                  .map((v) =>
                    'messages' in v ? (
                      <SingleThread
                        key={v.id}
                        id={id}
                        thread={v}
                        financeAccountId={financeAccount?.account.id}
                        setThread={setThread}
                        setConfirmDeleteThread={setConfirmDeleteThread}
                        refreshMessages={refreshMessages}
                        uiMode={uiMode}
                        clearUiMode={clearUiMode}
                        toggleUiMode={toggleUiMode}
                      />
                    ) : (
                      <SingleNote
                        key={v.note.id}
                        id={id}
                        note={v.note}
                        permittedActions={v.permittedActions}
                        text={text}
                        setText={setText}
                        setConfirmDeleteNote={setConfirmDeleteNote}
                        uiMode={uiMode}
                        clearUiMode={clearUiMode}
                        toggleUiMode={toggleUiMode}
                      />
                    )
                  )}
            </>
          )
        )}
      </CollapsibleContentArea>
    </>
  )
})

const SingleThread = React.memo(function SingleThread({
  id,
  thread,
  financeAccountId,
  setThread,
  setConfirmDeleteThread,
  refreshMessages,
  uiMode,
  clearUiMode,
  toggleUiMode
}: {
  id: PersonId
  thread: MessageThread
  financeAccountId: MessageAccountId
  setThread: (thread: MessageThread) => void
  setConfirmDeleteThread: (id: MessageThreadId) => void
  refreshMessages: (id: MessageAccountId) => void
  uiMode: string
  clearUiMode: () => void
  toggleUiMode: (mode: string) => void
}) {
  const { i18n } = useTranslation()
  const theme = useTheme()

  const [replyContent, setReplyContent] = useState('')
  const [threadsOpen, setThreadsOpen] = useState<Record<string, boolean>>({})

  const onUpdateContent = useCallback(
    (content: string) => setReplyContent(content),
    [setReplyContent]
  )

  const { recipients } = useRecipients(thread.messages, financeAccountId, null)

  return (
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
            <FontAwesomeIcon
              icon={thread.messages.length === 1 ? faEnvelope : faReply}
            />
            <Gap horizontal size="xxs" />
            {thread.title} ({thread.messages.length}) (
            <UnderlinedLink
              data-qa="finance-message-thread-link"
              to={`/messages?accountId=${financeAccountId}&messageBox=thread&threadId=${thread.id}`}
              target="_blank"
            >
              {i18n.personProfile.financeNotesAndMessages.link}
            </UnderlinedLink>
            )
          </Label>
          <Light style={{ fontSize: '14px' }}>
            <span data-qa="finance-thread-sent-at">
              {thread.messages[0].sentAt.format()}
            </span>
            , {thread.messages[0].sender.name}
          </Light>
        </FixedSpaceColumn>
        <FixedSpaceRow spacing="xs">
          <IconOnlyButton
            icon={faReply}
            onClick={() => {
              setThread(thread)
              toggleUiMode(`reply-finance-thread_${thread.id}`)
            }}
            size="s"
            data-qa="reply-finance-thread-button"
            aria-label={i18n.common.edit}
          />
          <IconOnlyButton
            icon={faTrash}
            onClick={() => {
              setConfirmDeleteThread(thread.id)
              toggleUiMode(`delete-finance-thread`)
            }}
            size="s"
            data-qa="delete-finance-thread-button"
            aria-label={i18n.common.remove}
          />
        </FixedSpaceRow>
      </FlexRow>
      <div>{formatParagraphs(thread.messages[0].content)}</div>

      {uiMode === `reply-finance-thread_${thread.id}` ? (
        <BorderedMessageArea
          key={thread.id}
          opaque
          paddingHorizontal="s"
          paddingVertical="s"
          data-qa="finance-reply"
        >
          <MessageReplyEditor
            mutation={replyToFinanceThreadMutation}
            onSubmit={() => ({
              id,
              accountId: financeAccountId,
              messageId: thread.messages[0].id,
              body: {
                content: replyContent,
                recipientAccountIds: recipients.map((r) => r.id)
              }
            })}
            onSuccess={() => {
              refreshMessages(financeAccountId)
              clearUiMode()
            }}
            onDiscard={() => {
              setReplyContent('')
              clearUiMode()
            }}
            onUpdateContent={onUpdateContent}
            recipients={recipients.map((r) => ({ ...r, selected: true }))}
            onToggleRecipient={() => undefined}
            replyContent={replyContent}
            sendEnabled={!!replyContent}
          />
        </BorderedMessageArea>
      ) : null}

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
                  data-qa="finance-message"
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
                ? i18n.personProfile.financeNotesAndMessages.hideMessages
                : i18n.personProfile.financeNotesAndMessages.showMessages}
              <Gap horizontal size="xs" />
              <FontAwesomeIcon
                icon={threadsOpen[thread.id] ? faChevronUp : faChevronDown}
                color={theme.colors.main.m2}
              />
            </AccordionToggle>
          </div>
        </>
      )}
    </BorderedContentArea>
  )
})

const SingleNote = React.memo(function SingleNote({
  id,
  note,
  permittedActions,
  text,
  setText,
  setConfirmDeleteNote,
  uiMode,
  clearUiMode,
  toggleUiMode
}: {
  id: PersonId
  note: FinanceNote
  permittedActions: Action.FinanceNote[]
  text: string
  setText: (text: string) => void
  setConfirmDeleteNote: (id: FinanceNoteId) => void
  uiMode: string
  clearUiMode: () => void
  toggleUiMode: (mode: string) => void
}) {
  const { i18n } = useTranslation()

  return (
    <BorderedContentArea
      key={note.id}
      opaque
      paddingHorizontal="0"
      paddingVertical="s"
      data-qa="finance-note"
    >
      <FlexRow justifyContent="space-between">
        <FixedSpaceColumn spacing="xxs">
          <Label>{i18n.personProfile.financeNotesAndMessages.note}</Label>
          <Light style={{ fontSize: '14px' }}>
            {i18n.personProfile.financeNotesAndMessages.created}{' '}
            <span data-qa="finance-note-created-at">
              {note.createdAt.format()}
            </span>
            , {note.createdByName}
          </Light>
          {uiMode === `edit-finance-note_${note.id}` && (
            <Light style={{ fontSize: '14px' }}>
              {i18n.personProfile.financeNotesAndMessages.inEdit}
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
