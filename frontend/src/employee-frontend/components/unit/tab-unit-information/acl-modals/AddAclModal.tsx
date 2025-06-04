// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'

import type { Action } from 'lib-common/generated/action'
import type { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import type { Employee } from 'lib-common/generated/api-types/pis'
import type {
  DaycareId,
  EmployeeId,
  ScheduledDaycareAclRow
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { addFullAclForRoleMutation } from '../../queries'
import type { DaycareAclRole } from '../UnitAccessControl'

import { useGroupOptions } from './common'

type Props = {
  onClose: () => void
  unitId: DaycareId
  groups: Record<string, DaycareGroupResponse>
  employees: Employee[]
  permittedActions: Action.Unit[]
  scheduledAclRows: ScheduledDaycareAclRow[]
}

interface EmployeeOption {
  label: string
  value: EmployeeId
}

type FormState = {
  role: DaycareAclRole
  selectedEmployee: EmployeeOption | null
  selectedGroups: DaycareGroupResponse[] | null
  hasStaffOccupancyEffect: boolean | null
  endDate: LocalDate | null
}

export default React.memo(function AddAclModal({
  onClose,
  unitId,
  groups,
  employees,
  permittedActions,
  scheduledAclRows
}: Props) {
  const { i18n, lang } = useTranslation()

  const [formData, setFormData] = useState<FormState>({
    role: 'STAFF',
    selectedEmployee: null,
    selectedGroups: null,
    hasStaffOccupancyEffect: permittedActions.includes(
      'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
    )
      ? false
      : null,
    endDate: null
  })

  const roles: DaycareAclRole[] = useMemo(
    () => [
      ...(permittedActions.includes('INSERT_ACL_UNIT_SUPERVISOR')
        ? (['UNIT_SUPERVISOR'] as const)
        : []),
      ...(permittedActions.includes(
        'INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
      )
        ? (['EARLY_CHILDHOOD_EDUCATION_SECRETARY'] as const)
        : []),
      ...(permittedActions.includes('INSERT_ACL_SPECIAL_EDUCATION_TEACHER')
        ? (['SPECIAL_EDUCATION_TEACHER'] as const)
        : []),
      ...(permittedActions.includes('INSERT_ACL_STAFF')
        ? (['STAFF'] as const)
        : [])
    ],
    [permittedActions]
  )

  const employeeOptions: EmployeeOption[] = useMemo(
    () =>
      employees.map(({ id, email, firstName, lastName }) => {
        const name = formatPersonName({ firstName, lastName }, 'First Last')
        return {
          label: email ? `${email} (${name})` : name,
          value: id
        }
      }),
    [employees]
  )

  const groupOptions = useGroupOptions(groups)

  const scheduledAclWarning = useMemo(
    () =>
      scheduledAclRows.some(
        (row) => row.id === formData.selectedEmployee?.value
      ),
    [scheduledAclRows, formData.selectedEmployee]
  )

  return (
    <MutateFormModal
      title={i18n.unit.accessControl.addDaycareAclModal.title}
      resolveMutation={addFullAclForRoleMutation}
      resolveAction={() =>
        formData.selectedEmployee
          ? {
              unitId,
              employeeId: formData.selectedEmployee.value,
              body: {
                role: formData.role,
                update: {
                  groupIds: permittedActions.includes('UPDATE_STAFF_GROUP_ACL')
                    ? formData.selectedGroups
                      ? formData.selectedGroups.map((g) => g.id)
                      : null
                    : null,
                  hasStaffOccupancyEffect: permittedActions.includes(
                    'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
                  )
                    ? formData.hasStaffOccupancyEffect
                    : null,
                  endDate: formData.endDate
                }
              }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.save}
      onSuccess={onClose}
      rejectLabel={i18n.common.cancel}
      rejectAction={onClose}
      resolveDisabled={!formData.selectedEmployee}
      data-qa="add-acl-modal"
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>
            {`${i18n.unit.accessControl.addDaycareAclModal.role} *`}
          </Label>
          <Combobox
            data-qa="role-combobox"
            placeholder={i18n.unit.accessControl.chooseRole}
            selectedItem={formData.role}
            onChange={(item) =>
              setFormData({ ...formData, role: item ?? 'STAFF' })
            }
            items={roles}
            menuEmptyLabel={i18n.common.noResults}
            getItemLabel={(item) => i18n.roles.adRoles[item]}
            getItemDataQa={(item) => `value-${item}`}
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xs">
          <Label>
            {`${i18n.unit.accessControl.addDaycareAclModal.employees} *`}
          </Label>
          <Combobox
            clearable
            data-qa="employee-combobox"
            placeholder={i18n.unit.accessControl.choosePerson}
            selectedItem={formData.selectedEmployee}
            onChange={(item) =>
              setFormData({ ...formData, selectedEmployee: item })
            }
            items={employeeOptions}
            menuEmptyLabel={i18n.common.noResults}
            getItemLabel={(item) => item?.label ?? ''}
            getItemDataQa={(item) => `value-${item?.value ?? 'none'}`}
          />
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

        {permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS') && (
          <Checkbox
            data-qa="coefficient-checkbox"
            checked={formData.hasStaffOccupancyEffect === true}
            disabled={false}
            label={i18n.unit.accessControl.hasOccupancyCoefficient}
            onChange={(checked) => {
              setFormData({
                ...formData,
                hasStaffOccupancyEffect: checked
              })
            }}
          />
        )}

        <FixedSpaceColumn spacing="xs">
          <Label>{`${i18n.unit.accessControl.aclEndDate}`}</Label>
          <DatePicker
            data-qa="end-date"
            date={formData.endDate}
            onChange={(date) =>
              setFormData((prev) => ({ ...prev, endDate: date }))
            }
            locale={lang}
            minDate={LocalDate.todayInHelsinkiTz()}
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>

      {scheduledAclWarning && (
        <AlertBox
          message={
            i18n.unit.accessControl.addDaycareAclModal.scheduledAclWarning
          }
        />
      )}
    </MutateFormModal>
  )
})
