// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getAge } from '@evaka/lib-common/src/utils/local-date'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSearch } from 'icon-set'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { formatName } from '~/utils'
import { isFailure, isLoading, isSuccess } from '~api'
import { triggerFamilyBatch, triggerVtjBatch } from '~api/hidden'
import { Gap } from '~components/shared/layout/white-space'
import AddButton from '~components/shared/atoms/buttons/AddButton'
import {
  Button,
  Container,
  ContentArea,
  Input,
  Loader,
  Table
} from '~components/shared/alpha'
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

const SearchButton = styled(Button)`
  position: absolute;
  top: 0;
  right: -20px;

  .loader-spinner {
    min-width: 20px;
    min-height: 20px;
    margin: 1em;
  }
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

  const isSortedBy = (column: string) =>
    sortColumn === column ? sortDirection : undefined

  return (
    <Container>
      <ContentArea opaque={true}>
        <TopBar>
          <Wrapper tabIndex={-1}>
            <Input
              placeholder={i18n.personSearch.inputPlaceholder}
              value={searchTerm}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                setSearchTerm(event.target.value)
              }}
              dataQa="search-input"
            />
            <SearchButton plain disabled={searchTerm.length < 2}>
              <FontAwesomeIcon icon={faSearch} size="lg" />
            </SearchButton>
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
                <HiddenButton onClick={() => triggerFamilyBatch()}>
                  Luo perheet
                </HiddenButton>
                <HiddenButton onClick={() => triggerVtjBatch()}>
                  VTJ-päivitys
                </HiddenButton>
              </RequireRole>
            </ButtonsContainer>
          </RequireRole>
        </TopBar>

        {/* TODO: move this to a component */}
        <div className="table-of-units">
          <Table.Table dataQa="table-of-units">
            <Table.Head>
              <Table.Row>
                <Table.HeadButton
                  sortable
                  sorted={isSortedBy('last_name,first_name')}
                  onClick={sortToggle('last_name,first_name')}
                >
                  {i18n.units.name}
                </Table.HeadButton>
                <Table.HeadButton
                  sortable
                  sorted={isSortedBy('date_of_birth')}
                  onClick={sortToggle('date_of_birth')}
                >
                  {i18n.personSearch.age}
                </Table.HeadButton>
                <Table.HeadButton
                  sortable
                  sorted={isSortedBy('street_address')}
                  onClick={sortToggle('street_address')}
                >
                  {i18n.personSearch.address}
                </Table.HeadButton>
                <Table.HeadButton
                  sortable
                  sorted={isSortedBy('social_security_number')}
                  onClick={sortToggle('social_security_number')}
                >
                  {i18n.personSearch.socialSecurityNumber}
                </Table.HeadButton>
              </Table.Row>
            </Table.Head>
            <Table.Body>
              {isSuccess(customers) && (
                <>
                  {customers.data.map((person) => (
                    <Table.Row key={person.id} dataQa="person-row">
                      <Table.Td align="left">
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
                      </Table.Td>
                      <Table.Td align="left">
                        {getAge(person.dateOfBirth)}
                      </Table.Td>
                      <Table.Td align="left">{person.streetAddress}</Table.Td>
                      <Table.Td align="left">
                        {person.socialSecurityNumber}
                      </Table.Td>
                    </Table.Row>
                  ))}
                  {customers.data.length > 99 && (
                    <Table.Row>
                      <Table.Td>{i18n.personSearch.maxResultsFound}</Table.Td>
                      <Table.Td></Table.Td>
                      <Table.Td></Table.Td>
                    </Table.Row>
                  )}
                </>
              )}
              {isLoading(customers) && (
                <Table.Row>
                  <Table.Td colSpan={4}>
                    <Loader />
                  </Table.Td>
                </Table.Row>
              )}
              {isFailure(customers) && (
                <Table.Row>
                  <Table.Td colSpan={4}>{i18n.common.loadingFailed}</Table.Td>
                </Table.Row>
              )}
            </Table.Body>
          </Table.Table>
        </div>
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
