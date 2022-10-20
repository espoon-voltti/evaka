// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { MutableRefObject, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import LabelValueList from 'employee-frontend/components/common/LabelValueList'
import { combine, Result } from 'lib-common/api'
import { Child } from 'lib-common/api-types/reservations'
import { UpsertStaffAndExternalAttendanceRequest } from 'lib-common/generated/api-types/attendance'
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

import {
  deleteExternalStaffAttendance,
  deleteStaffAttendance,
  getStaffAttendances,
  postStaffAndExternalAttendances
} from '../../../api/staff-attendance'
import { getUnitAttendanceReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { AbsenceLegend } from '../../absences/AbsenceLegend'
import { WeekData, WeekSavingFns } from '../TabCalendar'

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
  groupId: UUID | 'no-group' | 'staff' | 'all'
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
  isShiftCareUnit: boolean
  realtimeStaffAttendanceEnabled: boolean
  operationalDays: number[]
  groups: DaycareGroup[]
  week: WeekData
  weekSavingFns: MutableRefObject<WeekSavingFns>
}

export default React.memo(function UnitAttendanceReservationsView({
  unitId,
  groupId,
  selectedDate,
  isShiftCareUnit,
  realtimeStaffAttendanceEnabled,
  operationalDays,
  groups,
  week,
  weekSavingFns
}: Props) {
  const { i18n } = useTranslation()

  const [childReservations, reloadChildReservations] = useApiState(
    () =>
      week.savingPromise.then(() =>
        getUnitAttendanceReservations(unitId, week.dateRange)
      ),
    [unitId, week.dateRange, week.savingPromise]
  )

  const [staffAttendances, reloadStaffAttendances] = useApiState(
    () =>
      week.savingPromise.then(() =>
        getStaffAttendances(unitId, week.dateRange)
      ),
    [unitId, week.dateRange, week.savingPromise]
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

  const saveAttendances = useCallback(
    (body: UpsertStaffAndExternalAttendanceRequest) =>
      groupId === 'staff'
        ? postStaffAndExternalAttendances(unitId, body)
        : postStaffAndExternalAttendances(unitId, {
            staffAttendances: body.staffAttendances.map((a) => ({
              ...a,
              groupId
            })),
            externalAttendances: body.externalAttendances.map((a) => ({
              ...a,
              groupId
            }))
          }),
    [groupId, unitId]
  )

  const deleteAttendances = useCallback(
    (
      staffAttendanceIds: UUID[],
      externalStaffAttendanceIds: UUID[]
    ): Promise<Result<void>[]> => {
      const staffDeletes = staffAttendanceIds.map((id) =>
        deleteStaffAttendance(unitId, id)
      )
      const externalDeletes = externalStaffAttendanceIds.map((id) =>
        deleteExternalStaffAttendance(unitId, id)
      )
      return Promise.all([...staffDeletes, ...externalDeletes])
    },
    [unitId]
  )

  const groupFilter = useCallback((id) => id === groupId, [groupId])
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
        {groupId === 'staff' ? (
          groups.length > 0 ? (
            <StaffAttendanceTable
              unitId={unitId}
              operationalDays={childData.operationalDays}
              staffAttendances={staffData.staff}
              extraAttendances={staffData.extraAttendances}
              saveAttendances={saveAttendances}
              deleteAttendances={deleteAttendances}
              reloadStaffAttendances={reloadStaffAttendances}
              groups={groups}
              groupFilter={noFilter}
              defaultGroup={null}
              weekSavingFns={weekSavingFns}
            />
          ) : null
        ) : (
          <>
            {realtimeStaffAttendanceEnabled && (
              <StaffAttendanceTable
                unitId={unitId}
                operationalDays={childData.operationalDays}
                staffAttendances={
                  groupId === 'all'
                    ? staffData.staff
                    : staffData.staff.filter((s) => s.groups.includes(groupId))
                }
                extraAttendances={
                  groupId === 'all'
                    ? staffData.extraAttendances
                    : staffData.extraAttendances.filter(
                        (ea) => ea.groupId === groupId
                      )
                }
                saveAttendances={saveAttendances}
                deleteAttendances={deleteAttendances}
                reloadStaffAttendances={reloadStaffAttendances}
                groups={groups}
                groupFilter={groupFilter}
                defaultGroup={groupId}
                weekSavingFns={weekSavingFns}
              />
            )}
            <ChildReservationsTable
              unitId={unitId}
              operationalDays={childData.operationalDays}
              allDayRows={
                groupId === 'all'
                  ? childData.groups
                      .flatMap(({ children }) => children)
                      .concat(childData.ungrouped)
                  : groupId === 'no-group'
                  ? childData.ungrouped
                  : childData.groups.find((g) => g.group.id === groupId)
                      ?.children ?? []
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
