// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { Loading, Result } from 'lib-common/api'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { useTranslation } from '../../state/i18n'
import { InvalidServiceNeedReportRow } from '../../types/reports'
import { getInvalidServiceNeedReport } from '../../api/reports'
import { RowCountInfo, TableScrollable } from './common'
import { getEmployeeUrlPrefix } from '../../constants'

function InvalidServiceNeed() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<InvalidServiceNeedReportRow[]>>(
    Loading.of()
  )

  useEffect(() => {
    setRows(Loading.of())
    void getInvalidServiceNeedReport().then(setRows)
  }, [])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.invalidServiceNeed.title}</Title>

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>{i18n.reports.common.period}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row: InvalidServiceNeedReportRow) => (
                  <Tr
                    key={`${row.unitId}:${
                      row.childId
                    }:${row.startDate.formatIso()}`}
                  >
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <a
                        href={`${getEmployeeUrlPrefix()}/employee/units/${
                          row.unitId
                        }`}
                        target={'_blank'}
                        rel={'noreferrer'}
                      >
                        {row.unitName}
                      </a>
                    </Td>
                    <Td>
                      <a
                        href={`${getEmployeeUrlPrefix()}/employee/child-information/${
                          row.childId
                        }`}
                        target={'_blank'}
                        rel={'noreferrer'}
                      >
                        {row.lastName} {row.firstName}
                      </a>
                    </Td>
                    <Td>
                      {row.startDate.format()} - {row.endDate.format()}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={rows.value.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default InvalidServiceNeed
