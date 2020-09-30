// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { faChild } from 'icon-set'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import _ from 'lodash'
import { Link } from 'react-router-dom'
import { getGuardianDecisions } from '~api/person'
import { Decision } from '~types/decision'
import { DateTd, NameTd, StatusTd } from '~components/PersonProfile'

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
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  useEffect(() => {
    setDecisions(Loading())
    void getGuardianDecisions(id).then((response) => {
      setDecisions(response)
    })
  }, [id, setDecisions])

  const renderDecisions = () =>
    isSuccess(decisions)
      ? _.orderBy(
          decisions.data,
          ['startDate', 'preferredUnitName'],
          ['desc', 'desc']
        ).map((decision: Decision) => {
          return (
            <Table.Row key={`${decision.id}`} dataQa="table-decision-row">
              <NameTd dataQa="decision-child-name">
                <Link to={`/child-information/${decision.childId}`}>
                  {decision.childName}
                </Link>
              </NameTd>
              <Table.Td dataQa="decision-preferred-unit-id">
                <Link to={`/units/${decision.unit.id}`}>
                  {decision.unit.name}
                </Link>
              </Table.Td>
              <DateTd dataQa="decision-start-date">
                {decision.startDate.format()}
              </DateTd>
              <DateTd dataQa="decision-sent-date">
                {decision.sentDate.format()}
              </DateTd>
              <Table.Td dataQa="decision-type">
                {i18n.personProfile.application.types[decision.type]}
              </Table.Td>
              <StatusTd dataQa="decision-status">
                {i18n.personProfile.decision.statuses[decision.status]}
              </StatusTd>
            </Table.Row>
          )
        })
      : null

  return (
    <div>
      <Collapsible
        icon={faChild}
        title={i18n.personProfile.decision.decisions}
        open={toggled}
        onToggle={toggle}
        dataQa="person-decisions-collapsible"
      >
        <Table.Table dataQa="table-of-decisions">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.personProfile.application.child}</Table.Th>
              <Table.Th>{i18n.personProfile.decision.decisionUnit}</Table.Th>
              <Table.Th>{i18n.personProfile.decision.startDate}</Table.Th>
              <Table.Th>{i18n.personProfile.decision.sentDate}</Table.Th>
              <Table.Th>{i18n.personProfile.application.type}</Table.Th>
              <Table.Th>{i18n.personProfile.decision.status}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderDecisions()}</Table.Body>
        </Table.Table>
        {isLoading(decisions) && <Loader />}
        {isFailure(decisions) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonDecisions
