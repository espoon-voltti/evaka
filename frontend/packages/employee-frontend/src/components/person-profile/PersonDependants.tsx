// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { faChild } from 'icon-set'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import _ from 'lodash'
import { Link } from 'react-router-dom'
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
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  useEffect(() => {
    setDependants(Loading())
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
    isSuccess(dependants)
      ? _.orderBy(dependants.data, ['dateOfBirth'], ['asc']).map(
          (dependant: PersonWithChildren) => {
            return (
              <Table.Row key={`${dependant.id}`} dataQa="table-dependant-row">
                <NameTd dataQa="dependant-name">
                  <Link to={`/child-information/${dependant.id}`}>
                    {formatName(dependant.firstName, dependant.lastName, i18n)}
                  </Link>
                </NameTd>
                <Table.Td dataQa="dependant-ssn">
                  {dependant.socialSecurityNumber}
                </Table.Td>
                <Table.Td dataQa="dependant-age">
                  {getAge(dependant.dateOfBirth)}
                </Table.Td>
                <Table.Td dataQa="dependant-street-address">
                  {printableAddresses(dependant.addresses)}
                </Table.Td>
              </Table.Row>
            )
          }
        )
      : null

  return (
    <div>
      <Collapsible
        icon={faChild}
        title={i18n.personProfile.dependants}
        open={toggled}
        onToggle={toggle}
        dataQa="person-dependants-collapsible"
      >
        <Table.Table dataQa="table-of-dependants">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.personProfile.name}</Table.Th>
              <Table.Th>{i18n.personProfile.ssn}</Table.Th>
              <Table.Th>{i18n.personProfile.age}</Table.Th>
              <Table.Th>{i18n.personProfile.streetAddress}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderDependants()}</Table.Body>
        </Table.Table>
        {isLoading(dependants) && <Loader />}
        {isFailure(dependants) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonDependants
