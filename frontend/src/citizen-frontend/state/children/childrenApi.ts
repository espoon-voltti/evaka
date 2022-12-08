import { CitizenChildConsent } from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'

import { api, mutationResultHook, queryResultHook } from '../api'

export const childrenApi = api.injectEndpoints({
  endpoints: (build) => ({
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
  useChildConsentNotificationsQuery,
  useChildConsentsQuery,
  useInsertChildConsentsMutation
} = childrenApi

export const useChildConsentNotificationsQueryResult = queryResultHook(
  useChildConsentNotificationsQuery
)
export const useChildConsentsQueryResult = queryResultHook(
  useChildConsentsQuery
)
export const useInsertChildConsentsMutationResult = mutationResultHook(
  useInsertChildConsentsMutation
)
