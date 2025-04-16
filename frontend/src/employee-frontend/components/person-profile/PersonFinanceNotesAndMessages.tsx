// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled, { useTheme } from 'styled-components'

import { combine } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import {
  FinanceNote,
  FinanceNoteResponse
} from 'lib-common/generated/api-types/finance'
import {
  DraftContent,
  MessageThread,
  PostMessageBody
} from 'lib-common/generated/api-types/messaging'
import {
  MessageAccountId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  invalidateDependencies,
  useMutationResult,
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
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H2, Label, Light } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { Gap } from 'lib-components/white-space'
import {
  faBoxArchive,
  faChevronDown,
  faChevronUp,
  faEnvelope,
  faPen,
  faReply,
  faTrash
} from 'lib-icons'

import { getAttachmentUrl, messageAttachment } from '../../api/attachments'
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
  deleteDraftMutation,
  archiveFinanceThreadMutation,
  draftsQuery,
  financeThreadsQuery,
  markThreadReadMutation,
  replyToFinanceThreadMutation
} from '../messages/queries'

import {
  createFinanceNoteMutation,
  deleteFinanceNoteMutation,
  financeNotesQuery,
  updateFinanceNoteMutation
} from './queries'
import { PersonContext } from './state'

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
  const { refreshMessages, financeAccount } = useContext(MessageContext)
  const [open, setIsOpen] = useState(startOpen)
  const financeNotes = useQueryResult(financeNotesQuery({ personId: id }))
  const [text, setText] = useState<string>('')
  const financeMessages = useQueryResult(
    financeAccount ? financeThreadsQuery({ personId: id }) : constantQuery([])
  )
  const [sending, setSending] = useState(false)
  const [thread, setThread] = useState<MessageThread>()
  const messageDrafts = useQueryResult(
    financeAccount
      ? draftsQuery({ accountId: financeAccount?.account.id })
      : constantQuery([])
  )

  const { mutateAsync: markThreadRead } = useMutationResult(
    markThreadReadMutation
  )

  const financeThreads = useMemo(() => {
    if (financeMessages.isSuccess && financeAccount) {
      financeMessages.value.forEach((thread) => {
        if (
          thread.messages.some(
            (m) => !m.readAt && m.sender.id !== financeAccount?.account.id
          )
        ) {
          void markThreadRead({
            accountId: financeAccount.account.id,
            threadId: thread.id
          })
        }
      })
      refreshMessages(financeAccount.account.id)
    }
    return financeMessages
  }, [financeAccount, financeMessages, markThreadRead, refreshMessages])

  const onSuccessTimeout = isAutomatedTest ? 10 : 800 // same as used in async-button-behaviour

  const [personName, hasSsn] = useMemo(
    () =>
      person
        .map<
          [string | undefined, boolean]
        >((p) => [formatPersonName(p, i18n, true), !!p.socialSecurityNumber])
        .getOrElse([undefined, false]),
    [person, i18n]
  )

  const draftContent = useMemo((): DraftContent | undefined => {
    return personName && messageDrafts.isSuccess
      ? messageDrafts.value.find(
          (draft) =>
            draft.recipientNames.length === 1 &&
            draft.recipientNames[0] === personName
        )
      : undefined
  }, [personName, messageDrafts])

  const { mutateAsync: deleteDraftResult } =
    useMutationResult(deleteDraftMutation)

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

  function isMessageThread(
    item: MessageThread | FinanceNoteResponse
  ): item is MessageThread {
    return (item as MessageThread).messages !== undefined
  }

  return (
    <>
      {uiMode === 'new-finance-message-editor' &&
        financeAccount &&
        personName !== undefined && (
          <MessageEditor
            selectableRecipients={[
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
            draftContent={draftContent}
            getAttachmentUrl={getAttachmentUrl}
            accounts={[financeAccount]}
            folders={[]}
            onClose={() => clearUiMode()}
            onDiscard={(accountId, draftId) => {
              clearUiMode()
              void deleteDraftResult({ accountId, draftId }).then(() => {
                refreshMessages(accountId)
              })
            }}
            onSend={onSend}
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
          <ExpandingInfo
            info={
              hasSsn
                ? null
                : i18n.personProfile.financeNotesAndMessages.noMessaging
            }
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
                disabled={financeAccount === undefined || !hasSsn}
              />
            </FlexRow>
          </ExpandingInfo>
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
          combine(financeThreads, financeNotes),
          ([threads, notes]) => (
            // list note items eiter edit or view mode
            <>
              {[...threads, ...notes]
                .sort((a, b) => {
                  const dateA = isMessageThread(a)
                    ? a.messages[0].sentAt
                    : a.note.createdAt
                  const dateB = isMessageThread(b)
                    ? b.messages[0].sentAt
                    : b.note.createdAt
                  return dateB.formatIso().localeCompare(dateA.formatIso())
                })
                .map((item) =>
                  isMessageThread(item) ? (
                    <SingleThread
                      key={item.id}
                      id={id}
                      thread={item}
                      financeAccountId={financeAccount!.account.id}
                      setThread={setThread}
                      refreshMessages={refreshMessages}
                      uiMode={uiMode}
                      clearUiMode={clearUiMode}
                      toggleUiMode={toggleUiMode}
                    />
                  ) : (
                    <SingleNote
                      key={item.note.id}
                      id={id}
                      note={item.note}
                      permittedActions={item.permittedActions}
                      text={text}
                      setText={setText}
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
  refreshMessages,
  uiMode,
  clearUiMode,
  toggleUiMode
}: {
  id: PersonId
  thread: MessageThread
  financeAccountId: MessageAccountId
  setThread: (thread: MessageThread) => void
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
          <SmallLight>
            <span data-qa="finance-thread-sent-at">
              {thread.messages[0].sentAt.format()}
            </span>
            , {thread.messages[0].sender.name}
          </SmallLight>
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
          <ConfirmedMutation
            buttonStyle="ICON"
            icon={faBoxArchive}
            buttonAltText={i18n.common.remove}
            data-qa="archive-finance-thread-button"
            confirmationTitle={
              i18n.personProfile.financeNotesAndMessages.confirmArchiveThread
            }
            mutation={archiveFinanceThreadMutation}
            onClick={() => ({
              id,
              accountId: financeAccountId,
              threadId: thread.id
            })}
            onSuccess={() => {
              refreshMessages(financeAccountId)
            }}
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
                  <SmallLight>
                    {m.sentAt.format()}, {m.sender.name}
                  </SmallLight>
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
  uiMode,
  clearUiMode,
  toggleUiMode
}: {
  id: PersonId
  note: FinanceNote
  permittedActions: Action.FinanceNote[]
  text: string
  setText: (text: string) => void
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
          <SmallLight>
            {i18n.personProfile.financeNotesAndMessages.created}{' '}
            <span data-qa="finance-note-created-at">
              {note.createdAt.format()}
            </span>
            , {note.createdByName}
          </SmallLight>
          {uiMode === `edit-finance-note_${note.id}` && (
            <SmallLight>
              {i18n.personProfile.financeNotesAndMessages.inEdit}
            </SmallLight>
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
            <ConfirmedMutation
              buttonStyle="ICON"
              icon={faTrash}
              buttonAltText={i18n.common.remove}
              data-qa="delete-finance-note-button"
              confirmationTitle={
                i18n.personProfile.financeNotesAndMessages.confirmDeleteNote
              }
              mutation={deleteFinanceNoteMutation}
              onClick={() => ({
                id,
                noteId: note.id
              })}
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
const SmallLight = styled(Light)`
  font-size: 14px;
`
const UnderlinedLink = styled(Link)`
  font-weight: normal;
  text-decoration: underline;
`
