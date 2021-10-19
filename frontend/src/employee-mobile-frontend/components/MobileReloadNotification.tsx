// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import ReloadNotification from 'lib-components/molecules/ReloadNotification'
import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'

export default React.memo(function MobileReloadNotification() {
  const { i18n } = useTranslation()
  const { apiVersion } = useContext(UserContext)

  return (
    <ReloadNotification
      apiVersion={apiVersion}
      i18n={i18n.reloadNotification}
    />
  )
})
