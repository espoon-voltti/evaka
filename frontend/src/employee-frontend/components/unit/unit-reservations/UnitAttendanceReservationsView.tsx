// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { renderResult } from 'employee-frontend/components/async-rendering'
import { Child } from 'lib-common/api-types/reservations'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AbsenceType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronLeft, faChevronRight } from 'lib-icons'
import { getUnitAttendanceReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { AbsenceLegend } from '../../absences/AbsenceLegend'
import ReservationModalSingleChild from './ReservationModalSingleChild'
import ReservationsTable from './ReservationsTable'

const legendAbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE'
]

interface Props {
  unitId: UUID
  groupId: UUID | 'no-group'
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
  isShiftCareUnit: boolean
  operationalDays: number[]
}

export default React.memo(function UnitAttendanceReservationsView({
  unitId,
  groupId,
  selectedDate,
  setSelectedDate,
  isShiftCareUnit,
  operationalDays
}: Props) {
  const { i18n } = useTranslation()
  const dateRange = useMemo(
    () => getWeekDateRange(selectedDate),
    [selectedDate]
  )

  const [reservations, reload] = useApiState(
    () => getUnitAttendanceReservations(unitId, dateRange),
    [unitId, dateRange]
  )

  const [creatingReservationChild, setCreatingReservationChild] =
    useState<Child | null>(null)

  return (
    <>
      {renderResult(reservations, (data) => {
        const selectedGroup = data.groups.find((g) => g.group.id === groupId)

        return (
          <>
            {creatingReservationChild && (
              <ReservationModalSingleChild
                child={creatingReservationChild}
                onReload={reload}
                onClose={() => setCreatingReservationChild(null)}
                isShiftCareUnit={isShiftCareUnit}
                operationalDays={operationalDays}
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
                  selectedDate={selectedDate}
                />
              )}
              {groupId === 'no-group' && (
                <ReservationsTable
                  operationalDays={data.operationalDays}
                  reservations={data.ungrouped}
                  onMakeReservationForChild={setCreatingReservationChild}
                  selectedDate={selectedDate}
                />
              )}
            </FixedSpaceColumn>

            <div>
              <HorizontalLine dashed slim />
              <H3>{i18n.absences.legendTitle}</H3>
              <FixedSpaceColumn spacing="xs">
                <AbsenceLegend icons absenceTypes={legendAbsenceTypes} />
              </FixedSpaceColumn>
            </div>
          </>
        )
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
  color: ${colors.grayscale.g70};
`
