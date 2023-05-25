// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import flatMap from 'lodash/flatMap'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import {
  AbsenceCategory,
  AbsenceDelete,
  AbsenceType,
  AbsenceUpsert
} from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  deleteGroupAbsences,
  getGroupAbsences,
  postGroupAbsences
} from '../../api/absences'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { Cell, CellPart } from '../../types/absence'
import { renderResult } from '../async-rendering'

import { AbsenceLegend } from './AbsenceLegend'
import AbsenceModal from './AbsenceModal'
import AbsenceTable from './AbsenceTable'

interface Props {
  groupId: UUID
  selectedDate: LocalDate
  reservationEnabled: boolean
  staffAttendanceEnabled: boolean
}

export default React.memo(function Absences({
  groupId,
  selectedDate,
  reservationEnabled,
  staffAttendanceEnabled
}: Props) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [modalVisible, setModalVisible] = useState(false)
  const [selectedCells, setSelectedCells] = useState<Cell[]>([])

  const selectedYear = selectedDate.getYear()
  const selectedMonth = selectedDate.getMonth()

  const [absences, loadAbsences] = useApiState(
    () =>
      getGroupAbsences(groupId, { year: selectedYear, month: selectedMonth }),
    [groupId, selectedYear, selectedMonth]
  )

  const updateSelectedCells = useCallback(
    ({ id, parts }: Cell) =>
      setSelectedCells((currentSelectedCells) => {
        const selectedIds = currentSelectedCells.map((item) => item.id)

        const included = selectedIds.includes(id)
        const without = currentSelectedCells.filter((item) => item.id !== id)

        return included ? without : [{ id, parts }, ...without]
      }),
    []
  )

  const toggleCellSelection = useCallback(
    (id: UUID, parts: CellPart[]) => updateSelectedCells({ id, parts }),
    [updateSelectedCells]
  )

  const closeModal = useCallback(() => {
    setSelectedCells([])
    setModalVisible(false)
  }, [])

  const showCategorySelection = useMemo(
    () => selectedCells.some(({ parts }) => parts.length > 1),
    [selectedCells]
  )

  useEffect(() => {
    if (absences.isSuccess) {
      setTitle(`${absences.value.groupName} | ${absences.value.daycareName}`)
    }
  }, [absences, setTitle])

  const updateAbsences = useCallback(
    (absenceType: AbsenceType | null, absenceCategories: AbsenceCategory[]) => {
      const selectedParts: CellPart[] = flatMap(
        selectedCells,
        ({ parts }) => parts
      )

      const sentParts =
        absenceCategories.length > 0
          ? selectedParts.filter((part) =>
              absenceCategories.includes(part.category)
            )
          : selectedParts

      ;(absenceType === null
        ? deleteGroupAbsences(
            groupId,
            sentParts.map(
              ({ childId, date, category }): AbsenceDelete => ({
                childId,
                date,
                category
              })
            )
          )
        : postGroupAbsences(
            groupId,
            sentParts.map(
              ({ childId, date, category }): AbsenceUpsert => ({
                childId,
                date,
                category,
                absenceType
              })
            )
          )
      )
        .then(loadAbsences)
        .catch((e) => console.error(e))
        .finally(() => {
          closeModal()
        })
    },
    [closeModal, groupId, loadAbsences, selectedCells]
  )

  return (
    <div data-qa="absences-page">
      {modalVisible && selectedCells.length > 0 && (
        <AbsenceModal
          showCategorySelection={showCategorySelection}
          onSave={updateAbsences}
          onClose={closeModal}
        />
      )}
      {renderResult(absences, (absences) => (
        <FixedSpaceColumn spacing="zero">
          <AbsenceTable
            groupId={groupId}
            selectedCells={selectedCells}
            toggleCellSelection={toggleCellSelection}
            selectedDate={selectedDate}
            childList={absences.children}
            operationDays={absences.operationDays}
            reservationEnabled={reservationEnabled}
            staffAttendanceEnabled={staffAttendanceEnabled}
          />
          <Gap />
          <AddAbsencesButton
            data-qa="add-absences-button"
            onClick={() => setModalVisible(true)}
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
      ))}
    </div>
  )
})

const AddAbsencesButton = styled(Button)`
  @media print {
    display: none;
  }
`
