// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { UserContext } from 'employee-frontend/state/user'
import { isLoading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useQuery } from 'lib-common/utils/useQuery'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { UnitData } from '../../api/unit'
import UnitDataFilters from '../../components/unit/UnitDataFilters'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { requireRole } from '../../utils/roles'
import { UUID_REGEX } from '../../utils/validation/validations'
import Absences from '../absences/Absences'
import { renderResult } from '../async-rendering'
import { DataList } from '../common/DataList'

import GroupSelector from './tab-attendances/GroupSelector'
import Occupancy from './tab-unit-information/Occupancy'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'

type CalendarMode = 'week' | 'month'
type GroupId = UUID
type AttendanceGroupFilter = GroupId | 'no-group' | 'staff' | null

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`

const GroupSelectorWrapper = styled.div`
  width: 500px;
  margin-bottom: ${defaultMargins.m};
`

const getDefaultGroup = (groupParam: string): AttendanceGroupFilter | null =>
  ['no-group', 'staff'].includes(groupParam) || UUID_REGEX.test(groupParam)
    ? groupParam
    : null

export default React.memo(function TabAttendances() {
  const { i18n } = useTranslation()
  const { id: unitId } = useNonNullableParams<{ id: UUID }>()
  const { unitInformation, unitData, filters, setFilters } =
    useContext(UnitContext)
  const [mode, setMode] = useState<CalendarMode>(
    featureFlags.experimental?.realtimeStaffAttendance ? 'week' : 'month'
  )
  const [selectedDate, setSelectedDate] = useState<LocalDate>(LocalDate.today())
  const { roles } = useContext(UserContext)

  const groupParam = useQuery().get('group')
  const [groupId, setGroupId] = useState<AttendanceGroupFilter>(() =>
    groupParam ? getDefaultGroup(groupParam) : null
  )
  useSyncQueryParams(
    groupId ? { group: groupId } : ({} as Record<string, string>)
  )

  const reservationEnabled = unitInformation
    .map((u) => u.daycare.enabledPilotFeatures.includes('RESERVATIONS'))
    .getOrElse(false)

  const realtimeStaffAttendanceEnabled = unitInformation
    .map((u) =>
      u.daycare.enabledPilotFeatures.includes('REALTIME_STAFF_ATTENDANCE')
    )
    .getOrElse(false)

  const staffOrNoGroupSelected = groupId === 'staff' || groupId === 'no-group'
  return (
    <>
      <ContentArea
        opaque
        data-qa="unit-attendances"
        data-isloading={isLoading(unitData)}
      >
        <H2 data-qa="attendances-unit-name">
          {unitInformation.isSuccess ? unitInformation.value.daycare.name : ' '}
        </H2>
        <H3>{i18n.unit.occupancies}</H3>
        <Gap size="s" />
        <FixedSpaceRow alignItems="center">
          <Label>{i18n.unit.filters.title}</Label>
          <UnitDataFilters
            canEdit={requireRole(
              roles,
              'ADMIN',
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'FINANCE_ADMIN'
            )}
            filters={filters}
            setFilters={setFilters}
          />
        </FixedSpaceRow>
        <Gap size="s" />
        <DataList>
          <div>
            <label>{i18n.unit.info.caretakers.titleLabel}</label>
            <span data-qa="unit-total-caretaker-count">
              <Caretakers unitData={unitData} />
            </span>
          </div>
        </DataList>
        <Gap />
        {renderResult(unitData, (unitData) =>
          unitData.unitOccupancies ? (
            <Occupancy
              filters={filters}
              occupancies={unitData.unitOccupancies}
              realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
            />
          ) : null
        )}
      </ContentArea>
      <Gap size="s" />
      <ContentArea opaque>
        <TopRow>
          <H3 noMargin>{i18n.unit.attendances.title}</H3>
          {reservationEnabled && !staffOrNoGroupSelected && (
            <FixedSpaceRow spacing="xs">
              {(['week', 'month'] as const).map((m) => (
                <ChoiceChip
                  key={m}
                  data-qa={`choose-calendar-mode-${m}`}
                  text={i18n.unit.attendances.modes[m]}
                  selected={mode === m}
                  onChange={() => setMode(m)}
                />
              ))}
            </FixedSpaceRow>
          )}
        </TopRow>
        <Gap size="xs" />
        <GroupSelectorWrapper>
          <GroupSelector
            unitId={unitId}
            selected={groupId}
            onSelect={setGroupId}
            data-qa="attendances-group-select"
            realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
          />
        </GroupSelectorWrapper>

        {mode === 'month' && groupId !== null && !staffOrNoGroupSelected && (
          <Absences
            groupId={groupId}
            selectedDate={selectedDate}
            setSelectedDate={setSelectedDate}
            reservationEnabled={reservationEnabled}
            staffAttendanceEnabled={!realtimeStaffAttendanceEnabled}
          />
        )}

        {((mode === 'week' && groupId !== null) || staffOrNoGroupSelected) && (
          <UnitAttendanceReservationsView
            unitId={unitId}
            groupId={groupId}
            selectedDate={selectedDate}
            setSelectedDate={setSelectedDate}
            isShiftCareUnit={unitInformation
              .map(({ daycare }) => daycare.roundTheClock)
              .getOrElse(false)}
            operationalDays={
              unitInformation
                .map(({ daycare }) => daycare.operationDays)
                .getOrElse(null) ?? [1, 2, 3, 4, 5]
            }
            realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
          />
        )}
        <Gap size="L" />
      </ContentArea>
    </>
  )
})

const Caretakers = React.memo(function Caretakers({
  unitData
}: {
  unitData: Result<UnitData>
}) {
  const { i18n } = useTranslation()

  const formatNumber = (num: number) =>
    parseFloat(num.toFixed(2)).toLocaleString()

  return unitData
    .map((unitData) => {
      const min = formatNumber(unitData.caretakers.unitCaretakers.minimum)
      const max = formatNumber(unitData.caretakers.unitCaretakers.maximum)

      return min === max ? (
        <span>
          {min} {i18n.unit.info.caretakers.unitOfValue}
        </span>
      ) : (
        <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
      )
    })
    .getOrElse(null)
})
