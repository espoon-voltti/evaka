// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import {
  ApplicationType,
  PersonApplicationSummary
} from 'lib-common/generated/api-types/application'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'
import { faFileAlt } from 'lib-icons'

import { getGuardianApplicationSummaries } from '../../generated/api-clients/application'
import { useTranslation } from '../../state/i18n'
import { DateTd, NameTd, StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

const getGuardianApplicationSummariesResult = wrapResult(
  getGuardianApplicationSummaries
)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonApplications({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const [applications] = useApiState(
    () => getGuardianApplicationSummariesResult({ guardianId: id }),
    [id]
  )

  return (
    <div>
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.applications}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-applications-collapsible"
      >
        {renderResult(applications, (applications) => (
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
                <Tr
                  key={application.applicationId}
                  data-qa="table-application-row"
                >
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
      </CollapsibleContentArea>
    </div>
  )
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
