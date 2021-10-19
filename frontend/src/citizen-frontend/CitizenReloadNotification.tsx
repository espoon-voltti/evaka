// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import ReloadNotification from 'lib-components/molecules/ReloadNotification'
import { AuthContext } from './auth/state'
import { useTranslation } from './localization'

export default React.memo(function CitizenReloadNotification() {
  const t = useTranslation()
  const { apiVersion } = useContext(AuthContext)

  return (
    <ReloadNotification apiVersion={apiVersion} i18n={t.reloadNotification} />
  )
})
