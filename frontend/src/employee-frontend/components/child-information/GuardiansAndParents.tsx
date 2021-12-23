// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import _ from 'lodash'
import { Link } from 'react-router-dom'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { formatName } from '../../utils'
import { NameTd } from '../PersonProfile'
import { getAge } from 'lib-common/utils/local-date'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3 } from 'lib-components/typography'
import FridgeParents from './FridgeParents'
import { Gap } from 'lib-components/white-space'
import { renderResult } from '../async-rendering'
import { ChildContext } from '../../state'

interface Props {
  startOpen: boolean
}

export default React.memo(function Guardians({ startOpen }: Props) {
  const { i18n } = useTranslation()
  const { guardians } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  const printableAddresses = (guardian: PersonJSON) =>
    [
      guardian.streetAddress || '',
      guardian.postalCode || '',
      guardian.postOffice || ''
    ].join(', ')

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.personProfile.guardiansAndParents}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-guardians-collapsible"
      >
        <Gap size="m" />
        <H3 noMargin>{i18n.personProfile.guardians}</H3>
        {renderResult(guardians, (guardians) => (
          <Table data-qa="table-of-guardians">
            <Thead>
              <Tr>
                <Th>{i18n.personProfile.name}</Th>
                <Th>{i18n.personProfile.ssn}</Th>
                <Th>{i18n.personProfile.age}</Th>
                <Th>{i18n.personProfile.streetAddress}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {_.orderBy(guardians, ['lastName', 'firstName'], ['asc']).map(
                (guardian: PersonJSON) => (
                  <Tr key={`${guardian.id}`} data-qa="table-guardian-row">
                    <NameTd data-qa="guardian-name">
                      <Link to={`/profile/${guardian.id}`}>
                        {formatName(
                          guardian.firstName,
                          guardian.lastName,
                          i18n,
                          true
                        )}
                      </Link>
                    </NameTd>
                    <Td data-qa="guardian-ssn">
                      {guardian.socialSecurityNumber}
                    </Td>
                    <Td data-qa="guardian-age">
                      {getAge(guardian.dateOfBirth)}
                    </Td>
                    <Td data-qa="guardian-street-address">
                      {printableAddresses(guardian)}
                    </Td>
                  </Tr>
                )
              )}
            </Tbody>
          </Table>
        ))}
        <Gap size="XL" />
        <FridgeParents />
      </CollapsibleContentArea>
    </div>
  )
})
