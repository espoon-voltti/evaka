// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { onlineManager } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'

const subscribe = (onChange: () => void) => onlineManager.subscribe(onChange)
const getOnline = () => onlineManager.isOnline()

export function useIsOnline(): boolean {
  return useSyncExternalStore(subscribe, getOnline)
}
