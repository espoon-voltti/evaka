// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'
import { Link } from 'react-router'

import { PersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'

import { personDependantsQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import { NameTd } from './common'

interface Props {
  id: PersonId
  open: boolean
}

export default React.memo(function PersonDependants({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const dependants = useQueryResult(personDependantsQuery({ personId: id }))

  return (
    <div>
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.dependants}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
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
              {orderBy(dependants, ['dateOfBirth'], ['asc']).map(
                (dependant: PersonWithChildrenDTO) => (
                  <Tr
                    key={dependant.id}
                    data-qa={`table-dependant-row-${dependant.id}`}
                  >
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
              )}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})

function printableAddresses(address: PersonWithChildrenDTO['address']) {
  return address.streetAddress
    ? `${address.streetAddress}, ${address.postalCode}, ${address.postOffice}`
    : ''
}
