// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { useHistory, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  DailyReservationData,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'
import {
  Desktop,
  MobileAndTablet
} from 'lib-components/layout/responsive-layout'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { isHolidayFormCurrentlyActive } from '../holiday-periods/holiday-period'
import { useHolidayPeriods } from '../holiday-periods/state'

import AbsenceModal from './AbsenceModal'
import ActionPickerModal from './ActionPickerModal'
import CalendarGridView from './CalendarGridView'
import CalendarListView from './CalendarListView'
import DayView from './DayView'
import HolidayModal from './HolidayModal'
import ReservationModal from './ReservationModal'
import { getReservations } from './api'

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

  const { holidayPeriods } = useHolidayPeriods()

  const [data, loadDefaultRange] = useApiState(getReservationsDefaultRange, [])
  const [openModal, setOpenModal] = useState<
    | { type: 'pickAction' | 'reservations' | 'holidays' }
    | { type: 'absences'; initialDate: LocalDate | undefined }
  >()

  const openPickActionModal = useCallback(
    () => setOpenModal({ type: 'pickAction' }),
    []
  )
  const openReservationModal = useCallback(
    () => setOpenModal({ type: 'reservations' }),
    []
  )
  const openHolidayModal = useCallback(
    () => setOpenModal({ type: 'holidays' }),
    []
  )
  const openAbsenceModal = useCallback(
    (initialDate: LocalDate | undefined) =>
      setOpenModal({ type: 'absences', initialDate }),
    []
  )
  const closeModal = useCallback(() => setOpenModal(undefined), [])

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

  const dayIsHolidayPeriod = useCallback(
    (date: LocalDate) =>
      holidayPeriods
        .map((periods) => periods.some((p) => p.period.includes(date)))
        .getOrElse(false),
    [holidayPeriods]
  )

  const isHolidayFormActive: boolean = useMemo(
    () =>
      holidayPeriods
        .map((periods) => periods.some(isHolidayFormCurrentlyActive))
        .getOrElse(false),
    [holidayPeriods]
  )

  if (!user || !user.accessibleFeatures.reservations) return null

  return (
    <>
      {renderResult(data, (response) => (
        <>
          <MobileAndTablet>
            <ContentArea opaque paddingVertical="zero" paddingHorizontal="zero">
              <CalendarListView
                dailyData={response.dailyData}
                onHoverButtonClick={openPickActionModal}
                selectDate={selectDate}
                dayIsReservable={dayIsReservable}
                dayIsHolidayPeriod={dayIsHolidayPeriod}
              />
            </ContentArea>
          </MobileAndTablet>
          <DesktopOnly>
            <Gap size="s" />
            <CalendarGridView
              dailyData={response.dailyData}
              onCreateReservationClicked={openReservationModal}
              onCreateAbsencesClicked={openAbsenceModal}
              onReportHolidaysClicked={openHolidayModal}
              isHolidayFormActive={isHolidayFormActive}
              selectedDate={selectedDate}
              selectDate={selectDate}
              includeWeekends={response.includesWeekends}
              dayIsReservable={dayIsReservable}
            />
          </DesktopOnly>
          {selectedDate && (
            <DayView
              date={selectedDate}
              reservationsResponse={response}
              selectDate={selectDate}
              reloadData={loadDefaultRange}
              close={closeDayView}
              openAbsenceModal={openAbsenceModal}
            />
          )}
          {openModal?.type === 'pickAction' && (
            <ActionPickerModal
              close={closeModal}
              openReservations={openReservationModal}
              openAbsences={openAbsenceModal}
              openHolidays={openHolidayModal}
              isHolidayFormActive={isHolidayFormActive}
            />
          )}
          {openModal?.type === 'reservations' && (
            <ReservationModal
              onClose={closeModal}
              availableChildren={response.children}
              onReload={loadDefaultRange}
              reservableDays={response.reservableDays}
            />
          )}
          {openModal?.type === 'absences' && (
            <AbsenceModal
              close={closeModal}
              initialDate={openModal.initialDate}
              reload={loadDefaultRange}
              availableChildren={response.children}
            />
          )}
          {openModal?.type === 'holidays' && (
            <HolidayModal
              close={closeModal}
              reload={loadDefaultRange}
              availableChildren={response.children}
            />
          )}
        </>
      ))}
      <Footer />
    </>
  )
})

const DesktopOnly = styled(Desktop)`
  position: relative;
`
