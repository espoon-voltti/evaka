// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useMemo, useState } from 'react'

import type { MessageThreadId } from 'lib-common/generated/api-types/shared'

export interface MessageDraftsState {
  setReplyContent: (threadId: MessageThreadId, content: string) => void
  getReplyContent: (threadId: MessageThreadId) => string
}

const defaultDraftsState: MessageDraftsState = {
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageDraftsContext =
  createContext<MessageDraftsState>(defaultDraftsState)

// Kept above the router so that an unsent reply survives navigation away from
// the messages page.
export const MessageDraftsProvider = React.memo(function MessageDraftsProvider({
  children
}: {
  children: React.ReactNode
}) {
  const [replyContents, setReplyContents] = useState<
    Record<MessageThreadId, string>
  >({})

  const getReplyContent = useCallback(
    (threadId: MessageThreadId) => replyContents[threadId] ?? '',
    [replyContents]
  )
  const setReplyContent = useCallback(
    (threadId: MessageThreadId, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    },
    []
  )

  const value = useMemo(
    () => ({ getReplyContent, setReplyContent }),
    [getReplyContent, setReplyContent]
  )

  return (
    <MessageDraftsContext.Provider value={value}>
      {children}
    </MessageDraftsContext.Provider>
  )
})
