// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { UpdateStateFn } from 'lib-common/form-state'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { faExchange } from 'lib-icons'

import { useTranslation } from '../../../../../state/i18n'
import { UIContext } from '../../../../../state/ui'
import { DaycareGroupPlacementDetailed } from '../../../../../types/unit'
import { formatName } from '../../../../../utils'
import { transferGroupMutation } from '../../../queries'

interface Props {
  unitId: UUID
  placement: DaycareGroupPlacementDetailed
  groups: DaycareGroup[]
}

interface GroupPlacementForm {
  startDate: LocalDate
  group: DaycareGroup | null
  errors: string[]
}

export default React.memo(function GroupTransferModal({
  unitId,
  placement,
  groups
}: Props) {
  const {
    id: groupPlacementId,
    child,
    startDate: minDate,
    endDate: maxDate
  } = placement

  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  // filter out groups which are not active on any day during the maximum placement time range
  const openGroups = groups
    .filter((group) => !group.startDate.isAfter(maxDate))
    .filter((group) => !group.endDate || !group.endDate.isBefore(minDate))

  const initialFormState = {
    startDate: minDate,
    group: groups.find(({ id }) => id === placement.groupId) ?? null,
    errors: []
  }
  const [form, setForm] = useState<GroupPlacementForm>(initialFormState)

  const validate = (form: GroupPlacementForm): string[] => {
    const errors = []
    if (!form.group) {
      errors.push(i18n.unit.placements.modal.errors.noGroup)
    } else {
      const group = openGroups.find((g) => g.id === form.group?.id)
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

  return (
    <MutateFormModal
      data-qa="group-placement-modal"
      title={i18n.unit.placements.modal.transferTitle}
      icon={faExchange}
      type="info"
      resolveMutation={transferGroupMutation}
      resolveAction={() =>
        form.group !== null
          ? {
              unitId,
              groupPlacementId: groupPlacementId || '',
              body: {
                groupId: form.group.id,
                startDate: form.startDate
              }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.confirm}
      resolveDisabled={form.errors.length > 0}
      onSuccess={clearUiMode}
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <section>
          <div className="bold">{i18n.unit.placements.modal.child}</div>
          <span>{formatName(child.firstName, child.lastName, i18n)}</span>
        </section>
        <section>
          <div className="bold">{i18n.unit.placements.modal.group}</div>
          <Select
            items={openGroups}
            onChange={(group) =>
              group ? assignFormValues({ group }) : undefined
            }
            selectedItem={form.group}
            getItemLabel={(group) => group.name}
            getItemValue={(group) => group.id}
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
    </MutateFormModal>
  )
})
