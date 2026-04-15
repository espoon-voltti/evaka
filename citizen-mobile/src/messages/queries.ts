// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import * as api from '../api/messages'
import { useAuth } from '../auth/state'

export function useThreadsQuery() {
  const { state } = useAuth()
  return useQuery({
    queryKey: ['threads'],
    queryFn: () => {
      if (state.status !== 'signed-in') throw new Error('not signed in')
      return api.getThreads(state.token)
    },
    enabled: state.status === 'signed-in'
  })
}

export function useThreadQuery(threadId: string | undefined) {
  const { state } = useAuth()
  return useQuery({
    queryKey: ['thread', threadId],
    queryFn: () => {
      if (state.status !== 'signed-in' || !threadId)
        throw new Error('bad state')
      return api.getThread(state.token, threadId)
    },
    enabled: state.status === 'signed-in' && !!threadId
  })
}

export function useMyAccountQuery() {
  const { state } = useAuth()
  return useQuery({
    queryKey: ['my-account'],
    queryFn: () => {
      if (state.status !== 'signed-in') throw new Error('not signed in')
      return api.getMyAccount(state.token)
    },
    enabled: state.status === 'signed-in',
    staleTime: Infinity
  })
}

export function useUnreadCountQuery() {
  const { state } = useAuth()
  return useQuery({
    queryKey: ['unread-count'],
    queryFn: () => {
      if (state.status !== 'signed-in') throw new Error('not signed in')
      return api.getUnreadCount(state.token)
    },
    enabled: state.status === 'signed-in',
    refetchInterval: 60_000
  })
}

export function useReplyMutation() {
  const { state } = useAuth()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (args: {
      threadId: string
      content: string
      recipientAccountIds: string[]
    }) => {
      if (state.status !== 'signed-in') throw new Error('not signed in')
      return api.replyToThread(
        state.token,
        args.threadId,
        args.content,
        args.recipientAccountIds
      )
    },
    onSuccess: (_, { threadId }) => {
      void qc.invalidateQueries({ queryKey: ['thread', threadId] })
      void qc.invalidateQueries({ queryKey: ['threads'] })
    }
  })
}

export function useMarkReadMutation() {
  const { state } = useAuth()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (threadId: string) => {
      if (state.status !== 'signed-in') throw new Error('not signed in')
      return api.markThreadRead(state.token, threadId)
    },
    onSuccess: (_, threadId) => {
      void qc.invalidateQueries({ queryKey: ['thread', threadId] })
      void qc.invalidateQueries({ queryKey: ['threads'] })
      void qc.invalidateQueries({ queryKey: ['unread-count'] })
    }
  })
}
