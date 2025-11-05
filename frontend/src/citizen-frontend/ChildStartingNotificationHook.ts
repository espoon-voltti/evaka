// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInfo } from '@fortawesome/free-solid-svg-icons'
import { useContext, useEffectEvent } from 'react'
import { useEffect } from 'react'

import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { colors } from 'lib-customizations/common'

import { useUser } from './auth/state'
import { childrenQuery } from './children/queries'
import { useTranslation } from './localization'

type ChildWithUpcomingPlacement = ChildAndPermittedActions & {
  upcomingPlacementStartDate: LocalDate
  upcomingPlacementUnit: {
    id: string
    name: string
  }
}

// Type predicate function that acts as both filter and type guard
function hasUpcomingPlacement(
  child: ChildAndPermittedActions
): child is ChildWithUpcomingPlacement {
  return (
    child.upcomingPlacementType !== null &&
    child.upcomingPlacementStartDate !== null &&
    child.upcomingPlacementUnit !== null &&
    !child.upcomingPlacementIsCalendarOpen
  )
}

const notificationPrefix = 'child-not-started-toast'
const childStartingNotificationDismissalTTLDays = 7

function getChildStartingNotificationDismissalCookieName(
  userId: string,
  childId: string
): string {
  return `evaka-child-not-started-dismissal-${userId}-${childId}`
}

function isChildStartingNotificationDismissed(
  userId: string,
  childId: string
): boolean {
  const cookieName = getChildStartingNotificationDismissalCookieName(
    userId,
    childId
  )
  return document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith(`${cookieName}=`))
}

function dismissChildStartingNotification(
  userId: string,
  childId: string
): void {
  const cookieName = getChildStartingNotificationDismissalCookieName(
    userId,
    childId
  )
  const expires = new Date()
  expires.setDate(expires.getDate() + childStartingNotificationDismissalTTLDays)
  document.cookie = `${cookieName}=true; expires=${expires.toUTCString()}; path=/; SameSite=Strict`
}

export const useChildrenStartingNotification = () => {
  const user = useUser()
  const i18n = useTranslation()
  const childrenResult = useQueryResult(childrenQuery(), {
    enabled: user !== undefined
  })

  const { addNotification, removeNotifications } =
    useContext(NotificationsContext)

  const onNotificationDataLoaded = useEffectEvent(
    (upcomingChildren: ChildWithUpcomingPlacement[], userId: string) => {
      removeNotifications(notificationPrefix)
      upcomingChildren.forEach((child, index) => {
        if (isChildStartingNotificationDismissed(userId, child.id)) {
          return
        }

        addNotification(
          {
            icon: faInfo,
            iconColor: colors.main.m2,
            children: i18n.calendar.infoToast.childStartsInfo(
              formatPersonName(child, 'FirstFirst'),
              child.upcomingPlacementStartDate.format(),
              child.upcomingPlacementUnit.name
            ),
            onClose: () => dismissChildStartingNotification(userId, child.id),
            dataQa: `${notificationPrefix}-${index}`
          },
          `${notificationPrefix}-${index}`
        )
      })
    }
  )

  useEffect(() => {
    if (childrenResult.isSuccess && user) {
      const upcomingChildren = childrenResult.value.filter(hasUpcomingPlacement)
      onNotificationDataLoaded(upcomingChildren, user.id)
    }
  }, [childrenResult, user])
}
