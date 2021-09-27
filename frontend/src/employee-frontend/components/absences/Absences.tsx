// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import { flatMap, partition } from 'lodash'
import colors from 'lib-customizations/common'
import { fontWeights } from 'lib-components/typography'
import { Label, LabelText } from '../common/styled/common'
import Button from 'lib-components/atoms/buttons/Button'
import Radio from 'lib-components/atoms/form/Radio'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Loader from 'lib-components/atoms/Loader'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import { Loading } from 'lib-common/api'
import {
  deleteGroupAbsences,
  getGroupAbsences,
  postGroupAbsences
} from '../../api/absences'
import { AbsencesContext, AbsencesState } from '../../state/absence'
import { useTranslation } from '../../state/i18n'
import {
  AbsencePayload,
  defaultAbsenceType,
  AbsenceTypes,
  CareTypeCategories,
  CareTypeCategory,
  billableCareTypes,
  CellPart,
  defaultCareTypeCategory
} from '../../types/absence'
import AbsenceTable from './AbsenceTable'
import PeriodPicker from '../../components/absences/PeriodPicker'
import { TitleContext, TitleState } from '../../state/title'
import styled from 'styled-components'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { UUID } from 'lib-common/types'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import LocalDate from 'lib-common/local-date'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'

interface Props {
  groupId: UUID
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
}

export default function Absences({
  groupId,
  selectedDate,
  setSelectedDate
}: Props) {
  const { i18n } = useTranslation()
  const {
    absences,
    setAbsences,
    modalVisible,
    setModalVisible,
    selectedCells,
    setSelectedCells,
    selectedAbsenceType,
    setSelectedAbsenceType,
    selectedCareTypeCategories,
    setSelectedCareTypeCategories
  } = useContext<AbsencesState>(AbsencesContext)
  const { setTitle } = useContext<TitleState>(TitleContext)

  const resetModalState = () => {
    setSelectedCells([])
    setSelectedCareTypeCategories([])
    setSelectedAbsenceType(defaultAbsenceType)
    setSelectedCareTypeCategories(defaultCareTypeCategory)
    setModalVisible(false)
  }

  const loadAbsences = useCallback(() => {
    setAbsences(Loading.of())
    void getGroupAbsences(groupId, {
      year: selectedDate.getYear(),
      month: selectedDate.getMonth()
    }).then(setAbsences)
  }, [groupId, selectedDate, setAbsences])

  useEffect(() => {
    loadAbsences()
    return () => {
      setAbsences(Loading.of())
    }
  }, [loadAbsences]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (absences.isSuccess) {
      setTitle(`${absences.value.groupName} | ${absences.value.daycareName}`)
    }
  }, [absences, setTitle])

  function updateCareTypes(type: CareTypeCategory) {
    const included = selectedCareTypeCategories.includes(type)
    const without = selectedCareTypeCategories.filter((item) => item !== type)
    setSelectedCareTypeCategories(included ? without : [type, ...without])
  }

  function updateAbsences() {
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
  }

  const showModal = () => modalVisible && selectedCells.length > 0

  const renderAbsenceModal = () =>
    showModal() ? (
      <FormModal
        title=""
        className="absence-modal"
        resolve={{
          action: updateAbsences,
          label: i18n.absences.modal.saveButton,
          disabled: selectedCareTypeCategories.length === 0
        }}
        reject={{
          action: resetModalState,
          label: i18n.absences.modal.cancelButton
        }}
        size="lg"
      >
        <FixedSpaceColumn spacing={'L'}>
          <Label>
            <LabelText>{i18n.absences.modal.absenceSectionLabel}</LabelText>
          </Label>
          <FixedSpaceColumn spacing={'xs'}>
            {AbsenceTypes.map((absenceType, index) => (
              <Radio
                key={index}
                id={absenceType}
                label={i18n.absences.modal.absenceTypes[absenceType]}
                checked={selectedAbsenceType === absenceType}
                onChange={() => setSelectedAbsenceType(absenceType)}
              />
            ))}
            <Radio
              id={'PRESENCE'}
              label={i18n.absences.modal.absenceTypes.PRESENCE}
              checked={selectedAbsenceType === null}
              onChange={() => setSelectedAbsenceType(null)}
            />
          </FixedSpaceColumn>

          <Label>
            <LabelText>{i18n.absences.modal.placementSectionLabel}</LabelText>
          </Label>
          <FixedSpaceColumn spacing={'xs'}>
            {CareTypeCategories.map((careTypeCategory, index) => (
              <Checkbox
                key={index}
                label={i18n.absences.careTypeCategories[careTypeCategory]}
                checked={selectedCareTypeCategories.includes(careTypeCategory)}
                onChange={() => updateCareTypes(careTypeCategory)}
                data-qa={`absences-select-caretype-${careTypeCategory}`}
              />
            ))}
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </FormModal>
    ) : null

  return (
    <AbsencesPage data-qa="absences-page">
      {renderAbsenceModal()}
      {absences.isSuccess ? (
        <FixedSpaceColumn spacing="zero">
          <PeriodPicker onChange={setSelectedDate} date={selectedDate} />
          <AbsenceTable
            groupId={groupId}
            selectedDate={selectedDate}
            childList={absences.value.children}
            operationDays={absences.value.operationDays}
          />
          <Gap />
          <AddAbsencesButton
            data-qa="add-absences-button"
            onClick={() => setModalVisible(true)}
            disabled={selectedCells.length === 0}
            text={i18n.absences.addAbsencesButton(selectedCells.length)}
          />
          <Gap size="L" />
          <FixedSpaceFlexWrap>
            <StaticChip color={colors.blues.dark}>
              {i18n.absences.absenceTypes.OTHER_ABSENCE}
            </StaticChip>
            <StaticChip color={colors.accents.violet}>
              {i18n.absences.absenceTypes.SICKLEAVE}
            </StaticChip>
            <StaticChip color={colors.accents.green}>
              {i18n.absences.absenceTypes.UNKNOWN_ABSENCE}
            </StaticChip>
            <StaticChip color={colors.accents.water}>
              {i18n.absences.absenceTypes.PLANNED_ABSENCE}
            </StaticChip>
            <StaticChip color={colors.blues.primary}>
              {i18n.absences.absenceTypes.PARENTLEAVE}
            </StaticChip>
            <StaticChip color={colors.accents.red}>
              {i18n.absences.absenceTypes.FORCE_MAJEURE}
            </StaticChip>
            <StaticChip
              color={colors.greyscale.medium}
              textColor={colors.greyscale.white}
            >
              {i18n.absences.absenceTypes.PRESENCE}
            </StaticChip>
          </FixedSpaceFlexWrap>
        </FixedSpaceColumn>
      ) : null}
      {absences.isLoading && <Loader />}
      {absences.isFailure && <ErrorSegment />}
    </AbsencesPage>
  )
}

const AddAbsencesButton = styled(Button)`
  @media print {
    display: none;
  }
`

const cellSize = '20px'

const AbsencesPage = styled.div`
  button {
    &:disabled {
      color: #c4c4c4;
    }
  }

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
    color: #0f0f0f;

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
    background: rgba(158, 158, 158, 0.2);

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
    box-shadow: -8px 0 0 rgba(158, 158, 158, 0.2);
  }

  tr:hover .hover-highlight:last-child {
    box-shadow: 8px 0 0 rgba(158, 158, 158, 0.2);
  }

  .absence-header-today {
    background-color: #9e9e9e;
    color: ${colors.greyscale.white} !important;

    @media print {
      background: none;
      color: inherit !important;
      font-weight: bolder;
    }
  }

  .absence-header-weekday {
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
      border-top-color: ${colors.blues.dark};
    }
    &-SICKLEAVE {
      border-top-color: ${colors.accents.violet};
    }
    &-UNKNOWN_ABSENCE {
      border-top-color: ${colors.accents.green};
    }
    &-PLANNED_ABSENCE {
      border-top-color: ${colors.blues.light};
    }
    &-TEMPORARY_RELOCATION {
      border-top-color: ${colors.accents.orange};
    }
    &-TEMPORARY_VISITOR {
      border-top-color: ${colors.accents.yellow};
    }
    &-PARENTLEAVE {
      border-top-color: ${colors.blues.primary};
    }
    &-FORCE_MAJEURE {
      border-top-color: ${colors.accents.red};
    }
    &-weekend {
      border-top-color: ${colors.greyscale.lightest};
    }
    &-empty {
      border-top-color: ${colors.greyscale.lighter};
    }
  }

  .absence-cell-right {
    border-width: 0 0 ${cellSize} ${cellSize};

    &-OTHER_ABSENCE {
      border-bottom-color: ${colors.blues.dark};
    }
    &-SICKLEAVE {
      border-bottom-color: ${colors.accents.violet};
    }
    &-UNKNOWN_ABSENCE {
      border-bottom-color: ${colors.accents.green};
    }
    &-PLANNED_ABSENCE {
      border-bottom-color: ${colors.blues.light};
    }
    &-TEMPORARY_RELOCATION {
      border-bottom-color: ${colors.accents.orange};
    }
    &-TEMPORARY_VISITOR {
      border-bottom-color: ${colors.accents.yellow};
    }
    &-PARENTLEAVE {
      border-bottom-color: ${colors.blues.primary};
    }
    &-FORCE_MAJEURE {
      border-bottom-color: ${colors.accents.red};
    }
    &-weekend {
      border-bottom-color: ${colors.greyscale.lightest};
    }
    &-empty {
      border-bottom-color: ${colors.greyscale.lighter};
    }
  }

  .absence-cell-selected {
    border: 2px solid ${colors.blues.primary};
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
        min-height: 1.5rem;
        border-color: #9e9e9e;
        color: ${colors.greyscale.darkest};
        display: block;
        box-shadow: none;
        max-width: 100%;
        min-height: 2.5em;
        border-radius: 0;
        border-width: 0 0 1px;
        background-color: transparent;

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
