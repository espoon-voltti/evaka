// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  ReloadNotification,
  OuterContainer
} from 'lib-components/molecules/ReloadNotification'

import { useTranslation } from '../state/i18n'

import UndoMessageNotification from './messages/UndoMessageNotification'

interface Props {
  apiVersion: string
}

export default React.memo(function Notifications({ apiVersion }: Props) {
  const { i18n } = useTranslation()
  return (
    <OuterContainer>
      <ReloadNotification
        i18n={{
          ...i18n.reloadNotification,
          closeLabel: i18n.common.close
        }}
        apiVersion={apiVersion}
      />
      <UndoMessageNotification />
    </OuterContainer>
  )
})
