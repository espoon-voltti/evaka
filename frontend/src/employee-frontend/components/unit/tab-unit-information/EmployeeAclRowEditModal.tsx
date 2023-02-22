// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { DaycareGroupSummary } from 'employee-frontend/api/unit'
import { Failure, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights, H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import { FormattedRow } from './UnitAccessControl'

type EmployeeRowEditFormState = {
  selectedGroups: DaycareGroupSummary[]
}

type EmployeeRowEditModalProps = {
  onClose: () => void
  onSuccess: () => void
  updatesGroupAcl: (
    unitId: UUID,
    employeeId: UUID,
    groupIds: UUID[]
  ) => Promise<Result<unknown>>
  unitId: UUID
  groups: Record<string, DaycareGroupSummary>
  employeeRow?: FormattedRow
}

export default React.memo(function EmployeeAclRowEditModal({
  onClose,
  onSuccess,
  updatesGroupAcl,
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
    return groupOptions.filter((option) => groupIds.includes(option.id))
  }

  const [formData, setFormData] = useState<EmployeeRowEditFormState>({
    selectedGroups: initSelectedGroups(employeeRow?.groupIds ?? [])
  })

  const submit = useCallback(() => {
    if (!employeeRow) {
      return Promise.reject(Failure.of({ message: 'validation error' }))
    } else {
      return updatesGroupAcl(
        unitId,
        employeeRow.id,
        formData.selectedGroups.map((item) => item.id)
      )
    }
  }, [formData, unitId, employeeRow, updatesGroupAcl])

  return (
    <PlainModal margin="auto" data-qa="staff-edit-person-modal">
      <Content>
        <Centered>
          <IconWrapper>
            <FontAwesomeIcon icon={faPlus} />
          </IconWrapper>
          <H1 noMargin>{i18n.unit.accessControl.editEmployeeRowModal.title}</H1>

          <H2 noMargin>{employeeRow?.name ?? ''}</H2>
        </Centered>
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

        <Gap size="L" />
        <FixedSpaceRow justifyContent="space-between">
          <InlineButton
            text={i18n.common.cancel}
            data-qa="edit-person-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            text={i18n.common.save}
            data-qa="edit-person-save-btn"
            onClick={submit}
            onSuccess={onSuccess}
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
