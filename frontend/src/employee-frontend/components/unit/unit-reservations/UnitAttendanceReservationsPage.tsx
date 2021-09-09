// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { faChevronLeft, faChevronRight } from 'lib-icons'
import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
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

export default React.memo(function UnitAttendanceReservationsPage() {
  const { unitId } = useParams<{ unitId: string }>()
  const { i18n } = useTranslation()

  const [dateRange, setDateRange] = useState(
    getWeekDateRange(LocalDate.today())
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
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {reservations.mapAll({
          loading() {
            return <Loader />
          },
          failure() {
            return <div>{i18n.common.error.unknown}</div>
          },
          success(data) {
            return (
              <>
                {creatingReservationChild && (
                  <ReservationModalSingleChild
                    child={creatingReservationChild}
                    onReload={reload}
                    onClose={() => setCreatingReservationChild(null)}
                  />
                )}

                <H2>{data.unit}</H2>
                <Gap size="m" />
                <WeekPicker>
                  <WeekPickerButton
                    icon={faChevronLeft}
                    onClick={() =>
                      setDateRange(getWeekDateRange(dateRange.start.subDays(7)))
                    }
                    size="s"
                  />
                  <H3 noMargin>
                    {`${dateRange.start.format(
                      'dd.MM.'
                    )} - ${dateRange.end.format()}`}
                  </H3>
                  <WeekPickerButton
                    icon={faChevronRight}
                    onClick={() =>
                      setDateRange(getWeekDateRange(dateRange.start.addDays(7)))
                    }
                    size="s"
                  />
                </WeekPicker>
                <Gap size="s" />
                <FixedSpaceColumn spacing="L">
                  {data.groups.map(({ group, children }) => (
                    <div key={group}>
                      <H3 noMargin>{group}</H3>
                      <ReservationsTable
                        operationalDays={data.operationalDays}
                        reservations={children}
                        onMakeReservationForChild={setCreatingReservationChild}
                      />
                    </div>
                  ))}
                  {data.ungrouped.length > 0 ? (
                    <div>
                      <H3 noMargin>
                        {i18n.unit.attendanceReservations.ungrouped}
                      </H3>
                      <ReservationsTable
                        operationalDays={data.operationalDays}
                        reservations={data.ungrouped}
                        onMakeReservationForChild={setCreatingReservationChild}
                      />
                    </div>
                  ) : null}
                </FixedSpaceColumn>
              </>
            )
          }
        })}
      </ContentArea>
      <Gap size="L" />
    </Container>
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
