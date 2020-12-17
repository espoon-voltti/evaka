// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import FormModal from '~components/common/FormModal'
import Section from '~components/shared/layout/Section'
import { Result } from '~api'
import { faExchange } from '@evaka/lib-icons'
import { transferGroup } from '~api/unit'
import { UUID } from '~types'
import { DatePicker } from '~components/common/DatePicker'
import { formatName } from '~utils'
import { DaycareGroupPlacementDetailed, DaycareGroup } from '~types/unit'
import Select from '~components/common/Select'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'

interface Props {
  placement: DaycareGroupPlacementDetailed
  groups: DaycareGroup[]
  reload: () => void
}

interface GroupPlacementForm {
  startDate: LocalDate
  groupId: UUID | null
  errors: string[]
}

export default React.memo(function GroupTransferModal({
  placement,
  groups,
  reload
}: Props) {
  const {
    daycarePlacementId,
    id: groupPlacementId,
    child,
    startDate: minDate,
    endDate: maxDate
  } = placement

  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(maxDate))
    .filter((group) => !group.endDate || !group.endDate.isBefore(minDate))

  const initialFormState = {
    startDate: minDate,
    groupId: placement.groupId,
    errors: []
  }
  const [form, setForm] = useState<GroupPlacementForm>(initialFormState)

  const validate = (form: GroupPlacementForm): string[] => {
    const errors = []
    if (form.groupId == null)
      errors.push(i18n.unit.placements.modal.errors.noGroup)
    else {
      const group = openGroups.find((g) => g.id == form.groupId)
      if (group) {
        if (form.startDate.isBefore(group.startDate))
          errors.push(i18n.unit.placements.modal.errors.groupNotStarted)
        if (group.endDate && placement.endDate.isAfter(group.endDate))
          errors.push(i18n.unit.placements.modal.errors.groupEnded)
      }
    }
    return errors
  }

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

    void transferGroup(
      daycarePlacementId,
      groupPlacementId || '',
      form.groupId,
      form.startDate
    ).then((res: Result<null>) => {
      if (res.isFailure) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.transfer,
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
      title={i18n.unit.placements.modal.transferTitle}
      icon={faExchange}
      iconColour={'blue'}
      resolveLabel={i18n.common.confirm}
      rejectLabel={i18n.common.cancel}
      reject={() => clearUiMode()}
      resolveDisabled={form.errors.length > 0}
      resolve={() => submitForm()}
    >
      <FixedSpaceColumn>
        <Section>
          <div className="bold">{i18n.unit.placements.modal.child}</div>
          <span>{formatName(child.firstName, child.lastName, i18n)}</span>
        </Section>
        <Section>
          <div className="bold">{i18n.unit.placements.modal.group}</div>
          <Select
            options={openGroups.map((group) => ({
              value: group.id,
              label: group.name
            }))}
            onChange={(value) =>
              value && 'value' in value
                ? assignFormValues({
                    groupId: value.value
                  })
                : undefined
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
        {form.errors.length > 0 && (
          <Section>
            {form.errors.map((error, index) => (
              <div className="error" key={index}>
                {error}
              </div>
            ))}
          </Section>
        )}
      </FixedSpaceColumn>
    </FormModal>
  )
})
