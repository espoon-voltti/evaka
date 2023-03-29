// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  Child,
  ChildDailyRecords,
  ChildRecordOfDay,
  UnitAttendanceReservations
} from 'lib-common/api-types/reservations'
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
import { renderResult } from '../../async-rendering'
import LabelValueList from '../../common/LabelValueList'
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
    (ids: UUID[]) =>
      selectedGroup.type === 'group' && ids.includes(selectedGroup.id),
    [selectedGroup]
  )
  const noFilter = useCallback(() => true, [])

  const combinedData = combine(childReservations, staffAttendances)
  const staffAttendancesStatus = useDataStatus(combinedData)

  const [legendVisible, setLegendVisible] = useState(false)

  const dataForAllChildren = useCallback(
    (childData: UnitAttendanceReservations): ChildDailyRecords[] => {
      const groupedByChild = groupBy(
        childData.groups
          .flatMap(({ children }) => children)
          .concat(childData.ungrouped),
        (cd) => cd.child.id
      )

      const containsBackupGroup = (chd: ChildDailyRecords[]) =>
        chd.some(({ dailyData }) =>
          dailyData.some((data) =>
            Object.keys(data).some((key) => data[key].isInBackupGroup)
          )
        )

      return Object.keys(groupedByChild).flatMap((childId) => {
        const chd = groupedByChild[childId]
        if (containsBackupGroup(chd)) {
          const dailyData = chd
            .map(({ dailyData }) => dailyData)
            .reduce((acc, dailyData) => {
              for (let i = 0; i < dailyData.length; i++) {
                for (const key in dailyData[i]) {
                  if (!dailyData[i][key].isInBackupGroup) {
                    acc[key] = dailyData[i][key]
                  }
                }
              }
              return acc
            }, {} as Record<string, ChildRecordOfDay>)

          return {
            child: chd[0].child,
            dailyData: [dailyData]
          }
        } else {
          return chd
        }
      })
    },
    []
  )

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
            externalAttendances={staffData.extraAttendances}
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
                staffAttendances={staffData.staff}
                externalAttendances={staffData.extraAttendances}
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
                  ? dataForAllChildren(childData)
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
              childServiceNeedInfos={
                selectedGroup.type === 'all-children'
                  ? childData.unitServiceNeedInfo.groups
                      .flatMap(({ childInfos }) => childInfos)
                      .concat(childData.unitServiceNeedInfo.ungrouped)
                  : selectedGroup.type === 'no-group'
                  ? childData.unitServiceNeedInfo.ungrouped
                  : selectedGroup.type === 'group'
                  ? childData.unitServiceNeedInfo.groups.find(
                      (g) => g.groupId === selectedGroup.id
                    )?.childInfos ?? []
                  : []
              }
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
              <AbsenceLegend icons showAdditionalLegendItems />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        )}
      </div>
    </>
  ))
})
