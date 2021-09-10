// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { faChevronLeft, faChevronRight } from 'lib-icons'
import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  CalendarChild,
  getUnitAttendanceReservations,
  UnitAttendanceReservations
} from 'employee-frontend/api/unit'
import { useTranslation } from '../../../state/i18n'
import ReservationsTable from './ReservationsTable'
import ReservationModalSingleChild from './ReservationModalSingleChild'
import { UUID } from 'lib-common/types'

interface Props {
  unitId: UUID
  groupId: UUID | 'no-group'
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
}

export default React.memo(function UnitAttendanceReservationsView({
  unitId,
  groupId,
  selectedDate,
  setSelectedDate
}: Props) {
  const { i18n } = useTranslation()

  const dateRange = useMemo(
    () => getWeekDateRange(selectedDate),
    [selectedDate]
  )

  const [reservations, setReservations] = useState<
    Result<UnitAttendanceReservations>
  >(Loading.of())
  const loadAttendanceReservations = useRestApi(
    getUnitAttendanceReservations,
    setReservations
  )

  const reload = useCallback(
    () => loadAttendanceReservations(unitId, dateRange),
    [unitId, dateRange, loadAttendanceReservations]
  )
  useEffect(reload, [reload])

  const [creatingReservationChild, setCreatingReservationChild] =
    useState<CalendarChild | null>(null)

  return (
    <>
      {reservations.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <div>{i18n.common.error.unknown}</div>
        },
        success(data) {
          const selectedGroup = data.groups.find((g) => g.group.id === groupId)

          return (
            <>
              {creatingReservationChild && (
                <ReservationModalSingleChild
                  child={creatingReservationChild}
                  onReload={reload}
                  onClose={() => setCreatingReservationChild(null)}
                />
              )}

              <WeekPicker>
                <WeekPickerButton
                  icon={faChevronLeft}
                  onClick={() => setSelectedDate(selectedDate.addDays(-7))}
                  size="s"
                />
                <H3 noMargin>
                  {`${dateRange.start.format(
                    'dd.MM.'
                  )} - ${dateRange.end.format()}`}
                </H3>
                <WeekPickerButton
                  icon={faChevronRight}
                  onClick={() => setSelectedDate(selectedDate.addDays(7))}
                  size="s"
                />
              </WeekPicker>
              <Gap size="s" />
              <FixedSpaceColumn spacing="L">
                {selectedGroup && (
                  <ReservationsTable
                    operationalDays={data.operationalDays}
                    reservations={selectedGroup.children}
                    onMakeReservationForChild={setCreatingReservationChild}
                  />
                )}
                {groupId === 'no-group' && (
                  <ReservationsTable
                    operationalDays={data.operationalDays}
                    reservations={data.ungrouped}
                    onMakeReservationForChild={setCreatingReservationChild}
                  />
                )}
              </FixedSpaceColumn>
            </>
          )
        }
      })}
      <Gap size="L" />
    </>
  )
})

const getWeekDateRange = (date: LocalDate) =>
  new FiniteDateRange(date.startOfWeek(), date.startOfWeek().addDays(6))

const WeekPicker = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
`

const WeekPickerButton = styled(IconButton)`
  margin: 0 ${defaultMargins.s};
  color: ${colors.greyscale.dark};
`
