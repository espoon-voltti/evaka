// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import SessionExpiredModal from 'lib-components/molecules/modals/SessionExpiredModal'
import { useKeepSessionAlive } from 'lib-components/useKeepSessionAlive'

import { useSessionKeepalive, useUser } from './state'

export default React.memo(function SessionExpiry() {
  const loggedIn = useUser() !== undefined
  const sessionKeepalive = useSessionKeepalive()
  const { sessionExpirationDetected, dismissSessionExpiredDetection } =
    useKeepSessionAlive(sessionKeepalive, loggedIn)

  return sessionExpirationDetected ? (
    <SessionExpiredModal onClose={dismissSessionExpiredDetection} />
  ) : null
})
