// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useQuery } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { Translations } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { useCalendarModalState } from './CalendarPage'
import { activeQuestionnaireQuery, holidayPeriodsQuery } from './queries'

type NoCta = { type: 'none' }
type HolidayCta =
  | NoCta
  | { type: 'holiday'; period: FiniteDateRange; deadline: LocalDate }
  | { type: 'questionnaire'; deadline: LocalDate }

export default React.memo(function CalendarNotifications() {
  const { addNotification, removeNotification } =
    useContext(NotificationsContext)
  const i18n = useTranslation()

  const { openHolidayModal, openReservationModal } = useCalendarModalState()

  const { data: activeQuestionnaire } = useQuery(activeQuestionnaireQuery)
  const { data: holidayPeriods } = useQuery(holidayPeriodsQuery, {
    enabled: activeQuestionnaire !== undefined,
    onSuccess: (periods) => {
      let cta: HolidayCta
      if (activeQuestionnaire) {
        cta = {
          type: 'questionnaire',
          deadline: activeQuestionnaire.questionnaire.active.end
        }
      } else {
        const today = LocalDate.todayInSystemTz()
        const activeHolidayPeriod = periods.find((p) =>
          p.reservationDeadline?.isEqualOrAfter(today)
        )
        cta = activeHolidayPeriod?.reservationDeadline
          ? {
              type: 'holiday',
              deadline: activeHolidayPeriod.reservationDeadline,
              period: activeHolidayPeriod.period
            }
          : { type: 'none' }
      }

      if (cta.type === 'none') {
        removeNotification('holiday-period-cta')
      } else {
        addNotification(
          {
            icon: faTreePalm,
            iconColor: colors.status.warning,
            onClick() {
              switch (cta.type) {
                case 'questionnaire':
                  openHolidayModal()
                  break
                case 'holiday':
                  openReservationModal(cta.period)
                  break
              }
              return 'close'
            },
            children: getHolidayCtaText(cta, i18n),
            dataQa: 'holiday-period-cta'
          },
          'holiday-period-cta'
        )
      }
    }
  })

  return (
    <div
      data-holiday-period-cta-status={
        activeQuestionnaire !== undefined && holidayPeriods !== undefined
          ? 'success'
          : 'loading'
      }
    />
  )
})

function getHolidayCtaText(
  cta: Exclude<HolidayCta, NoCta>,
  i18n: Translations
) {
  switch (cta.type) {
    case 'holiday':
      return i18n.ctaToast.holidayPeriodCta(
        cta.period.formatCompact(''),
        cta.deadline.format()
      )
    case 'questionnaire':
      return i18n.ctaToast.fixedPeriodCta(cta.deadline.format())
  }
}
