// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading } from 'lib-common/api'
import { useContext } from 'react'
import Loader from 'lib-components/atoms/Loader'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import _ from 'lodash'
import { Link } from 'react-router-dom'
import { PersonDetails } from '../../types/person'
import { formatName } from '../../utils'
import { NameTd } from '../PersonProfile'
import { ChildContext } from '../../state'
import { getPersonGuardians } from '../../api/person'
import { getAge } from 'lib-common/utils/local-date'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

interface Props {
  id: UUID
  startOpen: boolean
}

const Guardians = React.memo(function Guardians({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { guardians, setGuardians } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  useEffect(() => {
    setGuardians(Loading.of())
    void getPersonGuardians(id).then((response) => {
      setGuardians(response)
    })
  }, [id, setGuardians])

  const printableAddresses = (guardian: PersonDetails) =>
    [
      guardian.streetAddress || '',
      guardian.postalCode || '',
      guardian.postOffice || ''
    ].join(', ')

  const renderGuardians = () =>
    guardians
      .map((gs) =>
        _.orderBy(gs, ['lastName', 'firstName'], ['asc']).map(
          (guardian: PersonDetails) => {
            return (
              <Tr key={`${guardian.id}`} data-qa="table-guardian-row">
                <NameTd data-qa="guardian-name">
                  <Link to={`/profile/${guardian.id}`}>
                    {formatName(guardian.firstName, guardian.lastName, i18n)}
                  </Link>
                </NameTd>
                <Td data-qa="guardian-ssn">{guardian.socialSecurityNumber}</Td>
                <Td data-qa="guardian-age">{getAge(guardian.dateOfBirth)}</Td>
                <Td data-qa="guardian-street-address">
                  {printableAddresses(guardian)}
                </Td>
              </Tr>
            )
          }
        )
      )
      .getOrElse(null)

  return (
    <div>
      <CollapsibleContentArea
        //icon={faFemale}
        title={<H2 noMargin>{i18n.personProfile.guardians}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-guardians-collapsible"
      >
        <Table data-qa="table-of-guardians">
          <Thead>
            <Tr>
              <Th>{i18n.personProfile.name}</Th>
              <Th>{i18n.personProfile.ssn}</Th>
              <Th>{i18n.personProfile.age}</Th>
              <Th>{i18n.personProfile.streetAddress}</Th>
            </Tr>
          </Thead>
          <Tbody>{renderGuardians()}</Tbody>
        </Table>
        {guardians.isLoading && <Loader />}
        {guardians.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleContentArea>
    </div>
  )
})

export default Guardians
