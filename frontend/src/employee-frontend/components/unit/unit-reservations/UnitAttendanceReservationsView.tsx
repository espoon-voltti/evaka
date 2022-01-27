// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { renderResult } from 'employee-frontend/components/async-rendering'
import LabelValueList from 'employee-frontend/components/common/LabelValueList'
import { Child } from 'lib-common/api-types/reservations'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AbsenceType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H3, Title } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronLeft, faChevronRight } from 'lib-icons'
import { getUnitAttendanceReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { AbsenceLegend } from '../../absences/AbsenceLegend'
import ReservationModalSingleChild from './ReservationModalSingleChild'
import ReservationsTable from './ReservationsTable'

const Time = styled.span`
  font-weight: ${fontWeights.normal};
  display: inline-block;
  // match absence legend row height
  min-height: 22px;
  padding: 1px 4px;
`

const AttendanceTime = styled(Time)`
  font-weight: ${fontWeights.semibold};
  background: ${colors.grayscale.g4};
`

const legendAbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE'
]

const formatWeekTitle = (dateRange: FiniteDateRange) =>
  `${dateRange.start.format('dd.MM.')} - ${dateRange.end.format()}`

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

  const legendTimeLabels = useMemo(() => {
    const t = i18n.unit.attendanceReservations.legend
    const indicator = i18n.unit.attendanceReservations.serviceTimeIndicator
    return Object.entries({
      [t.reservation]: <Time>{t.hhmm}</Time>,
      [t.serviceTime]: (
        <Time>
          {t.hhmm} {indicator}
        </Time>
      ),
      [t.attendanceTime]: <AttendanceTime>{t.hhmm}</AttendanceTime>
    }).map(([value, label]) => ({ label, value }))
  }, [i18n])

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
              <WeekTitle primary centered>
                {formatWeekTitle(dateRange)}
              </WeekTitle>
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
              <FixedSpaceRow alignItems="flex-start" spacing="XL">
                <LabelValueList
                  spacing="small"
                  horizontalSpacing="small"
                  labelWidth="fit-content(40%)"
                  contents={legendTimeLabels}
                />
                <FixedSpaceColumn spacing="xs">
                  <AbsenceLegend icons absenceTypes={legendAbsenceTypes} />
                </FixedSpaceColumn>
              </FixedSpaceRow>
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

const WeekTitle = styled(Title)`
  min-width: 14ch;
`
