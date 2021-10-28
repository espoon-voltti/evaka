// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useHistory, useLocation } from 'react-router-dom'
import { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import CalendarGridView from './CalendarGridView'
import { getReservations } from './api'
import LocalDate from 'lib-common/local-date'
import { Result } from 'lib-common/api'
import {
  DailyReservationData,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { useApiState } from 'lib-common/utils/useRestApi'
import { useUser } from '../auth'
import ReservationModal from './ReservationModal'
import AbsenceModal from './AbsenceModal'
import DayView from './DayView'
import styled from 'styled-components'
import { desktopMin } from 'lib-components/breakpoints'
import { Gap } from 'lib-components/white-space'
import ActionPickerModal from './ActionPickerModal'
import { UnwrapResult } from 'citizen-frontend/async-rendering'

async function getReservationsDefaultRange(): Promise<
  Result<ReservationsResponse>
> {
  return await getReservations(
    LocalDate.today().subMonths(1).startOfMonth().startOfWeek(),
    LocalDate.today().addYears(1).lastDayOfMonth()
  )
}

export default React.memo(function CalendarPage() {
  const history = useHistory()
  const location = useLocation()
  const user = useUser()

  const [data, loadDefaultRange] = useApiState(getReservationsDefaultRange, [])
  const [openModal, setOpenModal] = useState<
    'pickAction' | 'reservations' | 'absences'
  >()

  const dateParam = new URLSearchParams(location.search).get('day')
  const selectedDate = dateParam ? LocalDate.tryParseIso(dateParam) : undefined
  const selectDate = useCallback(
    (date: LocalDate) => history.replace(`calendar?day=${date.formatIso()}`),
    [history]
  )
  const closeDayView = useCallback(
    () => history.replace('/calendar'),
    [history]
  )

  const dayIsReservable = useCallback(
    ({ date, isHoliday }: DailyReservationData) =>
      data
        .map(({ children }) =>
          children.some(
            ({ maxOperationalDays, inShiftCareUnit }) =>
              maxOperationalDays.includes(date.getIsoDayOfWeek()) &&
              (inShiftCareUnit || !isHoliday)
          )
        )
        .getOrElse(false),
    [data]
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
          openAbsenceModal={() => setOpenModal('absences')}
        />
      ) : null}
      <UnwrapResult result={data}>
        {(response) => (
          <>
            <MobileOnly>
              <ContentArea
                opaque
                paddingVertical="zero"
                paddingHorizontal="zero"
              >
                <CalendarListView
                  dailyData={response.dailyData}
                  onHoverButtonClick={() => setOpenModal('pickAction')}
                  selectDate={selectDate}
                  dayIsReservable={dayIsReservable}
                />
              </ContentArea>
            </MobileOnly>
            <DesktopOnly>
              <Gap size="s" />
              <CalendarGridView
                dailyData={response.dailyData}
                onCreateReservationClicked={() => setOpenModal('reservations')}
                onCreateAbsencesClicked={() => setOpenModal('absences')}
                selectedDate={selectedDate}
                selectDate={selectDate}
                includeWeekends={response.includesWeekends}
                dayIsReservable={dayIsReservable}
              />
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
        )}
      </UnwrapResult>
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
  position: relative;
  @media (max-width: ${desktopMin}) {
    display: none;
  }
`
