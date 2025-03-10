// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import {
  globalRoles,
  scopedRoles as unitRoles
} from 'lib-common/api-types/employee-auth'
import { string } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import { nonBlank, optionalEmail } from 'lib-common/form/validators'
import { ssn } from 'lib-common/form-validation'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { EmployeeId, UserRole } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useQueryResult } from 'lib-common/query'
import { uri } from 'lib-common/uri'
import { useDebounce } from 'lib-common/utils/useDebounce'
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
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { unitProviderTypes } from 'lib-customizations/employee'
import { faPlus, faSearch } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { RequirePermittedGlobalAction } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import ReportDownload from '../reports/ReportDownload'

import { EmployeeList } from './EmployeeList'
import { createSsnEmployeeMutation, searchEmployeesQuery } from './queries'

const ResultCount = styled.div`
  text-wrap: nowrap;
`

export default React.memo(function EmployeesPage() {
  const { i18n } = useTranslation()
  const [searchTerm, setSearchTerm] = useState<string>('')
  const [hideDeactivated, setHideDeactivated] = useState<boolean>(true)
  const [selectedGlobalRoles, setSelectedGlobalRoles] = useState<UserRole[]>([])
  const [selectedUnitRoles, setSelectedUnitRoles] = useState<UserRole[]>([])
  const [selectedUnitProviderTypes, setSelectedUnitProviderTypes] = useState<
    ProviderType[]
  >([])

  const debouncedSearchTerm = useDebounce(searchTerm, 500)

  const [createWizardOpen, { on: showCreateWizard, off: hideCreateWizard }] =
    useBoolean(false)

  const employees = useQueryResult(
    searchEmployeesQuery({
      body: {
        searchTerm: debouncedSearchTerm,
        hideDeactivated,
        globalRoles: selectedGlobalRoles,
        unitRoles: selectedUnitRoles,
        unitProviderTypes: selectedUnitProviderTypes
      }
    })
  )

  const navigate = useNavigate()

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.titles.employees}</Title>
        <FixedSpaceRow justifyContent="space-between">
          <InputField
            data-qa="employee-name-filter"
            value={searchTerm}
            placeholder={i18n.employees.findByName}
            onChange={(s) => {
              setSearchTerm(s)
            }}
            icon={faSearch}
            width="L"
          />
          <RequirePermittedGlobalAction oneOf={['CREATE_EMPLOYEE']}>
            <Button
              primary
              data-qa="create-new-ssn-employee"
              text={i18n.employees.createNewSsnEmployee}
              onClick={showCreateWizard}
            />
          </RequirePermittedGlobalAction>
        </FixedSpaceRow>
        <FixedSpaceColumn spacing="m">
          <Checkbox
            label={i18n.employees.hideDeactivated}
            checked={hideDeactivated}
            onChange={(enabled) => {
              setHideDeactivated(enabled)
            }}
            data-qa="hide-deactivated-checkbox"
          />
          <FixedSpaceRow
            justifyContent="space-between"
            alignItems="flex-start"
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
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.employees.editor.unitRoles.name}</Label>
              <FixedSpaceFlexWrap horizontalSpacing="m">
                {unitRoles.map((role) => (
                  <Checkbox
                    key={role}
                    label={i18n.roles.adRoles[role]}
                    checked={selectedUnitRoles.includes(role)}
                    onChange={(checked) =>
                      setSelectedUnitRoles(
                        checked
                          ? [...selectedUnitRoles, role]
                          : selectedUnitRoles.filter((r) => r !== role)
                      )
                    }
                  />
                ))}
              </FixedSpaceFlexWrap>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.filters.providerType}</Label>
              <FixedSpaceFlexWrap horizontalSpacing="m">
                {unitProviderTypes.map((type) => (
                  <Checkbox
                    key={type}
                    label={i18n.common.providerType[type]}
                    checked={selectedUnitProviderTypes.includes(type)}
                    onChange={(checked) =>
                      setSelectedUnitProviderTypes(
                        checked
                          ? [...selectedUnitProviderTypes, type]
                          : selectedUnitProviderTypes.filter((t) => t !== type)
                      )
                    }
                    disabled={selectedUnitRoles.length === 0}
                  />
                ))}
              </FixedSpaceFlexWrap>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceRow
            alignItems="flex-end"
            justifyContent="flex-end"
            spacing="xxs"
          >
            <FixedSpaceColumn spacing="xxs" alignItems="flex-end">
              <ResultCount>
                {employees.isSuccess
                  ? i18n.common.resultCount(employees.value.length)
                  : null}
              </ResultCount>
              {employees.isSuccess && (
                <ReportDownload
                  data={employees.value}
                  columns={[
                    {
                      label: i18n.employees.name,
                      value: (row) => `${row.lastName} ${row.firstName}`
                    },
                    { label: i18n.employees.email, value: (row) => row.email },
                    {
                      label: i18n.employees.rights,
                      value: (row) =>
                        [
                          ...sortBy(
                            row.globalRoles.map(
                              (role) => i18n.roles.adRoles[role]
                            )
                          ),
                          ...sortBy(
                            row.daycareRoles.map(
                              (role) =>
                                `${role.daycareName} (${i18n.roles.adRoles[role.role].toLowerCase()})`
                            )
                          )
                        ].join(', ')
                    }
                  ]}
                  filename={`Käyttäjät-${HelsinkiDateTime.now().format()}.csv`}
                />
              )}
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </FixedSpaceColumn>

        {renderResult(employees, (employees) => (
          <EmployeeList employees={employees} />
        ))}
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

  const [ssnConflict, setSsnConflict] = useState(false)
  const form = useForm(
    createForm,
    () => ({ ssn: '', firstName: '', lastName: '', email: '' }),
    i18n.validationErrors,
    {
      onUpdate: (prev, next) => {
        if (prev.ssn !== next.ssn) {
          setSsnConflict(false)
        }
        return next
      }
    }
  )
  const ssn = useFormField(form, 'ssn')
  const firstName = useFormField(form, 'firstName')
  const lastName = useFormField(form, 'lastName')
  const email = useFormField(form, 'email')

  const onFailure = useCallback(
    (failure: Failure<unknown>) => setSsnConflict(failure.statusCode === 409),
    [setSsnConflict]
  )

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
      resolveDisabled={!form.isValid() || ssnConflict}
      onSuccess={({ id }) => onSuccess(id)}
      onFailure={onFailure}
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
        {ssnConflict && <AlertBox message={t.ssnConflict} />}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
