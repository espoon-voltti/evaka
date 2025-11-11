// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import type { DailyServiceTimeNotificationId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faExclamation } from 'lib-icons'

import { dismissDailyServiceTimeNotification } from '../generated/api-clients/dailyservicetimes'
import { useTranslation } from '../localization'

import { dailyServiceTimeNotificationsQuery } from './queries'

export default React.memo(function DailyServiceTimeNotification() {
  const i18n = useTranslation()

  const dailyServiceTimeNotifications = useQueryResult(
    dailyServiceTimeNotificationsQuery()
  )

  const notifications = dailyServiceTimeNotifications.getOrElse([])

  const [state, setState] = useState<{
    notificationIds: DailyServiceTimeNotificationId[]
    prevNotifications: DailyServiceTimeNotificationId[]
  }>({
    notificationIds: notifications,
    prevNotifications: notifications
  })

  if (state.prevNotifications !== notifications && notifications.length > 0) {
    setState({
      notificationIds: notifications,
      prevNotifications: notifications
    })
  }

  if (state.notificationIds.length === 0) {
    return null
  }

  return (
    <InfoModal
      type="warning"
      title={i18n.calendar.dailyServiceTimeModifiedModal.title}
      text={i18n.calendar.dailyServiceTimeModifiedModal.text}
      icon={faExclamation}
      resolve={{
        async action() {
          await dismissDailyServiceTimeNotification({
            body: state.notificationIds
          })
          setState((prev) => ({
            ...prev,
            notificationIds: []
          }))
        },
        label: i18n.calendar.dailyServiceTimeModifiedModal.ok
      }}
      data-qa="daily-service-time-notification-modal"
    />
  )
})
