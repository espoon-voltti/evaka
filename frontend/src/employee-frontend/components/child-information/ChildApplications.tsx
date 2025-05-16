// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext } from 'react'
import { Link } from 'react-router'

import type { PersonApplicationSummary } from 'lib-common/generated/api-types/application'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { faFileAlt } from 'lib-icons'

import CreateApplicationModal from '../../components/child-information/CreateApplicationModal'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { RequireRole } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import { inferApplicationType } from '../person-profile/PersonApplications'
import { DateTd, NameTd, StatusTd } from '../person-profile/common'

import { getChildApplicationSummariesQuery, guardiansQuery } from './queries'

interface Props {
  childId: ChildId
}

export default React.memo(function ChildApplications({ childId }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const { permittedActions, person } = useContext(ChildContext)

  const guardians = useQueryResult(
    permittedActions.has('READ_GUARDIANS')
      ? guardiansQuery({ personId: childId })
      : constantQuery(null)
  )
  const applications = useQueryResult(
    getChildApplicationSummariesQuery({ childId })
  )

  return (
    <>
      <RequireRole oneOf={['SERVICE_WORKER', 'ADMIN']}>
        <AddButtonRow
          text={i18n.childInformation.application.create.createButton}
          onClick={() => toggleUiMode('create-new-application')}
          data-qa="button-create-application"
        />
      </RequireRole>

      {renderResult(applications, (applications) => (
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
          <Tbody>
            {orderBy(
              applications,
              ['preferredStartDate', 'preferredUnitName'],
              ['desc', 'desc']
            ).map((application: PersonApplicationSummary) => (
              <Tr
                key={application.applicationId}
                data-qa="table-application-row"
              >
                <NameTd data-qa="application-guardian-name">
                  <Link to={`/profile/${application.guardianId}`}>
                    {application.guardianName}
                  </Link>
                </NameTd>
                <Td data-qa="application-preferred-unit-id">
                  <Link to={`/units/${application.preferredUnitId ?? ''}`}>
                    {application.preferredUnitName}
                  </Link>
                </Td>
                <DateTd data-qa="application-start-date">
                  {application.preferredStartDate?.format()}
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
                  {i18n.personProfile.application.statuses[
                    application.status
                  ] ?? application.status}
                </StatusTd>
                <Td>
                  <Link to={`/applications/${application.applicationId}`}>
                    <IconOnlyButton
                      onClick={() => undefined}
                      icon={faFileAlt}
                      aria-label={i18n.personProfile.application.open}
                    />
                  </Link>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      ))}

      {uiMode === 'create-new-application' &&
        person.isSuccess &&
        guardians.isSuccess && (
          <CreateApplicationModal
            child={person.value}
            guardians={guardians.map((g) => g?.guardians ?? []).getOrElse([])}
          />
        )}
    </>
  )
})
