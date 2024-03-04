// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine, isLoading, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { Child } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { getRealtimeStaffAttendances } from '../../../generated/api-clients/attendance'
import { useTranslation } from '../../../state/i18n'
import { AbsenceLegend } from '../../absences/AbsenceLegend'
import { renderResult } from '../../async-rendering'
import LabelValueList from '../../common/LabelValueList'
import { AttendanceGroupFilter } from '../TabCalendar'
import { unitAttendanceReservationsQuery } from '../queries'

import ChildDateModal, { ChildDateEditorTarget } from './ChildDateModal'
import ChildReservationsTable from './ChildReservationsTable'
import ReservationModalSingleChild from './ReservationModalSingleChild'
import StaffAttendanceTable from './StaffAttendanceTable'

const getRealtimeStaffAttendancesResult = wrapResult(
  getRealtimeStaffAttendances
)

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
  realtimeStaffAttendanceEnabled: boolean
  operationalDays: number[]
  groups: DaycareGroup[]
  weekRange: FiniteDateRange
}

export default React.memo(function UnitAttendanceReservationsView({
  unitId,
  selectedGroup,
  selectedDate,
  realtimeStaffAttendanceEnabled,
  operationalDays,
  groups,
  weekRange
}: Props) {
  const { i18n } = useTranslation()

  const childReservations = useQueryResult(
    unitAttendanceReservationsQuery({
      unitId,
      from: weekRange.start,
      to: weekRange.end,
      includeNonOperationalDays: featureFlags.intermittentShiftCare ?? false
    })
  )

  const [staffAttendances, reloadStaffAttendances] = useApiState(
    () =>
      getRealtimeStaffAttendancesResult({
        unitId,
        start: weekRange.start,
        end: weekRange.end
      }),
    [unitId, weekRange]
  )

  const [creatingReservationChild, setCreatingReservationChild] =
    useState<Child>()

  const [childDateEditorTarget, setChildDateEditorTarget] =
    useState<ChildDateEditorTarget | null>(null)

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

  const [legendVisible, setLegendVisible] = useState(false)

  return renderResult(combinedData, ([childData, staffData]) => (
    <>
      {creatingReservationChild && (
        <ReservationModalSingleChild
          child={creatingReservationChild}
          onClose={() => setCreatingReservationChild(undefined)}
          operationalDays={operationalDays}
        />
      )}
      {childDateEditorTarget && (
        <ChildDateModal
          target={childDateEditorTarget}
          unitId={unitId}
          onClose={() => setChildDateEditorTarget(null)}
        />
      )}

      <FixedSpaceColumn
        spacing="L"
        data-qa="staff-attendances-status"
        data-isloading={isLoading(combinedData)}
      >
        {selectedGroup.type === 'staff' ? (
          <StaffAttendanceTable
            unitId={unitId}
            operationalDays={childData.days}
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
                operationalDays={childData.days}
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
              days={childData.days}
              childBasics={childData.children}
              onMakeReservationForChild={setCreatingReservationChild}
              onOpenEditForChildDate={(childId, date) => {
                const child = childData.children.find((c) => c.id === childId)
                const day = childData.days.find((d) => d.date === date)
                const childDayRecord = day?.children?.find(
                  (c) => c.childId === childId
                )
                if (child && day && childDayRecord) {
                  setChildDateEditorTarget({
                    date,
                    dateInfo: day.dateInfo,
                    child,
                    childDayRecord
                  })
                }
              }}
              selectedDate={selectedDate}
              selectedGroup={selectedGroup}
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
