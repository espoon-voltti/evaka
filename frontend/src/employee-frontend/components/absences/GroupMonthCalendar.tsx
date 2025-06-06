// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { AbsenceCategory } from 'lib-common/generated/api-types/absence'
import type { GroupMonthCalendar } from 'lib-common/generated/api-types/absence'
import type { ChildId, GroupId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import type { MutateAsyncFn } from 'lib-common/query'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import type { AbsenceUpdate } from '../../types/absence'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'

import { AbsenceLegend } from './AbsenceLegend'
import AbsenceModal from './AbsenceModal'
import MonthCalendarTable from './MonthCalendarTable'
import {
  addPresencesMutation,
  deleteHolidayReservationsMutation,
  groupMonthCalendarQuery,
  upsertAbsencesMutation
} from './queries'

interface Props {
  groupId: GroupId
  selectedDate: LocalDate
  reservationEnabled: boolean
  staffAttendanceEnabled: boolean
}

export default React.memo(function GroupMonthCalendarWrapper({
  groupId,
  selectedDate,
  reservationEnabled,
  staffAttendanceEnabled
}: Props) {
  const selectedYear = selectedDate.getYear()
  const selectedMonth = selectedDate.getMonth()
  const groupMonthCalendar = useQueryResult(
    groupMonthCalendarQuery({
      groupId,
      year: selectedYear,
      month: selectedMonth
    })
  )

  return (
    <div data-qa="absences-page">
      {renderResult(groupMonthCalendar, (groupMonthCalendar) => (
        <GroupMonthCalendar
          groupId={groupId}
          groupMonthCalendar={groupMonthCalendar}
          selectedDate={selectedDate}
          reservationEnabled={reservationEnabled}
          staffAttendanceEnabled={staffAttendanceEnabled}
        />
      ))}
    </div>
  )
})

interface AbsenceCalendarProps {
  groupId: GroupId
  groupMonthCalendar: GroupMonthCalendar
  selectedDate: LocalDate
  reservationEnabled: boolean
  staffAttendanceEnabled: boolean
}

export interface SelectedCell {
  childId: ChildId
  date: LocalDate
  absenceCategories: AbsenceCategory[]
}

const GroupMonthCalendar = React.memo(function GroupMonthCalendar({
  groupId,
  groupMonthCalendar,
  selectedDate,
  reservationEnabled,
  staffAttendanceEnabled
}: AbsenceCalendarProps) {
  const { i18n } = useTranslation()
  const [modalVisible, useModalVisible] = useBoolean(false)
  const [selectedCells, setSelectedCells] = useState<SelectedCell[]>([])
  const { mutateAsync: upsertAbsences } = useMutationResult(
    upsertAbsencesMutation
  )
  const { mutateAsync: addPresences } = useMutationResult(addPresencesMutation)
  const { mutateAsync: deleteHolidayReservations } = useMutationResult(
    deleteHolidayReservationsMutation
  )

  const closeModal = useCallback(() => {
    useModalVisible.off()
    setSelectedCells([])
  }, [useModalVisible])

  const showCategorySelection = useMemo(
    () => selectedCells.some((cell) => cell.absenceCategories.length > 1),
    [selectedCells]
  )

  const showMissingHolidayReservation = useMemo(
    () =>
      selectedCells.some((cell) =>
        groupMonthCalendar.days.some(
          (day) => day.date.isEqual(cell.date) && day.isInHolidayPeriod
        )
      ),
    [groupMonthCalendar.days, selectedCells]
  )

  const toggleCellSelection = useCallback((cell: SelectedCell) => {
    setSelectedCells((cells) => {
      const removed = cells.filter(
        (c) => !(c.childId === cell.childId && c.date.isEqual(cell.date))
      )
      if (removed.length === cells.length) {
        return [...cells, cell]
      } else {
        return removed
      }
    })
  }, [])

  useTitle(
    `${groupMonthCalendar.groupName} | ${groupMonthCalendar.daycareName}`
  )

  const updateAbsences = useCallback(
    (update: AbsenceUpdate) =>
      sendAbsenceUpdates(
        groupId,
        selectedCells,
        update,
        upsertAbsences,
        addPresences,
        deleteHolidayReservations
      ),
    [
      addPresences,
      deleteHolidayReservations,
      groupId,
      selectedCells,
      upsertAbsences
    ]
  )

  return (
    <FixedSpaceColumn spacing="zero">
      {modalVisible && selectedCells.length > 0 && (
        <AbsenceModal
          showCategorySelection={showCategorySelection}
          showMissingHolidayReservation={showMissingHolidayReservation}
          onSave={updateAbsences}
          onSuccess={closeModal}
          onClose={closeModal}
        />
      )}
      <MonthCalendarTable
        groupId={groupId}
        groupMonthCalendar={groupMonthCalendar}
        selectedCells={selectedCells}
        toggleCellSelection={toggleCellSelection}
        selectedDate={selectedDate}
        reservationEnabled={reservationEnabled}
        staffAttendanceEnabled={staffAttendanceEnabled}
      />
      <Gap />
      <AddAbsencesButton
        data-qa="add-absences-button"
        onClick={useModalVisible.on}
        disabled={selectedCells.length === 0}
        text={i18n.absences.addAbsencesButton(selectedCells.length)}
      />
      <div>
        <HorizontalLine dashed slim />
        <H3>{i18n.absences.legendTitle}</H3>
        <FixedSpaceColumn spacing="xxs">
          <AbsenceLegend showNoAbsence />
        </FixedSpaceColumn>
      </div>
    </FixedSpaceColumn>
  )
})

function sendAbsenceUpdates(
  groupId: GroupId,
  selectedCells: SelectedCell[],
  update: AbsenceUpdate,
  upsertAbsences: MutateAsyncFn<typeof upsertAbsencesMutation>,
  addPresences: MutateAsyncFn<typeof addPresencesMutation>,
  deleteHolidayReservations: MutateAsyncFn<
    typeof deleteHolidayReservationsMutation
  >
): Promise<Result<void>> {
  switch (update.type) {
    case 'absence':
      return upsertAbsences({
        groupId,
        body: selectedCells.flatMap((cell) => {
          const categoriesToUpdate =
            // No categories selected => update all
            update.absenceCategories.length === 0
              ? cell.absenceCategories
              : cell.absenceCategories.filter((category) =>
                  update.absenceCategories.includes(category)
                )
          return categoriesToUpdate.map((category) => ({
            childId: cell.childId,
            date: cell.date,
            category,
            absenceType: update.absenceType
          }))
        })
      })
    case 'noAbsence':
      return addPresences({
        groupId,
        body: selectedCells.flatMap((cell) => {
          const categoriesToUpdate =
            // No categories selected => update all
            update.absenceCategories.length === 0
              ? cell.absenceCategories
              : cell.absenceCategories.filter((category) =>
                  update.absenceCategories.includes(category)
                )
          return categoriesToUpdate.map((category) => ({
            childId: cell.childId,
            date: cell.date,
            category
          }))
        })
      })
    case 'missingHolidayReservation':
      return deleteHolidayReservations({
        groupId,
        body: selectedCells.map((cell) => ({
          childId: cell.childId,
          date: cell.date
        }))
      })
  }
}

const AddAbsencesButton = styled(LegacyButton)`
  @media print {
    display: none;
  }
`
