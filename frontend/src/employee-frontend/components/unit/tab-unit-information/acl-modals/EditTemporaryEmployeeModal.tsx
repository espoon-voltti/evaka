// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow, DaycareId } from 'lib-common/generated/api-types/shared'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { isValidPinCode } from '../../../employee/EmployeePinCodePage'
import { updateTemporaryEmployeeMutation } from '../../queries'

import { useGroupOptions } from './common'

interface Props {
  onClose: () => void
  unitId: DaycareId
  groups: Record<string, DaycareGroupResponse>
  row: DaycareAclRow
}

type FormState = {
  firstName: string
  lastName: string
  selectedGroups: DaycareGroupResponse[]
  hasStaffOccupancyEffect: boolean
  pinCode: string
}

export default React.memo(function EditTemporaryEmployeeModal({
  onClose,
  unitId,
  groups,
  row
}: Props) {
  const { i18n } = useTranslation()
  const { employee } = row
  const groupOptions = useGroupOptions(groups)

  const [formData, setFormData] = useState<FormState>({
    firstName: employee.firstName,
    lastName: employee.lastName,
    selectedGroups: groupOptions.filter((option) =>
      row.groupIds.includes(option.id)
    ),
    hasStaffOccupancyEffect: employee.hasStaffOccupancyEffect ?? false,
    pinCode: ''
  })

  // empty pin keeps the original
  const pinCodeIsValid =
    formData.pinCode.length > 0 ? isValidPinCode(formData.pinCode) : true
  const isValid = formData.firstName && formData.lastName && pinCodeIsValid

  return (
    <MutateFormModal
      title={i18n.unit.accessControl.editDaycareAclModal.title}
      resolveMutation={updateTemporaryEmployeeMutation}
      resolveAction={() => ({
        unitId,
        employeeId: employee.id,
        body: {
          firstName: formData.firstName,
          lastName: formData.lastName,
          groupIds: formData.selectedGroups.map((group) => group.id),
          hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect,
          pinCode:
            formData.pinCode.length > 0 ? { pin: formData.pinCode } : null
        }
      })}
      resolveLabel={i18n.common.save}
      resolveDisabled={!isValid}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      onSuccess={onClose}
      data-qa="edit-temporary-employee-modal"
    >
      <FixedSpaceColumn>
        <H1 noMargin>
          {i18n.unit.accessControl.editTemporaryEmployeeModal.title}
        </H1>

        <FixedSpaceColumn spacing="xs">
          <Label>
            {`${i18n.unit.accessControl.temporaryEmployees.firstName} *`}
          </Label>
          <InputField
            value={formData.firstName}
            onChange={(firstName) => setFormData({ ...formData, firstName })}
            placeholder={
              i18n.unit.accessControl.temporaryEmployees.firstNamePlaceholder
            }
            data-qa="first-name"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xs">
          <Label>
            {`${i18n.unit.accessControl.temporaryEmployees.lastName} *`}
          </Label>
          <InputField
            value={formData.lastName}
            onChange={(lastName) => setFormData({ ...formData, lastName })}
            placeholder={
              i18n.unit.accessControl.temporaryEmployees.lastNamePlaceholder
            }
            data-qa="last-name"
          />
        </FixedSpaceColumn>
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
        <Checkbox
          data-qa="coefficient-checkbox"
          checked={formData.hasStaffOccupancyEffect}
          label={i18n.unit.accessControl.hasOccupancyCoefficient}
          onChange={(checked) => {
            setFormData({
              ...formData,
              hasStaffOccupancyEffect: checked
            })
          }}
        />
        <FixedSpaceColumn spacing="xs">
          <Label>
            {`${i18n.unit.accessControl.temporaryEmployees.pinCode}`}
          </Label>
          <InputField
            value={formData.pinCode}
            onChange={(pinCode) => setFormData({ ...formData, pinCode })}
            placeholder="****"
            data-qa="pin-code"
            info={
              formData.pinCode && !pinCodeIsValid
                ? { text: i18n.pinCode.error, status: 'warning' }
                : undefined
            }
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
