// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  DaycareGroupSummary,
  getTemporaryEmployee,
  updateTemporaryEmployee
} from 'employee-frontend/api/unit'
import { isValidPinCode } from 'employee-frontend/components/employee/EmployeePinCodePage'
import { Failure, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { AclUpdate } from 'lib-common/generated/api-types/daycare'
import { TemporaryEmployee } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
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

import { useTranslation } from '../../../state/i18n'

import { FormattedRow } from './UnitAccessControl'

type EmployeeRowEditFormState = {
  firstName: string
  lastName: string
  selectedGroups: DaycareGroupSummary[] | null
  hasStaffOccupancyEffect: boolean | null
  pinCode: string
}

type EmployeeRowEditModalProps = {
  onClose: () => void
  onSuccess: () => void
  updatesGroupAcl: (
    unitId: UUID,
    employeeId: UUID,
    update: AclUpdate
  ) => Promise<Result<unknown>>
  permittedActions: Set<Action.Unit>
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employeeRow: FormattedRow
}

export default React.memo(function EmployeeAclRowEditModal({
  onClose,
  onSuccess,
  updatesGroupAcl,
  permittedActions,
  unitId,
  groups,
  employeeRow
}: EmployeeRowEditModalProps) {
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

  const initSelectedGroups = (groupIds: UUID[]) => {
    return permittedActions.has('UPDATE_STAFF_GROUP_ACL')
      ? groupOptions.filter((option) => groupIds.includes(option.id))
      : null
  }

  const [formData, setFormData] = useState<EmployeeRowEditFormState>({
    firstName: employeeRow.firstName ?? '',
    lastName: employeeRow.lastName ?? '',
    selectedGroups: initSelectedGroups(employeeRow.groupIds ?? []),
    hasStaffOccupancyEffect: employeeRow
      ? employeeRow.hasStaffOccupancyEffect
      : null,
    pinCode: ''
  })

  const setTemporaryEmployee = useCallback(
    (result: Result<TemporaryEmployee>) => {
      if (result.isSuccess) {
        setFormData({
          firstName: result.value.firstName,
          lastName: result.value.lastName,
          selectedGroups: groupOptions.filter((option) =>
            result.value.groupIds.includes(option.id)
          ),
          hasStaffOccupancyEffect: result.value.hasStaffOccupancyEffect,
          pinCode: result.value.pinCode?.pin ?? ''
        })
      }
    },
    [groupOptions]
  )
  const fetchTemporaryEmployee = useRestApi(
    getTemporaryEmployee,
    setTemporaryEmployee
  )
  useEffect(() => {
    if (employeeRow.temporary) {
      void fetchTemporaryEmployee(unitId, employeeRow.id ?? '')
    }
  }, [employeeRow.id, employeeRow.temporary, fetchTemporaryEmployee, unitId])

  const submit = useCallback(() => {
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
    if (!employeeRow) {
      return Promise.reject(Failure.of({ message: 'no parameters available' }))
    } else if (employeeRow.temporary) {
      return updateTemporaryEmployee(unitId, employeeRow.id, {
        firstName: formData.firstName,
        lastName: formData.lastName,
        groupIds: formData.selectedGroups
          ? formData.selectedGroups.map((item) => item.id)
          : [],
        hasStaffOccupancyEffect: formData.hasStaffOccupancyEffect ?? false,
        pinCode: formData.pinCode ? { pin: formData.pinCode } : null
      })
    } else {
      return updatesGroupAcl(unitId, employeeRow.id, updateBody)
    }
  }, [formData, unitId, employeeRow, updatesGroupAcl, permittedActions])

  const pinCodeIsValid = isValidPinCode(formData.pinCode)
  const isValid =
    !employeeRow.temporary ||
    (formData.firstName && formData.lastName && pinCodeIsValid)

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
            <H2 noMargin>{employeeRow.name ?? ''}</H2>
          )}
        </Centered>
        {employeeRow.temporary && (
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
        {permittedActions.has('UPDATE_STAFF_GROUP_ACL') && (
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
        {permittedActions.has('READ_STAFF_OCCUPANCY_COEFFICIENTS') && (
          <Checkbox
            data-qa="edit-acl-modal-coeff-checkbox"
            checked={formData.hasStaffOccupancyEffect === true}
            disabled={
              !permittedActions.has('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
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
      </Content>
    </PlainModal>
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
