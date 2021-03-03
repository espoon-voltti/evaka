// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import { Link } from 'react-router-dom'

import { faChild } from '@evaka/lib-icons'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading } from '@evaka/lib-common/api'
import { useContext } from 'react'
import { PersonContext } from '../../state/person'
import CollapsibleSection from '@evaka/lib-components/molecules/CollapsibleSection'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/layout/Table'
import Loader from '@evaka/lib-components/atoms/Loader'
import { getGuardianDecisions } from '../../api/person'
import { Decision } from '../../types/decision'
import { DateTd, NameTd, StatusTd } from '../../components/PersonProfile'

interface Props {
  id: UUID
  open: boolean
}

const PersonDecisions = React.memo(function PersonDecisions({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { decisions, setDecisions } = useContext(PersonContext)

  useEffect(() => {
    setDecisions(Loading.of())
    void getGuardianDecisions(id).then((response) => {
      setDecisions(response)
    })
  }, [id, setDecisions])

  const renderDecisions = () =>
    decisions.isSuccess
      ? _.orderBy(
          decisions.value,
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
        })
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.decision.decisions}
        startCollapsed={!open}
        data-qa="person-decisions-collapsible"
      >
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
          <Tbody>{renderDecisions()}</Tbody>
        </Table>
        {decisions.isLoading && <Loader />}
        {decisions.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonDecisions
