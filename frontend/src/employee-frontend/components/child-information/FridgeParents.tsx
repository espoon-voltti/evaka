// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext } from 'react'
import { Link } from 'react-router'

import { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H3 } from 'lib-components/typography'

import StatusLabel from '../../components/common/StatusLabel'
import { ChildContext, ChildState } from '../../state/child'
import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'
import { renderResult } from '../async-rendering'
import { NameTd } from '../person-profile/common'

const FridgeParents = React.memo(function FridgeParents() {
  const { i18n } = useTranslation()
  const { parentships } = useContext<ChildState>(ChildContext)

  return (
    <div className="fridge-parents-section">
      <H3 noMargin>{i18n.childInformation.fridgeParents.title}</H3>
      {renderResult(parentships, (data) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.fridgeParents.name}</Th>
              <Th>{i18n.childInformation.fridgeParents.ssn}</Th>
              <Th>{i18n.childInformation.fridgeParents.startDate}</Th>
              <Th>{i18n.childInformation.fridgeParents.endDate}</Th>
              <Th>{i18n.childInformation.fridgeParents.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(data, ['startDate'], ['desc']).map(
              ({ data: parentship }: ParentshipWithPermittedActions) => (
                <Tr key={parentship.id}>
                  <NameTd>
                    <Link to={`/profile/${parentship.headOfChildId}`}>
                      {parentship.headOfChild.lastName}{' '}
                      {parentship.headOfChild.firstName}
                    </Link>
                  </NameTd>
                  <Td>{parentship.headOfChild.socialSecurityNumber}</Td>
                  <Td>{parentship.startDate.format()}</Td>
                  <Td>{parentship.endDate?.format()}</Td>
                  <Td>
                    <StatusLabel
                      status={
                        parentship.conflict
                          ? 'conflict'
                          : getStatusLabelByDateRange(parentship)
                      }
                    />
                  </Td>
                </Tr>
              )
            )}
          </Tbody>
        </Table>
      ))}
    </div>
  )
})

export default FridgeParents
