// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { skipToken } from '@reduxjs/toolkit/query'

import { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import {
  Child,
  CitizenChildConsent
} from 'lib-common/generated/api-types/children'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { User } from '../../auth/state'
import { api } from '../api'
import { queryResultHook, mutationResultHook } from '../utils'

export const childrenApi = api.injectEndpoints({
  endpoints: (build) => ({
    applicationNotifications: build.query<number, void>({
      query: () => '/citizen/applications/by-guardian/notifications',
      providesTags: ['ApplicationNotifications']
    }),
    acceptDecision: build.mutation<
      void,
      { applicationId: UUID; decisionId: UUID; requestedStartDate: LocalDate }
    >({
      query: ({ applicationId, decisionId, requestedStartDate }) => ({
        method: 'POST',
        url: `/citizen/applications/${applicationId}/actions/accept-decision`,
        body: { decisionId, requestedStartDate }
      }),
      invalidatesTags: ['ApplicationNotifications']
    }),
    rejectDecision: build.mutation<
      void,
      { applicationId: UUID; decisionId: UUID }
    >({
      query: ({ applicationId, decisionId }) => ({
        method: 'POST',
        url: `/citizen/applications/${applicationId}/actions/reject-decision`,
        body: {
          decisionId
        }
      }),
      invalidatesTags: ['ApplicationNotifications']
    }),
    children: build.query<JsonOf<Child[]>, void>({
      query: () => '/citizen/children'
    }),
    unreadPedagogicalDocumentsCount: build.query<Record<UUID, number>, void>({
      query: () => '/citizen/pedagogical-documents/unread-count',
      providesTags: ['ChildUnreadPedagogicalDocumentsCount']
    }),
    pedagogicalDocuments: build.query<
      JsonOf<PedagogicalDocumentCitizen[]>,
      UUID
    >({
      query: (childId) => `/citizen/children/${childId}/pedagogical-documents`,
      providesTags: [{ type: 'ChildPedagogicalDocument', id: 'LIST' }]
    }),
    markPedagogicalDocumentRead: build.mutation<void, UUID>({
      query: (documentId) => ({
        method: 'POST',
        url: `/citizen/pedagogical-documents/${documentId}/mark-read`
      }),
      invalidatesTags: [
        'ChildUnreadPedagogicalDocumentsCount',
        { type: 'ChildPedagogicalDocument', id: 'LIST' }
      ]
    }),
    unreadAssistanceNeedDecisionsCount: build.query<
      UnreadAssistanceNeedDecisionItem[],
      void
    >({
      query: () => '/citizen/children/assistance-need-decisions/unread-counts',
      providesTags: ['ChildUnreadAssistanceNeedDecisionsCount']
    }),
    markAssistanceNeedDecisionAsRead: build.mutation<void, UUID>({
      query: (decisionId) => ({
        method: 'POST',
        url: `/citizen/children/assistance-need-decision/${decisionId}/read`
      }),
      invalidatesTags: ['ChildUnreadAssistanceNeedDecisionsCount']
    }),
    unreadVasuDocumentsCount: build.query<Record<UUID, number>, void>({
      query: () => '/citizen/vasu/children/unread-count',
      providesTags: ['ChildUnreadVasuDocumentsCount']
    }),
    givePermissionToShareVasu: build.mutation<void, UUID>({
      query: (documentId) => ({
        method: 'POST',
        url: `/citizen/vasu/${documentId}/give-permission-to-share`
      }),
      invalidatesTags: ['ChildUnreadVasuDocumentsCount']
    }),
    childConsentNotifications: build.query<Record<string, number>, void>({
      query: () => '/citizen/children/consents/notifications',
      providesTags: ['ChildConsentNotifications']
    }),
    childConsents: build.query<Record<UUID, CitizenChildConsent[]>, void>({
      query: () => '/citizen/children/consents',
      providesTags: ['ChildConsents']
    }),
    insertChildConsents: build.mutation<
      void,
      {
        childId: UUID
        consents: CitizenChildConsent[]
      }
    >({
      query: ({ childId, consents }) => ({
        method: 'POST',
        url: `/citizen/children/${childId}/consent`,
        body: consents
      }),
      invalidatesTags: ['ChildConsents', 'ChildConsentNotifications']
    })
  })
})

export const {
  useAcceptDecisionMutation,
  useRejectDecisionMutation,
  useChildrenQuery,
  useUnreadPedagogicalDocumentsCountQuery,
  usePedagogicalDocumentsQuery,
  useMarkPedagogicalDocumentReadMutation,
  useMarkAssistanceNeedDecisionAsReadMutation,
  useUnreadVasuDocumentsCountQuery,
  useGivePermissionToShareVasuMutation,
  useChildConsentNotificationsQuery,
  useChildConsentsQuery,
  useInsertChildConsentsMutation
} = childrenApi

export function useApplicationNotificationsQuery(user: User | undefined) {
  return childrenApi.useApplicationNotificationsQuery(
    !user ? skipToken : undefined
  )
}

export const useAcceptDecisionMutationResult = mutationResultHook(
  useAcceptDecisionMutation
)
export const useRejectDecisionMutationResult = mutationResultHook(
  useRejectDecisionMutation
)

export const useChildrenQueryResult = queryResultHook(useChildrenQuery)

export const deserializePedagogicalDocuments = (
  data: JsonOf<PedagogicalDocumentCitizen[]>
): PedagogicalDocumentCitizen[] =>
  data.map((item) => ({
    ...item,
    created: HelsinkiDateTime.parseIso(item.created)
  }))

export function useUnreadAssistanceNeedDecisionsCountQuery(
  user: User | undefined
) {
  return childrenApi.useUnreadAssistanceNeedDecisionsCountQuery(
    !user ? skipToken : undefined
  )
}

export const usePedagogicalDocumentsQueryResult = queryResultHook(
  usePedagogicalDocumentsQuery,
  deserializePedagogicalDocuments
)
export const useGivePermissionToShareVasuMutationResult = mutationResultHook(
  useGivePermissionToShareVasuMutation
)
export const useChildConsentsQueryResult = queryResultHook(
  useChildConsentsQuery
)
export const useInsertChildConsentsMutationResult = mutationResultHook(
  useInsertChildConsentsMutation
)
