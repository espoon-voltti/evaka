// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { globalRoles } from 'lib-common/api-types/employee-auth'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Pagination from 'lib-components/Pagination'
import Title from 'lib-components/atoms/Title'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { faSearch } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { EmployeeList } from './EmployeeList'
import { searchEmployeesQuery } from './queries'

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

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.titles.employees}</Title>
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
      </ContentArea>
    </Container>
  )
})
