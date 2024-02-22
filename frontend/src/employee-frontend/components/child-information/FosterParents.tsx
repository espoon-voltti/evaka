// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H3 } from 'lib-components/typography'

import { getFosterParents } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'
import { NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'
import StatusLabel from '../common/StatusLabel'

const getFosterParentsResult = wrapResult(getFosterParents)

interface Props {
  id: UUID
}

export default React.memo(function FosterParents({ id }: Props) {
  const { i18n } = useTranslation()
  const [fosterParents] = useApiState(
    () => getFosterParentsResult({ childId: id }),
    [id]
  )

  return (
    <>
      <H3 noMargin>{i18n.personProfile.fosterParents}</H3>
      {renderResult(fosterParents, (fosterParents) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.fosterParents.name}</Th>
              <Th>{i18n.childInformation.fosterParents.ssn}</Th>
              <Th>{i18n.childInformation.fosterParents.startDate}</Th>
              <Th>{i18n.childInformation.fosterParents.endDate}</Th>
              <Th>{i18n.childInformation.fosterParents.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(
              fosterParents,
              [
                ({ parent }) => parent.lastName,
                ({ parent }) => parent.firstName
              ],
              ['asc']
            ).map(({ relationshipId, parent, validDuring }) => (
              <Tr
                key={relationshipId}
                data-qa={`foster-parent-row-${parent.id}`}
              >
                <NameTd>
                  <Link to={`/profile/${parent.id}`}>
                    {parent.lastName} {parent.firstName}
                  </Link>
                </NameTd>
                <Td>{parent.socialSecurityNumber}</Td>
                <Td data-qa="start">{validDuring.start.format()}</Td>
                <Td data-qa="end">{validDuring.end?.format() ?? ''}</Td>
                <Td>
                  <StatusLabel
                    status={getStatusLabelByDateRange({
                      startDate: validDuring.start,
                      endDate: validDuring.end
                    })}
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      ))}
    </>
  )
})
