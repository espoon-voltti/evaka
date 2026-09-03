// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useSyncExternalStore } from 'react'

const handheldQuery = () => window.matchMedia('(pointer: coarse)')

const subscribe = (onChange: () => void) => {
  const query = handheldQuery()
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

const getIsHandheld = () => handheldQuery().matches

export function useIsHandheld(): boolean {
  return useSyncExternalStore(subscribe, getIsHandheld)
}
