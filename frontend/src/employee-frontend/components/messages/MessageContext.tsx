// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import uniqBy from 'lodash/uniqBy'
import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState
} from 'react'
import { useSearchParams } from 'wouter'

import type { Result } from 'lib-common/api'
import { Failure, Loading, wrapResult } from 'lib-common/api'
import type {
  DraftContent,
  Message,
  MessageThread,
  AuthorizedMessageAccount,
  SentMessage,
  ThreadReply,
  UnreadCountByAccount,
  MessageCopy,
  PagedMessageThreads,
  PagedSentMessages,
  PagedMessageCopies,
  MessageThreadFolder
} from 'lib-common/generated/api-types/messaging'
import type {
  ApplicationId,
  DaycareId,
  MessageAccountId,
  MessageId,
  MessageThreadFolderId,
  MessageThreadId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromNullableUuid, fromUuid, tryFromUuid } from 'lib-common/id-type'
import { constantQuery, useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import type { GroupMessageAccount } from 'lib-components/messages/types'
import {
  isGroupMessageAccount,
  isMunicipalMessageAccount,
  isPersonalMessageAccount,
  isServiceWorkerMessageAccount,
  isFinanceMessageAccount
} from 'lib-components/messages/types'
import type { SelectOption } from 'lib-components/molecules/Select'

import {
  getArchivedMessages,
  getMessagesInFolder,
  getDraftMessages,
  getMessageCopies,
  getReceivedMessages,
  getSentMessages,
  getThread
} from '../../generated/api-clients/messaging'
import { UserContext } from '../../state/user'

import {
  accountsByUserQuery,
  foldersQuery,
  unreadMessagesQuery
} from './queries'
import type { AccountView } from './types-view'
import {
  groupMessageBoxes,
  isFolderView,
  isStandardView,
  municipalMessageBoxes,
  personalMessageBoxes,
  serviceWorkerMessageBoxes,
  financeMessageBoxes
} from './types-view'

const getMessageCopiesResult = wrapResult(getMessageCopies)
const getDraftMessagesResult = wrapResult(getDraftMessages)
const getArchivedMessagesResult = wrapResult(getArchivedMessages)
const getMessagesInFolderResult = wrapResult(getMessagesInFolder)
const getReceivedMessagesResult = wrapResult(getReceivedMessages)
const getSentMessagesResult = wrapResult(getSentMessages)
const getThreadResult = wrapResult(getThread)

type RepliesByThread = Record<UUID, string>

export interface MessagesState {
  accounts: Result<AuthorizedMessageAccount[]>
  municipalAccount: AuthorizedMessageAccount | undefined
  serviceWorkerAccount: AuthorizedMessageAccount | undefined
  financeAccount: AuthorizedMessageAccount | undefined
  personalAccount: AuthorizedMessageAccount | undefined
  groupAccounts: GroupMessageAccount[]
  unitOptions: SelectOption<DaycareId>[]
  selectedDraft: DraftContent | undefined
  setSelectedDraft: (draft: DraftContent | undefined) => void
  selectedAccount: AccountView | undefined
  selectAccount: (v: AccountView) => void
  selectDefaultAccount: () => void
  selectUnit: (v: string) => void
  page: number
  setPage: (page: number) => void
  pages: number | undefined
  setPages: (pages: number) => void
  receivedMessages: Result<MessageThread[]>
  sentMessages: Result<SentMessage[]>
  messageDrafts: Result<DraftContent[]>
  messageCopies: Result<MessageCopy[]>
  archivedMessages: Result<MessageThread[]>
  messagesInFolder: Result<MessageThread[]>
  folders: Result<MessageThreadFolder[]>
  setSelectedThread: (threadId: UUID) => void
  selectedThread: MessageThread | undefined
  selectThread: (thread: MessageThread | undefined) => void
  onReplySent: (reply: ThreadReply) => void
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
  refreshMessages: (account?: UUID) => void
  unreadCountsByAccount: Result<UnreadCountByAccount[]>
  sentMessagesAsThreads: Result<MessageThread[]>
  messageCopiesAsThreads: Result<MessageThread[]>
  prefilledRecipient: PersonId | null
  prefilledTitle: string | null
  relatedApplicationId: ApplicationId | null
  accountAllowsNewMessage: () => boolean
}

const defaultState: MessagesState = {
  accounts: Loading.of(),
  municipalAccount: undefined,
  serviceWorkerAccount: undefined,
  financeAccount: undefined,
  personalAccount: undefined,
  groupAccounts: [],
  unitOptions: [],
  selectedDraft: undefined,
  setSelectedDraft: () => undefined,
  selectedAccount: undefined,
  selectAccount: () => undefined,
  selectDefaultAccount: () => undefined,
  selectUnit: () => undefined,
  page: 1,
  setPage: () => undefined,
  pages: undefined,
  setPages: () => undefined,
  receivedMessages: Loading.of(),
  sentMessages: Loading.of(),
  messageDrafts: Loading.of(),
  messageCopies: Loading.of(),
  archivedMessages: Loading.of(),
  messagesInFolder: Loading.of(),
  folders: Loading.of(),
  setSelectedThread: () => undefined,
  selectedThread: undefined,
  selectThread: () => undefined,
  onReplySent: () => undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined,
  refreshMessages: () => undefined,
  unreadCountsByAccount: Loading.of(),
  sentMessagesAsThreads: Loading.of(),
  messageCopiesAsThreads: Loading.of(),
  prefilledRecipient: null,
  prefilledTitle: null,
  relatedApplicationId: null,
  accountAllowsNewMessage: () => false
}

export const MessageContext = createContext<MessagesState>(defaultState)

const appendMessageAndMoveThreadToTopOfList =
  (threadId: UUID, message: Message) => (state: Result<MessageThread[]>) =>
    state.map((threads) => {
      const thread = threads.find((t) => t.id === threadId)
      if (!thread) return threads
      const otherThreads = threads.filter((t) => t.id !== threadId)
      return [
        {
          ...thread,
          messages: [...thread.messages, message]
        },
        ...otherThreads
      ]
    })

export const MessageContextProvider = React.memo(
  function MessageContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const { user } = useContext(UserContext)
    const [searchParams, setSearchParams] = useSearchParams()
    const accountId = searchParams.get('accountId')
    const messageBox = searchParams.get('messageBox')
    const unitId = searchParams.get('unitId')
    const threadId = fromNullableUuid<MessageThreadId>(
      searchParams.get('threadId')
    )
    const prefilledTitle = searchParams.get('title')
    const prefilledRecipient = fromNullableUuid<PersonId>(
      searchParams.get('recipient')
    )
    const relatedApplicationId = fromNullableUuid<ApplicationId>(
      searchParams.get('applicationId')
    )
    const replyBoxActive = searchParams.get('reply')

    const setParams = useCallback(
      (params: {
        accountId?: string | null
        messageBox?: string | null
        unitId?: string | null
        threadId?: string | null
      }) => {
        setSearchParams(
          {
            ...(params.accountId ? { accountId: params.accountId } : undefined),
            ...(params.messageBox
              ? { messageBox: params.messageBox }
              : undefined),
            ...(params.unitId ? { unitId: params.unitId } : undefined),
            ...(params.threadId ? { threadId: params.threadId } : undefined),
            ...(prefilledTitle ? { title: prefilledTitle } : undefined),
            ...(prefilledRecipient
              ? { recipient: prefilledRecipient }
              : undefined),
            ...(relatedApplicationId
              ? { applicationId: relatedApplicationId }
              : undefined),
            ...(replyBoxActive ? { reply: replyBoxActive } : undefined)
          },
          { replace: true }
        )
      },
      [
        setSearchParams,
        prefilledTitle,
        prefilledRecipient,
        relatedApplicationId,
        replyBoxActive
      ]
    )

    const [page, setPage] = useState<number>(1)
    const [pages, setPages] = useState<number>()
    const [receivedMessages, setReceivedMessages] = useState<
      Result<MessageThread[]>
    >(Loading.of())
    const [messageDrafts, setMessageDrafts] = useState<Result<DraftContent[]>>(
      Loading.of()
    )
    const [sentMessages, setSentMessages] = useState<Result<SentMessage[]>>(
      Loading.of()
    )
    const [messageCopies, setMessageCopies] = useState<Result<MessageCopy[]>>(
      Loading.of()
    )
    const [archivedMessages, setArchivedMessages] = useState<
      Result<MessageThread[]>
    >(Loading.of())
    const [messagesInFolder, setMessagesInFolder] = useState<
      Result<MessageThread[]>
    >(Loading.of())

    const accounts = useQueryResult(
      user?.accessibleFeatures.messages
        ? accountsByUserQuery()
        : constantQuery<AuthorizedMessageAccount[]>([])
    )

    const folders = useQueryResult(
      user?.accessibleFeatures.messages ? foldersQuery() : constantQuery([])
    )

    const municipalAccount = useMemo(
      () =>
        accounts
          .map((accounts) => accounts.find(isMunicipalMessageAccount))
          .getOrElse(undefined),
      [accounts]
    )
    const serviceWorkerAccount = useMemo(
      () =>
        accounts
          .map((accounts) => accounts.find(isServiceWorkerMessageAccount))
          .getOrElse(undefined),
      [accounts]
    )
    const financeAccount = useMemo(
      () =>
        accounts
          .map((accounts) => accounts.find(isFinanceMessageAccount))
          .getOrElse(undefined),
      [accounts]
    )
    const personalAccount = useMemo(
      () =>
        accounts
          .map((accounts) => accounts.find(isPersonalMessageAccount))
          .getOrElse(undefined),
      [accounts]
    )
    const groupAccounts = useMemo(
      () =>
        accounts
          .map((accounts) =>
            accounts
              .filter(isGroupMessageAccount)
              .sort((a, b) =>
                a.daycareGroup.name
                  .toLocaleLowerCase()
                  .localeCompare(b.daycareGroup.name.toLocaleLowerCase())
              )
              .sort((a, b) =>
                a.daycareGroup.unitName
                  .toLocaleLowerCase()
                  .localeCompare(b.daycareGroup.unitName.toLocaleLowerCase())
              )
          )
          .getOrElse([]),
      [accounts]
    )
    const unitOptions = useMemo(
      () =>
        sortBy(
          uniqBy(
            groupAccounts.map(({ daycareGroup }) => ({
              value: daycareGroup.unitId,
              label: daycareGroup.unitName
            })),
            (val) => val.value
          ),
          (u) => u.label
        ),
      [groupAccounts]
    )

    const unreadCountsByAccount = useQueryResult(
      user?.accessibleFeatures.messages
        ? unreadMessagesQuery()
        : constantQuery([]),
      {
        refetchInterval: 60 * 1000 // 1 minute
      }
    )

    const selectedAccount: AccountView | undefined = useMemo(() => {
      const account = accounts
        .map(
          (accounts) =>
            accounts.find((a) => a.account.id === accountId)?.account
        )
        .getOrElse(undefined)
      if (messageBox && account) {
        return {
          account,
          view: isStandardView(messageBox)
            ? messageBox
            : folders
                .map(
                  (res) =>
                    res.find(
                      (f) =>
                        f.id ===
                          tryFromUuid<MessageThreadFolderId>(messageBox) &&
                        f.ownerId === account.id
                    ) ?? 'received'
                )
                .getOrElse('received'),
          unitId
        }
      }
      return undefined
    }, [accountId, accounts, messageBox, unitId, folders])

    const accountAllowsNewMessage = useCallback(
      () =>
        selectedAccount?.account.type !== 'SERVICE_WORKER' &&
        selectedAccount?.account.type !== 'FINANCE',
      [selectedAccount]
    )

    const [selectedDraft, setSelectedDraft] = useState(
      defaultState.selectedDraft
    )

    const setReceivedMessagesResult = useCallback(
      (result: Result<PagedMessageThreads>) => {
        setReceivedMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadReceivedMessages = useRestApi(
      (accountId: MessageAccountId, page: number) =>
        getReceivedMessagesResult({ accountId, page }),
      setReceivedMessagesResult
    )

    const loadMessageDrafts = useRestApi(
      getDraftMessagesResult,
      setMessageDrafts
    )

    const setSentMessagesResult = useCallback(
      (result: Result<PagedSentMessages>) => {
        setSentMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadSentMessages = useRestApi(
      (accountId: MessageAccountId, page: number) =>
        getSentMessagesResult({ accountId, page }),
      setSentMessagesResult
    )

    const setMessageCopiesResult = useCallback(
      (result: Result<PagedMessageCopies>) => {
        setMessageCopies(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadMessageCopies = useRestApi(
      (accountId: MessageAccountId, page: number) =>
        getMessageCopiesResult({ accountId, page }),
      setMessageCopiesResult
    )

    const setArchivedMessagesResult = useCallback(
      (result: Result<PagedMessageThreads>) => {
        setArchivedMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadArchivedMessages = useRestApi(
      (accountId: MessageAccountId, page: number) =>
        getArchivedMessagesResult({ accountId, page }),
      setArchivedMessagesResult
    )

    const setMessagesInFolderResult = useCallback(
      (result: Result<PagedMessageThreads>) => {
        setMessagesInFolder(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadMessagesInFolder = useRestApi(
      (
        accountId: MessageAccountId,
        folderId: MessageThreadFolderId,
        page: number
      ) => getMessagesInFolderResult({ accountId, folderId, page }),
      setMessagesInFolderResult
    )

    const [singleThread, setSingleThread] = useState<Result<MessageThread>>()

    const loadThread = useRestApi(
      (accountId: MessageAccountId, threadId: MessageThreadId | null) =>
        threadId
          ? getThreadResult({ accountId, threadId })
          : Promise.resolve(Failure.of({ message: 'threadId is null' })),
      setSingleThread
    )

    // load messages if account, view or page changes
    const loadMessages = useCallback(() => {
      if (!selectedAccount) {
        return
      }

      switch (selectedAccount.view) {
        case 'received':
          return void loadReceivedMessages(selectedAccount.account.id, page)
        case 'sent':
          return void loadSentMessages(selectedAccount.account.id, page)
        case 'drafts':
          return void loadMessageDrafts({
            accountId: selectedAccount.account.id
          })
        case 'copies':
          return void loadMessageCopies(selectedAccount.account.id, page)
        case 'archive':
          return void loadArchivedMessages(selectedAccount.account.id, page)
        case 'thread':
          return void loadThread(selectedAccount.account.id, threadId)
        default:
          return void loadMessagesInFolder(
            selectedAccount.account.id,
            selectedAccount.view.id,
            page
          )
      }
    }, [
      loadMessageDrafts,
      loadReceivedMessages,
      loadSentMessages,
      loadMessageCopies,
      loadArchivedMessages,
      loadMessagesInFolder,
      loadThread,
      threadId,
      page,
      selectedAccount
    ])

    const refreshMessages = useCallback(
      (accountId?: UUID) => {
        if (!accountId || selectedAccount?.account.id === accountId) {
          loadMessages()
        }
      },
      [loadMessages, selectedAccount]
    )

    const sentMessagesAsThreads: Result<MessageThread[]> = useMemo(
      () =>
        sentMessages.map((value) =>
          selectedAccount
            ? value.map((message) => {
                const fakeMessageId = fromUuid<MessageId>(message.contentId)
                const fakeThreadId = fromUuid<MessageThreadId>(
                  message.contentId
                )
                return {
                  id: fakeThreadId,
                  type: message.type,
                  title: message.threadTitle,
                  urgent: message.urgent,
                  sensitive: message.sensitive,
                  isCopy: false,
                  participants: message.recipientNames,
                  applicationStatus: null,
                  children: [],
                  messages: [
                    {
                      id: fakeMessageId,
                      threadId: fakeThreadId,
                      sender: { ...selectedAccount.account },
                      sentAt: message.sentAt,
                      recipients: [], // recipientNames should be used when viewing sent messages
                      readAt: HelsinkiDateTime.now(),
                      content: message.content,
                      attachments: message.attachments,
                      recipientNames: message.recipientNames
                    }
                  ]
                }
              })
            : []
        ),
      [selectedAccount, sentMessages]
    )

    const messageCopiesAsThreads: Result<MessageThread[]> = useMemo(
      () =>
        messageCopies.map((value) =>
          value.map((message) => ({
            ...message,
            id: message.threadId,
            isCopy: true,
            applicationStatus: null,
            participants: [message.recipientName],
            children: [],
            messages: [
              {
                id: message.messageId,
                threadId: message.threadId,
                sender: {
                  id: message.senderId,
                  name: message.senderName,
                  type: message.senderAccountType
                },
                sentAt: message.sentAt,
                recipients: [
                  {
                    id: message.recipientId,
                    name: message.recipientName,
                    type: message.recipientAccountType
                  }
                ],
                readAt: message.readAt,
                content: message.content,
                attachments: message.attachments,
                recipientNames: message.recipientNames
              }
            ]
          }))
        ),
      [messageCopies]
    )

    const setSelectedThread = useCallback(
      (threadId: string | undefined) =>
        setParams({
          threadId,
          accountId,
          messageBox,
          unitId
        }),
      [accountId, messageBox, setParams, unitId]
    )
    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        setSelectedThread(thread?.id)
      },
      [setSelectedThread]
    )
    const selectedThread = useMemo(
      () =>
        [
          ...receivedMessages.getOrElse([]),
          ...sentMessagesAsThreads.getOrElse([]),
          ...messageCopiesAsThreads.getOrElse([]),
          ...archivedMessages.getOrElse([]),
          ...messagesInFolder.getOrElse([]),
          singleThread?.getOrElse(undefined)
        ].find((t) => t?.id === threadId),
      [
        receivedMessages,
        sentMessagesAsThreads,
        messageCopiesAsThreads,
        archivedMessages,
        messagesInFolder,
        singleThread,
        threadId
      ]
    )

    const appendMessageToSingleThread = useCallback(
      (message: Message) =>
        setSingleThread((prevState: Result<MessageThread> | undefined) =>
          prevState?.map((thread) => ({
            ...thread,
            messages: [...thread.messages, message]
          }))
        ),
      [setSingleThread]
    )

    const appendMessageToThreadInFolder = useCallback(
      (message: Message) =>
        setMessagesInFolder((prevState: Result<MessageThread[]>) =>
          prevState.map((threads) =>
            threads.map((thread) =>
              thread.id === message.threadId
                ? { ...thread, messages: [...thread.messages, message] }
                : thread
            )
          )
        ),
      [setMessagesInFolder]
    )

    const onReplySent = useCallback(
      ({ message, threadId }: ThreadReply) => {
        if (selectedAccount?.view === 'thread') {
          appendMessageToSingleThread(message)
        } else if (selectedAccount && isFolderView(selectedAccount.view)) {
          appendMessageToThreadInFolder(message)
        } else {
          setReceivedMessages(
            appendMessageAndMoveThreadToTopOfList(threadId, message)
          )
        }
        setSelectedThread(threadId)
      },
      [
        appendMessageToSingleThread,
        appendMessageToThreadInFolder,
        selectedAccount,
        setSelectedThread
      ]
    )

    const [replyContents, setReplyContents] = useState<RepliesByThread>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const selectUnit = useCallback(
      (unitId: string) => {
        const firstUnitGroupAccount = groupAccounts.find(
          (acc) => acc.daycareGroup.unitId === unitId
        )
        if (firstUnitGroupAccount) {
          setParams({
            accountId: firstUnitGroupAccount?.account.id,
            messageBox: groupMessageBoxes[0],
            unitId
          })
        } else {
          setParams({ unitId })
        }
      },
      [groupAccounts, setParams]
    )

    const selectAccount = useCallback(
      (accountView: AccountView) =>
        setParams({
          accountId: accountView.account.id,
          messageBox: isStandardView(accountView.view)
            ? accountView.view
            : accountView.view.id,
          unitId: accountView.unitId
        }),
      [setParams]
    )

    const selectDefaultAccount = useCallback(() => {
      if (serviceWorkerAccount) {
        setParams({
          messageBox: messageBox ?? serviceWorkerMessageBoxes[0],
          accountId: serviceWorkerAccount.account.id,
          unitId: null,
          threadId: threadId
        })
      } else if (financeAccount) {
        setParams({
          messageBox: messageBox ?? financeMessageBoxes[0],
          accountId: financeAccount.account.id,
          unitId: null,
          threadId: threadId
        })
      } else if (municipalAccount) {
        setParams({
          messageBox: messageBox ?? municipalMessageBoxes[0],
          accountId: municipalAccount.account.id,
          unitId: null,
          threadId: threadId
        })
      } else if (personalAccount) {
        setParams({
          messageBox: personalMessageBoxes[0],
          accountId: personalAccount.account.id,
          unitId: null,
          threadId: threadId
        })
      } else if (groupAccounts.length > 0) {
        setParams({
          messageBox: groupMessageBoxes[0],
          accountId: groupAccounts[0].account.id,
          unitId: groupAccounts[0].daycareGroup.unitId,
          threadId: threadId
        })
      }
    }, [
      setParams,
      groupAccounts,
      municipalAccount,
      serviceWorkerAccount,
      financeAccount,
      personalAccount,
      messageBox,
      threadId
    ])

    const value = useMemo(
      () => ({
        accounts,
        municipalAccount,
        serviceWorkerAccount,
        financeAccount,
        personalAccount,
        groupAccounts,
        unitOptions,
        selectedDraft,
        setSelectedDraft,
        selectedAccount,
        selectAccount,
        selectDefaultAccount,
        selectUnit,
        page,
        setPage,
        pages,
        setPages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        messageCopies,
        archivedMessages,
        messagesInFolder,
        folders,
        setSelectedThread,
        selectedThread,
        selectThread,
        onReplySent,
        getReplyContent,
        setReplyContent,
        refreshMessages,
        unreadCountsByAccount,
        sentMessagesAsThreads,
        messageCopiesAsThreads,
        prefilledRecipient,
        prefilledTitle,
        relatedApplicationId,
        accountAllowsNewMessage
      }),
      [
        accounts,
        municipalAccount,
        serviceWorkerAccount,
        financeAccount,
        personalAccount,
        groupAccounts,
        unitOptions,
        selectedDraft,
        selectedAccount,
        selectAccount,
        selectDefaultAccount,
        selectUnit,
        page,
        pages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        messageCopies,
        archivedMessages,
        messagesInFolder,
        folders,
        setSelectedThread,
        selectedThread,
        selectThread,
        onReplySent,
        getReplyContent,
        setReplyContent,
        refreshMessages,
        unreadCountsByAccount,
        sentMessagesAsThreads,
        messageCopiesAsThreads,
        prefilledRecipient,
        prefilledTitle,
        relatedApplicationId,
        accountAllowsNewMessage
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
