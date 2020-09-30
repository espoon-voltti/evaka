// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import FormModal from '~components/common/FormModal'
import { Section } from '~components/shared/alpha'
import { isFailure, Result } from '~api'
import { faChild } from 'icon-set'
import { createPlacement } from '~api/unit'
import { UUID } from '~types'
import Select from '~components/common/Select'
import { DatePicker } from '~components/common/DatePicker'
import { formatName } from '~utils'
import { DaycareGroupPlacementDetailed, DaycareGroup } from '~types/unit'
import { EVAKA_START } from '~constants'

interface Props {
  groups: DaycareGroup[]
  missingPlacement: DaycareGroupPlacementDetailed
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
    daycarePlacementId,
    child,
    startDate: minDate,
    endDate: maxDate
  } = missingPlacement

  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(maxDate))
    .filter((group) => !group.endDate || group.endDate.isBefore(minDate))

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

  const assignFormValues = (values: Partial<GroupPlacementForm>) => {
    setForm({
      ...form,
      ...values,
      errors: validate({
        ...form,
        ...values
      })
    })
  }

  const submitForm = () => {
    if (form.groupId == null) return

    void createPlacement(
      daycarePlacementId,
      form.groupId,
      form.startDate,
      form.endDate
    ).then((res: Result<string>) => {
      if (isFailure(res)) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.create,
          text: i18n.common.tryAgain
        })
      } else {
        clearUiMode()
        reload()
      }
    })
  }

  return (
    <FormModal
      data-qa="group-placement-modal"
      title={i18n.unit.placements.modal.createTitle}
      icon={faChild}
      iconColour={'blue'}
      resolveLabel={i18n.common.confirm}
      rejectLabel={i18n.common.cancel}
      reject={() => clearUiMode()}
      resolveDisabled={form.errors.length > 0}
      resolve={() => submitForm()}
    >
      <Section>
        <div className="bold">{i18n.unit.placements.modal.child}</div>
        <span>{formatName(child.firstName, child.lastName, i18n)}</span>
      </Section>
      <Section>
        <div className="bold">{i18n.unit.placements.modal.group}</div>
        <Select
          options={openGroups.map((group) => ({
            id: group.id,
            label: group.name
          }))}
          value={form.groupId || undefined}
          onChange={(event) =>
            assignFormValues({
              groupId: event.target.value
            })
          }
        />
      </Section>
      <Section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <DatePicker
          date={form.startDate}
          onChange={(startDate) => assignFormValues({ startDate })}
          type="full-width"
          minDate={minDate}
          maxDate={maxDate}
        />
      </Section>
      <Section>
        <div className="bold">{i18n.common.form.endDate}</div>
        <DatePicker
          date={form.endDate}
          onChange={(endDate) => assignFormValues({ endDate })}
          type="full-width"
          minDate={minDate}
          maxDate={maxDate}
        />
      </Section>
      {form.errors.length > 0 && (
        <Section>
          {form.errors.map((error, index) => (
            <div className="error" key={index}>
              {error}
            </div>
          ))}
        </Section>
      )}
    </FormModal>
  )
})
