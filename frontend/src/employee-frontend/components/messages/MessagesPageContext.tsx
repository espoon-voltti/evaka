import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Loading, Paged, Result } from '../../../lib-common/api'
import { useRestApi } from '../../../lib-common/utils/useRestApi'
import { UUID } from '../../types'
import {
  getMessageDrafts,
  getMessagingAccounts,
  getReceivedMessages,
  getSentMessages,
  ReplyResponse,
  replyToThread,
  ReplyToThreadParams
} from './api'
import {
  DraftContent,
  Message,
  MessageAccount,
  MessageThread,
  SentMessage
} from './types'
import { AccountView } from './types-view'

const PAGE_SIZE = 20

export interface MessagesPageState {
  accounts: Result<MessageAccount[]>
  loadAccounts: () => void
  selectedDraft: DraftContent | undefined
  setSelectedDraft: (draft: DraftContent | undefined) => void
  selectedAccount: AccountView | undefined
  setSelectedAccount: (view: AccountView) => void
  page: number
  setPage: (page: number) => void
  pages: number | undefined
  setPages: (pages: number) => void
  receivedMessages: Result<MessageThread[]>
  sentMessages: Result<SentMessage[]>
  messageDrafts: Result<DraftContent[]>
  selectedThread: MessageThread | undefined
  setSelectedThread: (thread: MessageThread | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  replyState: Result<void> | undefined
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
  refreshMessages: (account?: UUID) => void
}

const defaultState: MessagesPageState = {
  accounts: Loading.of(),
  loadAccounts: () => undefined,
  selectedDraft: undefined,
  setSelectedDraft: () => undefined,
  selectedAccount: undefined,
  setSelectedAccount: () => undefined,
  page: 1,
  setPage: () => undefined,
  pages: undefined,
  setPages: () => undefined,
  receivedMessages: Loading.of(),
  sentMessages: Loading.of(),
  messageDrafts: Loading.of(),
  selectedThread: undefined,
  setSelectedThread: () => undefined,
  sendReply: () => undefined,
  replyState: undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined,
  refreshMessages: () => undefined
}

export const MessagesPageContext = createContext<MessagesPageState>(
  defaultState
)

const appendMessageAndMoveThreadToTopOfList = (
  threadId: UUID,
  message: Message
) => (state: Result<MessageThread[]>) =>
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

export const MessagesPageContextProvider = React.memo(
  function MessagesPageContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [selectedAccount, setSelectedAccount] = useState<AccountView>()

    const [accounts, setAccounts] = useState<Result<MessageAccount[]>>(
      Loading.of()
    )
    const loadAccounts = useRestApi(getMessagingAccounts, setAccounts)

    const [selectedDraft, setSelectedDraft] = useState(
      defaultState.selectedDraft
    )

    const [selectedThread, setSelectedThread] = useState<MessageThread>()

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

    const setReceivedMessagesResult = useCallback(
      (result: Result<Paged<MessageThread>>) => {
        setReceivedMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadReceivedMessages = useRestApi(
      getReceivedMessages,
      setReceivedMessagesResult
    )

    const loadMessageDrafts = useRestApi(getMessageDrafts, setMessageDrafts)

    const setSentMessagesResult = useCallback(
      (result: Result<Paged<SentMessage>>) => {
        setSentMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadSentMessages = useRestApi(getSentMessages, setSentMessagesResult)

    // load messages if account, view or page changes
    const loadMessages = useCallback(() => {
      if (!selectedAccount) {
        return
      }
      switch (selectedAccount.view) {
        case 'RECEIVED':
          loadReceivedMessages(selectedAccount.account.id, page, PAGE_SIZE)
          break
        case 'SENT':
          loadSentMessages(selectedAccount.account.id, page, PAGE_SIZE)
          break
        case 'DRAFTS':
          loadMessageDrafts(selectedAccount.account.id)
      }
    }, [
      loadMessageDrafts,
      loadReceivedMessages,
      loadSentMessages,
      page,
      selectedAccount
    ])

    useEffect(loadMessages, [loadMessages])

    const [replyState, setReplyState] = useState<Result<void>>()
    const setReplyResponse = useCallback((res: Result<ReplyResponse>) => {
      setReplyState(res.map(() => undefined))
      if (res.isSuccess) {
        const {
          value: { message, threadId }
        } = res
        setReceivedMessages(
          appendMessageAndMoveThreadToTopOfList(threadId, message)
        )
        setSelectedThread((thread) =>
          thread?.id === threadId
            ? { ...thread, messages: [...thread.messages, message] }
            : thread
        )
        setReplyContents((state) => ({ ...state, [threadId]: '' }))
      }
    }, [])
    const reply = useRestApi(replyToThread, setReplyResponse)
    const sendReply = useCallback(reply, [reply])

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const refreshMessages = useCallback(
      (accountId?: UUID) => {
        if (!accountId || selectedAccount?.account.id === accountId) {
          loadMessages()
        }
      },
      [loadMessages, selectedAccount]
    )

    const value = useMemo(
      () => ({
        accounts,
        loadAccounts,
        selectedDraft,
        setSelectedDraft,
        selectedAccount,
        setSelectedAccount,
        page,
        setPage,
        pages,
        setPages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        selectedThread,
        setSelectedThread,
        replyState,
        sendReply,
        getReplyContent,
        setReplyContent,
        refreshMessages
      }),
      [
        accounts,
        loadAccounts,
        selectedDraft,
        selectedAccount,
        page,
        pages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        selectedThread,
        replyState,
        sendReply,
        getReplyContent,
        setReplyContent,
        refreshMessages
      ]
    )

    return (
      <MessagesPageContext.Provider value={value}>
        {children}
      </MessagesPageContext.Provider>
    )
  }
)
