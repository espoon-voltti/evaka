// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios, { AxiosError } from 'axios'

import { isAutomatedTest } from 'lib-common/utils/helpers'

export const API_URL = '/api/internal'

export const client = axios.create({
  baseURL: API_URL
})

export const setAntiCsrfToken = (value: string | undefined) => {
  for (const method of ['delete', 'patch', 'post', 'put'] as const) {
    client.defaults.headers[method]['x-evaka-csrf'] = value
  }
}

if (isAutomatedTest) {
  client.interceptors.request.use((config) => {
    const evakaMockedTime =
      typeof window !== 'undefined'
        ? window.evaka?.mockedTime?.toISOString()
        : undefined
    if (evakaMockedTime) {
      config.headers.set('EvakaMockedTime', evakaMockedTime)
    }
    return config
  })
}

client.interceptors.response.use(undefined, async (err: AxiosError) => {
  if (err.response && err.response.status == 401) {
    window.location.replace('/employee/mobile')
  }

  return Promise.reject(err)
})
