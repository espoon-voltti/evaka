import { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import { CitizenChildConsent } from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'

import { api, mutationResultHook, queryResultHook } from '../api'

export const childrenApi = api.injectEndpoints({
  endpoints: (build) => ({
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
  useUnreadAssistanceNeedDecisionsCountQuery,
  useMarkAssistanceNeedDecisionAsReadMutation,
  useUnreadVasuDocumentsCountQuery,
  useGivePermissionToShareVasuMutation,
  useChildConsentNotificationsQuery,
  useChildConsentsQuery,
  useInsertChildConsentsMutation
} = childrenApi

export const useUnreadAssistanceNeedDecisionsCountQueryResult = queryResultHook(
  useUnreadAssistanceNeedDecisionsCountQuery
)
export const useUnreadVasuDocumentsCountQueryResult = queryResultHook(
  useUnreadVasuDocumentsCountQuery
)
export const useGivePermissionToShareVasuMutationResult = mutationResultHook(
  useGivePermissionToShareVasuMutation
)
export const useChildConsentNotificationsQueryResult = queryResultHook(
  useChildConsentNotificationsQuery
)
export const useChildConsentsQueryResult = queryResultHook(
  useChildConsentsQuery
)
export const useInsertChildConsentsMutationResult = mutationResultHook(
  useInsertChildConsentsMutation
)
