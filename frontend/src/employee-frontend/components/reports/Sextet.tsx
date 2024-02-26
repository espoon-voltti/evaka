// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Success, wrapResult } from 'lib-common/api'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { getSextetReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { TableScrollable } from './common'

const getSextetReportResult = wrapResult(getSextetReport)

const years = (() => {
  const result: number[] = []
  for (let y = LocalDate.todayInSystemTz().year; y >= 2019; y--) {
    result.push(y)
  }
  return result
})()

const placementTypes: PlacementType[] = [
  'CLUB',
  'DAYCARE',
  'DAYCARE_PART_TIME',
  'DAYCARE_FIVE_YEAR_OLDS',
  'DAYCARE_PART_TIME_FIVE_YEAR_OLDS',
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'PRESCHOOL_CLUB',
  'PREPARATORY',
  'PREPARATORY_DAYCARE'
]

export default React.memo(function ReportSextet() {
  const { i18n } = useTranslation()

  const [year, setYear] = useState<number | null>(null)
  const [placementType, setPlacementType] = useState<PlacementType | null>(null)

  const [report] = useApiState(
    async () =>
      year !== null && placementType !== null
        ? await getSextetReportResult({ year, placementType })
        : Success.of([]),
    [year, placementType]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.sextet.title}</Title>

        <Combobox
          placeholder={i18n.reports.sextet.year}
          items={years}
          selectedItem={year}
          onChange={setYear}
        />
        <Combobox
          placeholder={i18n.reports.sextet.placementType}
          items={placementTypes}
          selectedItem={placementType}
          getItemLabel={(placementType) => i18n.placement.type[placementType]}
          onChange={setPlacementType}
        />

        {renderResult(report, (report) => (
          <>
            <ReportDownload
              data={report}
              headers={[
                { label: i18n.reports.sextet.unitName, key: 'unitName' },
                {
                  label: i18n.reports.sextet.placementType,
                  key: 'placementType'
                },
                {
                  label: i18n.reports.sextet.attendanceDays,
                  key: 'attendanceDays'
                }
              ]}
              filename={`kuusikkovertailu-${year ?? ''}-${
                placementType ?? ''
              }.csv`}
            />
            <TableScrollable data-qa="report-sextet-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.sextet.unitName}</Th>
                  <Th>{i18n.reports.sextet.placementType}</Th>
                  <Th>{i18n.reports.sextet.attendanceDays}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {report.map((row) => (
                  <Tr key={`${row.unitId}-${row.placementType}`}>
                    <Td>{row.unitName}</Td>
                    <Td>{i18n.placement.type[row.placementType]}</Td>
                    <Td>{row.attendanceDays}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
