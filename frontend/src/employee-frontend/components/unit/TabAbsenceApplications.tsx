// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useMemo } from 'react'
import { Link } from 'react-router'

import { useBoolean } from 'lib-common/form/hooks'
import type { AbsenceApplicationSummaryEmployee } from 'lib-common/generated/api-types/absence'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { formatPersonName } from '../../utils'
import { NotificationCounter } from '../UnitPage'
import { renderResult } from '../async-rendering'
import { getAbsenceApplicationsQuery } from '../child-information/queries'

interface Props {
  unitId: DaycareId
}

export const TabAbsenceApplications = (props: Props) => {
  const { i18n } = useTranslation()
  const [open, { toggle: toggleOpen }] = useBoolean(true)
  const applicationsResult = useQueryResult(
    getAbsenceApplicationsQuery({
      unitId: props.unitId,
      status: 'WAITING_DECISION'
    })
  )

  return (
    <CollapsibleContentArea
      title={
        <Title size={2}>
          {i18n.childInformation.absenceApplications.title}
          {applicationsResult.isSuccess &&
          applicationsResult.value.length > 0 ? (
            <NotificationCounter>
              {applicationsResult.value.length}
            </NotificationCounter>
          ) : null}
        </Title>
      }
      opaque
      open={open}
      toggleOpen={toggleOpen}
    >
      {renderResult(applicationsResult, (applications) => (
        <AbsenceApplicationTable applications={applications} />
      ))}
    </CollapsibleContentArea>
  )
}

const AbsenceApplicationTable = (props: {
  applications: AbsenceApplicationSummaryEmployee[]
}) => {
  const { i18n } = useTranslation()
  const sortedApplications = useMemo(
    () =>
      sortBy(props.applications, (application) => application.data.createdAt),
    [props.applications]
  )

  return (
    <Table data-qa="absence-applications-table">
      <Thead>
        <Tr>
          <Th>{i18n.childInformation.personDetails.name}</Th>
          <Th>{i18n.childInformation.absenceApplications.range}</Th>
          <Th>{i18n.childInformation.absenceApplications.description}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {sortedApplications.map((application) => (
          <Tr key={application.data.id} data-qa="absence-application-row">
            <Td data-qa="child-name">
              <Link to={`/child-information/${application.data.child.id}`}>
                {formatPersonName(application.data.child, i18n, true)}
              </Link>
            </Td>
            <Td data-qa="range">
              {application.data.startDate.format()} -{' '}
              {application.data.endDate.format()}
            </Td>
            <Td data-qa="description">{application.data.description}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}
