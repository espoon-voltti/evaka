// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { isValidPinCode } from 'employee-frontend/components/employee/EmployeePinCodePage'
import { Result, Success, wrapResult } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import {
  AclUpdate,
  DaycareGroupResponse
} from 'lib-common/generated/api-types/daycare'
import { TemporaryEmployee } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { StaticChip } from 'lib-components/atoms/Chip'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import {
  getTemporaryEmployee,
  updateTemporaryEmployee
} from '../../../generated/api-clients/daycare'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'

import { FormattedRow } from './UnitAccessControl'

const getTemporaryEmployeeResult = wrapResult(getTemporaryEmployee)
const updateTemporaryEmployeeResult = wrapResult(updateTemporaryEmployee)

type EmployeeRowEditFormState = {
  firstName: string
  lastName: string
  selectedGroups: DaycareGroupResponse[] | null
  hasStaffOccupancyEffect: boolean | null
  pinCode: string
}

interface Props {
  onClose: () => void
  onSuccess: () => void
  updatesGroupAcl: (arg: {
    daycareId: UUID
    employeeId: UUID
    body: AclUpdate
  }) => Promise<Result<unknown>>
  permittedActions: Action.Unit[]
  unitId: UUID
  groups: Record<string, DaycareGroupResponse>
  employeeRow: FormattedRow
}

export default React.memo(function EmployeeAclRowEditModal({
  unitId,
  employeeRow,
  ...props
}: Props) {
  const { i18n } = useTranslation()

  const [temporaryEmployee] = useApiState(
    (): Promise<Result<TemporaryEmployee | undefined>> =>
      employeeRow.temporary
        ? getTemporaryEmployeeResult({ unitId, employeeId: employeeRow.id })
        : Promise.resolve(Success.of(undefined)),
    [employeeRow.temporary, employeeRow.id, unitId]
  )

  return (
    <PlainModal margin="auto" data-qa="employee-row-edit-person-modal">
      <Content>
        <Centered>
          <H1 noMargin>{i18n.unit.accessControl.editEmployeeRowModal.title}</H1>

          {employeeRow.temporary ? (
            <StaticChip color={colors.accents.a8lightBlue}>
              {i18n.unit.accessControl.addDaycareAclModal.type.TEMPORARY}
            </StaticChip>
          ) : (
            <H2 noMargin>{employeeRow.name}</H2>
          )}
        </Centered>
        {renderResult(temporaryEmployee, (temporaryEmployee) => (
          <EmployeeAclEditForm
            temporaryEmployee={temporaryEmployee}
            employeeRow={employeeRow}
            unitId={unitId}
            {...props}
          />
        ))}
      </Content>
    </PlainModal>
  )
})

type FormProps = Props & { temporaryEmployee: TemporaryEmployee | undefined }

const EmployeeAclEditForm = React.memo(function EmployeeAclEditForm({
  onClose,
  onSuccess,
  updatesGroupAcl,
  permittedActions,
  unitId,
  groups,
  employeeRow,
  temporaryEmployee
}: FormProps) {
  const { i18n } = useTranslation()

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

  const initSelectedGroups = (groupIds: UUID[]) =>
    permittedActions.includes('UPDATE_STAFF_GROUP_ACL')
      ? groupOptions.filter((option) => groupIds.includes(option.id))
      : null

  const [formData, setFormData] = useState<EmployeeRowEditFormState>(
    temporaryEmployee
      ? {
          firstName: temporaryEmployee.firstName,
          lastName: temporaryEmployee.lastName,
          selectedGroups: groupOptions.filter((option) =>
            temporaryEmployee.groupIds.includes(option.id)
          ),
          hasStaffOccupancyEffect: temporaryEmployee.hasStaffOccupancyEffect,
          pinCode: temporaryEmployee.pinCode?.pin ?? ''
        }
      : {
          firstName: employeeRow.firstName ?? '',
          lastName: employeeRow.lastName ?? '',
          selectedGroups: initSelectedGroups(employeeRow.groupIds ?? []),
          hasStaffOccupancyEffect: employeeRow
            ? employeeRow.hasStaffOccupancyEffect
            : null,
          pinCode: ''
        }
  )

  const submit = useCallback(() => {
    const updateBody: AclUpdate = {
      groupIds: permittedActions.includes('UPDATE_STAFF_GROUP_ACL')
        ? formData.selectedGroups
          ? formData.selectedGroups.map((g) => g.id)
          : null
        : null,
      hasStaffOccupancyEffect: permittedActions.includes(
        'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
      )
        ? formData.hasStaffOccupancyEffect
        : null
    }
    if (employeeRow.temporary) {
      return updateTemporaryEmployeeResult({
        unitId,
        employeeId: employeeRow.id,
        body: {
          firstName: formData.firstName,
          lastName: formData.lastName,
          groupIds: formData.selectedGroups
            ? formData.selectedGroups.map((item) => item.id)
            : [],
          hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect ?? false,
          pinCode: formData.pinCode ? { pin: formData.pinCode } : null
        }
      })
    } else {
      return updatesGroupAcl({
        daycareId: unitId,
        employeeId: employeeRow.id,
        body: updateBody
      })
    }
  }, [formData, unitId, employeeRow, updatesGroupAcl, permittedActions])

  const pinCodeIsValid = isValidPinCode(formData.pinCode)
  const isValid =
    !employeeRow.temporary ||
    (formData.firstName && formData.lastName && pinCodeIsValid)

  return (
    <>
      {employeeRow.temporary && (
        <>
          <FormControl>
            <FieldLabel>
              {`${i18n.unit.accessControl.addDaycareAclModal.firstName} *`}
            </FieldLabel>
            <InputField
              value={formData.firstName}
              onChange={(firstName) => setFormData({ ...formData, firstName })}
              placeholder={
                i18n.unit.accessControl.addDaycareAclModal.firstNamePlaceholder
              }
              data-qa="first-name"
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
              data-qa="last-name"
            />
          </FormControl>
        </>
      )}
      {permittedActions.includes('UPDATE_STAFF_GROUP_ACL') && (
        <FormControl>
          <FieldLabel>
            {i18n.unit.accessControl.addDaycareAclModal.groups}
          </FieldLabel>
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
        </FormControl>
      )}
      {permittedActions.includes('READ_STAFF_OCCUPANCY_COEFFICIENTS') && (
        <Checkbox
          data-qa="edit-acl-modal-coeff-checkbox"
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
      {employeeRow.temporary && (
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
            data-qa="pin-code"
            info={
              formData.pinCode && !pinCodeIsValid
                ? { text: i18n.pinCode.error, status: 'warning' }
                : undefined
            }
          />
        </FormControl>
      )}
      <BottomRow>
        <InlineButton
          text={i18n.common.cancel}
          data-qa="edit-acl-row-cancel-btn"
          onClick={onClose}
        />
        <InlineAsyncButton
          text={i18n.common.save}
          data-qa="edit-acl-row-save-btn"
          onClick={submit}
          onSuccess={onSuccess}
          disabled={!isValid}
        />
      </BottomRow>
    </>
  )
})

const BottomRow = styled.div`
  display: flex;
  justify-content: space-between;
  margin-top: auto;
`
const FormControl = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`

const Content = styled.div`
  padding: ${defaultMargins.XL};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.m};
  position: relative;
  min-height: 80vh;
  max-height: 800px;
`
const Centered = styled(FixedSpaceColumn)`
  align-self: center;
  text-align: center;
  gap: ${defaultMargins.L};
`
const FieldLabel = styled(Label)`
  margin-bottom: 8px;
`
