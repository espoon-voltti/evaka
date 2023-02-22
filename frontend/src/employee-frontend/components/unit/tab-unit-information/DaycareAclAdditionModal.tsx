// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { DaycareGroupSummary } from 'employee-frontend/api/unit'
import { Employee } from 'employee-frontend/types/employee'
import { formatName } from 'employee-frontend/utils'
import { Failure, Result } from 'lib-common/api'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights, H1, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

interface EmployeeOption {
  label: string
  value: string
}

type DaycareAclAdditionFormState = {
  selectedEmployee: EmployeeOption | null
  selectedGroups: DaycareGroupSummary[]
}

const validateForm = (
  formState: DaycareAclAdditionFormState
):
  | [{ employeeId: UUID; groupIds: UUID[] }]
  | [undefined, ErrorsOf<DaycareAclAdditionFormState>] => {
  const errors: ErrorsOf<DaycareAclAdditionFormState> = {
    selectedGroups: undefined,
    selectedEmployee: !formState.selectedEmployee ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0 || !formState.selectedEmployee) {
    return [undefined, errors]
  }

  return [
    {
      employeeId: formState.selectedEmployee.value,
      groupIds: formState.selectedGroups.map((g) => g.id)
    }
  ]
}

type DaycareAclAdditionModalProps = {
  onClose: () => void
  onSuccess: () => void
  addDaycareAcl?: (unitId: UUID, employeeId: UUID) => Promise<Result<unknown>>
  updateGroupsAcl: (
    unitId: UUID,
    employeeId: UUID,
    groupIds: UUID[]
  ) => Promise<Result<unknown>>
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employees: Employee[]
}

export default React.memo(function DaycareAclAdditionModal({
  onClose,
  onSuccess,
  addDaycareAcl,
  updateGroupsAcl,
  unitId,
  groups,
  employees
}: DaycareAclAdditionModalProps) {
  const { i18n } = useTranslation()

  const [formData, setFormData] = useState<DaycareAclAdditionFormState>({
    selectedEmployee: null,
    selectedGroups: []
  })

  const [requestBody, errors] = useMemo(
    () => validateForm(formData),
    [formData]
  )

  const submit = useCallback(async () => {
    if (requestBody === undefined || addDaycareAcl === undefined) {
      return Promise.reject(Failure.of({ message: 'validation error' }))
    } else {
      try {
        await addDaycareAcl(unitId, requestBody.employeeId)
        return updateGroupsAcl(
          unitId,
          requestBody.employeeId,
          requestBody.groupIds
        )
      } catch (err) {
        return Promise.reject(Failure.fromError(err))
      }
    }
  }, [requestBody, unitId, addDaycareAcl, updateGroupsAcl])

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
          <IconWrapper>
            <FontAwesomeIcon icon={faPlus} />
          </IconWrapper>
          <H1 noMargin>{i18n.unit.accessControl.addDaycareAclModal.title}</H1>
        </Centered>
        <div>
          <FieldLabel>
            {i18n.unit.accessControl.addDaycareAclModal.employees}
          </FieldLabel>
          <Combobox
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

          {errors?.selectedEmployee && (
            <InputFieldUnderRow className={classNames('warning')}>
              <span>{i18n.validationErrors[errors.selectedEmployee]}</span>
              <StatusIcon status="warning" />
            </InputFieldUnderRow>
          )}
        </div>
        <div>
          <FieldLabel>
            {i18n.unit.accessControl.addDaycareAclModal.groups}
          </FieldLabel>
          <MultiSelect
            data-qa="add-person-group-select"
            value={formData.selectedGroups}
            options={groupOptions}
            getOptionId={(item) => item.id}
            getOptionLabel={(item) => item.name}
            onChange={(values) =>
              setFormData({ ...formData, selectedGroups: values })
            }
            placeholder={`${i18n.common.select}...`}
          />
        </div>

        <Gap size="xs" />
        <FixedSpaceRow justifyContent="space-between">
          <InlineButton
            text={i18n.common.cancel}
            data-qa="add-person-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            text={i18n.unit.staffAttendance.addPerson}
            data-qa="add-person-save-btn"
            onClick={submit}
            onSuccess={onSuccess}
            disabled={!formData.selectedEmployee}
          />
        </FixedSpaceRow>
      </Content>
    </PlainModal>
  )
})

const Content = styled.div`
  padding: ${defaultMargins.XL};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`
const Centered = styled(FixedSpaceColumn)`
  align-self: center;
  text-align: center;
  gap: ${defaultMargins.L};
`
const IconWrapper = styled.div`
  align-self: center;
  height: 64px !important;
  width: 64px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 0;

  font-size: 40px;
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-weight: ${fontWeights.normal};
  background: ${(p) => p.theme.colors.main.m2};
  border-radius: 100%;
`
const FieldLabel = styled(Label)`
  margin-bottom: 8px;
`
