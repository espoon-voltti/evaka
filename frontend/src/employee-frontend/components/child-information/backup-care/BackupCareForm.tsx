// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  BackupCareUnit,
  ChildBackupCare,
  ChildBackupCareResponse
} from 'lib-common/generated/api-types/backupcare'
import { UnitStub } from 'lib-common/generated/api-types/daycare'
import { ChildId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import {
  constantQuery,
  first,
  second,
  useQueryResult,
  useSelectMutation
} from 'lib-common/query'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import {
  cancelMutation,
  MutateButton
} from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { fontWeights } from 'lib-components/typography'

import { unitsQuery } from '../../../queries'
import { ChildContext } from '../../../state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DateRange } from '../../../utils/date'
import {
  isDateRangeInverted,
  isDateRangeOverlappingWithExisting
} from '../../../utils/validation/validations'
import {
  createBackupCareMutation,
  placementsQuery,
  updateBackupCareMutation
} from '../queries'

export interface Props {
  childId: ChildId
  backupCares: ChildBackupCareResponse[]
  backupCare?: ChildBackupCare
}

interface FormState {
  unit: UnitStub | BackupCareUnit | undefined
  startDate: LocalDate | null
  endDate: LocalDate | null
}

interface ValidatedFormState {
  unit: UnitStub | BackupCareUnit
  startDate: LocalDate
  endDate: LocalDate
}

const FormField = styled.div`
  display: flex;
  align-items: center;
  margin: 20px 0;
`

const FormLabel = styled.div`
  font-weight: ${fontWeights.semibold};
  flex: 0 1 auto;
  width: 300px;
`

const Unit = styled.div`
  flex: 1 1 auto;
`

const ActionButtons = styled(FixedSpaceRow)`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`

export default function BackupCareForm({
  childId,
  backupCares,
  backupCare
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(ChildContext)

  const units = useQueryResult(
    unitsQuery({
      areaIds: null,
      type: 'DAYCARE',
      from: LocalDate.todayInHelsinkiTz()
    })
  )

  const placements = useQueryResult(
    permittedActions.has('READ_PLACEMENT')
      ? placementsQuery({ childId })
      : constantQuery(null)
  )

  const consecutivePlacementRanges = useMemo(
    () =>
      placements
        .map((p) =>
          p !== null
            ? sortBy(p.placements, (placement) =>
                placement.startDate.toSystemTzDate().getTime()
              ).reduce((prev, curr) => {
                const currentRange = new FiniteDateRange(
                  curr.startDate,
                  curr.endDate
                )
                const fittingExistingIndex = prev.findIndex((range) =>
                  range.adjacentTo(currentRange)
                )

                if (fittingExistingIndex > -1) {
                  const fittingExisting = prev[fittingExistingIndex]

                  const newRange = fittingExisting.leftAdjacentTo(currentRange)
                    ? fittingExisting.withEnd(curr.endDate)
                    : fittingExisting.withStart(curr.startDate)

                  const copy = Array.from(prev)
                  copy[fittingExistingIndex] = newRange
                  return copy
                }

                return [...prev, currentRange]
              }, [] as FiniteDateRange[])
            : []
        )
        .getOrElse([]),
    [placements]
  )

  const initialFormState: FormState = {
    unit: backupCare?.unit,
    startDate: backupCare?.period.start ?? LocalDate.todayInSystemTz(),
    endDate: backupCare?.period.end ?? LocalDate.todayInSystemTz()
  }

  const [formState, setFormState] = useState<FormState>(initialFormState)

  const [validatedState, formErrors] = useMemo((): [
    ValidatedFormState | null,
    string[]
  ] => {
    const errors: string[] = []
    const existing: DateRange[] = backupCares
      .filter(
        (it) => backupCare === undefined || it.backupCare.id !== backupCare.id
      )
      .map(({ backupCare: { period } }) => ({
        startDate: period.start,
        endDate: period.end
      }))

    const { unit, startDate, endDate } = formState
    if (!unit) {
      return [null, []]
    }
    if (!startDate || !endDate) {
      errors.push(i18n.validationError.mandatoryField)
      return [null, errors]
    }
    if (isDateRangeInverted({ startDate, endDate }))
      errors.push(i18n.validationError.invertedDateRange)
    else if (
      isDateRangeOverlappingWithExisting({ startDate, endDate }, existing)
    )
      errors.push(i18n.validationError.existingDateRangeError)
    else if (
      !consecutivePlacementRanges.some((placementRange) =>
        placementRange.contains(new FiniteDateRange(startDate, endDate))
      )
    )
      errors.push(
        i18n.childInformation.backupCares.validationNoMatchingPlacement
      )

    if (errors.length > 0) {
      return [null, errors]
    } else {
      return [{ unit, startDate, endDate }, []]
    }
  }, [backupCare, backupCares, consecutivePlacementRanges, formState, i18n])

  const updateFormState: UpdateStateFn<FormState> = (value) => {
    const newState = { ...formState, ...value }
    setFormState(newState)
  }

  const options = useMemo(
    () =>
      units
        .map((us) =>
          orderBy(us, (x) => x.name).map(({ id, name }) => ({ id, name }))
        )
        .getOrElse([]),
    [units]
  )

  const [createOrUpdateBackupCare, onClick] = useSelectMutation(
    () => (backupCare === undefined ? first() : second(backupCare)),
    [
      createBackupCareMutation,
      () =>
        validatedState
          ? {
              childId,
              body: {
                groupId: null,
                unitId: validatedState.unit.id,
                period: new FiniteDateRange(
                  validatedState.startDate,
                  validatedState.endDate
                )
              }
            }
          : cancelMutation
    ],
    [
      updateBackupCareMutation,
      (backupCare) =>
        validatedState !== null
          ? {
              id: backupCare.id,
              unitId: backupCare.unit.id,
              body: {
                groupId: null,
                period: new FiniteDateRange(
                  validatedState.startDate,
                  validatedState.endDate
                )
              }
            }
          : cancelMutation
    ]
  )

  return (
    <div>
      <form data-qa="backup-care-form">
        <FormField>
          <FormLabel>{i18n.childInformation.backupCares.unit}</FormLabel>
          <Unit>
            {backupCare ? (
              backupCare.unit.name
            ) : (
              <div data-qa="backup-care-select-unit">
                <Combobox
                  items={options}
                  selectedItem={formState.unit ?? null}
                  onChange={(unit) =>
                    updateFormState({ unit: unit || undefined })
                  }
                  getItemLabel={({ name }) => name}
                  getItemDataQa={({ id }) => `unit-${id}`}
                />
              </div>
            )}
          </Unit>
        </FormField>
        <FormField>
          <FormLabel>{i18n.childInformation.backupCares.dateRange}</FormLabel>
          <FixedSpaceRow>
            <DatePicker
              date={formState.startDate}
              onChange={(startDate) => updateFormState({ startDate })}
              locale="fi"
              data-qa="backup-care-start-date"
            />
            <DatePickerSpacer />
            <DatePicker
              date={formState.endDate}
              onChange={(endDate) => updateFormState({ endDate })}
              locale="fi"
              data-qa="backup-care-end-date"
            />
          </FixedSpaceRow>
        </FormField>
        {formErrors.map((error, index) => (
          <div className="error" key={index} data-qa="form-error">
            {error}
          </div>
        ))}
        <ActionButtons>
          <LegacyButton
            onClick={() => clearUiMode()}
            text={i18n.common.cancel}
          />
          <MutateButton
            primary
            type="submit"
            mutation={createOrUpdateBackupCare}
            onClick={onClick}
            onSuccess={clearUiMode}
            disabled={formErrors.length > 0 || !formState.unit}
            data-qa="submit-backup-care-form"
            text={i18n.common.confirm}
          />
        </ActionButtons>
      </form>
      {uiMode === 'create-new-backup-care' && <div className="separator" />}
    </div>
  )
}
