// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios, { AxiosInstance } from 'axios'
import { useEffect, useState } from 'react'

export function useAxiosResponseInterceptor(client: AxiosInstance): {
  lastResponse: number
} {
  const [lastResponse, setLastResponse] = useState(Date.now())

  useEffect(() => {
    const id = client.interceptors.response.use(
      (res) => {
        setLastResponse(Date.now())
        return res
      },
      (err: unknown) => {
        if (axios.isAxiosError(err) && err.response) {
          setLastResponse(Date.now())
        }
        return Promise.reject(err)
      }
    )
    return () => {
      client.interceptors.response.eject(id)
    }
  }, [client.interceptors.response])

  return { lastResponse }
}
