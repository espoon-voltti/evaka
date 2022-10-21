// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import LabelValueList from 'employee-frontend/components/common/LabelValueList'
import { combine } from 'lib-common/api'
import { Child } from 'lib-common/api-types/reservations'
import FiniteDateRange from 'lib-common/finite-date-range'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useDataStatus } from 'lib-common/utils/result-to-data-status'
import { useApiState } from 'lib-common/utils/useRestApi'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { getStaffAttendances } from '../../../api/staff-attendance'
import { getUnitAttendanceReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { AbsenceLegend } from '../../absences/AbsenceLegend'
import { AttendanceGroupFilter } from '../TabCalendar'

import ChildReservationsTable from './ChildReservationsTable'
import ReservationModalSingleChild from './ReservationModalSingleChild'
import StaffAttendanceTable from './StaffAttendanceTable'

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

interface Props {
  unitId: UUID
  selectedGroup: AttendanceGroupFilter
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
  isShiftCareUnit: boolean
  realtimeStaffAttendanceEnabled: boolean
  operationalDays: number[]
  groups: DaycareGroup[]
  weekRange: FiniteDateRange
}

export default React.memo(function UnitAttendanceReservationsView({
  unitId,
  selectedGroup,
  selectedDate,
  isShiftCareUnit,
  realtimeStaffAttendanceEnabled,
  operationalDays,
  groups,
  weekRange
}: Props) {
  const { i18n } = useTranslation()

  const [childReservations, reloadChildReservations] = useApiState(
    () => getUnitAttendanceReservations(unitId, weekRange),
    [unitId, weekRange]
  )

  const [staffAttendances, reloadStaffAttendances] = useApiState(
    () => getStaffAttendances(unitId, weekRange),
    [unitId, weekRange]
  )

  const [creatingReservationChild, setCreatingReservationChild] =
    useState<Child>()

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

  const groupFilter = useCallback(
    (id) => selectedGroup.type === 'group' && selectedGroup.id === id,
    [selectedGroup]
  )
  const noFilter = useCallback(() => true, [])

  const combinedData = combine(childReservations, staffAttendances)
  const staffAttendancesStatus = useDataStatus(combinedData)

  const [legendVisible, setLegendVisible] = useState(false)

  return renderResult(combinedData, ([childData, staffData]) => (
    <>
      <div
        data-qa="staff-attendances-status"
        data-qa-value={staffAttendancesStatus}
      />

      {creatingReservationChild && (
        <ReservationModalSingleChild
          child={creatingReservationChild}
          onReload={reloadChildReservations}
          onClose={() => setCreatingReservationChild(undefined)}
          isShiftCareUnit={isShiftCareUnit}
          operationalDays={operationalDays}
        />
      )}

      <FixedSpaceColumn spacing="L">
        {selectedGroup.type === 'staff' ? (
          <StaffAttendanceTable
            unitId={unitId}
            operationalDays={childData.operationalDays}
            staffAttendances={staffData.staff}
            extraAttendances={staffData.extraAttendances}
            reloadStaffAttendances={reloadStaffAttendances}
            groups={groups}
            groupFilter={noFilter}
            defaultGroup={null}
          />
        ) : (
          <>
            {realtimeStaffAttendanceEnabled &&
            selectedGroup.type === 'group' ? (
              <StaffAttendanceTable
                unitId={unitId}
                operationalDays={childData.operationalDays}
                staffAttendances={staffData.staff.filter((s) =>
                  s.groups.includes(selectedGroup.id)
                )}
                extraAttendances={staffData.extraAttendances.filter(
                  (ea) => ea.groupId === selectedGroup.id
                )}
                reloadStaffAttendances={reloadStaffAttendances}
                groups={groups}
                groupFilter={groupFilter}
                defaultGroup={selectedGroup.id}
              />
            ) : null}
            <ChildReservationsTable
              unitId={unitId}
              operationalDays={childData.operationalDays}
              allDayRows={
                selectedGroup.type === 'all-children'
                  ? childData.groups
                      .flatMap(({ children }) => children)
                      .concat(childData.ungrouped)
                  : selectedGroup.type === 'no-group'
                  ? childData.ungrouped
                  : selectedGroup.type === 'group'
                  ? childData.groups.find(
                      (g) => g.group.id === selectedGroup.id
                    )?.children ?? []
                  : []
              }
              onMakeReservationForChild={setCreatingReservationChild}
              selectedDate={selectedDate}
              reloadReservations={reloadChildReservations}
            />
          </>
        )}
      </FixedSpaceColumn>

      <div>
        <Gap size="s" />
        <FixedSpaceRow alignItems="center">
          <Label id="legend-title-label">{i18n.absences.legendTitle}</Label>
          <IconButton
            icon={legendVisible ? faChevronUp : faChevronDown}
            onClick={() => setLegendVisible(!legendVisible)}
            aria-labelledby="legend-title-label"
          />
        </FixedSpaceRow>
        {legendVisible && (
          <FixedSpaceRow alignItems="flex-start" spacing="XL">
            <LabelValueList
              spacing="small"
              horizontalSpacing="small"
              labelWidth="fit-content(40%)"
              contents={legendTimeLabels}
            />
            <FixedSpaceColumn spacing="xs">
              <AbsenceLegend icons />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        )}
      </div>
    </>
  ))
})
