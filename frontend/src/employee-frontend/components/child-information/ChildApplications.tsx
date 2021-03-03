// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'

import { faFileAlt } from '@evaka/lib-icons'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading } from '@evaka/lib-common/api'
import { useContext } from 'react'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/layout/Table'
import Loader from '@evaka/lib-components/atoms/Loader'
import { getChildApplicationSummaries } from '../../api/person'
import { ApplicationSummary } from '../../types/application'
import { DateTd, NameTd, StatusTd } from '../../components/PersonProfile'
import IconButton from '@evaka/lib-components/atoms/buttons/IconButton'
import { ChildContext } from '../../state'
import CollapsibleSection from '@evaka/lib-components/molecules/CollapsibleSection'
import { RequireRole } from '../../utils/roles'
import { AddButtonRow } from '@evaka/lib-components/atoms/buttons/AddButton'
import { UIContext } from '../../state/ui'
import CreateApplicationModal from '../../components/child-information/CreateApplicationModal'
import { inferApplicationType } from '../../components/person-profile/PersonApplications'

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
    setApplications(Loading.of())
    void getChildApplicationSummaries(id).then(setApplications)
  }

  useEffect(loadData, [id])

  function renderApplications() {
    if (applications.isLoading) {
      return <Loader />
    } else if (applications.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else
      return _.orderBy(
        applications.value,
        ['startDate', 'preferredUnitName'],
        ['desc', 'desc']
      ).map((application: ApplicationSummary) => {
        return (
          <Tr
            key={`${application.applicationId}`}
            data-qa="table-application-row"
          >
            <NameTd data-qa="application-guardian-name">
              <Link to={`/profile/${application.guardianId}`}>
                {application.guardianName}
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
  }

  return (
    <CollapsibleSection
      data-qa="applications-collapsible"
      icon={faFileAlt}
      title={i18n.childInformation.application.title}
      startCollapsed={!open}
    >
      <RequireRole oneOf={['SERVICE_WORKER', 'ADMIN']}>
        <AddButtonRow
          text={i18n.childInformation.application.create.createButton}
          onClick={() => toggleUiMode('create-new-application')}
          data-qa="button-create-application"
        />
      </RequireRole>

      {applications.isLoading && <Loader />}
      {applications.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {applications.isSuccess && (
        <Table data-qa="table-of-applications">
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.application.guardian}</Th>
              <Th>{i18n.childInformation.application.preferredUnit}</Th>
              <Th>{i18n.childInformation.application.startDate}</Th>
              <Th>{i18n.childInformation.application.sentDate}</Th>
              <Th>{i18n.childInformation.application.type}</Th>
              <Th>{i18n.childInformation.application.status}</Th>
              <Th>{i18n.childInformation.application.open}</Th>
            </Tr>
          </Thead>
          <Tbody>{renderApplications()}</Tbody>
        </Table>
      )}

      {uiMode === 'create-new-application' &&
        person.isSuccess &&
        !guardians.isLoading && (
          <CreateApplicationModal
            child={person.value}
            guardians={guardians.getOrElse([])}
          />
        )}
    </CollapsibleSection>
  )
})

export default ChildApplications
