// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

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
import { PersonName } from 'lib-components/molecules/PersonNames'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faSearch } from 'lib-icons'

import { PROFILE_AGE_THRESHOLD_DEFAULT } from '../../constants'
import { searchPersonQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { hasGlobalAction } from '../../utils/roles'

import AddVTJPersonModal from './AddVTJPersonModal'
import CreatePersonModal from './CreatePersonModal'
import { CustomersContext } from './customers'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: ${defaultMargins.L};
`

const Wrapper = styled.div`
  position: relative;
  padding-bottom: 50px;
  width: 500px;
`

const ButtonsContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.xs} ${defaultMargins.s};
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
  const { user } = useContext(UserContext)
  const customers = useQueryResult(searchPersonQuery(personSearchParams))

  const canCreatePersonFromVtj = hasGlobalAction(user, 'CREATE_PERSON_FROM_VTJ')
  const canCreatePerson = hasGlobalAction(user, 'CREATE_PERSON')
  const canSearchByAddress = hasGlobalAction(user, 'SEARCH_PEOPLE_BY_ADDRESS')
  const columnCount = canSearchByAddress ? 3 : 2

  const [showAddPersonFromVTJModal, setShowAddPersonFromVTJModal] =
    useState(false)
  const [showCreatePersonModal, setShowCreatePersonModal] = useState(false)

  return (
    <Container>
      <ContentArea $opaque>
        <Gap $size="xs" />
        <TopBar>
          <Wrapper tabIndex={-1}>
            <InputField
              placeholder={
                canSearchByAddress
                  ? i18n.personSearch.inputPlaceholder
                  : i18n.personSearch.inputPlaceholderWithoutAddress
              }
              value={searchTerm}
              onChange={(value) => {
                setSearchTerm(value)
              }}
              width="L"
              data-qa="search-input"
              icon={faSearch}
            />
          </Wrapper>
          {(canCreatePersonFromVtj || canCreatePerson) && (
            <ButtonsContainer>
              {canCreatePersonFromVtj && (
                <AddButton
                  text={i18n.personSearch.addPersonFromVTJ.title}
                  onClick={() => setShowAddPersonFromVTJModal(true)}
                  data-qa="add-vtj-person-button"
                />
              )}
              {canCreatePerson && (
                <AddButton
                  text={i18n.personSearch.createNewPerson.title}
                  onClick={() => setShowCreatePersonModal(true)}
                  data-qa="create-person-button"
                />
              )}
            </ButtonsContainer>
          )}
        </TopBar>
        <Gap $size="XL" />

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
                {canSearchByAddress && (
                  <SortableTh
                    sorted={
                      sortColumn === 'street_address'
                        ? sortDirection
                        : undefined
                    }
                    onClick={sortToggle('street_address')}
                    data-qa="address-column"
                  >
                    {i18n.personSearch.address}
                  </SortableTh>
                )}
              </Tr>
            </Thead>
            <Tbody>
              {customers.isSuccess && (
                <>
                  {customers.value.map((person) => (
                    <Tr key={person.id} data-qa="person-row">
                      <Td $align="left">
                        <Link
                          to={
                            getAge(person.dateOfBirth) >=
                            PROFILE_AGE_THRESHOLD_DEFAULT
                              ? `/profile/${person.id}`
                              : `/child-information/${person.id}`
                          }
                        >
                          <PersonName person={person} format="Last First" />
                        </Link>
                      </Td>
                      <Td $align="left">{getAge(person.dateOfBirth)}</Td>
                      {canSearchByAddress && (
                        <Td $align="left" data-qa="person-street-address">
                          {person.streetAddress}
                        </Td>
                      )}
                    </Tr>
                  ))}
                  {customers.value.length > 99 && (
                    <Tr>
                      <Td colSpan={columnCount}>
                        {i18n.personSearch.maxResultsFound}
                      </Td>
                    </Tr>
                  )}
                </>
              )}
              {customers.isLoading && (
                <Tr>
                  <Td colSpan={columnCount}>
                    <Loader />
                  </Td>
                </Tr>
              )}
              {customers.isFailure && (
                <Tr>
                  <Td colSpan={columnCount}>{i18n.common.loadingFailed}</Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
        <Gap $size="XXL" />
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
