// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  addDaycareFullAcl,
  DaycareGroupSummary
} from 'employee-frontend/api/unit'
import { Employee } from 'employee-frontend/types/employee'
import { formatName } from 'employee-frontend/utils'
import { Failure } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'

import { DaycareAclRole } from './UnitAccessControl'

interface EmployeeOption {
  label: string
  value: string
}

type DaycareAclAdditionFormState = {
  selectedEmployee: EmployeeOption | null
  selectedGroups?: DaycareGroupSummary[]
}

type DaycareAclAdditionModalProps = {
  onClose: () => void
  onSuccess: () => void
  role?: DaycareAclRole
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employees: Employee[]
  groupsPermitted: boolean
}

export default React.memo(function DaycareAclAdditionModal({
  onClose,
  onSuccess,
  role,
  unitId,
  groups,
  employees,
  groupsPermitted
}: DaycareAclAdditionModalProps) {
  const { i18n } = useTranslation()

  const [formData, setFormData] = useState<DaycareAclAdditionFormState>({
    selectedEmployee: null,
    selectedGroups: undefined
  })

  const submit = useCallback(async () => {
    const requestBody = {
      employeeId: formData.selectedEmployee?.value ?? '',
      groupIds: formData.selectedGroups?.map((g) => g.id)
    }
    if (requestBody.employeeId === '' || !role) {
      return Promise.reject(Failure.of({ message: 'no parameters available' }))
    } else {
      return addDaycareFullAcl(
        unitId,
        requestBody.employeeId,
        role,
        requestBody.groupIds
      )
    }
  }, [formData, unitId, role])

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

  return (
    <PlainModal margin="auto" data-qa="staff-add-person-modal">
      <Content>
        <Centered>
          <H1 noMargin>{i18n.unit.accessControl.addDaycareAclModal.title}</H1>
        </Centered>
        <FormControl>
          <FieldLabel>
            {`${i18n.unit.accessControl.addDaycareAclModal.employees} *`}
          </FieldLabel>
          <Combobox
            clearable
            data-qa="acl-combobox"
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
        {groupsPermitted && (
          <FormControl>
            <FieldLabel>
              {i18n.unit.accessControl.addDaycareAclModal.groups}
            </FieldLabel>
            <MultiSelect
              data-qa="add-person-group-select"
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
        <BottomRow>
          <InlineButton
            className="left-button"
            text={i18n.common.cancel}
            data-qa="add-person-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            className="right-button"
            text={i18n.common.save}
            data-qa="add-person-save-btn"
            onClick={submit}
            onSuccess={onSuccess}
            disabled={!formData.selectedEmployee}
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
