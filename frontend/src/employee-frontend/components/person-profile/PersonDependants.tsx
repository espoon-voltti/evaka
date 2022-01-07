// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChild } from 'lib-icons'
import _ from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import { PersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { getAge } from 'lib-common/utils/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { getPersonDependants } from '../../api/person'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonDependants({ id, open }: Props) {
  const { i18n } = useTranslation()
  const [dependants] = useApiState(() => getPersonDependants(id), [id])

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.dependants}
        startCollapsed={!open}
        data-qa="person-dependants-collapsible"
      >
        {renderResult(dependants, (dependants) => (
          <Table data-qa="table-of-dependants">
            <Thead>
              <Tr>
                <Th>{i18n.personProfile.name}</Th>
                <Th>{i18n.personProfile.ssn}</Th>
                <Th>{i18n.personProfile.age}</Th>
                <Th>{i18n.personProfile.streetAddress}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {_.orderBy(dependants, ['dateOfBirth'], ['asc']).map(
                (dependant: PersonWithChildrenDTO) => {
                  return (
                    <Tr key={`${dependant.id}`} data-qa="table-dependant-row">
                      <NameTd data-qa="dependant-name">
                        <Link to={`/child-information/${dependant.id}`}>
                          {formatName(
                            dependant.firstName,
                            dependant.lastName,
                            i18n,
                            true
                          )}
                        </Link>
                      </NameTd>
                      <Td data-qa="dependant-ssn">
                        {dependant.socialSecurityNumber}
                      </Td>
                      <Td data-qa="dependant-age">
                        {getAge(dependant.dateOfBirth)}
                      </Td>
                      <Td data-qa="dependant-street-address">
                        {printableAddresses(dependant.address)}
                      </Td>
                    </Tr>
                  )
                }
              )}
            </Tbody>
          </Table>
        ))}
      </CollapsibleSection>
    </div>
  )
})

function printableAddresses(address: PersonWithChildrenDTO['address']) {
  return address.streetAddress
    ? `${address.streetAddress}, ${address.postalCode}, ${address.city}`
    : ''
}
