// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInfo } from 'Icons'
import React, { useContext } from 'react'
import { useNavigate } from 'react-router-dom'

import { useTranslation } from 'citizen-frontend/localization'
import FiniteDateRange from 'lib-common/finite-date-range'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useQuery } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { Translations } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faTreePalm } from 'lib-icons'

import { useCalendarModalState } from './CalendarPage'
import {
  activeQuestionnaireQuery,
  holidayPeriodsQuery,
  incomeExpirationDatesQuery
} from './queries'

type NoCta = { type: 'none' }
type HolidayCta =
  | NoCta
  | { type: 'holiday'; period: FiniteDateRange; deadline: LocalDate }
  | { type: 'questionnaire'; deadline: LocalDate }

interface Props {
  reservationChildren: ReservationChild[]
  dailyData: DailyReservationData[]
}

export default React.memo(function CalendarNotifications({
  reservationChildren,
  dailyData
}: Props) {
  const navigate = useNavigate()

  const { addNotification, removeNotification } =
    useContext(NotificationsContext)
  const i18n = useTranslation()

  const { openHolidayModal, openReservationModal } = useCalendarModalState()

  const { data: incomeExpirationDate } = useQuery(incomeExpirationDatesQuery, {
    onSuccess: (incomeExpirationDate) => {
      if (incomeExpirationDate) {
        addNotification(
          {
            icon: faInfo,
            iconColor: colors.main.m2,
            children: i18n.ctaToast.incomeExpirationCta(
              incomeExpirationDate.format()
            ),
            onClick: () => {
              navigate('/income')
              removeNotification('expiring-income-cta')
            },
            dataQa: 'expiring-income-cta'
          },
          'expiring-income-cta'
        )
      }
    }
  })
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
          p.reservationDeadline.isEqualOrAfter(today)
        )
        cta =
          activeHolidayPeriod !== undefined &&
          !hasReservationsForHolidayPeriod(
            reservationChildren,
            dailyData,
            activeHolidayPeriod
          )
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
                  removeNotification('holiday-period-cta')
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
      data-expiring-income-cta-status={
        incomeExpirationDate !== undefined ? 'success' : 'loading'
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

function hasReservationsForHolidayPeriod(
  children: ReservationChild[],
  dailyData: DailyReservationData[],
  holidayPeriod: HolidayPeriod
) {
  return [...holidayPeriod.period.dates()].every((date) => {
    const day = dailyData.find((d) => d.date.isEqual(date))
    if (day === undefined) return false

    return children
      .filter((child) => child.placements.some((p) => p.includes(date)))
      .every((child) =>
        day.children.some(
          (c) =>
            c.childId === child.id &&
            (c.reservations.length > 0 || c.absence !== null)
        )
      )
  })
}
