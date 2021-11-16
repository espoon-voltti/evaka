// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import { Link } from 'react-router-dom'

import { faChild } from 'lib-icons'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading } from 'lib-common/api'
import { useContext } from 'react'
import { PersonContext } from '../../state/person'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import { getPersonDependants } from '../../api/person'
import { PersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { formatName } from '../../utils'
import { NameTd } from '../PersonProfile'
import { getAge } from 'lib-common/utils/local-date'
import { UUID } from 'lib-common/types'

interface Props {
  id: UUID
  open: boolean
}

const PersonDependants = React.memo(function PersonDependants({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { dependants, setDependants } = useContext(PersonContext)
  useEffect(() => {
    setDependants(Loading.of())
    void getPersonDependants(id).then((response) => {
      setDependants(response)
    })
  }, [id, setDependants])

  const printableAddresses = (address: PersonWithChildrenDTO['address']) =>
    address.streetAddress
      ? `${address.streetAddress}, ${address.postalCode}, ${address.city}`
      : ''

  const renderDependants = () =>
    dependants.isSuccess
      ? _.orderBy(dependants.value, ['dateOfBirth'], ['asc']).map(
          (dependant: PersonWithChildrenDTO) => {
            return (
              <Tr key={`${dependant.id}`} data-qa="table-dependant-row">
                <NameTd data-qa="dependant-name">
                  <Link to={`/child-information/${dependant.id}`}>
                    {formatName(dependant.firstName, dependant.lastName, i18n)}
                  </Link>
                </NameTd>
                <Td data-qa="dependant-ssn">
                  {dependant.socialSecurityNumber}
                </Td>
                <Td data-qa="dependant-age">{getAge(dependant.dateOfBirth)}</Td>
                <Td data-qa="dependant-street-address">
                  {printableAddresses(dependant.address)}
                </Td>
              </Tr>
            )
          }
        )
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.dependants}
        startCollapsed={!open}
        data-qa="person-dependants-collapsible"
      >
        <Table data-qa="table-of-dependants">
          <Thead>
            <Tr>
              <Th>{i18n.personProfile.name}</Th>
              <Th>{i18n.personProfile.ssn}</Th>
              <Th>{i18n.personProfile.age}</Th>
              <Th>{i18n.personProfile.streetAddress}</Th>
            </Tr>
          </Thead>
          <Tbody>{renderDependants()}</Tbody>
        </Table>
        {dependants.isLoading && <Loader />}
        {dependants.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonDependants
