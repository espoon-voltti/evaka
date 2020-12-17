// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faSearch } from '@evaka/lib-icons'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { getAge } from '@evaka/lib-common/src/utils/local-date'

import { formatName } from '~/utils'
import { triggerDvvBatch } from '~api/hidden'
import { Gap } from '@evaka/lib-components/src/white-space'
import AddButton from '~components/shared/atoms/buttons/AddButton'
import Button from 'components/shared/atoms/buttons/Button'
import InputField from 'components/shared/atoms/form/InputField'
import { Container, ContentArea } from 'components/shared/layout/Container'
import {
  Table,
  Tr,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '~components/shared/layout/Table'
import Loader from './shared/atoms/Loader'
import AddVTJPersonModal from '~components/person-search/AddVTJPersonModal'
import CreatePersonModal from '~components/person-search/CreatePersonModal'
import { CHILD_AGE } from '~constants.ts'
import { CustomersContext } from '~state/customers'
import { useTranslation } from '~state/i18n'
import { RequireRole } from '~utils/roles'

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

const HiddenButton = styled(Button)`
  display: none;
`

function Search() {
  const { i18n } = useTranslation()
  const {
    searchTerm,
    setSearchTerm,
    useCustomerSearch,
    customers,
    sortColumn,
    sortDirection,
    sortToggle
  } = useContext(CustomersContext)
  const [showAddPersonFromVTJModal, setShowAddPersonFromVTJModal] = useState(
    false
  )
  const [showCreatePersonModal, setShowCreatePersonModal] = useState(false)

  useCustomerSearch()

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Gap size={'XXL'} />
        <TopBar>
          <Wrapper tabIndex={-1}>
            <InputField
              placeholder={i18n.personSearch.inputPlaceholder}
              value={searchTerm}
              onChange={(value) => {
                setSearchTerm(value)
              }}
              dataQa="search-input"
              icon={faSearch}
            />
          </Wrapper>
          <RequireRole oneOf={['SERVICE_WORKER', 'FINANCE_ADMIN']}>
            <ButtonsContainer>
              <AddButton
                text={i18n.personSearch.addPersonFromVTJ.title}
                onClick={() => setShowAddPersonFromVTJModal(true)}
                dataQa="add-vtj-person-button"
              />
              <Gap size="s" horizontal />
              <AddButton
                text={i18n.personSearch.createNewPerson.title}
                onClick={() => setShowCreatePersonModal(true)}
                dataQa="create-person-button"
              />
              <RequireRole oneOf={['ADMIN']}>
                <HiddenButton
                  onClick={() => triggerDvvBatch()}
                  text="DVV-mutp -päivitys"
                />
              </RequireRole>
            </ButtonsContainer>
          </RequireRole>
        </TopBar>
        <Gap size={'XL'} />

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
                            getAge(person.dateOfBirth) >= CHILD_AGE
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
                      <Td></Td>
                      <Td></Td>
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
        <Gap size={'XXL'} />
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
}

export default Search
