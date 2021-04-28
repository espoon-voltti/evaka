// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H4 } from 'lib-components/typography'
import React from 'react'
import { NewServiceNeed } from '../../../types/child'
import _ from 'lodash'
import StatusLabel from '../../common/StatusLabel'
import { getStatusLabelByDateRange } from '../../../utils/date'
import styled from 'styled-components'
import { defaultMargins } from '../../../../lib-components/white-space'
import { useTranslation } from '../../../state/i18n'
interface Props {
  serviceNeeds: NewServiceNeed[]
}

function NewServiceNeeds({ serviceNeeds }: Props) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  return (
    <IndentedSection>
      <H4 noMargin>{t.title}</H4>
      {serviceNeeds.length === 0 ? (
        <div>{t.noServiceNeeds}</div>
      ) : (
        <Table>
          <Thead>
            <Tr>
              <Th>{t.period}</Th>
              <Th>{t.description}</Th>
              <Th>{t.shiftCare}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {_.orderBy(serviceNeeds, ['startDate'], ['desc']).map((sn) => (
              <Tr key={sn.id}>
                <Td>
                  {sn.startDate.format()} - {sn.endDate.format()}
                </Td>
                <Td>{sn.option.name}</Td>
                <Td>{sn.shiftCare ? i18n.common.yes : i18n.common.no}</Td>
                <Td>
                  <StatusLabel status={getStatusLabelByDateRange(sn)} />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </IndentedSection>
  )
}

const IndentedSection = styled.div`
  padding-left: ${defaultMargins.L};
  width: 100%;
`

export default NewServiceNeeds
