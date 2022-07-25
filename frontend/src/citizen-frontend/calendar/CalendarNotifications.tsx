// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEmpty from 'lodash/isEmpty'
import omit from 'lodash/omit'
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'

import {
  HolidayCta,
  NoCta,
  useHolidayPeriods
} from 'citizen-frontend/holiday-periods/state'
import { useTranslation } from 'citizen-frontend/localization'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Toast from 'lib-components/molecules/Toast'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { useCalendarModalState } from './CalendarPage'

interface Notification {
  icon: IconDefinition
  iconColor: string
  onClick?: () => void | 'close'
  onClose?: () => void
  children: React.ReactNode
  dataQa?: string
}

export const CalendarNotificationsContext = createContext<{
  notifications: Record<string, Notification>
  removeNotification: (id: string) => void
  addNotification: (notification: Notification, customId?: string) => string
}>({
  notifications: {},
  removeNotification: () => undefined,
  addNotification: () => ''
})

let idCounter = 1

export const CalendarNotificationsProvider = React.memo(
  function CalendarNotificationsProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [notifications, setNotifications] = useState({})

    const i18n = useTranslation()

    const getHolidayCtaText = useCallback(
      (cta: Exclude<HolidayCta, NoCta>) => {
        switch (cta.type) {
          case 'holiday':
            return i18n.ctaToast.holidayPeriodCta(
              cta.period.formatCompact(''),
              cta.deadline.format()
            )
          case 'questionnaire':
            return i18n.ctaToast.fixedPeriodCta(cta.deadline.format())
        }
      },
      [i18n]
    )

    const { openHolidayModal } = useCalendarModalState()
    const { holidayCta } = useHolidayPeriods()

    const addNotification = useCallback(
      (notification: Notification, customId?: string) => {
        const id = customId ?? (idCounter++).toString(10)
        setNotifications((notifs) => ({
          ...notifs,
          [id]: notification
        }))
        return id
      },
      []
    )

    const removeNotification = useCallback((id: string) => {
      setNotifications((notifs) => omit(notifs, id))
    }, [])

    useEffect(() => {
      holidayCta.map((cta) => {
        if (cta.type === 'none') {
          removeNotification('holiday-period-cta')
          return
        }

        addNotification(
          {
            icon: faTreePalm,
            iconColor: colors.status.warning,
            onClick() {
              if (cta.type === 'questionnaire') {
                openHolidayModal()
                return 'close'
              }

              return undefined
            },
            children: getHolidayCtaText(cta),
            dataQa: 'holiday-period-cta'
          },
          'holiday-period-cta'
        )
      })
    }, [
      addNotification,
      getHolidayCtaText,
      holidayCta,
      i18n.ctaToast,
      openHolidayModal,
      removeNotification
    ])

    const holidayCtaStatus = useDataStatus(holidayCta)

    return (
      <CalendarNotificationsContext.Provider
        value={{
          notifications,
          addNotification,
          removeNotification
        }}
      >
        <div data-holiday-period-cta-status={holidayCtaStatus} />
        {children}
      </CalendarNotificationsContext.Provider>
    )
  }
)

export const CalendarNotificationsSlot = React.memo(
  function CalendarNotificationsSlot() {
    const { notifications, removeNotification } = useContext(
      CalendarNotificationsContext
    )

    if (isEmpty(notifications)) {
      return null
    }

    return (
      <FixedSpaceColumn spacing="s">
        {Object.entries(notifications).map(
          ([id, { children, onClose, onClick, ...props }]) => (
            <Toast
              {...props}
              key={id}
              onClose={() => {
                removeNotification(id)
                onClose?.()
              }}
              onClick={() => {
                if (onClick?.() === 'close') {
                  removeNotification(id)
                }
              }}
            >
              {children}
            </Toast>
          )
        )}
      </FixedSpaceColumn>
    )
  }
)
