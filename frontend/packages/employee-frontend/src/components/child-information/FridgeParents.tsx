// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useTranslation } from '~/state/i18n'
import { isFailure, isLoading } from '~/api'
import { ChildContext, ChildState } from '~/state/child'
import { faUser } from 'icon-set'
import { Loader, Table } from '~components/shared/alpha'

import * as _ from 'lodash'
import { Parentship } from '~types/fridge'
import { Link } from 'react-router-dom'
import { getStatusLabelByDateRange } from '~utils/date'
import StatusLabel from '~components/common/StatusLabel'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'

type Props = {
  open: boolean
}

const FridgeParents = React.memo(function FridgeParents({ open }: Props) {
  const { i18n } = useTranslation()
  const { parentships } = useContext<ChildState>(ChildContext)

  function renderFridgeParents() {
    if (isLoading(parentships)) {
      return <Loader />
    } else if (isFailure(parentships)) {
      return <div>{i18n.common.loadingFailed}</div>
    }

    return (
      <Table.Table>
        <Table.Head>
          <Table.Row>
            <Table.Th>{i18n.childInformation.fridgeParents.name}</Table.Th>
            <Table.Th>{i18n.childInformation.fridgeParents.ssn}</Table.Th>
            <Table.Th>{i18n.childInformation.fridgeParents.startDate}</Table.Th>
            <Table.Th>{i18n.childInformation.fridgeParents.endDate}</Table.Th>
            <Table.Th>{i18n.childInformation.fridgeParents.status}</Table.Th>
          </Table.Row>
        </Table.Head>
        <Table.Body>
          {_.orderBy(parentships.data, ['startDate'], ['desc']).map(
            (parentship: Parentship) => (
              <Table.Row key={parentship.id}>
                <Table.Td>
                  <Link to={`/profile/${parentship.headOfChildId}`}>
                    {parentship.headOfChild.lastName}{' '}
                    {parentship.headOfChild.firstName}
                  </Link>
                </Table.Td>
                <Table.Td>
                  {parentship.headOfChild.socialSecurityNumber}
                </Table.Td>
                <Table.Td>{parentship.startDate.format()}</Table.Td>
                <Table.Td>{parentship.endDate?.format()}</Table.Td>
                <Table.Td>
                  <StatusLabel
                    status={
                      parentship.conflict
                        ? 'conflict'
                        : getStatusLabelByDateRange(parentship)
                    }
                  />
                </Table.Td>
              </Table.Row>
            )
          )}
        </Table.Body>
      </Table.Table>
    )
  }

  return (
    <div className="fridge-parents-section">
      <CollapsibleSection
        icon={faUser}
        title={i18n.childInformation.fridgeParents.title}
        startCollapsed={!open}
      >
        {renderFridgeParents()}
      </CollapsibleSection>
    </div>
  )
})

export default FridgeParents
