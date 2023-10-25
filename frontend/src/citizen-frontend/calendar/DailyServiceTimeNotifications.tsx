// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'
import React, { useContext, useEffect, useState } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { DailyServiceTimeNotification } from 'lib-common/generated/api-types/dailyservicetimes'
import { useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import colors from 'lib-customizations/common'
import { faExclamation, faInfo } from 'lib-icons'

import { dismissDailyServiceTimeNotifications } from './api'
import { dailyServiceTimeNotificationsQuery } from './queries'

export default React.memo(function DailyServiceTimeNotification() {
  const i18n = useTranslation()

  const dailyServiceTimeNotifications = useQueryResult(
    dailyServiceTimeNotificationsQuery()
  )

  const notifications = dailyServiceTimeNotifications.getOrElse([])

  const [modalNotificationDates, setModalNotificationDates] = useState<
    DailyServiceTimeNotification[]
  >([])

  const { addNotification } = useContext(NotificationsContext)

  useEffect(() => {
    if (notifications.length === 0) return

    const [modalNotifications, toastNotifications] = partition(
      notifications,
      (n) => n.hasDeletedReservations
    )

    for (const notification of toastNotifications) {
      addNotification({
        icon: faInfo,
        iconColor: colors.main.m2,
        onClose() {
          void dismissDailyServiceTimeNotifications([notification.id])
        },
        children: i18n.calendar.dailyServiceTimeModifiedNotification(
          notification.dateFrom.format()
        ),
        dataQa: 'daily-service-time-notification'
      })
    }

    setModalNotificationDates(modalNotifications)
  }, [notifications, addNotification, i18n.calendar])

  if (modalNotificationDates.length === 0) {
    return null
  }

  return (
    <InfoModal
      type="warning"
      title={i18n.calendar.dailyServiceTimeModifiedDestructivelyModal.title}
      text={i18n.calendar.dailyServiceTimeModifiedDestructivelyModal.text(
        modalNotificationDates
          .map(({ dateFrom }) => dateFrom.format())
          .join(', ')
      )}
      icon={faExclamation}
      resolve={{
        async action() {
          await dismissDailyServiceTimeNotifications(
            modalNotificationDates.map(({ id }) => id)
          )
          setModalNotificationDates([])
        },
        label: i18n.calendar.dailyServiceTimeModifiedDestructivelyModal.ok
      }}
      data-qa="daily-service-time-notification-modal"
    />
  )
})
