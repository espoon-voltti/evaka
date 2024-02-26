// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import { PersonApplicationSummary } from 'lib-common/generated/api-types/application'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'
import { faFileAlt } from 'lib-icons'

import CreateApplicationModal from '../../components/child-information/CreateApplicationModal'
import { getChildApplicationSummaries } from '../../generated/api-clients/application'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { RequireRole } from '../../utils/roles'
import { DateTd, NameTd, StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'
import { inferApplicationType } from '../person-profile/PersonApplications'

const getChildApplicationSummariesResult = wrapResult(
  getChildApplicationSummaries
)

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function ChildApplications({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { person, guardians } = useContext(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [applications] = useApiState(
    () => getChildApplicationSummariesResult({ childId: id }),
    [id]
  )
  const [open, setOpen] = useState(startOpen)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.application.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="applications-collapsible"
    >
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
                    <IconButton
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
        !guardians.isLoading && (
          <CreateApplicationModal
            child={person.value}
            guardians={guardians.getOrElse([])}
          />
        )}
    </CollapsibleContentArea>
  )
})
