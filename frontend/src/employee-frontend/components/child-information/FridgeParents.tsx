// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import * as _ from 'lodash'

import { useTranslation } from '../../state/i18n'
import { ChildContext, ChildState } from '../../state/child'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import { Parentship } from '../../types/fridge'
import { Link } from 'react-router-dom'
import { getStatusLabelByDateRange } from '../../utils/date'
import StatusLabel from '../../components/common/StatusLabel'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

type Props = {
  startOpen: boolean
}

const FridgeParents = React.memo(function FridgeParents({ startOpen }: Props) {
  const { i18n } = useTranslation()
  const { parentships } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(startOpen)

  function renderFridgeParents() {
    if (parentships.isLoading) {
      return <Loader />
    } else if (parentships.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    }

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
          {_.orderBy(parentships.value, ['startDate'], ['desc']).map(
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

  return (
    <div className="fridge-parents-section">
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.fridgeParents.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="fridge-parents-collapsible"
      >
        {renderFridgeParents()}
      </CollapsibleContentArea>
    </div>
  )
})

export default FridgeParents
