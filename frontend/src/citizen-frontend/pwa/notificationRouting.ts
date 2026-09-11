// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useLocation } from 'wouter'

interface NotificationClick {
  type: 'notification-click'
  path: string
}

const isNotificationClick = (data: unknown): data is NotificationClick =>
  typeof data === 'object' &&
  data !== null &&
  'type' in data &&
  data.type === 'notification-click' &&
  'path' in data &&
  typeof data.path === 'string'

// Open the path of a notification without reloading the app
export function useNotificationClickRouting(): void {
  const [, navigate] = useLocation()
  const queryClient = useQueryClient()
  useEffect(() => {
    if (!('serviceWorker' in navigator)) return
    const onMessage = (event: MessageEvent) => {
      if (!isNotificationClick(event.data)) return
      event.ports[0]?.postMessage(true)
      navigate(event.data.path)
    }
    navigator.serviceWorker.addEventListener('message', onMessage)
    return () =>
      navigator.serviceWorker.removeEventListener('message', onMessage)
  }, [navigate, queryClient])
}
