// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'
import { Link } from 'react-router'

import {
  ApplicationType,
  PersonApplicationSummary
} from 'lib-common/generated/api-types/application'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { DateTd, NameTd, StatusTd } from './common'
import { guardianApplicationSummariesQuery } from './queries'

interface Props {
  id: PersonId
}

export default React.memo(function PersonApplications({ id }: Props) {
  const { i18n } = useTranslation()
  const applications = useQueryResult(
    guardianApplicationSummariesQuery({ guardianId: id })
  )

  return renderResult(applications, (applications) => (
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
      <Tbody>
        {orderBy(
          applications,
          ['preferredStartDate', 'preferredUnitName'],
          ['desc', 'desc']
        ).map((application: PersonApplicationSummary) => (
          <Tr key={application.applicationId} data-qa="table-application-row">
            <NameTd data-qa="application-child-name">
              <Link to={`/child-information/${application.childId}`}>
                {application.childName}
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
              {i18n.personProfile.application.statuses[application.status] ??
                application.status}
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
  ))
})

export type InferredApplicationType =
  | ApplicationType
  | 'PRESCHOOL_WITH_DAYCARE'
  | 'PREPARATORY_WITH_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export function inferApplicationType(
  application: PersonApplicationSummary
): InferredApplicationType {
  const baseType = application.type
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
