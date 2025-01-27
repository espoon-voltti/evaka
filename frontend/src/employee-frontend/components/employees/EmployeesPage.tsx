// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { globalRoles } from 'lib-common/api-types/employee-auth'
import { string } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import { nonBlank, optionalEmail } from 'lib-common/form/validators'
import { ssn } from 'lib-common/form-validation'
import { EmployeeId, UserRole } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { uri } from 'lib-common/uri'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Pagination from 'lib-components/Pagination'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField, { InputFieldF } from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { faPlus } from 'lib-icons'
import { faSearch } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { RequirePermittedGlobalAction } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { EmployeeList } from './EmployeeList'
import { createSsnEmployeeMutation, searchEmployeesQuery } from './queries'

const ResultCount = styled.div`
  text-wrap: nowrap;
`

export default React.memo(function EmployeesPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState<number>(1)
  const [searchTerm, setSearchTerm] = useState<string>('')
  const [hideDeactivated, setHideDeactivated] = useState<boolean>(false)
  const [selectedGlobalRoles, setSelectedGlobalRoles] = useState<UserRole[]>([])

  const debouncedSearchTerm = useDebounce(searchTerm, 500)

  const [createWizardOpen, { on: showCreateWizard, off: hideCreateWizard }] =
    useBoolean(false)

  const employees = useQueryResult(
    searchEmployeesQuery({
      body: {
        page,
        searchTerm: debouncedSearchTerm,
        hideDeactivated,
        globalRoles: selectedGlobalRoles
      }
    })
  )

  const navigate = useNavigate()

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.titles.employees}</Title>
        <RequirePermittedGlobalAction oneOf={['CREATE_EMPLOYEE']}>
          <CreateButton
            primary
            data-qa="create-new-ssn-employee"
            text={i18n.employees.createNewSsnEmployee}
            onClick={showCreateWizard}
          />
        </RequirePermittedGlobalAction>
        <FixedSpaceColumn spacing="m">
          <InputField
            data-qa="employee-name-filter"
            value={searchTerm}
            placeholder={i18n.employees.findByName}
            onChange={(s) => {
              setSearchTerm(s)
              setPage(1)
            }}
            icon={faSearch}
            width="L"
          />
          <Checkbox
            label={i18n.employees.hideDeactivated}
            checked={hideDeactivated}
            onChange={(enabled) => {
              setHideDeactivated(enabled)
              setPage(1)
            }}
            data-qa="hide-deactivated-checkbox"
          />
          <FixedSpaceRow
            justifyContent="space-between"
            alignItems="center"
            spacing="XL"
          >
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.employees.editor.globalRoles}</Label>
              <FixedSpaceFlexWrap horizontalSpacing="m">
                {globalRoles.map((role) => (
                  <Checkbox
                    key={role}
                    label={i18n.roles.adRoles[role]}
                    checked={selectedGlobalRoles.includes(role)}
                    onChange={(checked) =>
                      setSelectedGlobalRoles(
                        checked
                          ? [...selectedGlobalRoles, role]
                          : selectedGlobalRoles.filter((r) => r !== role)
                      )
                    }
                  />
                ))}
              </FixedSpaceFlexWrap>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xxs" alignItems="flex-end">
              <Pagination
                pages={employees.map((res) => res.pages).getOrElse(1)}
                currentPage={page}
                setPage={setPage}
                label={i18n.common.page}
              />
              <ResultCount>
                {employees.isSuccess
                  ? i18n.common.resultCount(employees.value.total)
                  : null}
              </ResultCount>
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </FixedSpaceColumn>

        {renderResult(employees, (employees) => (
          <EmployeeList employees={employees.data} />
        ))}
        {employees?.isSuccess && (
          <FlexRow justifyContent="flex-end">
            <Pagination
              pages={employees.value.pages}
              currentPage={page}
              setPage={setPage}
              label={i18n.common.page}
            />
          </FlexRow>
        )}
        {createWizardOpen && (
          <CreateModal
            onSuccess={(id) => navigate(uri`/employees/${id}`.value)}
            onClose={hideCreateWizard}
          />
        )}
      </ContentArea>
    </Container>
  )
})

const createForm = object({
  ssn: validated(required(string()), ssn),
  firstName: validated(required(string()), nonBlank),
  lastName: validated(required(string()), nonBlank),
  email: validated(string(), optionalEmail)
})

const CreateModal = React.memo(function CreateModal({
  onSuccess,
  onClose
}: {
  onSuccess: (id: EmployeeId) => void
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const t = i18n.employees.newSsnEmployeeModal

  const form = useForm(
    createForm,
    () => ({ ssn: '', firstName: '', lastName: '', email: '' }),
    i18n.validationErrors
  )
  const ssn = useFormField(form, 'ssn')
  const firstName = useFormField(form, 'firstName')
  const lastName = useFormField(form, 'lastName')
  const email = useFormField(form, 'email')

  return (
    <MutateFormModal
      icon={faPlus}
      data-qa="create-ssn-employee-wizard"
      title={t.title}
      resolveMutation={createSsnEmployeeMutation}
      resolveAction={() => ({
        body: form.value()
      })}
      resolveLabel={t.createButton}
      resolveDisabled={!form.isValid()}
      onSuccess={({ id }) => onSuccess(id)}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn spacing="s">
        <div>
          <Label htmlFor="new-employee-ssn">
            {i18n.common.form.socialSecurityNumber}
          </Label>
          <InputFieldF
            id="new-employee-ssn"
            data-qa="new-employee-ssn"
            bind={ssn}
            hideErrorsBeforeTouched={true}
          />
        </div>
        <div>
          <Label htmlFor="new-employee-first-name">
            {i18n.common.form.firstName}
          </Label>
          <InputFieldF
            id="new-employee-first-name"
            data-qa="new-employee-first-name"
            bind={firstName}
            hideErrorsBeforeTouched={true}
          />
        </div>
        <div>
          <Label htmlFor="new-employee-last-name">
            {i18n.common.form.lastName}
          </Label>
          <InputFieldF
            id="new-employee-last-name"
            data-qa="new-employee-last-name"
            bind={lastName}
            hideErrorsBeforeTouched={true}
          />
        </div>
        <div>
          <Label htmlFor="new-employee-email">{i18n.common.form.email}</Label>
          <InputFieldF
            id="new-employee-email"
            data-qa="new-employee-email"
            bind={email}
          />
        </div>
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const CreateButton = styled(Button)`
  display: block;
  float: right;
`
