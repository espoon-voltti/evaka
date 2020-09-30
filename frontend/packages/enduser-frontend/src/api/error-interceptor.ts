// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios, { AxiosError } from 'axios'
import { ApiError, toApiError } from './api-error'

type Listener = (payload: ApiError) => void
const listeners: Listener[] = []

const onError = (error: ApiError) => listeners.map((l: Listener) => l(error))

export const handleRequestError = (error: AxiosError) => {
  if (error.code !== 'ECONNABORTED') {
    onError(toApiError(error))
  }
  return Promise.reject(error)
}

axios.interceptors.response.use((r) => r, handleRequestError)

export const addErrorListener = (fn: Listener) => {
  listeners.push(fn)
  return () => removeErrorListener(fn)
}

export const removeErrorListener = (fn: Listener) =>
  listeners.splice(listeners.indexOf(fn), 1)
