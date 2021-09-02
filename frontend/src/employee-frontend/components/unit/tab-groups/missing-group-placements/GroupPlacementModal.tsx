// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import styled from 'styled-components'

import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Result } from 'lib-common/api'
import { faChild } from 'lib-icons'
import {
  createGroupPlacement,
  MissingGroupPlacement
} from '../../../../api/unit'
import { updateBackupCare } from '../../../../api/child/backup-care'
import { UUID } from '../../../../types'
import Select from '../../../../components/common/Select'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { formatName } from '../../../../utils'
import { DaycareGroup } from '../../../../types/unit'
import { EVAKA_START } from '../../../../constants'
import FiniteDateRange from 'lib-common/finite-date-range'

const Bold = styled.div`
  font-weight: 600;
`

interface Props {
  groups: DaycareGroup[]
  missingPlacement: MissingGroupPlacement
  reload: () => void
}

interface GroupPlacementForm {
  startDate: LocalDate
  endDate: LocalDate
  groupId: UUID | null
  errors: string[]
}

export default React.memo(function GroupPlacementModal({
  groups,
  missingPlacement,
  reload
}: Props) {
  const {
    placementId,
    firstName,
    lastName,
    gap: { start: minDate, end: maxDate }
  } = missingPlacement

  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

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

  const submitGroupPlacement = () => {
    if (form.groupId == null) return

    void createGroupPlacement(
      placementId,
      form.groupId,
      form.startDate,
      form.endDate
    ).then((res: Result<string>) => {
      if (res.isFailure) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.create,
          text: i18n.common.tryAgain,
          resolveLabel: i18n.common.ok
        })
      } else {
        clearUiMode()
        reload()
      }
    })
  }

  const submitBackupCarePlacement = () => {
    if (form.groupId == null) return

    void updateBackupCare(placementId, {
      period: new FiniteDateRange(form.startDate, form.endDate),
      groupId: form.groupId
    }).then((res) => {
      if (res.isFailure) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.create,
          text: i18n.common.tryAgain,
          resolveLabel: i18n.common.ok
        })
      } else {
        clearUiMode()
        reload()
      }
    })
  }

  const submitForm = missingPlacement.backup
    ? submitBackupCarePlacement
    : submitGroupPlacement

  const disableDateEditIfBackupPlacement = missingPlacement.backup
    ? {
        disabled: true,
        onFocus: (e: React.FocusEvent<HTMLInputElement>): void =>
          e.target.blur()
      }
    : {}

  return (
    <FormModal
      data-qa="group-placement-modal"
      title={i18n.unit.placements.modal.createTitle}
      icon={faChild}
      iconColour={'blue'}
      resolve={{
        action: submitForm,
        label: i18n.common.confirm,
        disabled: form.errors.length > 0
      }}
      reject={{
        action: clearUiMode,
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn>
        <section>
          <Bold>{i18n.unit.placements.modal.child}</Bold>
          <span>{formatName(firstName, lastName, i18n)}</span>
        </section>
        <section>
          <Bold>{i18n.unit.placements.modal.group}</Bold>
          <Select
            items={openGroups.map((group) => ({
              value: group.id,
              label: group.name
            }))}
            onChange={(value) =>
              value
                ? assignFormValues({
                    groupId: value.value
                  })
                : undefined
            }
            selectedItem={
              openGroups
                .map((group) => ({
                  value: group.id,
                  label: group.name
                }))
                .find(({ value }) => value === form.groupId) ?? null
            }
            placeholder={i18n.common.select}
          />
        </section>
        <section>
          <Bold>{i18n.common.form.startDate}</Bold>
          <DatePickerDeprecated
            {...disableDateEditIfBackupPlacement}
            date={form.startDate}
            onChange={(startDate) => assignFormValues({ startDate })}
            type="full-width"
            minDate={minDate}
            maxDate={maxDate}
          />
        </section>
        <section>
          <Bold>{i18n.common.form.endDate}</Bold>
          <DatePickerDeprecated
            {...disableDateEditIfBackupPlacement}
            date={form.endDate}
            onChange={(endDate) => assignFormValues({ endDate })}
            type="full-width"
            minDate={minDate}
            maxDate={maxDate}
          />
        </section>
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
    </FormModal>
  )
})
