// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Table } from '~components/shared/alpha'
import _ from 'lodash'
import { useTranslation } from '~state/i18n'
import { Title } from '~components/shared/alpha'
import { DaycarePlacementPlan } from '~types/unit'
import { Link } from 'react-router-dom'
import { careTypesFromPlacementType } from '~components/common/CareTypeLabel'
import { formatName } from '~utils'
import { getEmployeeUrlPrefix } from '~constants'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import { faFileAlt } from '@evaka/icons'
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
        <Table.Table dataQa="table-of-missing-groups" className="compact">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.unit.placementPlans.name}</Table.Th>
              <Table.Th>{i18n.unit.placementPlans.birthday}</Table.Th>
              <Table.Th>{i18n.unit.placementPlans.placementDuration}</Table.Th>
              <Table.Th>{i18n.unit.placementPlans.type}</Table.Th>
              <Table.Th>{i18n.unit.placementPlans.application}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>
            {sortedRows.map((p) => (
              <Table.Row key={`${p.id}`} dataQa="placement-plan-row">
                <Table.Td dataQa="child-name">
                  <Link to={`/child-information/${p.child.id}`}>
                    {formatName(
                      p.child.firstName,
                      p.child.lastName,
                      i18n,
                      true
                    )}
                  </Link>
                </Table.Td>
                <Table.Td dataQa="child-dob">
                  {p.child.dateOfBirth.format()}
                </Table.Td>
                <Table.Td dataQa="placement-duration">
                  {`${p.period.start.format()} - ${p.period.end.format()}`}
                </Table.Td>
                <Table.Td dataQa="placement-type">
                  {careTypesFromPlacementType(p.type)}
                </Table.Td>
                <Table.Td dataQa="application-link">
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
                </Table.Td>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Table>
      </div>
    </>
  )
})
