// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFileAlt } from '@evaka/icons'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { useContext } from 'react'
import { Loader, Table } from '~components/shared/alpha'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'
import { getChildApplicationSummaries } from '~api/person'
import { ApplicationSummary } from '~types/application'
import { DateTd, NameTd, StatusTd } from '~components/PersonProfile'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import { ChildContext } from 'state'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { RequireRole } from 'utils/roles'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import { UIContext } from 'state/ui'
import CreateApplicationModal from 'components/child-information/CreateApplicationModal'

interface Props {
  id: UUID
  open: boolean
}

const ChildApplications = React.memo(function ChildApplications({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { applications, setApplications, person, guardians } = useContext(
    ChildContext
  )
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const loadData = () => {
    setApplications(Loading())
    void getChildApplicationSummaries(id).then(setApplications)
  }

  useEffect(loadData, [id])

  function renderApplications() {
    if (isLoading(applications)) {
      return <Loader />
    } else if (isFailure(applications)) {
      return <div>{i18n.common.loadingFailed}</div>
    } else
      return _.orderBy(
        applications.data,
        ['startDate', 'preferredUnitName'],
        ['desc', 'desc']
      ).map((application: ApplicationSummary) => {
        return (
          <Table.Row
            key={`${application.applicationId}`}
            dataQa="table-application-row"
          >
            <NameTd dataQa="application-guardian-name">
              <Link to={`/profile/${application.guardianId}`}>
                {application.guardianName}
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
  }

  return (
    <CollapsibleSection
      dataQa="applications-collapsible"
      icon={faFileAlt}
      title={i18n.childInformation.application.title}
      startCollapsed={!open}
    >
      <RequireRole oneOf={['SERVICE_WORKER', 'ADMIN']}>
        <AddButtonRow
          text={i18n.childInformation.application.create.createButton}
          onClick={() => toggleUiMode('create-new-application')}
          dataQa="button-create-application"
        />
      </RequireRole>

      {isLoading(applications) && <Loader />}
      {isFailure(applications) && <div>{i18n.common.loadingFailed}</div>}
      {isSuccess(applications) && (
        <Table.Table dataQa="table-of-applications">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.childInformation.application.guardian}</Table.Th>
              <Table.Th>
                {i18n.childInformation.application.preferredUnit}
              </Table.Th>
              <Table.Th>{i18n.childInformation.application.startDate}</Table.Th>
              <Table.Th>{i18n.childInformation.application.sentDate}</Table.Th>
              <Table.Th>{i18n.childInformation.application.type}</Table.Th>
              <Table.Th>{i18n.childInformation.application.status}</Table.Th>
              <Table.Th>{i18n.childInformation.application.open}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderApplications()}</Table.Body>
        </Table.Table>
      )}

      {uiMode === 'create-new-application' &&
        isSuccess(person) &&
        !isLoading(guardians) && (
          <CreateApplicationModal
            child={person.data}
            guardians={isSuccess(guardians) ? guardians.data : []}
          />
        )}
    </CollapsibleSection>
  )
})

export default ChildApplications
