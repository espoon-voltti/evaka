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

import type {
  AbsenceCategory,
  AbsenceDelete,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
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
import type { TitleState } from '../../state/title'
import { TitleContext } from '../../state/title'
import type { Cell, CellPart } from '../../types/absence'
import {
  defaultAbsenceType,
  defaultAbsenceCategories
} from '../../types/absence'
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
  const [selectedAbsenceType, setSelectedAbsenceType] =
    useState<AbsenceType | null>(defaultAbsenceType)
  const [selectedCategories, setSelectedCategories] = useState(
    defaultAbsenceCategories
  )

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

  const resetModalState = useCallback(() => {
    setSelectedCells([])
    setSelectedAbsenceType(defaultAbsenceType)
    setSelectedCategories(defaultAbsenceCategories)
    setModalVisible(false)
  }, [setSelectedAbsenceType, setSelectedCategories, setSelectedCells])

  useEffect(() => {
    if (absences.isSuccess) {
      setTitle(`${absences.value.groupName} | ${absences.value.daycareName}`)
    }
  }, [absences, setTitle])

  const showCategorySelection = useMemo(
    () => selectedCells.some(({ parts }) => parts.length > 1),
    [selectedCells]
  )

  const updateCategories = useCallback((category: AbsenceCategory) => {
    setSelectedCategories((categories) =>
      categories.includes(category)
        ? categories.filter((c) => c !== category)
        : [...categories, category]
    )
  }, [])

  const updateAbsences = useCallback(() => {
    const selectedParts: CellPart[] = flatMap(
      selectedCells,
      ({ parts }) => parts
    )

    const sentParts = showCategorySelection
      ? selectedParts.filter((part) =>
          selectedCategories.includes(part.category)
        )
      : selectedParts

    const payload: AbsenceDelete[] = sentParts.map(
      ({ childId, date, category }) => ({
        childId,
        date,
        category
      })
    )

    ;(selectedAbsenceType === null
      ? deleteGroupAbsences(groupId, payload)
      : postGroupAbsences(
          groupId,
          payload.map((data) => ({
            ...data,
            absenceType: selectedAbsenceType
          }))
        )
    )
      .then(loadAbsences)
      .catch((e) => console.error(e))
      .finally(() => {
        resetModalState()
      })
  }, [
    groupId,
    loadAbsences,
    resetModalState,
    selectedAbsenceType,
    selectedCategories,
    selectedCells,
    showCategorySelection
  ])

  return (
    <div data-qa="absences-page">
      {modalVisible && selectedCells.length > 0 && (
        <AbsenceModal
          onSave={updateAbsences}
          saveDisabled={
            showCategorySelection && selectedCategories.length === 0
          }
          onCancel={resetModalState}
          selectedAbsenceType={selectedAbsenceType}
          setSelectedAbsenceType={setSelectedAbsenceType}
          showCategorySelection={showCategorySelection}
          selectedCategories={selectedCategories}
          updateCategories={updateCategories}
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
