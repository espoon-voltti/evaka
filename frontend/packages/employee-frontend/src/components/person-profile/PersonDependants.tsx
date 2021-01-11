// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import { Link } from 'react-router-dom'

import { faChild } from '@evaka/lib-icons'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { Loading } from '@evaka/lib-common/src/api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import CollapsibleSection from '@evaka/lib-components/src/molecules/CollapsibleSection'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/src/layout/Table'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { getPersonDependants } from '~api/person'
import { DependantAddress, PersonWithChildren } from '~/types/person'
import { formatName } from '~utils'
import { NameTd } from '~components/PersonProfile'
import { getAge } from '@evaka/lib-common/src/utils/local-date'

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

  const printableAddresses = (addresses: DependantAddress[] | null) =>
    addresses && addresses.length > 0
      ? [
          addresses[0].streetAddress,
          addresses[0].postalCode,
          addresses[0].city
        ].join(', ')
      : ''

  const renderDependants = () =>
    dependants.isSuccess
      ? _.orderBy(dependants.value, ['dateOfBirth'], ['asc']).map(
          (dependant: PersonWithChildren) => {
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
                  {printableAddresses(dependant.addresses)}
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
        dataQa="person-dependants-collapsible"
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
