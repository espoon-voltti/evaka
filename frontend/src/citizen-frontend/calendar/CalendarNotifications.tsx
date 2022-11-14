// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'

import {
  HolidayCta,
  NoCta,
  useHolidayPeriods
} from 'citizen-frontend/holiday-periods/state'
import { useTranslation } from 'citizen-frontend/localization'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import { NotificationsContext } from 'lib-components/Notifications'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { useCalendarModalState } from './CalendarPage'

export default React.memo(function CalendarNotifications() {
  const { addNotification, removeNotification } =
    useContext(NotificationsContext)
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

  const { openHolidayModal, openReservationModal } = useCalendarModalState()
  const { holidayCta } = useHolidayPeriods()

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
            switch (cta.type) {
              case 'questionnaire':
                openHolidayModal()
                return 'close'
              case 'holiday':
                openReservationModal(cta.period)
                return 'close'
            }
          },
          children: getHolidayCtaText(cta),
          dataQa: 'holiday-period-cta'
        },
        'holiday-period-cta'
      )
    })
  }, [addNotification, getHolidayCtaText, holidayCta, i18n.ctaToast, openHolidayModal, openReservationModal, removeNotification])

  const holidayCtaStatus = useDataStatus(holidayCta)

  return <div data-holiday-period-cta-status={holidayCtaStatus} />
})
