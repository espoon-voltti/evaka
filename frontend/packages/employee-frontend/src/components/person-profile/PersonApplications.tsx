// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { faFileAlt } from 'icon-set'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'
import { getGuardianApplicationSummaries } from '~api/person'
import { ApplicationSummary } from '~types/application'
import { DateTd, NameTd, StatusTd } from '~components/PersonProfile'
import IconButton from 'components/shared/atoms/buttons/IconButton'

interface Props {
  id: UUID
  open: boolean
}

const PersonApplications = React.memo(function PersonApplications({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { applications, setApplications } = useContext(PersonContext)
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  useEffect(() => {
    setApplications(Loading())
    void getGuardianApplicationSummaries(id).then((response) => {
      setApplications(response)
    })
  }, [id, setApplications])

  const renderApplications = () =>
    isSuccess(applications)
      ? _.orderBy(
          applications.data,
          ['startDate', 'preferredUnitName'],
          ['desc', 'desc']
        ).map((application: ApplicationSummary) => {
          return (
            <Table.Row
              key={`${application.applicationId}`}
              dataQa="table-application-row"
            >
              <NameTd dataQa="application-child-name">
                <Link to={`/child-information/${application.childId}`}>
                  {application.childName}
                </Link>
              </NameTd>
              <Table.Td dataQa="application-preferred-unit-id">
                <Link to={`/units/${application.preferredUnitId}`}>
                  {application.preferredUnitName}
                </Link>
              </Table.Td>
              <DateTd dataQa="application-start-date">
                {application.startDate.format()}
              </DateTd>
              <DateTd dataQa="application-sent-date">
                {application.sentDate?.format()}
              </DateTd>
              <Table.Td dataQa="application-type">
                {
                  i18n.personProfile.application.types[
                    application.type.toUpperCase()
                  ]
                }
              </Table.Td>
              <StatusTd>
                {i18n.personProfile.application.statuses[application.status] ??
                  application.status}
              </StatusTd>
              <Table.Td>
                <Link to={`/applications/${application.applicationId}`}>
                  <IconButton
                    onClick={() => undefined}
                    icon={faFileAlt}
                    altText={i18n.personProfile.application.open}
                  />
                </Link>
              </Table.Td>
            </Table.Row>
          )
        })
      : null

  return (
    <div>
      <Collapsible
        icon={faFileAlt}
        title={i18n.personProfile.applications}
        open={toggled}
        onToggle={toggle}
        dataQa="person-applications-collapsible"
      >
        <Table.Table dataQa="table-of-applications">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.personProfile.application.child}</Table.Th>
              <Table.Th>
                {i18n.personProfile.application.preferredUnit}
              </Table.Th>
              <Table.Th>{i18n.personProfile.application.startDate}</Table.Th>
              <Table.Th>{i18n.personProfile.application.sentDate}</Table.Th>
              <Table.Th>{i18n.personProfile.application.type}</Table.Th>
              <Table.Th>{i18n.personProfile.application.status}</Table.Th>
              <Table.Th>{i18n.personProfile.application.open}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderApplications()}</Table.Body>
        </Table.Table>
        {isLoading(applications) && <Loader />}
        {isFailure(applications) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonApplications
