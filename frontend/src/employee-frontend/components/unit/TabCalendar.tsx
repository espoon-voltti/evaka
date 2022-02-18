// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useQuery } from 'lib-common/utils/useQuery'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { UUID_REGEX } from '../../utils/validation/validations'
import Absences from '../absences/Absences'

import GroupSelector from './tab-calendar/GroupSelector'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'

type Mode = 'week' | 'month'

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

export default React.memo(function TabCalendar() {
  const { i18n } = useTranslation()
  const { id: unitId } = useParams<{ id: UUID }>()
  const { unitInformation } = useContext(UnitContext)
  const [mode, setMode] = useState<Mode>('month')
  const [selectedDate, setSelectedDate] = useState<LocalDate>(LocalDate.today())

  const groupParam = useQuery().get('group')
  const defaultGroup =
    groupParam && (groupParam === 'no-group' || UUID_REGEX.test(groupParam))
      ? groupParam
      : null
  const [groupId, setGroupId] = useState<UUID | 'no-group' | null>(defaultGroup)
  useSyncQueryParams(
    groupId ? { group: groupId } : ({} as Record<string, string>)
  )

  const reservationEnabled = unitInformation
    .map((u) => u.daycare.enabledPilotFeatures.includes('RESERVATIONS'))
    .getOrElse(false)

  return (
    <ContentArea opaque>
      <TopRow>
        <H2>{i18n.unit.calendar.title}</H2>
        {reservationEnabled && (
          <FixedSpaceRow spacing="xs">
            {(['week', 'month'] as const).map((m) => (
              <ChoiceChip
                key={m}
                data-qa={`choose-calendar-mode-${m}`}
                text={i18n.unit.calendar.modes[m]}
                selected={mode === m}
                onChange={() => setMode(m)}
              />
            ))}
          </FixedSpaceRow>
        )}
      </TopRow>

      <H3 noMargin data-qa="calendar-unit-name">
        {unitInformation.isSuccess ? unitInformation.value.daycare.name : ' '}
      </H3>
      <Gap size="xs" />
      <GroupSelectorWrapper>
        <GroupSelector
          unitId={unitId}
          selected={groupId}
          onSelect={setGroupId}
          data-qa="calendar-group-select"
        />
      </GroupSelectorWrapper>

      {mode === 'month' && groupId !== null && groupId !== 'no-group' && (
        <Absences
          groupId={groupId}
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
          reservationEnabled={reservationEnabled}
        />
      )}

      {mode === 'week' && groupId !== null && (
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
        />
      )}
    </ContentArea>
  )
})
