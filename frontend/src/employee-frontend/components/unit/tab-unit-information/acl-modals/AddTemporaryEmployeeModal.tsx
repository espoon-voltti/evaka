// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { isValidPinCode } from 'employee-frontend/components/employee/EmployeePinCodePage'
import { Action } from 'lib-common/generated/action'
import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { createTemporaryEmployeeMutation } from '../../queries'

import { useGroupOptions } from './common'

type Props = {
  onClose: () => void
  unitId: DaycareId
  groups: Record<string, DaycareGroupResponse>
  permittedActions: Action.Unit[]
}

type FormState = {
  firstName: string
  lastName: string
  selectedGroups: DaycareGroupResponse[] | null
  hasStaffOccupancyEffect: boolean | null
  pinCode: string
}

export default React.memo(function AddTemporaryEmployeeModal({
  onClose,
  unitId,
  groups,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()

  const [formData, setFormData] = useState<FormState>({
    firstName: '',
    lastName: '',
    selectedGroups: null,
    hasStaffOccupancyEffect: permittedActions.includes(
      'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
    )
      ? false
      : null,
    pinCode: ''
  })

  const groupOptions = useGroupOptions(groups)

  const pinCodeIsValid = isValidPinCode(formData.pinCode)
  const isValid = formData.firstName && formData.lastName && pinCodeIsValid

  return (
    <MutateFormModal
      title={i18n.unit.accessControl.addTemporaryEmployeeModal.title}
      resolveMutation={createTemporaryEmployeeMutation}
      resolveLabel={i18n.common.save}
      resolveAction={() => ({
        unitId,
        body: {
          firstName: formData.firstName,
          lastName: formData.lastName,
          groupIds: formData.selectedGroups?.map((g) => g.id) ?? [],
          hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect ?? false,
          pinCode: formData.pinCode ? { pin: formData.pinCode } : null
        }
      })}
      resolveDisabled={!isValid}
      onSuccess={onClose}
      rejectLabel={i18n.common.cancel}
      rejectAction={onClose}
      data-qa="add-temporary-employee-modal"
    >
      <FixedSpaceColumn>
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
          <Label>
            {`${i18n.unit.accessControl.temporaryEmployees.pinCode} *`}
          </Label>
          <InputField
            value={formData.pinCode}
            onChange={(pinCode) => setFormData({ ...formData, pinCode })}
            placeholder={
              i18n.unit.accessControl.temporaryEmployees.pinCodePlaceholder
            }
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
