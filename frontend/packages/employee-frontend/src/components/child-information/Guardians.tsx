// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { useContext } from 'react'
import { Loader, Table } from '~components/shared/alpha'
import _ from 'lodash'
import { Link } from 'react-router-dom'
import { PersonDetails } from '~/types/person'
import { formatName } from '~utils'
import { NameTd } from '~components/PersonProfile'
import { ChildContext } from '~state'
import { faFemale } from 'icon-set'
import { getPersonGuardians } from '~api/person'
import { getAge } from '@evaka/lib-common/src/utils/local-date'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'

interface Props {
  id: UUID
  open: boolean
}

const Guardians = React.memo(function Guardians({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { guardians, setGuardians } = useContext(ChildContext)

  useEffect(() => {
    setGuardians(Loading())
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
    isSuccess(guardians)
      ? _.orderBy(guardians.data, ['lastName', 'firstName'], ['asc']).map(
          (guardian: PersonDetails) => {
            return (
              <Table.Row key={`${guardian.id}`} dataQa="table-guardian-row">
                <NameTd dataQa="guardian-name">
                  <Link to={`/profile/${guardian.id}`}>
                    {formatName(guardian.firstName, guardian.lastName, i18n)}
                  </Link>
                </NameTd>
                <Table.Td dataQa="guardian-ssn">
                  {guardian.socialSecurityNumber}
                </Table.Td>
                <Table.Td dataQa="guardian-age">
                  {getAge(guardian.dateOfBirth)}
                </Table.Td>
                <Table.Td dataQa="guardian-street-address">
                  {printableAddresses(guardian)}
                </Table.Td>
              </Table.Row>
            )
          }
        )
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faFemale}
        title={i18n.personProfile.guardians}
        startCollapsed={!open}
        dataQa="person-guardians-collapsible"
      >
        <Table.Table dataQa="table-of-guardians">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.personProfile.name}</Table.Th>
              <Table.Th>{i18n.personProfile.ssn}</Table.Th>
              <Table.Th>{i18n.personProfile.age}</Table.Th>
              <Table.Th>{i18n.personProfile.streetAddress}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderGuardians()}</Table.Body>
        </Table.Table>
        {isLoading(guardians) && <Loader />}
        {isFailure(guardians) && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default Guardians
