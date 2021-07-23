// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import * as _ from 'lodash'

import { useTranslation } from '../../state/i18n'
import { ChildContext, ChildState } from '../../state/child'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import { Parentship } from '../../types/fridge'
import { Link } from 'react-router-dom'
import { getStatusLabelByDateRange } from '../../utils/date'
import StatusLabel from '../../components/common/StatusLabel'
import { H3 } from '../../../lib-components/typography'

const FridgeParents = React.memo(function FridgeParents() {
  const { i18n } = useTranslation()
  const { parentships } = useContext<ChildState>(ChildContext)

  return (
    <div className="fridge-parents-section">
      <H3 noMargin>{i18n.childInformation.fridgeParents.title}</H3>
      {parentships.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <div>{i18n.common.loadingFailed}</div>
        },
        success(data) {
          return (
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
                {_.orderBy(data, ['startDate'], ['desc']).map(
                  (parentship: Parentship) => (
                    <Tr key={parentship.id}>
                      <Td>
                        <Link to={`/profile/${parentship.headOfChildId}`}>
                          {parentship.headOfChild.lastName}{' '}
                          {parentship.headOfChild.firstName}
                        </Link>
                      </Td>
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
          )
        }
      })}
    </div>
  )
})

export default FridgeParents
