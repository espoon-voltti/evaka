// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import Constants from 'expo-constants'

const DEFAULT_BASE = 'http://10.0.2.2:3000/api' // Android emulator → host loopback

export const apiBaseUrl: string =
  (Constants.expoConfig?.extra?.apiBaseUrl as string | undefined) ??
  DEFAULT_BASE

/**
 * Axios instance used by generated API clients. Hand-written callers should
 * prefer the `api()` helper below, which carries Authorization automatically.
 */
export const client = axios.create({ baseURL: apiBaseUrl })

let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

function notifyUnauthorized(): void {
  if (unauthorizedHandler) unauthorizedHandler()
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public bodyText: string
  ) {
    super(`API error ${status}`)
  }
}

type FetchOpts = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  token?: string | null
  signal?: AbortSignal
}

export async function api<T>(path: string, opts: FetchOpts = {}): Promise<T> {
  const res = await fetch(`${apiBaseUrl}${path}`, {
    method: opts.method ?? 'GET',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(opts.token ? { Authorization: `Bearer ${opts.token}` } : {})
    },
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
    signal: opts.signal
  })
  const text = await res.text()
  if (!res.ok) {
    if (res.status === 401) notifyUnauthorized()
    throw new ApiError(res.status, text)
  }
  return text.length > 0 ? (JSON.parse(text) as T) : (undefined as T)
}
