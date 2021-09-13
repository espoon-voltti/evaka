// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'
import { ContentArea } from 'lib-components/layout/Container'
import { useTranslation } from '../../state/i18n'
import { defaultMargins } from 'lib-components/white-space'
import { H1, H2 } from 'lib-components/typography'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import Absences from '../absences/Absences'
import GroupSelector from './tab-calendar/GroupSelector'
import { UUID } from 'lib-common/types'
import { UnitContext } from '../../state/unit'
import LocalDate from 'lib-common/local-date'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'
import { useQuery } from '../../utils/useQuery'
import { useSyncQueryParams } from '../../utils/useSyncQueryParams'
import { UUID_REGEX } from '../../utils/validation/validations'
import { useParams } from 'react-router-dom'

type Mode = 'week' | 'month'

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: ${defaultMargins.m};
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
        <H1 noMargin>{i18n.unit.calendar.title}</H1>
        {reservationEnabled && (
          <FixedSpaceRow spacing="xs">
            {(['week', 'month'] as const).map((m) => (
              <ChoiceChip
                key={m}
                text={i18n.unit.calendar.modes[m]}
                selected={mode === m}
                onChange={() => setMode(m)}
              />
            ))}
          </FixedSpaceRow>
        )}
      </TopRow>

      <H2 data-qa="calendar-unit-name">
        {unitInformation.isSuccess ? unitInformation.value.daycare.name : ' '}
      </H2>

      <GroupSelectorWrapper>
        <GroupSelector
          unitId={unitId}
          selected={groupId}
          onSelect={setGroupId}
        />
      </GroupSelectorWrapper>

      {mode === 'month' && groupId !== null && groupId !== 'no-group' && (
        <Absences
          groupId={groupId}
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
        />
      )}

      {mode === 'week' && groupId !== null && (
        <UnitAttendanceReservationsView
          unitId={unitId}
          groupId={groupId}
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
        />
      )}
    </ContentArea>
  )
})
