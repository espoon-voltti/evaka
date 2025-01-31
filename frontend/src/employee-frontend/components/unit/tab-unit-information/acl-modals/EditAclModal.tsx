// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Action } from 'lib-common/generated/action'
import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow, DaycareId } from 'lib-common/generated/api-types/shared'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { formatName } from '../../../../utils'
import { updateGroupAclWithOccupancyCoefficientMutation } from '../../queries'

import { useGroupOptions } from './common'

interface Props {
  onClose: () => void
  permittedActions: Action.Unit[]
  unitId: DaycareId
  groups: Record<string, DaycareGroupResponse>
  row: DaycareAclRow
}

type FormState = {
  selectedGroups: DaycareGroupResponse[] | null
  hasStaffOccupancyEffect: boolean | null
}

export default React.memo(function EditAclModal({
  onClose,
  permittedActions,
  unitId,
  groups,
  row
}: Props) {
  const { i18n } = useTranslation()
  const { employee, role } = row
  const groupOptions = useGroupOptions(groups)

  const [formData, setFormData] = useState<FormState>({
    selectedGroups: permittedActions.includes('UPDATE_STAFF_GROUP_ACL')
      ? groupOptions.filter((option) => row.groupIds.includes(option.id))
      : null,
    hasStaffOccupancyEffect: permittedActions.includes(
      'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
    )
      ? employee.hasStaffOccupancyEffect
      : null
  })

  return (
    <MutateFormModal
      title={i18n.unit.accessControl.editDaycareAclModal.title}
      resolveMutation={updateGroupAclWithOccupancyCoefficientMutation}
      resolveAction={() => ({
        unitId,
        employeeId: row.employee.id,
        body: {
          groupIds: formData.selectedGroups?.map(({ id }) => id) ?? null,
          hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect
        }
      })}
      resolveLabel={i18n.common.save}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      onSuccess={onClose}
      data-qa="edit-acl-modal"
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.unit.accessControl.name}</Label>
          <div>
            {formatName(row.employee.firstName, row.employee.lastName, i18n)}
          </div>
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.unit.accessControl.role}</Label>
          <div>{i18n.roles.adRoles[role]}</div>
        </FixedSpaceColumn>
        {permittedActions.includes('UPDATE_STAFF_GROUP_ACL') && (
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.unit.accessControl.chooseGroup}</Label>
            <MultiSelect
              data-qa="group-select"
              value={formData.selectedGroups ?? []}
              options={groupOptions}
              getOptionId={(item) => item.id}
              getOptionLabel={(item) => item.name}
              onChange={(values) =>
                setFormData({ ...formData, selectedGroups: values })
              }
              placeholder={`${i18n.common.select}...`}
            />
          </FixedSpaceColumn>
        )}
        {permittedActions.includes('READ_STAFF_OCCUPANCY_COEFFICIENTS') && (
          <Checkbox
            data-qa="coefficient-checkbox"
            checked={formData.hasStaffOccupancyEffect === true}
            disabled={
              !permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
            }
            label={i18n.unit.accessControl.hasOccupancyCoefficient}
            onChange={(checked) => {
              setFormData({
                ...formData,
                hasStaffOccupancyEffect: checked
              })
            }}
          />
        )}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
