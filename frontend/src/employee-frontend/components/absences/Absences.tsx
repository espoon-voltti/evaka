// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { flatMap, partition } from 'lodash'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { AbsenceType } from 'lib-common/generated/enums'
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
  AbsencePayload,
  billableCareTypes,
  CareTypeCategory,
  Cell,
  CellPart,
  defaultAbsenceType,
  defaultCareTypeCategory
} from '../../types/absence'
import { renderResult } from '../async-rendering'
import { AbsenceLegend } from './AbsenceLegend'
import AbsenceModal from './AbsenceModal'
import AbsenceTable from './AbsenceTable'

interface Props {
  groupId: UUID
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
}

export default React.memo(function Absences({
  groupId,
  selectedDate,
  setSelectedDate
}: Props) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [modalVisible, setModalVisible] = useState(false)
  const [selectedCells, setSelectedCells] = useState<Cell[]>([])
  const [selectedAbsenceType, setSelectedAbsenceType] =
    useState<AbsenceType | null>(defaultAbsenceType)
  const [selectedCareTypeCategories, setSelectedCareTypeCategories] = useState(
    defaultCareTypeCategory
  )

  const selectedYear = selectedDate.getYear()
  const selectedMonth = selectedDate.getMonth()

  const [absences, loadAbsences] = useApiState(
    () =>
      getGroupAbsences(groupId, {
        year: selectedYear,
        month: selectedMonth
      }),
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
    setSelectedCareTypeCategories([])
    setSelectedAbsenceType(defaultAbsenceType)
    setSelectedCareTypeCategories(defaultCareTypeCategory)
    setModalVisible(false)
  }, [setSelectedAbsenceType, setSelectedCareTypeCategories, setSelectedCells])

  useEffect(() => {
    if (absences.isSuccess) {
      setTitle(`${absences.value.groupName} | ${absences.value.daycareName}`)
    }
  }, [absences, setTitle])

  const updateCareTypes = useCallback(
    (type: CareTypeCategory) => {
      const included = selectedCareTypeCategories.includes(type)
      const without = selectedCareTypeCategories.filter((item) => item !== type)
      setSelectedCareTypeCategories(included ? without : [type, ...without])
    },
    [selectedCareTypeCategories, setSelectedCareTypeCategories]
  )

  const updateAbsences = useCallback(() => {
    const selectedParts: CellPart[] = flatMap(
      selectedCells,
      ({ parts }) => parts
    )
    const [billableParts, otherParts] = partition(selectedParts, (part) =>
      billableCareTypes.includes(part.careType)
    )

    const payload: AbsencePayload[] = flatMap(
      selectedCareTypeCategories,
      (careTypeCategory) =>
        careTypeCategory === 'BILLABLE' ? billableParts : otherParts
    ).map(({ childId, date, careType }) => {
      return {
        childId,
        date,
        careType
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
    selectedCareTypeCategories,
    selectedCells
  ])

  return (
    <AbsencesPage data-qa="absences-page">
      {modalVisible && selectedCells.length > 0 && (
        <AbsenceModal
          onSave={updateAbsences}
          saveDisabled={selectedCareTypeCategories.length === 0}
          onCancel={resetModalState}
          selectedAbsenceType={selectedAbsenceType}
          setSelectedAbsenceType={setSelectedAbsenceType}
          selectedCareTypeCategories={selectedCareTypeCategories}
          updateCareTypes={updateCareTypes}
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
          />
          <Gap />
          <AddAbsencesButton
            data-qa="add-absences-button"
            onClick={() => setModalVisible(true)}
            disabled={selectedCells.length === 0}
            text={i18n.absences.addAbsencesButton(selectedCells.length)}
          />
          <HorizontalLine dashed slim />
          <H3 noMargin>{i18n.absences.legendTitle}</H3>
          <Gap size="m" />
          <FixedSpaceColumn spacing="xxs">
            <AbsenceLegend />
          </FixedSpaceColumn>
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

  .table {
    border-collapse: collapse;
    width: 100%;
  }

  .table th {
    text-align: left;
    text-transform: uppercase;
    color: ${colors.greyscale.dark};
    vertical-align: bottom;

    &.absence-header {
      text-align: center;
    }
  }

  .table td {
    text-align: left;
    color: ${colors.greyscale.darkest};

    &.absence-cell-wrapper {
      text-align: center;
    }

    &.absence-cell-wrapper > div {
      margin: 0 auto;
    }
  }

  .table td,
  .table th {
    padding: 5px 3px !important;
    border-bottom: none !important;
  }

  .table thead th {
    font-weight: ${fontWeights.semibold} !important;
    font-size: 0.8rem !important;
  }

  .table td {
    cursor: pointer;

    &.empty-row {
      cursor: inherit;
      height: 20px;
    }
  }

  .table .has-text-center {
    text-align: center;
  }

  .absence-modal .input-group-item {
    margin-bottom: 0;
  }

  .absence-child-name {
    white-space: nowrap;
    overflow: hidden;
    width: 130px;
    max-width: 130px;

    @media screen and (min-width: 1216px) {
      width: 206px;
      max-width: 206px;
    }
  }

  .absence-tooltip {
    span {
      text-align: left;
    }
  }

  .absence-cell-today,
  .table tr:hover .hover-highlight {
    background-color: ${colors.greyscale.lightest};

    .absence-cell-selected {
      .absence-cell-left-weekend {
        border-top-color: transparent;
      }

      .absence-cell-right-weekend {
        border-bottom-color: transparent;
      }
    }

    .absence-cell-left-weekend {
      border-top-color: ${colors.greyscale.lighter};
    }

    .absence-cell-right-weekend {
      border-bottom-color: ${colors.greyscale.lighter};
    }
  }

  tr:hover .hover-highlight:first-child {
    box-shadow: -8px 0 0 ${colors.greyscale.lightest};
  }

  tr:hover .hover-highlight:last-child {
    box-shadow: 8px 0 0 ${colors.greyscale.lightest};
  }

  .absence-header-today {
    background-color: ${colors.greyscale.lightest};

    @media print {
      background: none;
      color: inherit !important;
      font-weight: bolder;
    }
  }

  .absence-header-weekend {
    color: ${colors.greyscale.darkest} !important;
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

    &-TEMPORARY_VISITOR {
      border-top-color: ${absenceColors.TEMPORARY_VISITOR};
    }

    &-PARENTLEAVE {
      border-top-color: ${absenceColors.PARENTLEAVE};
    }

    &-FORCE_MAJEURE {
      border-top-color: ${absenceColors.FORCE_MAJEURE};
    }

    &-weekend {
      border-top-color: ${colors.greyscale.lightest};
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

    &-TEMPORARY_VISITOR {
      border-bottom-color: ${absenceColors.TEMPORARY_VISITOR};
    }

    &-PARENTLEAVE {
      border-bottom-color: ${absenceColors.PARENTLEAVE};
    }

    &-FORCE_MAJEURE {
      border-bottom-color: ${absenceColors.FORCE_MAJEURE};
    }

    &-weekend {
      border-bottom-color: ${colors.greyscale.lightest};
    }

    &-empty {
      border-bottom-color: ${absenceColors.NO_ABSENCE};
    }
  }

  .absence-cell-selected {
    border: 2px solid ${colors.main.primary};
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
        border-color: ${colors.greyscale.medium};
        color: ${colors.greyscale.darkest};
        display: block;
        box-shadow: none;
        max-width: 100%;
        min-height: 2.5em;
        border-radius: 0;
        border-width: 0 0 1px;
        background-color: transparent;

        :disabled {
          color: ${colors.greyscale.medium};
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
