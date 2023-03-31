// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  AclUpdateDetails,
  DaycareGroupSummary
} from 'employee-frontend/api/unit'
import { StaffOccupancyCoefficientUtil } from 'employee-frontend/utils/StaffOccupancyCoefficientUtil'
import { Failure, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'

import { FormattedRow } from './UnitAccessControl'

type EmployeeRowEditFormState = {
  selectedGroups?: DaycareGroupSummary[]
  occupancyCoefficient?: number
}

type EmployeeRowEditModalProps = {
  onClose: () => void
  onSuccess: () => void
  updatesGroupAcl: (
    unitId: UUID,
    employeeId: UUID,
    update: AclUpdateDetails
  ) => Promise<Result<unknown>>
  permittedActions: Set<Action.Unit>
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employeeRow?: FormattedRow
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
      : undefined
  }

  const [formData, setFormData] = useState<EmployeeRowEditFormState>({
    selectedGroups: initSelectedGroups(employeeRow?.groupIds ?? []),
    occupancyCoefficient: employeeRow?.coefficient
  })

  const submit = useCallback(() => {
    const updateBody = {
      groupIds: formData.selectedGroups?.map((g) => g.id),
      occupancyCoefficient: permittedActions.has(
        'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
      )
        ? formData.occupancyCoefficient
        : undefined
    }
    if (!employeeRow) {
      return Promise.reject(Failure.of({ message: 'no parameters available' }))
    } else {
      return updatesGroupAcl(unitId, employeeRow.id, updateBody)
    }
  }, [formData, unitId, employeeRow, updatesGroupAcl, permittedActions])

  return (
    <PlainModal margin="auto" data-qa="employee-row-edit-person-modal">
      <Content>
        <Centered>
          <H1 noMargin>{i18n.unit.accessControl.editEmployeeRowModal.title}</H1>

          <H2 noMargin>{employeeRow?.name ?? ''}</H2>
        </Centered>
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
            checked={StaffOccupancyCoefficientUtil.parseToBoolean(
              formData.occupancyCoefficient
            )}
            disabled={
              !permittedActions.has('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
            }
            label={i18n.unit.accessControl.hasOccupancyCoefficient}
            onChange={(checked) => {
              setFormData({
                ...formData,
                occupancyCoefficient:
                  StaffOccupancyCoefficientUtil.parseToNumber(checked)
              })
            }}
          />
        )}
        <BottomRow>
          <InlineButton
            className="left-button"
            text={i18n.common.cancel}
            data-qa="edit-acl-row-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            className="right-button"
            text={i18n.common.save}
            data-qa="edit-acl-row-save-btn"
            onClick={submit}
            onSuccess={onSuccess}
          />
        </BottomRow>
      </Content>
    </PlainModal>
  )
})

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
