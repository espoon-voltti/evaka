// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import { useHistory, useLocation } from 'react-router-dom'
import { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import CalendarGridView from './CalendarGridView'
import { getReservations } from './api'
import LocalDate from 'lib-common/local-date'
import { Loading, Result } from 'lib-common/api'
import { ReservationsResponse } from 'lib-common/generated/api-types/reservations'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useTranslation } from '../localization'
import { useUser } from '../auth'
import ReservationModal from './ReservationModal'
import AbsenceModal from './AbsenceModal'
import DayView from './DayView'

import styled from 'styled-components'
import { desktopMin } from 'lib-components/breakpoints'
import { Gap } from 'lib-components/white-space'
import _ from 'lodash'
import ActionPickerModal from './ActionPickerModal'
import { UnwrapResult } from 'citizen-frontend/async-rendering'

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
        LocalDate.today().subMonths(1).startOfMonth().startOfWeek(),
        LocalDate.today().addYears(1).lastDayOfMonth()
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
          openAbsenceModal={() => setOpenModal('absences')}
        />
      ) : null}
      <UnwrapResult
        result={data}
        failure={() => <div>{i18n.common.errors.genericGetError}</div>}
      >
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
