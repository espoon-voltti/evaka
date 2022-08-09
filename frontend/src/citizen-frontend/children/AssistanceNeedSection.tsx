// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { tabletMin } from 'lib-components/breakpoints'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faFileAlt } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

import { getAssistanceNeedDecisions } from './api'

const Highlight = styled.div`
  background-color: ${(p) => p.theme.colors.status.success};
  width: 6px;
  position: absolute;
  left: 0.5px;
  top: 0.5px;
  bottom: 0.5px;
`

const RelativeTr = styled(Tr)`
  position: relative;
`

interface AssistanceNeedProps {
  childId: UUID
}

export default React.memo(function AssistanceNeedSection({
  childId
}: AssistanceNeedProps) {
  const t = useTranslation()

  const navigate = useNavigate()

  const [assistanceNeedDecisions] = useApiState(
    () => getAssistanceNeedDecisions(childId),
    [childId]
  )

  const unreadCount = useMemo(
    () =>
      assistanceNeedDecisions.getOrElse([]).filter(({ isUnread }) => isUnread)
        .length,
    [assistanceNeedDecisions]
  )

  const [open, setOpen] = useState(false)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{t.children.assistanceNeed.title}</H2>}
      countIndicator={{
        count: unreadCount,
        ariaLabel: `${unreadCount} ${t.children.assistanceNeed.unreadCount}`
      }}
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
      data-qa="assistance-need-decisions"
    >
      <H3>{t.children.assistanceNeed.decisions.title}</H3>
      {renderResult(assistanceNeedDecisions, (decisions) => (
        <ResponsiveTable>
          <Thead>
            <Tr>
              <Th>{t.children.assistanceNeed.decisions.form}</Th>
              <Th>{t.children.assistanceNeed.decisions.assistanceLevel}</Th>
              <Th>{t.children.assistanceNeed.decisions.validityPeriod}</Th>
              <Th>{t.children.assistanceNeed.decisions.unit}</Th>
              <Th>{t.children.assistanceNeed.decisions.decisionMade}</Th>
              <Th>{t.children.assistanceNeed.decisions.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {decisions.map((decision) => (
              <RelativeTr
                key={decision.id}
                onClick={() =>
                  navigate(
                    `/children/${childId}/assistance-need-decision/${decision.id}`
                  )
                }
                data-qa="assistance-need-decision-row"
              >
                <Td minimalWidth>
                  {decision.isUnread && (
                    <Highlight data-qa="unopened-indicator" />
                  )}

                  <FixedSpaceRow justifyContent="center" alignItems="center">
                    <IconButton
                      icon={faFileAlt}
                      altText={t.children.assistanceNeed.decisions.openDecision}
                      onClick={(ev) => {
                        ev.stopPropagation()
                        navigate(
                          `/children/${childId}/assistance-need-decision/${decision.id}`
                        )
                      }}
                    />
                  </FixedSpaceRow>
                </Td>
                <Td data-qa="assistance-level">
                  {sortBy(decision.assistanceLevels, (level) => level)
                    .map(
                      (level) =>
                        ({
                          ASSISTANCE_ENDS:
                            t.children.assistanceNeed.decisions.decision
                              .assistanceLevel.assistanceEnds,
                          ASSISTANCE_SERVICES_FOR_TIME:
                            t.children.assistanceNeed.decisions.decision
                              .assistanceLevel.assistanceServicesForTime,
                          ENHANCED_ASSISTANCE:
                            t.children.assistanceNeed.decisions.decision
                              .assistanceLevel.enhancedAssistance,
                          SPECIAL_ASSISTANCE:
                            t.children.assistanceNeed.decisions.decision
                              .assistanceLevel.specialAssistance
                        }[level])
                    )
                    .join(', ')}
                </Td>
                <Td data-qa="validity-period">
                  {decision.validityPeriod.start.format() ?? ''} –{' '}
                  {decision.validityPeriod.end?.format() ?? ''}
                </Td>
                <Td data-qa="selected-unit">{decision.selectedUnit?.name}</Td>
                <Td data-qa="decision-made">
                  {decision.decisionMade?.format()}
                </Td>
                <Td>
                  <AssistanceNeedDecisionStatusChip
                    decisionStatus={decision.status}
                    texts={
                      t.children.assistanceNeed.decisions.decision.statuses
                    }
                    data-qa="status"
                  />
                </Td>
                <JoinedStatusAndDecisionMade>
                  <AssistanceNeedDecisionStatusChip
                    decisionStatus={decision.status}
                    texts={
                      t.children.assistanceNeed.decisions.decision.statuses
                    }
                    data-qa="status"
                  />
                  <div>{decision.decisionMade?.format()}</div>
                </JoinedStatusAndDecisionMade>
              </RelativeTr>
            ))}
          </Tbody>
        </ResponsiveTable>
      ))}
    </CollapsibleContentArea>
  )
})

const ResponsiveTable = styled(Table)`
  @media (max-width: ${tabletMin}) {
    margin: 0 -${defaultMargins.s};
    width: calc(100% + 2 * ${defaultMargins.s});

    thead {
      display: none;
    }

    tbody tr {
      display: grid;
      grid-template-columns: min-content auto;
      border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
      padding: ${defaultMargins.s};

      td {
        grid-column: 2;
        border: none;
        padding: ${defaultMargins.xxs} 0;
      }

      td:first-child {
        grid-column: 1;
        width: auto;
      }

      td:nth-last-child(3),
      td:nth-last-child(2) {
        display: none;
      }

      &:hover {
        box-shadow: none;
      }
    }
  }
`

const JoinedStatusAndDecisionMade = styled.td`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: flex;
    align-items: center;
    gap: ${defaultMargins.xs};
  }
`
