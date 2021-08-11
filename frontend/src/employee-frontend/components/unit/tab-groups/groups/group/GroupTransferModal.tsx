// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { faExchange } from 'lib-icons'
import React, { useContext, useState } from 'react'
import { transferGroup } from '../../../../../api/unit'
import Select, { SelectOption } from '../../../../../components/common/Select'
import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import {
  DaycareGroup,
  DaycareGroupPlacementDetailed
} from '../../../../../types/unit'
import { formatName } from '../../../../../utils'

interface Props {
  placement: DaycareGroupPlacementDetailed
  groups: DaycareGroup[]
  reload: () => void
}

interface GroupPlacementForm {
  startDate: LocalDate
  group: SelectOption | null
  errors: string[]
}

export default React.memo(function GroupTransferModal({
  placement,
  groups,
  reload
}: Props) {
  const {
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
    group: placement.groupId
      ? {
          value: placement.groupId,
          label: groups.find((g) => g.id === placement.groupId)?.name ?? '??'
        }
      : null,
    errors: []
  }
  const [form, setForm] = useState<GroupPlacementForm>(initialFormState)

  const validate = (form: GroupPlacementForm): string[] => {
    const errors = []
    if (!form.group) {
      errors.push(i18n.unit.placements.modal.errors.noGroup)
    } else {
      const group = openGroups.find((g) => g.id == form.group?.value)
      if (group) {
        if (form.startDate.isBefore(group.startDate))
          errors.push(i18n.unit.placements.modal.errors.groupNotStarted)
        if (group.endDate && placement.endDate.isAfter(group.endDate))
          errors.push(i18n.unit.placements.modal.errors.groupEnded)
      }
    }
    return errors
  }

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

  const submitForm = () => {
    if (!form.group) return

    void transferGroup(
      groupPlacementId || '',
      form.group.value,
      form.startDate
    ).then((res: Result<null>) => {
      if (res.isFailure) {
        clearUiMode()
        setErrorMessage({
          type: 'error',
          title: i18n.unit.error.placement.transfer,
          text: i18n.common.tryAgain,
          resolveLabel: i18n.common.ok
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
          <div className="bold">{i18n.unit.placements.modal.child}</div>
          <span>{formatName(child.firstName, child.lastName, i18n)}</span>
        </section>
        <section>
          <div className="bold">{i18n.unit.placements.modal.group}</div>
          <Select
            items={openGroups.map((group) => ({
              value: group.id,
              label: group.name
            }))}
            onChange={(group) =>
              group ? assignFormValues({ group }) : undefined
            }
            selectedItem={form.group}
          />
        </section>
        <section>
          <div className="bold">{i18n.common.form.startDate}</div>
          <DatePickerDeprecated
            date={form.startDate}
            onChange={(startDate) => assignFormValues({ startDate })}
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
