// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router'

import { isLoading } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { ReservationResponseDay } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQuery, useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import { featureFlags } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faInfo } from 'lib-icons'
import { faTreePalm } from 'lib-icons'

import { useTranslation } from '../localization'

import { useCalendarModalState } from './CalendarPage'
import { showSurveyReservationToast } from './discussion-reservation-modal/discussion-survey'
import {
  activeQuestionnaireQuery,
  holidayPeriodsQuery,
  incomeExpirationDatesQuery,
  unansweredChildDocumentsQuery
} from './queries'

type NoCta = { type: 'none' }
type HolidayCta =
  | NoCta
  | { type: 'holiday'; period: FiniteDateRange; deadline: LocalDate }
  | { type: 'questionnaire'; deadline: LocalDate }

interface Props {
  calendarDays: ReservationResponseDay[]
  events: CitizenCalendarEvent[]
}

export default React.memo(function CalendarNotifications({
  calendarDays,
  events
}: Props) {
  const navigate = useNavigate()

  const { addNotification, removeNotification } =
    useContext(NotificationsContext)
  const i18n = useTranslation()

  const { openHolidayModal, openReservationModal, openDiscussionSurveyModal } =
    useCalendarModalState()

  const incomeExpirationDateResult = useQueryResult(
    incomeExpirationDatesQuery()
  )

  const activeDiscussionSurveys = useMemo(
    () =>
      events.filter(
        (e) =>
          e.eventType === 'DISCUSSION_SURVEY' && showSurveyReservationToast(e)
      ),
    [events]
  )

  const {
    data: unansweredChildDocuments,
    isRefetching: unansweredChildDocumentsIsRefetching
  } = useQuery(
    featureFlags.citizenChildDocumentTypes
      ? unansweredChildDocumentsQuery()
      : constantQuery([])
  )

  useEffect(() => {
    if (
      !incomeExpirationDateResult.isSuccess ||
      incomeExpirationDateResult.isReloading
    ) {
      return
    }

    const incomeExpirationDate = incomeExpirationDateResult.value
    if (incomeExpirationDate) {
      addNotification(
        {
          icon: faInfo,
          iconColor: colors.main.m2,
          children: i18n.ctaToast.incomeExpirationCta(
            incomeExpirationDate.format()
          ),
          onClick: () => {
            void navigate('/income')
            removeNotification('expiring-income-cta')
          },
          dataQa: 'expiring-income-cta'
        },
        'expiring-income-cta'
      )
    }
  }, [
    addNotification,
    removeNotification,
    navigate,
    i18n,
    incomeExpirationDateResult
  ])

  const { data: activeQuestionnaire } = useQuery(activeQuestionnaireQuery())
  const { data: holidayPeriods = [] } = useQuery(holidayPeriodsQuery(), {
    enabled: activeQuestionnaire !== undefined
  })
  useEffect(() => {
    if (activeQuestionnaire === undefined) return

    let cta: HolidayCta
    if (
      activeQuestionnaire &&
      Object.keys(activeQuestionnaire.eligibleChildren).some(
        (c) => !activeQuestionnaire.previousAnswers.some((a) => a.childId === c)
      )
    ) {
      cta = {
        type: 'questionnaire',
        deadline: activeQuestionnaire.questionnaire.active.end
      }
    } else {
      const today = LocalDate.todayInSystemTz()
      const activeHolidayPeriod = holidayPeriods.find(
        (p) =>
          p.reservationsOpenOn.isEqualOrBefore(today) &&
          today.isEqualOrBefore(p.reservationDeadline)
      )
      cta =
        activeHolidayPeriod !== undefined &&
        !reservationsExistForPeriod(activeHolidayPeriod.period, calendarDays)
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
  }, [
    activeQuestionnaire,
    addNotification,
    calendarDays,
    holidayPeriods,
    i18n,
    openHolidayModal,
    openReservationModal,
    removeNotification
  ])

  useEffect(() => {
    if (
      featureFlags.discussionReservations &&
      activeDiscussionSurveys.length > 0
    ) {
      addNotification(
        {
          icon: faInfo,
          iconColor: colors.main.m2,
          children: (
            <>
              {i18n.calendar.discussionTimeReservation.surveyToastMessage}
              <Gap size="xs" />
              <Button
                appearance="link"
                text={
                  i18n.calendar.discussionTimeReservation.surveyModalButtonText
                }
              />
            </>
          ),
          onClick: () => {
            openDiscussionSurveyModal()
            removeNotification('active-discussions-cta')
          },
          dataQa: 'active-discussions-cta'
        },
        'active-discussions-cta'
      )
    }
  }, [
    addNotification,
    removeNotification,
    openDiscussionSurveyModal,
    i18n,
    activeDiscussionSurveys.length
  ])

  useEffect(() => {
    if (
      unansweredChildDocuments === undefined ||
      unansweredChildDocumentsIsRefetching
    )
      return

    unansweredChildDocuments.forEach((childDocument) => {
      addNotification(
        {
          icon: faInfo,
          iconColor: colors.status.info,
          onClick(close) {
            void navigate({
              pathname: `/child-documents/${childDocument.id}`,
              search: '?returnTo=calendar'
            })
            close()
          },
          children: i18n.ctaToast.unansweredChildDocumentCta(
            childDocument.child
          ),
          dataQa: `toast-child-document-${childDocument.id}`
        },
        `child-document-${childDocument.id}`
      )
    })
  }, [
    addNotification,
    i18n.ctaToast,
    navigate,
    unansweredChildDocuments,
    unansweredChildDocumentsIsRefetching
  ])

  return (
    <div
      data-holiday-period-cta-status={
        activeQuestionnaire !== undefined && holidayPeriods !== undefined
          ? 'success'
          : 'loading'
      }
      data-expiring-income-cta-status={
        isLoading(incomeExpirationDateResult) ? 'loading' : 'success'
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
      return i18n.ctaToast.holidayPeriodCta(cta.period, cta.deadline)
    case 'questionnaire':
      return i18n.ctaToast.fixedPeriodCta(cta.deadline)
  }
}

function reservationsExistForPeriod(
  period: FiniteDateRange,
  calendarDays: ReservationResponseDay[]
) {
  return calendarDays.every((day) => {
    if (period.includes(day.date)) {
      return day.children.every(
        (child) =>
          child.scheduleType !== 'RESERVATION_REQUIRED' ||
          child.reservations.length > 0 ||
          child.absence !== null
      )
    } else {
      return true
    }
  })
}
