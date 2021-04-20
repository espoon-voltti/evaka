import { sortBy } from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { Result } from '../../../lib-common/api'
import Loader from '../../../lib-components/atoms/Loader'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '../../../lib-components/layout/Table'
import Pagination from '../../../lib-components/Pagination'
import { useTranslation } from '../../state/i18n'
import { Employee } from '../../types/users'

const PaginationContainer = styled(Pagination)`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
`

interface Props {
  users?: Result<Employee[]>
  total?: number
  pages?: number
  currentPage: number
  setPage: (page: number) => void
}

export function UsersList({ users, pages, currentPage, setPage }: Props) {
  const { i18n } = useTranslation()

  const userRows =
    users?.isSuccess &&
    users.value.map((user) => (
      <Tr key={user.id}>
        <Td>
          {user.lastName} {user.firstName}
        </Td>
        <Td>
          {[
            ...sortBy(user.globalRoles.map((r) => i18n.roles.adRoles[r])),
            ...sortBy(user.daycareRoles, 'daycareName').map((r) => (
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
    ))

  return (
    <>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.users.name}</Th>
            <Th>{i18n.users.rights}</Th>
          </Tr>
        </Thead>
        <Tbody>{userRows}</Tbody>
      </Table>
      {users?.isSuccess && (
        <PaginationContainer
          pages={pages}
          currentPage={currentPage}
          setPage={setPage}
          label={i18n.common.page}
        />
      )}
      {users?.isLoading && <Loader />}
      {users?.isFailure && <div>{i18n.common.error.unknown}</div>}
    </>
  )
}
