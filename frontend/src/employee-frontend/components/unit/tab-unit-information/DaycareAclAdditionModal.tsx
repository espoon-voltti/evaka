// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  addDaycareFullAcl,
  createTemporaryEmployee,
  DaycareGroupSummary
} from 'employee-frontend/api/unit'
import { isValidPinCode } from 'employee-frontend/components/employee/EmployeePinCodePage'
import { formatName } from 'employee-frontend/utils'
import { Failure } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { AclUpdate } from 'lib-common/generated/api-types/daycare'
import { Employee, TemporaryEmployee } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { SelectionChip } from 'lib-components/atoms/Chip'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'

import { DaycareAclRole } from './UnitAccessControl'

interface EmployeeOption {
  label: string
  value: string
}

type DaycareAclAdditionFormState = {
  type: 'PERMANENT' | 'TEMPORARY'
  firstName: string
  lastName: string
  selectedEmployee: EmployeeOption | null
  selectedGroups: DaycareGroupSummary[] | null
  hasStaffOccupancyEffect: boolean | null
  pinCode: string
}

type DaycareAclAdditionModalProps = {
  onClose: () => void
  onSuccess: () => void
  role?: DaycareAclRole
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employees: Employee[]
  permittedActions: Set<Action.Unit>
}

export default React.memo(function DaycareAclAdditionModal({
  onClose,
  onSuccess,
  role,
  unitId,
  groups,
  employees,
  permittedActions
}: DaycareAclAdditionModalProps) {
  const { i18n } = useTranslation()

  const [formData, setFormData] = useState<DaycareAclAdditionFormState>({
    type: 'PERMANENT',
    firstName: '',
    lastName: '',
    selectedEmployee: null,
    selectedGroups: null,
    hasStaffOccupancyEffect: permittedActions.has(
      'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
    )
      ? false
      : null,
    pinCode: ''
  })

  const submit = useCallback(async () => {
    if (formData.type === 'TEMPORARY') {
      const requestBody: TemporaryEmployee = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        groupIds: formData.selectedGroups?.map((g) => g.id) ?? [],
        hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect ?? false,
        pinCode: formData.pinCode ? { pin: formData.pinCode } : null
      }
      return createTemporaryEmployee(unitId, requestBody)
    }

    const employeeId = formData.selectedEmployee?.value ?? ''
    const updateBody: AclUpdate = {
      groupIds: permittedActions.has('UPDATE_STAFF_GROUP_ACL')
        ? formData.selectedGroups
          ? formData.selectedGroups.map((g) => g.id)
          : null
        : null,
      hasStaffOccupancyEffect: permittedActions.has(
        'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
      )
        ? formData.hasStaffOccupancyEffect
        : null
    }
    if (employeeId === '' || !role) {
      return Promise.reject(Failure.of({ message: 'no parameters available' }))
    } else {
      return addDaycareFullAcl(unitId, employeeId, role, updateBody)
    }
  }, [formData, unitId, role, permittedActions])

  const employeeOptions: EmployeeOption[] = useMemo(
    () =>
      employees.map(({ id, email, firstName, lastName }) => {
        const name = formatName(firstName, lastName, i18n)
        return {
          label: email ? `${email} (${name})` : name,
          value: id
        }
      }),
    [i18n, employees]
  )

  const groupOptions = useMemo(
    () =>
      sortBy(
        Object.values(groups).filter(
          ({ endDate }) =>
            endDate === null || endDate.isAfter(LocalDate.todayInHelsinkiTz())
        ),
        ({ name }) => name
      ),
    [groups]
  )

  const isValid =
    (formData.type === 'PERMANENT' && formData.selectedEmployee) ||
    (formData.type === 'TEMPORARY' &&
      formData.firstName &&
      formData.lastName &&
      isValidPinCode(formData.pinCode))

  return (
    <PlainModal margin="auto" data-qa="add-daycare-acl-modal">
      <Content>
        <Centered>
          <H1 noMargin>{i18n.unit.accessControl.addDaycareAclModal.title}</H1>
        </Centered>
        {featureFlags.experimental?.temporaryEmployee && role === 'STAFF' && (
          <FormControl>
            <FieldLabel>
              {`${i18n.unit.accessControl.addDaycareAclModal.employees} *`}
            </FieldLabel>
            <FixedSpaceRow fullWidth justifyContent="space-evenly">
              <SelectionChip
                text={i18n.unit.accessControl.addDaycareAclModal.type.PERMANENT}
                onChange={() => setFormData({ ...formData, type: 'PERMANENT' })}
                selected={formData.type === 'PERMANENT'}
                data-qa="add-daycare-acl-type-permanent"
              />
              <SelectionChip
                text={i18n.unit.accessControl.addDaycareAclModal.type.TEMPORARY}
                onChange={() => setFormData({ ...formData, type: 'TEMPORARY' })}
                selected={formData.type === 'TEMPORARY'}
                data-qa="add-daycare-acl-type-temporary"
              />
            </FixedSpaceRow>
          </FormControl>
        )}
        {formData.type === 'PERMANENT' ? (
          <FormControl>
            <FieldLabel>
              {`${i18n.unit.accessControl.addDaycareAclModal.employees} *`}
            </FieldLabel>
            <Combobox
              clearable
              data-qa="add-daycare-acl-emp-combobox"
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
          </FormControl>
        ) : (
          <>
            <FormControl>
              <FieldLabel>
                {`${i18n.unit.accessControl.addDaycareAclModal.firstName} *`}
              </FieldLabel>
              <InputField
                value={formData.firstName}
                onChange={(firstName) =>
                  setFormData({ ...formData, firstName })
                }
                placeholder={
                  i18n.unit.accessControl.addDaycareAclModal
                    .firstNamePlaceholder
                }
                data-qa="add-daycare-acl-first-name"
              />
            </FormControl>
            <FormControl>
              <FieldLabel>
                {`${i18n.unit.accessControl.addDaycareAclModal.lastName} *`}
              </FieldLabel>
              <InputField
                value={formData.lastName}
                onChange={(lastName) => setFormData({ ...formData, lastName })}
                placeholder={
                  i18n.unit.accessControl.addDaycareAclModal.lastNamePlaceholder
                }
                data-qa="add-daycare-acl-last-name"
              />
            </FormControl>
          </>
        )}
        {permittedActions.has('UPDATE_STAFF_GROUP_ACL') && (
          <FormControl>
            <FieldLabel>
              {i18n.unit.accessControl.addDaycareAclModal.groups}
            </FieldLabel>
            <MultiSelect
              data-qa="add-daycare-acl-group-select"
              value={formData.selectedGroups ?? []}
              options={groupOptions}
              getOptionId={(item) => item.id}
              getOptionLabel={(item) => item.name}
              onChange={(values) =>
                setFormData({ ...formData, selectedGroups: values })
              }
              placeholder={`${i18n.common.select}...`}
            />
          </FormControl>
        )}
        {permittedActions.has('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS') && (
          <Checkbox
            data-qa="add-daycare-acl-coeff-checkbox"
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
        {formData.type === 'TEMPORARY' && (
          <FormControl>
            <FieldLabel>
              {`${i18n.unit.accessControl.addDaycareAclModal.pinCode} *`}
            </FieldLabel>
            <InputField
              value={formData.pinCode}
              onChange={(pinCode) => setFormData({ ...formData, pinCode })}
              placeholder={
                i18n.unit.accessControl.addDaycareAclModal.pinCodePlaceholder
              }
              data-qa="add-daycare-acl-pin-code"
            />
          </FormControl>
        )}
        <BottomRow>
          <InlineButton
            className="left-button"
            text={i18n.common.cancel}
            data-qa="add-daycare-acl-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            className="right-button"
            text={i18n.common.save}
            data-qa="add-daycare-acl-save-btn"
            onClick={submit}
            onSuccess={onSuccess}
            disabled={!isValid}
          />
        </BottomRow>
      </Content>
    </PlainModal>
  )
})

const Content = styled.div`
  padding: ${defaultMargins.XL};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.m};
  min-height: 80vh;
  max-height: 800px;
  position: relative;
`
const Centered = styled(FixedSpaceColumn)`
  align-self: center;
  text-align: center;
  gap: ${defaultMargins.s};
`
const FieldLabel = styled(Label)`
  margin-bottom: 8px;
  margin-left: 2px;
`
const FormControl = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`
const BottomRow = styled.div`
  position: absolute;
  width: 100%;
  bottom: 50px;
  left: 0px;

  & > .left-button {
    position: absolute;
    left: 30px;
  }

  & > .right-button {
    position: absolute;
    right: 30px;
  }
`
