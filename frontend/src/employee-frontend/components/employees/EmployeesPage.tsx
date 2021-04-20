import { Paged, Result } from 'lib-common/api'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useRestApi } from 'lib-common/utils/useRestApi'
import InputField from 'lib-components/atoms/form/InputField'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Pagination from 'lib-components/Pagination'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faSearch } from 'lib-icons'
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { searchEmployees } from '../../api/employees'
import { useTranslation } from '../../state/i18n'
import { EmployeeUser as Employee } from '../../types/employee'
import { EmployeeList } from './EmployeeList'

const PAGE_SIZE = 50

const TopBar = styled.section`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.s};
`

const SearchBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`

const PaginationContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
`

export default function EmployeesPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState<number>(1)
  const [totalEmployees, setTotalEmployees] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [employees, setEmployees] = useState<Result<Employee[]>>()
  const [searchTerm, setSearchTerm] = useState<string>('')

  const debouncedSearchTerm = useDebounce(searchTerm, 500)

  const setEmployeesResult = useCallback(
    (result: Result<Paged<Employee>>) => {
      setEmployees(result.map((r) => r.data))
      if (result.isSuccess) {
        setTotalEmployees(result.value.total)
        setTotalPages(result.value.pages)
      }
    },
    [page]
  )

  const loadEmployees = useRestApi(searchEmployees, setEmployeesResult)

  useEffect(() => {
    loadEmployees(page, PAGE_SIZE, debouncedSearchTerm)
  }, [loadEmployees, page, PAGE_SIZE, debouncedSearchTerm])

  useEffect(() => setPage(1), [searchTerm])

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title>{i18n.employees.title}</Title>
        <TopBar>
          <SearchBar>
            <InputField
              dataQa="employee-name-filter"
              value={searchTerm}
              placeholder={i18n.employees.findByName}
              onChange={setSearchTerm}
              icon={faSearch}
              width={'L'}
            />
          </SearchBar>
          <PaginationContainer>
            <div>
              {totalEmployees !== undefined
                ? i18n.common.resultCount(totalEmployees)
                : null}
            </div>
            <Pagination
              pages={totalPages}
              currentPage={page}
              setPage={setPage}
              label={i18n.common.page}
            />
          </PaginationContainer>
        </TopBar>
        <EmployeeList employees={employees} />
        {employees?.isSuccess && (
          <PaginationContainer>
            <Pagination
              pages={totalPages}
              currentPage={page}
              setPage={setPage}
              label={i18n.common.page}
            />
          </PaginationContainer>
        )}
      </ContentArea>
    </Container>
  )
}
