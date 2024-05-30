// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { PlacementPlanDetails } from 'lib-common/generated/api-types/placement'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { P } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faFileAlt, faTimes } from 'lib-icons'

import { getEmployeeUrlPrefix } from '../../constants'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { isPartDayPlacement } from '../../utils/placements'
import { NotificationCounter } from '../UnitPage'
import { CircleIconSmallOrange } from '../applications/CircleIcon'
import { CareTypeChip } from '../common/CareTypeLabel'
import { FlexRow } from '../common/styled/containers'

const CenteredDiv = styled.div`
  display: flex;
  justify-content: center;
`

function earliestStartDate(p: PlacementPlanDetails) {
  return p.preschoolDaycarePeriod?.start &&
    p.preschoolDaycarePeriod.start < p.period.start
    ? p.preschoolDaycarePeriod.start
    : p.period.start
}

function latestEndDate(p: PlacementPlanDetails) {
  return p.preschoolDaycarePeriod?.end &&
    p.preschoolDaycarePeriod.end > p.period.end
    ? p.preschoolDaycarePeriod.end
    : p.period.end
}

interface Props {
  placementPlans: PlacementPlanDetails[]
}

export default React.memo(function TabWaitingConfirmation({
  placementPlans
}: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState<boolean>(true)

  const sortedRows = useMemo(
    () =>
      sortBy(placementPlans, [
        (p) => p.child.lastName,
        (p) => p.child.firstName,
        (p) => p.period.start
      ]),
    [placementPlans]
  )

  const nonRejectedRowCount = useMemo(
    () => placementPlans.filter((p) => !p.rejectedByCitizen).length,
    [placementPlans]
  )

  return (
    <CollapsibleContentArea
      opaque
      open={open}
      title={
        <Title size={2}>
          {i18n.unit.placementPlans.title}
          {nonRejectedRowCount > 0 ? (
            <NotificationCounter data-qa="notification-counter">
              {nonRejectedRowCount}
            </NotificationCounter>
          ) : null}
        </Title>
      }
      toggleOpen={() => setOpen(!open)}
      data-qa="waiting-confirmation-section"
    >
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
              <Tr
                key={p.id}
                data-qa="placement-plan-row"
                data-application-id={p.applicationId}
                data-rejected={p.rejectedByCitizen}
              >
                <Td data-qa="child-name">
                  {!p.rejectedByCitizen ? (
                    <Link to={`/child-information/${p.child.id}`}>
                      {formatName(
                        p.child.firstName,
                        p.child.lastName,
                        i18n,
                        true
                      )}
                    </Link>
                  ) : (
                    <P noMargin color={colors.grayscale.g35}>
                      {formatName(
                        p.child.firstName,
                        p.child.lastName,
                        i18n,
                        true
                      )}
                    </P>
                  )}
                </Td>
                <Td
                  data-qa="child-dob"
                  color={p.rejectedByCitizen ? colors.grayscale.g35 : undefined}
                >
                  {p.child.dateOfBirth.format()}
                </Td>
                <Td data-qa="placement-duration">
                  {!p.rejectedByCitizen ? (
                    `${earliestStartDate(p).format()} - ${latestEndDate(
                      p
                    ).format()}`
                  ) : (
                    <FlexRow>
                      <CircleIconSmallOrange>
                        <FontAwesomeIcon icon={faTimes} />
                      </CircleIconSmallOrange>
                      <P noMargin>
                        {
                          i18n.unit.placementProposals
                            .citizenHasRejectedPlacement
                        }
                      </P>
                    </FlexRow>
                  )}
                </Td>
                <Td data-qa="placement-type">
                  <CareTypeChip type={p.type} />
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
                        aria-label={i18n.personProfile.application.open}
                      />
                    </a>
                  </CenteredDiv>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </CollapsibleContentArea>
  )
})
