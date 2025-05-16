// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosError } from 'axios'
import axios from 'axios'

import { isAutomatedTest } from 'lib-common/utils/helpers'
import { LoginStatusChangeEvent } from 'lib-common/utils/login-status'

export const API_URL = '/api'

export const client = axios.create({
  baseURL: API_URL
})
client.defaults.headers.common['x-evaka-csrf'] = '1'

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
  if (err.response && err.response.status === 401) {
    const event = new LoginStatusChangeEvent(false)
    window.dispatchEvent(event)

    // Check if the event was handled by a listener
    if (!event.defaultPrevented) {
      window.location.replace('/employee/login')
    }
  }

  return Promise.reject(err)
})
