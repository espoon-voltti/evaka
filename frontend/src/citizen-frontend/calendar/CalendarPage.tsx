// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import { useHistory, useLocation } from 'react-router-dom'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import CalendarGridView from './CalendarGridView'
import {
  ChildDailyData,
  DailyReservationData,
  getReservations,
  ReservationsResponse
} from './api'
import { formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import { useTranslation, Translations } from '../localization'
import { useUser } from '../auth'
import ReservationModal from './ReservationModal'
import AbsenceModal from './AbsenceModal'
import DayView from './DayView'
import styled from 'styled-components'
import { desktopMin } from 'lib-components/breakpoints'
import { Gap } from 'lib-components/white-space'
import _ from 'lodash'
import { WeekProps } from './WeekElem'
import ActionPickerModal from './ActionPickerModal'

export default React.memo(function CalendarPage() {
  const history = useHistory()
  const location = useLocation()
  const i18n = useTranslation()
  const user = useUser()

  const [data, setData] = useState<Result<ReservationsResponse>>(Loading.of())
  const [openModal, setOpenModal] = useState<
    'pickAction' | 'reservations' | 'absences'
  >()

  const loadData = useRestApi(getReservations, setData)
  const loadDefaultRange = useCallback(
    () =>
      loadData(
        LocalDate.today().startOfWeek(),
        LocalDate.today().addMonths(2).startOfWeek().subDays(1)
      ),
    [loadData]
  )

  useEffect(loadDefaultRange, [loadDefaultRange])

  const dateParam = new URLSearchParams(location.search).get('day')
  const selectedDate = dateParam ? LocalDate.tryParseIso(dateParam) : undefined
  const selectDate = useCallback(
    (date: LocalDate) =>
      void history.replace(`calendar?day=${date.formatIso()}`),
    [history]
  )
  const closeDayView = useCallback(
    () => void history.replace('/calendar'),
    [history]
  )

  if (!user || !user.accessibleFeatures.reservations) return null

  return (
    <>
      {selectedDate && data.isSuccess ? (
        <DayView
          date={selectedDate}
          data={data.value}
          selectDate={selectDate}
          reloadData={loadDefaultRange}
          close={closeDayView}
        />
      ) : null}
      <Container>
        {data.mapAll({
          loading() {
            return <Loader />
          },
          failure() {
            return <div>{i18n.common.errors.genericGetError}</div>
          },
          success(response) {
            const weeklyData = asWeeklyData(i18n, response.dailyData)

            return (
              <>
                <MobileOnly>
                  <ContentArea
                    opaque
                    paddingVertical="zero"
                    paddingHorizontal="zero"
                  >
                    <CalendarListView
                      weeklyData={weeklyData}
                      onHoverButtonClick={() => setOpenModal('pickAction')}
                      selectDate={selectDate}
                    />
                  </ContentArea>
                </MobileOnly>
                <DesktopOnly>
                  <Gap size="s" />
                  <ContentArea opaque>
                    <CalendarGridView
                      weeklyData={weeklyData}
                      onCreateReservationClicked={() =>
                        setOpenModal('reservations')
                      }
                      onCreateAbsencesClicked={() => setOpenModal('absences')}
                      selectDate={selectDate}
                    />
                  </ContentArea>
                </DesktopOnly>
                {openModal === 'pickAction' && (
                  <ActionPickerModal
                    close={() => setOpenModal(undefined)}
                    openReservations={() => setOpenModal('reservations')}
                    openAbsences={() => setOpenModal('absences')}
                  />
                )}
                {openModal === 'reservations' && (
                  <ReservationModal
                    onClose={() => setOpenModal(undefined)}
                    availableChildren={response.children}
                    onReload={loadDefaultRange}
                    reservableDays={response.reservableDays}
                  />
                )}
                {openModal === 'absences' && (
                  <AbsenceModal
                    close={() => setOpenModal(undefined)}
                    reload={loadDefaultRange}
                    availableChildren={response.children}
                  />
                )}
              </>
            )
          }
        })}
      </Container>
      <Footer />
    </>
  )
})

const MobileOnly = styled.div`
  display: none;
  @media (max-width: ${desktopMin}) {
    display: block;
  }
`

const DesktopOnly = styled.div`
  @media (max-width: ${desktopMin}) {
    display: none;
  }
`

const asWeeklyData = (
  i18n: Translations,
  dailyData: DailyReservationData[]
): WeekProps[] =>
  dailyData.reduce<WeekProps[]>((weekly, daily) => {
    const last = _.last(weekly)
    if (last === undefined || daily.date.getIsoWeek() !== last.weekNumber) {
      return [
        ...weekly,
        {
          weekNumber: daily.date.getIsoWeek(),
          dailyReservations: [
            { ...daily, reservations: uniqueReservations(i18n, daily.children) }
          ]
        }
      ]
    } else {
      return [
        ..._.dropRight(weekly),
        {
          ...last,
          dailyReservations: [
            ...last.dailyReservations,
            { ...daily, reservations: uniqueReservations(i18n, daily.children) }
          ]
        }
      ]
    }
  }, [])

const uniqueReservations = (
  i18n: Translations,
  reservations: ChildDailyData[]
): string[] => {
  const uniqueReservationTimes: string[] = reservations
    .map(({ absence, reservation }) =>
      absence === null && reservation !== null
        ? `${formatDate(reservation.startTime, 'HH:mm')} – ${formatDate(
            reservation.endTime,
            'HH:mm'
          )}`
        : undefined
    )
    .filter((reservation): reservation is string => reservation !== undefined)
    .reduce<string[]>(
      (uniq, reservation) =>
        uniq.some((res) => res === reservation) ? uniq : [...uniq, reservation],
      []
    )

  const someoneIsAbsent = reservations.some(({ absence }) => absence !== null)

  return [
    ...(someoneIsAbsent ? [i18n.calendar.absent] : []),
    ...uniqueReservationTimes
  ]
}
