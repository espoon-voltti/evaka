// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React from 'react'
import { Link } from 'wouter'

import type { TerminatedPlacement } from 'lib-common/generated/api-types/placement'
import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { PersonName } from 'lib-components/molecules/PersonNames'

import { useTranslation } from '../../../state/i18n'
import { CareTypeChip } from '../../common/CareTypeLabel'

function TerminatedPlacementRow({
  placement
}: {
  placement: TerminatedPlacement
}) {
  return (
    <Tr key="$placement.child.id}" data-qa="terminated-placement-row">
      <Td data-qa="child-name">
        <Link to={`/child-information/${placement.child.id}`}>
          <PersonName person={placement.child} format="Last First" />
        </Link>
      </Td>
      <Td data-qa="child-dob">{placement.child.dateOfBirth.format()}</Td>
      <Td data-qa="placement-type">
        <CareTypeChip
          type={
            placement.connectedDaycareOnly
              ? 'connected-daycare'
              : placement.type
          }
        />
      </Td>
      <Td data-qa="termination-requested-date">
        {placement.terminationRequestedDate?.format() ?? ''}
      </Td>
      <Td data-qa="placement-end-date">{placement.endDate.format()}</Td>
      <Td data-qa="group-name">{placement.currentDaycareGroupName}</Td>
    </Tr>
  )
}

type Props = {
  recentlyTerminatedPlacements: TerminatedPlacement[]
}

export default React.memo(function TerminatedPlacements({
  recentlyTerminatedPlacements
}: Props) {
  const { i18n } = useTranslation()

  const sortedRows = sortBy(recentlyTerminatedPlacements, [
    (p: TerminatedPlacement) => p.terminationRequestedDate,
    (p: TerminatedPlacement) => p.child.lastName,
    (p: TerminatedPlacement) => p.child.firstName
  ])

  return (
    <>
      <ExpandingInfo info={i18n.unit.termination.info}>
        <Title size={2}>{i18n.unit.termination.title}</Title>
      </ExpandingInfo>
      <div>
        <Table data-qa="table-of-terminated-placements">
          <Thead>
            <Tr>
              <Th>{i18n.unit.placements.name}</Th>
              <Th>{i18n.unit.placements.birthday}</Th>
              <Th>{i18n.unit.placements.type}</Th>
              <Th>{i18n.unit.termination.terminationRequestedDate}</Th>
              <Th>{i18n.unit.termination.endDate}</Th>
              <Th>{i18n.unit.termination.groupName}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {sortedRows.map((row) => (
              <TerminatedPlacementRow
                key={`${row.id}-${row.type}`}
                placement={row}
              />
            ))}
          </Tbody>
        </Table>
      </div>
    </>
  )
})
