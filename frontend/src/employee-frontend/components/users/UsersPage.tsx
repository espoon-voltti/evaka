import { Paged, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useEffect, useState } from 'react'
import { getUsers } from '../../api/users'
import { useTranslation } from '../../state/i18n'
import { Employee } from '../../types/users'
import { UsersList } from './UsersList'

const pageSize = 50

export default React.memo(function UsersPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState<number>(1)
  const [totalUsers, setTotalUsers] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [users, setUsers] = useState<Result<Employee[]>>()

  const setUsersResult = useCallback(
    (result: Result<Paged<Employee>>) => {
      setUsers(result.map((r) => r.data))
      if (result.isSuccess) {
        setTotalUsers(result.value.total)
        setTotalPages(result.value.pages)
      }
    },
    [page]
  )

  const reloadUsers = useRestApi(getUsers, setUsersResult)

  const loadUsers = useCallback(() => {
    reloadUsers(page, pageSize)
  }, [page, pageSize])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title>{i18n.users.title}</Title>
        <section>TODO Add filters here</section>
        <Gap size="L" />
        <UsersList
          users={users}
          total={totalUsers}
          currentPage={page}
          pages={totalPages}
          setPage={setPage}
        />
      </ContentArea>
    </Container>
  )
})
