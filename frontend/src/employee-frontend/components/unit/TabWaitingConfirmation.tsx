{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext } from 'react'
import { ContentArea } from '@evaka/lib-components/layout/Container'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from '@evaka/lib-components/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/atoms/state/ErrorSegment'
import Title from '@evaka/lib-components/atoms/Title'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/layout/Table'
import { Link } from 'react-router-dom'
import { formatName } from '../../utils'
import { careTypesFromPlacementType } from '../../components/common/CareTypeLabel'
import { getEmployeeUrlPrefix } from '../../constants'
import IconButton from '@evaka/lib-components/atoms/buttons/IconButton'
import { faFileAlt } from '@evaka/lib-icons'
import { useTranslation } from '../../state/i18n'
import _ from 'lodash'
import { DaycarePlacementPlan } from '../../types/unit'
import styled from 'styled-components'
import PlacementCircle from '@evaka/lib-components/atoms/PlacementCircle'
import { isPartDayPlacement } from '../../utils/placements'

const CenteredDiv = styled.div`
  display: flex;
  justify-content: center;
`

function TabWaitingConfirmation() {
  const { i18n } = useTranslation()

  const { unitData } = useContext(UnitContext)

  if (unitData.isLoading) {
    return <SpinnerSegment />
  }

  if (unitData.isFailure || !unitData.value.placementPlans) {
    return <ErrorSegment />
  }

  const sortedRows = _.sortBy(unitData.value.placementPlans, [
    (p: DaycarePlacementPlan) => p.child.lastName,
    (p: DaycarePlacementPlan) => p.child.firstName,
    (p: DaycarePlacementPlan) => p.period.start
  ])

  return (
    <ContentArea opaque>
      <Title size={2}>{i18n.unit.placementPlans.title}</Title>
      <div>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.unit.placementPlans.name}</Th>
              <Th>{i18n.unit.placementPlans.birthday}</Th>
              <Th>{i18n.unit.placementPlans.placementDuration}</Th>
              <Th>{i18n.unit.placementPlans.type}</Th>
              <Th>{i18n.unit.placementPlans.subtype}</Th>
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
                <Td data-qa="placement-subtype">
                  <PlacementCircle
                    type={isPartDayPlacement(p.type) ? 'half' : 'full'}
                    label={i18n.placement.type[p.type]}
                  />
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
    </ContentArea>
  )
}

export default React.memo(TabWaitingConfirmation)
