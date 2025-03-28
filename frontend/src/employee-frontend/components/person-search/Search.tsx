// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import Loader from 'lib-components/atoms/Loader'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  Table,
  Tr,
  Td,
  Thead,
  Tbody,
  SortableTh
} from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import { faSearch } from 'lib-icons'

import { PROFILE_AGE_THRESHOLD_DEFAULT } from '../../constants'
import { searchPersonQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { RequireRole } from '../../utils/roles'

import AddVTJPersonModal from './AddVTJPersonModal'
import CreatePersonModal from './CreatePersonModal'
import { CustomersContext } from './customers'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`

const Wrapper = styled.div`
  position: relative;
  padding-bottom: 50px;
  width: 500px;
  margin-right: 20px;
`

const ButtonsContainer = styled.div`
  display: flex;
  flex-direction: row;
`

export default React.memo(function Search() {
  const { i18n } = useTranslation()
  const {
    searchTerm,
    setSearchTerm,
    sortColumn,
    sortDirection,
    sortToggle,
    personSearchParams
  } = useContext(CustomersContext)
  const customers = useQueryResult(searchPersonQuery(personSearchParams))

  const [showAddPersonFromVTJModal, setShowAddPersonFromVTJModal] =
    useState(false)
  const [showCreatePersonModal, setShowCreatePersonModal] = useState(false)

  return (
    <Container>
      <ContentArea opaque>
        <Gap size="xs" />
        <TopBar>
          <Wrapper tabIndex={-1}>
            <InputField
              placeholder={i18n.personSearch.inputPlaceholder}
              value={searchTerm}
              onChange={(value) => {
                setSearchTerm(value)
              }}
              width="L"
              data-qa="search-input"
              icon={faSearch}
            />
          </Wrapper>
          <RequireRole oneOf={['SERVICE_WORKER', 'FINANCE_ADMIN']}>
            <ButtonsContainer>
              <AddButton
                text={i18n.personSearch.addPersonFromVTJ.title}
                onClick={() => setShowAddPersonFromVTJModal(true)}
                data-qa="add-vtj-person-button"
              />
              <Gap size="s" horizontal />
              <AddButton
                text={i18n.personSearch.createNewPerson.title}
                onClick={() => setShowCreatePersonModal(true)}
                data-qa="create-person-button"
              />
            </ButtonsContainer>
          </RequireRole>
        </TopBar>
        <Gap size="XL" />

        {/* TODO: move this to a component */}
        <div className="table-of-units">
          <Table data-qa="table-of-units">
            <Thead>
              <Tr>
                <SortableTh
                  sorted={
                    sortColumn === 'last_name,first_name'
                      ? sortDirection
                      : undefined
                  }
                  onClick={sortToggle('last_name,first_name')}
                >
                  {i18n.units.name}
                </SortableTh>
                <SortableTh
                  sorted={
                    sortColumn === 'date_of_birth' ? sortDirection : undefined
                  }
                  onClick={sortToggle('date_of_birth')}
                >
                  {i18n.personSearch.age}
                </SortableTh>
                <SortableTh
                  sorted={
                    sortColumn === 'street_address' ? sortDirection : undefined
                  }
                  onClick={sortToggle('street_address')}
                >
                  {i18n.personSearch.address}
                </SortableTh>
                <SortableTh
                  sorted={
                    sortColumn === 'social_security_number'
                      ? sortDirection
                      : undefined
                  }
                  onClick={sortToggle('social_security_number')}
                >
                  {i18n.personSearch.socialSecurityNumber}
                </SortableTh>
              </Tr>
            </Thead>
            <Tbody>
              {customers.isSuccess && (
                <>
                  {customers.value.map((person) => (
                    <Tr key={person.id} data-qa="person-row">
                      <Td align="left">
                        <Link
                          to={
                            getAge(person.dateOfBirth) >=
                            PROFILE_AGE_THRESHOLD_DEFAULT
                              ? `/profile/${person.id}`
                              : `/child-information/${person.id}`
                          }
                        >
                          {formatName(
                            person.firstName,
                            person.lastName,
                            i18n,
                            true
                          )}
                        </Link>
                      </Td>
                      <Td align="left">{getAge(person.dateOfBirth)}</Td>
                      <Td align="left">{person.streetAddress}</Td>
                      <Td align="left">{person.socialSecurityNumber}</Td>
                    </Tr>
                  ))}
                  {customers.value.length > 99 && (
                    <Tr>
                      <Td>{i18n.personSearch.maxResultsFound}</Td>
                      <Td />
                      <Td />
                    </Tr>
                  )}
                </>
              )}
              {customers.isLoading && (
                <Tr>
                  <Td colSpan={4}>
                    <Loader />
                  </Td>
                </Tr>
              )}
              {customers.isFailure && (
                <Tr>
                  <Td colSpan={4}>{i18n.common.loadingFailed}</Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
        <Gap size="XXL" />
      </ContentArea>
      {showAddPersonFromVTJModal ? (
        <AddVTJPersonModal
          closeModal={() => setShowAddPersonFromVTJModal(false)}
        />
      ) : null}
      {showCreatePersonModal ? (
        <CreatePersonModal closeModal={() => setShowCreatePersonModal(false)} />
      ) : null}
    </Container>
  )
})
