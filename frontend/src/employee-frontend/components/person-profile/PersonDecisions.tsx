// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'
import { Link } from 'wouter'

import type { Decision } from 'lib-common/generated/api-types/decision'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { featureFlags } from 'lib-customizations/employee'
import { faBoxArchive } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { DateTd, NameTd, StatusTd } from './common'
import {
  decisionsByGuardianQuery,
  planArchiveDecisionMutation
} from './queries'

interface Props {
  id: PersonId
}

const PersonDecisions = React.memo(function PersonDecisions({ id }: Props) {
  const { i18n } = useTranslation()
  const decisionsResponse = useQueryResult(decisionsByGuardianQuery({ id }))

  return renderResult(decisionsResponse, (decisionsResponse) => (
    <Table data-qa="table-of-decisions">
      <Thead>
        <Tr>
          <Th>{i18n.personProfile.application.child}</Th>
          <Th>{i18n.personProfile.decision.decisionUnit}</Th>
          <Th>{i18n.personProfile.decision.startDate}</Th>
          <Th>{i18n.personProfile.decision.sentDate}</Th>
          <Th>{i18n.personProfile.application.type}</Th>
          <Th>{i18n.personProfile.decision.status}</Th>
          {featureFlags.archiveIntegration?.decisions && (
            <Th>{i18n.personProfile.decision.archived}</Th>
          )}
        </Tr>
      </Thead>
      <Tbody>
        {orderBy(
          decisionsResponse.decisions,
          ['startDate', 'preferredUnitName', 'childName'],
          ['desc', 'desc']
        ).map((decision: Decision) => (
          <Tr key={decision.id} data-qa="table-decision-row">
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
              {decision.sentDate?.format() ?? ''}
            </DateTd>
            <Td data-qa="decision-type">
              {i18n.personProfile.application.types[decision.type]}
            </Td>
            <StatusTd data-qa="decision-status">
              {i18n.personProfile.decision.statuses[decision.status]}
            </StatusTd>
            {featureFlags.archiveIntegration?.decisions && (
              <Td>
                {decision.archivedAt !== null ? (
                  decision.archivedAt.toLocalDate().format()
                ) : (
                  <MutateButton
                    icon={faBoxArchive}
                    text={i18n.personProfile.decision.archive}
                    mutation={planArchiveDecisionMutation}
                    onClick={() => ({ decisionId: decision.id })}
                    data-qa="archive-button"
                    disabled={
                      decision.status !== 'ACCEPTED' &&
                      decision.status !== 'REJECTED'
                    }
                  />
                )}
              </Td>
            )}
          </Tr>
        ))}
      </Tbody>
    </Table>
  ))
})

export default PersonDecisions
