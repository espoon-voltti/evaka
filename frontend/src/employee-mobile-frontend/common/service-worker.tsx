// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import React, { createContext, useEffect, useState } from 'react'

interface ServiceWorkerState {
  registration: ServiceWorkerRegistration | undefined
}

export const ServiceWorkerContext = createContext<ServiceWorkerState>({
  registration: undefined
})

export const ServiceWorkerContextProvider = React.memo(
  function ServiceWorkerContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [registration, setRegistration] =
      useState<ServiceWorkerRegistration>()

    useEffect(() => {
      registerServiceWorker()
        .then(setRegistration)
        .catch((err) => {
          Sentry.captureException(err)
        })
    }, [])

    const value = { registration }

    return (
      <ServiceWorkerContext.Provider value={value}>
        {children}
      </ServiceWorkerContext.Provider>
    )
  }
)

const registerServiceWorker = async () => {
  if ('serviceWorker' in navigator) {
    await navigator.serviceWorker.register('/employee/mobile/service-worker.js')
    return await navigator.serviceWorker.ready
  } else {
    return undefined
  }
}
