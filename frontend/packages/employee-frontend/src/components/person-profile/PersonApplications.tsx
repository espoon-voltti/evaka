// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'

import { faFileAlt } from '@evaka/lib-icons'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { Loading } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { Table, Tbody, Td, Th, Thead, Tr } from 'components/shared/layout/Table'
import Loader from '~components/shared/atoms/Loader'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
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

  useEffect(() => {
    setApplications(Loading.of())
    void getGuardianApplicationSummaries(id).then((response) => {
      setApplications(response)
    })
  }, [id, setApplications])

  const renderApplications = () =>
    applications.isSuccess
      ? _.orderBy(
          applications.value,
          ['startDate', 'preferredUnitName'],
          ['desc', 'desc']
        ).map((application: ApplicationSummary) => {
          return (
            <Tr
              key={`${application.applicationId}`}
              data-qa="table-application-row"
            >
              <NameTd data-qa="application-child-name">
                <Link to={`/child-information/${application.childId}`}>
                  {application.childName}
                </Link>
              </NameTd>
              <Td data-qa="application-preferred-unit-id">
                <Link to={`/units/${application.preferredUnitId}`}>
                  {application.preferredUnitName}
                </Link>
              </Td>
              <DateTd data-qa="application-start-date">
                {application.startDate.format()}
              </DateTd>
              <DateTd data-qa="application-sent-date">
                {application.sentDate?.format()}
              </DateTd>
              <Td data-qa="application-type">
                {
                  i18n.personProfile.application.types[
                    inferApplicationType(application)
                  ]
                }
              </Td>
              <StatusTd>
                {i18n.personProfile.application.statuses[application.status] ??
                  application.status}
              </StatusTd>
              <Td>
                <Link to={`/applications/${application.applicationId}`}>
                  <IconButton
                    onClick={() => undefined}
                    icon={faFileAlt}
                    altText={i18n.personProfile.application.open}
                  />
                </Link>
              </Td>
            </Tr>
          )
        })
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faFileAlt}
        title={i18n.personProfile.applications}
        startCollapsed={!open}
        dataQa="person-applications-collapsible"
      >
        <Table data-qa="table-of-applications">
          <Thead>
            <Tr>
              <Th>{i18n.personProfile.application.child}</Th>
              <Th>{i18n.personProfile.application.preferredUnit}</Th>
              <Th>{i18n.personProfile.application.startDate}</Th>
              <Th>{i18n.personProfile.application.sentDate}</Th>
              <Th>{i18n.personProfile.application.type}</Th>
              <Th>{i18n.personProfile.application.status}</Th>
              <Th>{i18n.personProfile.application.open}</Th>
            </Tr>
          </Thead>
          <Tbody>{renderApplications()}</Tbody>
        </Table>
        {applications.isLoading && <Loader />}
        {applications.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonApplications

export function inferApplicationType(application: ApplicationSummary) {
  const baseType = application.type.toUpperCase()
  if (baseType !== 'PRESCHOOL') return baseType
  else if (application.connectedDaycare && !application.preparatoryEducation) {
    return 'PRESCHOOL_WITH_DAYCARE'
  } else if (application.connectedDaycare && application.preparatoryEducation) {
    return 'PREPARATORY_WITH_DAYCARE'
  } else if (
    !application.connectedDaycare &&
    application.preparatoryEducation
  ) {
    return 'PREPARATORY_EDUCATION'
  } else return 'PRESCHOOL'
}
