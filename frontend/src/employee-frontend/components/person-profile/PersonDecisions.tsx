// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChild } from 'lib-icons'
import _ from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'

import { getGuardianDecisions } from '../../api/person'
import { useTranslation } from '../../state/i18n'
import { Decision } from '../../types/decision'
import { DateTd, NameTd, StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

interface Props {
  id: UUID
  open: boolean
}

const PersonDecisions = React.memo(function PersonDecisions({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const [decisions] = useApiState(() => getGuardianDecisions(id), [id])

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.decision.decisions}
        startCollapsed={!open}
        data-qa="person-decisions-collapsible"
      >
        {renderResult(decisions, (decisions) => (
          <Table data-qa="table-of-decisions">
            <Thead>
              <Tr>
                <Th>{i18n.personProfile.application.child}</Th>
                <Th>{i18n.personProfile.decision.decisionUnit}</Th>
                <Th>{i18n.personProfile.decision.startDate}</Th>
                <Th>{i18n.personProfile.decision.sentDate}</Th>
                <Th>{i18n.personProfile.application.type}</Th>
                <Th>{i18n.personProfile.decision.status}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {_.orderBy(
                decisions,
                ['startDate', 'preferredUnitName'],
                ['desc', 'desc']
              ).map((decision: Decision) => {
                return (
                  <Tr key={`${decision.id}`} data-qa="table-decision-row">
                    <NameTd data-qa="decision-child-name">
                      <Link to={`/child-information/${decision.childId}`}>
                        {decision.childName}
                      </Link>
                    </NameTd>
                    <Td data-qa="decision-preferred-unit-id">
                      <Link to={`/units/${decision.unit.id}`}>
                        {decision.unit.name}
                      </Link>
                    </Td>
                    <DateTd data-qa="decision-start-date">
                      {decision.startDate.format()}
                    </DateTd>
                    <DateTd data-qa="decision-sent-date">
                      {decision.sentDate.format()}
                    </DateTd>
                    <Td data-qa="decision-type">
                      {i18n.personProfile.application.types[decision.type]}
                    </Td>
                    <StatusTd data-qa="decision-status">
                      {i18n.personProfile.decision.statuses[decision.status]}
                    </StatusTd>
                  </Tr>
                )
              })}
            </Tbody>
          </Table>
        ))}
      </CollapsibleSection>
    </div>
  )
})

export default PersonDecisions
