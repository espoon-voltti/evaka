// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { MissingGroupPlacement } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { first, second, useSelectMutation } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Bold } from 'lib-components/typography'
import { faChild } from 'lib-icons'

import { EVAKA_START } from '../../../../constants'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import { formatName } from '../../../../utils'
import {
  createGroupPlacementMutation,
  updateBackupCareMutation
} from '../../queries'

const FieldWrapper = styled.section`
  display: flex;
  flex-direction: column;
`

interface Props {
  unitId: UUID
  groups: DaycareGroup[]
  missingPlacement: MissingGroupPlacement
}

interface GroupPlacementForm {
  startDate: LocalDate
  endDate: LocalDate
  groupId: UUID | null
  errors: string[]
}

export default React.memo(function GroupPlacementModal({
  unitId,
  groups,
  missingPlacement
}: Props) {
  const {
    placementId,
    firstName,
    lastName,
    gap: { start: minDate, end: maxDate }
  } = missingPlacement

  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(maxDate))
    .filter((group) => !group.endDate || !group.endDate.isBefore(minDate))

  const validate = (form: GroupPlacementForm): string[] => {
    const errors = []
    if (form.endDate.isBefore(form.startDate))
      errors.push(i18n.validationError.invertedDateRange)

    if (form.groupId == null)
      errors.push(i18n.unit.placements.modal.errors.noGroup)
    else {
      const group = openGroups.find((g) => g.id == form.groupId)
      if (group) {
        if (form.startDate.isBefore(group.startDate))
          errors.push(i18n.unit.placements.modal.errors.groupNotStarted)
        if (group.endDate && form.endDate.isAfter(group.endDate))
          errors.push(i18n.unit.placements.modal.errors.groupEnded)
      }
    }
    return errors
  }

  const getInitialFormState = (): GroupPlacementForm => {
    const initialFormState = {
      startDate: minDate.isBefore(EVAKA_START) ? EVAKA_START : minDate,
      endDate: maxDate,
      groupId: openGroups.length > 0 ? openGroups[0].id : null,
      errors: []
    }
    const errors = validate(initialFormState)
    return { ...initialFormState, errors }
  }
  const [form, setForm] = useState<GroupPlacementForm>(getInitialFormState())

  const assignFormValues: UpdateStateFn<GroupPlacementForm> = (values) => {
    setForm({
      ...form,
      ...values,
      errors: validate({
        ...form,
        ...values
      })
    })
  }

  const disableDateEditIfBackupPlacement = missingPlacement.backup
    ? {
        disabled: true,
        onFocus: (e: React.FocusEvent<HTMLInputElement>): void =>
          e.target.blur()
      }
    : {}

  const [createGroupPlacementOrBackupCare, onClick] = useSelectMutation(
    () =>
      form.groupId === null
        ? cancelMutation
        : !missingPlacement.backup
          ? first(form.groupId)
          : second(form.groupId),
    [
      createGroupPlacementMutation,
      (groupId) => ({
        unitId,
        placementId,
        body: {
          groupId,
          startDate: form.startDate,
          endDate: form.endDate
        }
      })
    ],
    [
      updateBackupCareMutation,
      (groupId) => ({
        id: placementId,
        unitId,
        body: {
          period: new FiniteDateRange(form.startDate, form.endDate),
          groupId
        }
      })
    ]
  )

  return (
    <MutateFormModal
      data-qa="group-placement-modal"
      title={i18n.unit.placements.modal.createTitle}
      icon={faChild}
      type="info"
      resolveMutation={createGroupPlacementOrBackupCare}
      resolveAction={onClick}
      onSuccess={clearUiMode}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={form.errors.length > 0}
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <FieldWrapper>
          <Bold>{i18n.unit.placements.modal.child}</Bold>
          <span>{formatName(firstName, lastName, i18n)}</span>
        </FieldWrapper>
        <FieldWrapper>
          <Bold>{i18n.unit.placements.modal.group}</Bold>
          <Select
            items={openGroups}
            onChange={(group) =>
              assignFormValues({
                groupId: group?.id ?? null
              })
            }
            selectedItem={
              openGroups.find(({ id }) => id === form.groupId) ?? null
            }
            getItemValue={({ id }) => id}
            getItemLabel={({ name }) => name}
            placeholder={i18n.common.select}
          />
        </FieldWrapper>
        <FieldWrapper>
          <Bold>{i18n.common.form.startDate}</Bold>
          <DatePickerDeprecated
            {...disableDateEditIfBackupPlacement}
            date={form.startDate}
            onChange={(startDate) => assignFormValues({ startDate })}
            type="full-width"
            minDate={minDate}
            maxDate={maxDate}
          />
        </FieldWrapper>
        <FieldWrapper>
          <Bold>{i18n.common.form.endDate}</Bold>
          <DatePickerDeprecated
            {...disableDateEditIfBackupPlacement}
            date={form.endDate}
            onChange={(endDate) => assignFormValues({ endDate })}
            type="full-width"
            minDate={minDate}
            maxDate={maxDate}
          />
        </FieldWrapper>
        {form.errors.length > 0 && (
          <section>
            {form.errors.map((error, index) => (
              <div className="error" key={index}>
                {error}
              </div>
            ))}
          </section>
        )}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
