// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
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

  const activeQuestionnaire = useQueryResult(activeQuestionnaireQuery)
  const holidayPeriods = useQueryResult(holidayPeriodsQuery, {
    enabled: activeQuestionnaire.isSuccess
  })

  const holidayCta = combine(
    activeQuestionnaire,
    holidayPeriods
  ).map<HolidayCta>(([questionnaire, periods]) => {
    if (questionnaire) {
      return {
        type: 'questionnaire',
        deadline: questionnaire.questionnaire.active.end
      }
    }

    const today = LocalDate.todayInSystemTz()
    const activeHolidayPeriod = periods.find((p) =>
      p.reservationDeadline?.isEqualOrAfter(today)
    )
    return activeHolidayPeriod?.reservationDeadline
      ? {
          type: 'holiday',
          deadline: activeHolidayPeriod.reservationDeadline,
          period: activeHolidayPeriod.period
        }
      : { type: 'none' }
  })

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
          children: getHolidayCtaText(cta, i18n),
          dataQa: 'holiday-period-cta'
        },
        'holiday-period-cta'
      )
    })
  }, [addNotification, holidayCta, i18n, openHolidayModal, openReservationModal, removeNotification])

  const holidayCtaStatus = useDataStatus(holidayCta)

  return <div data-holiday-period-cta-status={holidayCtaStatus} />
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
