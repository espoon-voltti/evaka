// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { flatMap, partition } from 'lodash'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Loading } from 'lib-common/api'
import {
  AbsenceCategory,
  AbsenceDelete,
  AbsenceGroup,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors, { absenceColors } from 'lib-customizations/common'

import {
  deleteGroupAbsences,
  getGroupAbsences,
  postGroupAbsences
} from '../../api/absences'
import PeriodPicker from '../../components/absences/PeriodPicker'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import {
  Cell,
  CellPart,
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
  setSelectedDate: (date: LocalDate) => void
  reservationEnabled: boolean
  staffAttendanceEnabled: boolean
}

export default React.memo(function Absences({
  groupId,
  selectedDate,
  setSelectedDate,
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
      groupId !== 'staff'
        ? getGroupAbsences(groupId, {
            year: selectedYear,
            month: selectedMonth
          })
        : Promise.resolve(Loading.of<AbsenceGroup>()),
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

  const updateCategories = useCallback(
    (category: AbsenceCategory) => {
      const included = selectedCategories.includes(category)
      const without = selectedCategories.filter((item) => item !== category)
      setSelectedCategories(included ? without : [category, ...without])
    },
    [selectedCategories, setSelectedCategories]
  )

  const updateAbsences = useCallback(() => {
    const selectedParts: CellPart[] = flatMap(
      selectedCells,
      ({ parts }) => parts
    )
    const [billableParts, otherParts] = partition(
      selectedParts,
      (part) => part.category === 'BILLABLE'
    )

    const payload: AbsenceDelete[] = flatMap(selectedCategories, (category) =>
      category === 'BILLABLE' ? billableParts : otherParts
    ).map(({ childId, date, category }) => {
      return {
        childId,
        date,
        category
      }
    })

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
    selectedCells
  ])

  return (
    <AbsencesPage data-qa="absences-page">
      {modalVisible && selectedCells.length > 0 && (
        <AbsenceModal
          onSave={updateAbsences}
          saveDisabled={selectedCategories.length === 0}
          onCancel={resetModalState}
          selectedAbsenceType={selectedAbsenceType}
          setSelectedAbsenceType={setSelectedAbsenceType}
          selectedCategories={selectedCategories}
          updateCategories={updateCategories}
        />
      )}
      {renderResult(absences, (absences) => (
        <FixedSpaceColumn spacing="zero">
          <PeriodPicker onChange={setSelectedDate} date={selectedDate} />
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
    </AbsencesPage>
  )
})

const AddAbsencesButton = styled(Button)`
  @media print {
    display: none;
  }
`

const cellSize = '20px'

const AbsencesPage = styled.div`
  h1,
  h2 {
    @media print {
      margin: 0;
    }
  }

  table {
    border-collapse: collapse;
    width: 100%;
  }

  td,
  th {
    padding: 5px 3px;
    border-bottom: none;
  }

  th {
    text-align: center;
    text-transform: uppercase;
    color: ${colors.grayscale.g70};
    vertical-align: center;
    font-weight: ${fontWeights.semibold};
    font-size: 0.8rem;
  }

  td {
    cursor: pointer;

    &.empty-row {
      cursor: inherit;
      height: 20px;
    }
  }

  .absence-child-name {
    white-space: nowrap;

    a {
      display: inline-block;
      overflow: hidden;
      width: 100px;
      max-width: 100px;

      @media screen and (min-width: 1216px) {
        width: 176px;
        max-width: 176px;
      }
    }
  }

  .absence-tooltip {
    span {
      text-align: left;
    }
  }

  .absence-cell-today,
  .table tr:hover .hover-highlight {
    background-color: ${colors.grayscale.g4};

    .absence-cell-selected {
      .absence-cell-left-weekend {
        border-top-color: transparent;
      }

      .absence-cell-right-weekend {
        border-bottom-color: transparent;
      }
    }

    .absence-cell-left-weekend {
      border-top-color: ${colors.grayscale.g15};
    }

    .absence-cell-right-weekend {
      border-bottom-color: ${colors.grayscale.g15};
    }
  }

  tr:hover .hover-highlight:first-child {
    box-shadow: -8px 0 0 ${colors.grayscale.g4};
  }

  tr:hover .hover-highlight:last-child {
    box-shadow: 8px 0 0 ${colors.grayscale.g4};
  }

  .absence-header-today {
    background-color: ${colors.grayscale.g4};

    @media print {
      background: none;
      color: inherit;
      font-weight: bolder;
    }
  }

  .absence-header-weekend {
    color: ${colors.grayscale.g100};
  }

  .absence-cell {
    position: relative;
    height: ${cellSize};
    width: ${cellSize};
  }

  .absence-cell-disabled {
    background: transparent;
  }

  .absence-cell-left,
  .absence-cell-right {
    position: absolute;
    height: ${cellSize};
    width: ${cellSize};
    border-style: solid;
    border-color: transparent;
  }

  .absence-cell-left {
    border-width: ${cellSize} ${cellSize} 0 0;

    &-OTHER_ABSENCE {
      border-top-color: ${absenceColors.OTHER_ABSENCE};
    }

    &-SICKLEAVE {
      border-top-color: ${absenceColors.SICKLEAVE};
    }

    &-UNKNOWN_ABSENCE {
      border-top-color: ${absenceColors.UNKNOWN_ABSENCE};
    }

    &-PLANNED_ABSENCE {
      border-top-color: ${absenceColors.PLANNED_ABSENCE};
    }

    &-TEMPORARY_RELOCATION {
      border-top-color: ${absenceColors.TEMPORARY_RELOCATION};
    }

    &-PARENTLEAVE {
      border-top-color: ${absenceColors.PARENTLEAVE};
    }

    &-FORCE_MAJEURE {
      border-top-color: ${absenceColors.FORCE_MAJEURE};
    }

    &-FREE_ABSENCE {
      border-top-color: ${absenceColors.FREE_ABSENCE};
    }

    &-UNAUTHORIZED_ABSENCE {
      border-top-color: ${absenceColors.UNAUTHORIZED_ABSENCE};
    }

    &-weekend {
      border-top-color: ${colors.grayscale.g4};
    }

    &-empty {
      border-top-color: ${absenceColors.NO_ABSENCE};
    }
  }

  .absence-cell-right {
    border-width: 0 0 ${cellSize} ${cellSize};

    &-OTHER_ABSENCE {
      border-bottom-color: ${absenceColors.OTHER_ABSENCE};
    }

    &-SICKLEAVE {
      border-bottom-color: ${absenceColors.SICKLEAVE};
    }

    &-UNKNOWN_ABSENCE {
      border-bottom-color: ${absenceColors.UNKNOWN_ABSENCE};
    }

    &-PLANNED_ABSENCE {
      border-bottom-color: ${absenceColors.PLANNED_ABSENCE};
    }

    &-TEMPORARY_RELOCATION {
      border-bottom-color: ${absenceColors.TEMPORARY_RELOCATION};
    }

    &-PARENTLEAVE {
      border-bottom-color: ${absenceColors.PARENTLEAVE};
    }

    &-FORCE_MAJEURE {
      border-bottom-color: ${absenceColors.FORCE_MAJEURE};
    }

    &-FREE_ABSENCE {
      border-bottom-color: ${absenceColors.FREE_ABSENCE};
    }

    &-UNAUTHORIZED_ABSENCE {
      border-bottom-color: ${absenceColors.UNAUTHORIZED_ABSENCE};
    }

    &-weekend {
      border-bottom-color: ${colors.grayscale.g4};
    }

    &-empty {
      border-bottom-color: ${absenceColors.NO_ABSENCE};
    }
  }

  .absence-cell-selected {
    border: 2px solid ${colors.main.m2};
    border-radius: 2px;

    .absence-cell-left,
    .absence-cell-right {
      border-color: transparent;
    }
  }

  .empty-row {
    color: transparent;

    td {
      cursor: default;
    }
  }

  .staff-attendance-row {
    font-weight: ${fontWeights.semibold};

    td {
      cursor: default;
      vertical-align: middle;
    }

    .staff-attendance-cell {
      /* disable spin buttons Chrome, Safari, Edge, Opera */

      input::-webkit-outer-spin-button,
      input::-webkit-inner-spin-button {
        -webkit-appearance: none;
      }

      input {
        /* disable spin buttons Firefox */
        -moz-appearance: textfield;
        text-align: center;
        margin: 0 auto;
        padding: 0;
        font-weight: ${fontWeights.semibold};
        font-size: 0.8rem;
        height: ${cellSize};
        width: ${cellSize};
        border-color: ${colors.grayscale.g35};
        color: ${colors.grayscale.g100};
        display: block;
        box-shadow: none;
        max-width: 100%;
        min-height: 2.5em;
        border-radius: 0;
        border-width: 0 0 1px;
        background-color: transparent;

        :disabled {
          color: ${colors.grayscale.g35};
        }

        @media print {
          border: none;
        }
      }
    }

    .disabled-staff-cell-container {
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }
  }
`
