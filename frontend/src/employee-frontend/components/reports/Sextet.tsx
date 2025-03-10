// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Tfoot, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { unitProviderTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { TableScrollable } from './common'
import { sextetReport } from './queries'

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
  const [selectedUnitProviderTypes, setSelectedUnitProviderTypes] = useState<
    ProviderType[]
  >([])

  const report = useQueryResult(
    year !== null && placementType !== null
      ? sextetReport({
          year,
          placementType,
          unitProviderTypes: selectedUnitProviderTypes
        })
      : constantQuery([])
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
          data-qa="filter-year"
        />
        <Combobox
          placeholder={i18n.reports.sextet.placementType}
          items={placementTypes}
          selectedItem={placementType}
          getItemLabel={(placementType) => i18n.placement.type[placementType]}
          onChange={setPlacementType}
          data-qa="filter-placement-type"
          getItemDataQa={(placementType) =>
            `filter-placement-type-${placementType}`
          }
        />
        <FixedSpaceRow>
          {unitProviderTypes.map((type) => (
            <Checkbox
              key={type}
              label={i18n.common.providerType[type]}
              checked={selectedUnitProviderTypes.includes(type)}
              onChange={(checked) =>
                setSelectedUnitProviderTypes(
                  checked
                    ? [...selectedUnitProviderTypes, type]
                    : selectedUnitProviderTypes.filter((t) => t !== type)
                )
              }
              data-qa={`filter-unit-provider-type-${type}`}
            />
          ))}
        </FixedSpaceRow>

        {renderResult(report, (report) => (
          <>
            <ReportDownload
              data={report}
              columns={[
                {
                  label: i18n.reports.sextet.unitName,
                  value: (row) => row.unitName
                },
                {
                  label: i18n.reports.sextet.placementType,
                  value: (row) => row.placementType
                },
                {
                  label: i18n.reports.sextet.attendanceDays,
                  value: (row) => row.attendanceDays
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
                  <Tr
                    key={`${row.unitId}-${row.placementType}`}
                    data-qa="data-rows"
                  >
                    <Td>{row.unitName}</Td>
                    <Td>{i18n.placement.type[row.placementType]}</Td>
                    <Td>{row.attendanceDays}</Td>
                  </Tr>
                ))}
              </Tbody>
              {report.length > 0 && (
                <Tfoot>
                  <Tr>
                    <Th />
                    <Th />
                    <Th data-qa="data-sum">
                      {report.reduce((sum, row) => sum + row.attendanceDays, 0)}
                    </Th>
                  </Tr>
                </Tfoot>
              )}
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
