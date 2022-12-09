// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { BaseQueryFn, createApi } from '@reduxjs/toolkit/query/react'
import { AxiosError, AxiosRequestConfig } from 'axios'

import { client } from '../api-client'

const axiosBaseQuery =
  (): BaseQueryFn<
    | string
    | {
        url: string
        method: AxiosRequestConfig['method']
        body?: AxiosRequestConfig['data']
        params?: AxiosRequestConfig['params']
      },
    unknown,
    unknown
  > =>
  async (urlOrOptions) => {
    const options =
      typeof urlOrOptions === 'string'
        ? { url: urlOrOptions, method: 'GET' }
        : urlOrOptions
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const { url, method, body, params } = options
    try {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      const result = await client({ url, method, data: body, params })
      return { data: result.data }
    } catch (axiosError) {
      const err = axiosError as AxiosError
      return {
        error: {
          status: err.response?.status,
          data: err.response?.data || err.message
        }
      }
    }
  }

export const api = createApi({
  reducerPath: 'api',
  baseQuery: axiosBaseQuery(),
  tagTypes: [
    'ChildUnreadPedagogicalDocumentsCount',
    'ChildPedagogicalDocument',
    'ChildUnreadAssistanceNeedDecisionsCount',
    'ChildUnreadVasuDocumentsCount',
    'ChildConsents',
    'ChildConsentNotifications'
  ],
  endpoints: () => ({})
})
