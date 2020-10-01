// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'

import {
  Table,
  Td,
  Th,
  Tr,
  Thead,
  Tbody
} from '~components/shared/layout/Table'
import Title from '~components/shared/atoms/Title'
import { useTranslation } from '~state/i18n'
import { DaycarePlacementPlan } from '~types/unit'
import { Link } from 'react-router-dom'
import { careTypesFromPlacementType } from '~components/common/CareTypeLabel'
import { formatName } from '~utils'
import { getEmployeeUrlPrefix } from '~constants'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import { faFileAlt } from 'icon-set'
import styled from 'styled-components'

const CenteredDiv = styled.div`
  display: flex;
  justify-content: center;
`

type Props = {
  placementPlans: DaycarePlacementPlan[]
}

export default React.memo(function PlacementPlans({ placementPlans }: Props) {
  const { i18n } = useTranslation()

  const sortedRows = _.sortBy(placementPlans, [
    (p: DaycarePlacementPlan) => p.child.lastName,
    (p: DaycarePlacementPlan) => p.child.firstName,
    (p: DaycarePlacementPlan) => p.period.start
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.placementPlans.title}</Title>
      <div
        className="table-of-missing-groups"
        data-qa="table-of-missing-groups"
      >
        <Table data-qa="table-of-missing-groups" className="compact">
          <Thead>
            <Tr>
              <Th>{i18n.unit.placementPlans.name}</Th>
              <Th>{i18n.unit.placementPlans.birthday}</Th>
              <Th>{i18n.unit.placementPlans.placementDuration}</Th>
              <Th>{i18n.unit.placementPlans.type}</Th>
              <Th>{i18n.unit.placementPlans.application}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {sortedRows.map((p) => (
              <Tr key={`${p.id}`} data-qa="placement-plan-row">
                <Td data-qa="child-name">
                  <Link to={`/child-information/${p.child.id}`}>
                    {formatName(
                      p.child.firstName,
                      p.child.lastName,
                      i18n,
                      true
                    )}
                  </Link>
                </Td>
                <Td data-qa="child-dob">{p.child.dateOfBirth.format()}</Td>
                <Td data-qa="placement-duration">
                  {`${p.period.start.format()} - ${p.period.end.format()}`}
                </Td>
                <Td data-qa="placement-type">
                  {careTypesFromPlacementType(p.type)}
                </Td>
                <Td data-qa="application-link">
                  <CenteredDiv>
                    <a
                      href={`${getEmployeeUrlPrefix()}/employee/applications/${
                        p.applicationId
                      }`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      <IconButton
                        onClick={() => undefined}
                        icon={faFileAlt}
                        altText={i18n.personProfile.application.open}
                      />
                    </a>
                  </CenteredDiv>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </>
  )
})
