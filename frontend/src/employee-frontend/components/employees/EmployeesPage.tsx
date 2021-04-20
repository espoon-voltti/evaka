import { Paged, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useEffect, useState } from 'react'
import { searchEmployees } from '../../api/employees'
import { useTranslation } from '../../state/i18n'
import { EmployeeUser as Employee } from '../../types/employee'
import { EmployeeList } from './EmployeeList'

const PAGE_SIZE = 50

export default function EmployeesPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState<number>(1)
  const [totalEmployees, setTotalEmployees] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [employees, setEmployees] = useState<Result<Employee[]>>()

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

  const reloadEmployees = useRestApi(searchEmployees, setEmployeesResult)

  const loadEmployees = useCallback(() => {
    reloadEmployees(page, PAGE_SIZE)
  }, [page, PAGE_SIZE])

  useEffect(() => {
    loadEmployees()
  }, [loadEmployees])

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title>{i18n.employees.title}</Title>
        <Gap size="L" />
        <EmployeeList
          employees={employees}
          total={totalEmployees}
          currentPage={page}
          pages={totalPages}
          setPage={setPage}
        />
      </ContentArea>
    </Container>
  )
}
