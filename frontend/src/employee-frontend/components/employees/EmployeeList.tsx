import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Pagination from 'lib-components/Pagination'
import { sortBy } from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { EmployeeUser as Employee } from '../../types/employee'

const Name = styled.div`
  font-weight: 600;
`

const Email = styled.div`
  font-weight: 600;
  font-size: 14px;
`

interface Props {
  employees?: Result<Employee[]>
  total?: number
  pages?: number
  currentPage: number
  setPage: (page: number) => void
}

export function EmployeeList({
  employees,
  pages,
  currentPage,
  setPage
}: Props) {
  const { i18n } = useTranslation()

  const rows =
    employees?.isSuccess &&
    employees.value.map(
      ({ daycareRoles, email, firstName, globalRoles, id, lastName }) => (
        <Tr key={id}>
          <Td>
            <Name>
              {lastName} {firstName}
            </Name>
            <Email>{email}</Email>
          </Td>
          <Td>
            {[
              ...sortBy(globalRoles.map((r) => i18n.roles.adRoles[r])),
              ...sortBy(daycareRoles, 'daycareName').map((r) => (
                <>
                  <Link to={`/units/${r.daycareId}`}>{r.daycareName}</Link> (
                  {i18n.roles.adRoles[r.role]?.toLowerCase()})
                </>
              ))
            ].map((role, i) => (
              <div key={i}>{role}</div>
            ))}
          </Td>
        </Tr>
      )
    )

  return (
    <>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.employees.name}</Th>
            <Th>{i18n.employees.rights}</Th>
          </Tr>
        </Thead>
        <Tbody>{rows}</Tbody>
      </Table>
      {employees?.isSuccess && (
        <Pagination
          pages={pages}
          currentPage={currentPage}
          setPage={setPage}
          label={i18n.common.page}
        />
      )}
      {employees?.isLoading && <Loader />}
      {employees?.isFailure && <div>{i18n.common.error.unknown}</div>}
    </>
  )
}
