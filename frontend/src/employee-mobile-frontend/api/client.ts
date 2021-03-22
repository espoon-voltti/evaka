// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios, { AxiosError } from 'axios'

export const API_URL = '/api/internal'

export const client = axios.create({
  baseURL: API_URL,
  xsrfCookieName: 'evaka.employee.xsrf'
})

client.interceptors.response.use(undefined, async (err: AxiosError) => {
  if (err.response && err.response.status == 401) {
    window.location.replace('/employee/mobile')
  }

  return Promise.reject(err)
})
