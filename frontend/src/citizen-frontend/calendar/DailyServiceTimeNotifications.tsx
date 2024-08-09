// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faExclamation } from 'lib-icons'

import { dismissDailyServiceTimeNotification } from '../generated/api-clients/dailyservicetimes'

import { dailyServiceTimeNotificationsQuery } from './queries'

export default React.memo(function DailyServiceTimeNotification() {
  const i18n = useTranslation()

  const dailyServiceTimeNotifications = useQueryResult(
    dailyServiceTimeNotificationsQuery()
  )

  const notifications = dailyServiceTimeNotifications.getOrElse([])

  const [notificationIds, setNotificationIds] = useState<UUID[]>([])

  useEffect(() => {
    if (notifications.length === 0) return

    setNotificationIds(notifications)
  }, [notifications, i18n.calendar])

  if (notificationIds.length === 0) {
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
          await dismissDailyServiceTimeNotification({ body: notificationIds })
          setNotificationIds([])
        },
        label: i18n.calendar.dailyServiceTimeModifiedModal.ok
      }}
      data-qa="daily-service-time-notification-modal"
    />
  )
})
