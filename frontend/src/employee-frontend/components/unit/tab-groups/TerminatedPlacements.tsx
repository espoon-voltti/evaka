// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import _ from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '../../../components/common/CareTypeLabel'
import { Translations, useTranslation } from '../../../state/i18n'
import {TerminatedPlacement} from '../../../types/unit'
import { formatName } from '../../../utils'

function renderTerminatedPlacementRow(
  placement: TerminatedPlacement,
  i18n: Translations
) {
  return (
    <Tr key={`${placement.child.id}`} data-qa="terminated-placement-row">
      <Td data-qa="child-name">
        <Link to={`/child-information/${placement.child.id}`}>
          {formatName(
            placement.child.firstName,
            placement.child.lastName,
            i18n,
            true
          )}
        </Link>
      </Td>
      <Td data-qa="child-dob">{placement.child.dateOfBirth.format()}</Td>
      <Td data-qa="placement-type">
        {placement.type ? (
          careTypesFromPlacementType(placement.type)
        ) : (
          <CareTypeLabel type="backup-care" />
        )}
      </Td>
      <Td data-qa="termination-requested-date">{`${placement.terminationRequestedDate.format()}`}</Td>
      <Td data-qa="placement-end-date">{`${placement.endDate.format()}`}</Td>
      <Td data-qa="group-name">{placement.currentDaycareGroupName}</Td>
    </Tr>
  )
}

type Props = {
  recentlyTerminatedPlacements: TerminatedPlacement[]
}

export default React.memo(function TerminatedPlacements({ recentlyTerminatedPlacements }: Props) {
  const { i18n } = useTranslation()

  const sortedRows = _.sortBy(recentlyTerminatedPlacements, [
    (p: TerminatedPlacement) => p.terminationRequestedDate,
    (p: TerminatedPlacement) => p.child.lastName,
    (p: TerminatedPlacement) => p.child.firstName
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.termination.title}</Title>
      <div
        className="table-of-terminated-placements"
        data-qa="table-of-terminated-placements"
      >
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
            {sortedRows.map((row) => renderTerminatedPlacementRow(row, i18n))}
          </Tbody>
        </Table>
      </div>
    </>
  )
})
