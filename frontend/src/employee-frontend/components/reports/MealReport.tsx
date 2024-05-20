// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Daycare } from 'lib-common/generated/api-types/daycare'
import { MealReportData } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitsQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { mealReportByUnitQuery } from './queries'

export default React.memo(function MealReport() {
  const { lang, i18n } = useTranslation()

  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const units = useQueryResult(unitsQuery({ includeClosed: false })).getOrElse(
    []
  )
  const [date, setDate] = useState<LocalDate | null>(
    LocalDate.todayInHelsinkiTz()
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.meals.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <Combobox
              items={units}
              onChange={setSelectedUnit}
              selectedItem={selectedUnit}
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <DatePicker date={date} onChange={setDate} locale={lang} />
          </FlexRow>
        </FilterRow>
        {date && selectedUnit && (
          <MealReportData date={date} unitId={selectedUnit.id} />
        )}
      </ContentArea>
    </Container>
  )
})

const MealReportData = ({
  date,
  unitId
}: {
  date: LocalDate
  unitId: string
}) => {
  const { i18n } = useTranslation()
  const reportResult = useQueryResult(mealReportByUnitQuery({ date, unitId }))
  return renderResult(reportResult, (report) => {
    const tableData = report.meals.map((mealRow) => ({
      ...mealRow,
      mealName: i18n.reports.meals.mealName[mealRow.mealType]
    }))

    // Shared column definitions for both ReportDownload and HTML-table
    const columns = {
      mealName: i18n.reports.meals.headings.mealName,
      mealId: i18n.reports.meals.headings.mealId,
      mealCount: i18n.reports.meals.headings.mealCount,
      dietId: i18n.reports.meals.headings.dietId,
      dietAbbreviation: i18n.reports.meals.headings.dietAbbreviation,
      mealTextureId: i18n.reports.meals.headings.mealTextureId,
      mealTextureName: i18n.reports.meals.headings.mealTextureName,
      additionalInfo: i18n.reports.meals.headings.additionalInfo
    }
    const headers = Object.entries(columns).map(([columnKey, label]) => ({
      label: label,
      key: columnKey as keyof typeof columns
    }))
    return (
      <>
        <ReportDownload
          data={tableData}
          headers={headers}
          filename={`${i18n.reports.meals.title} ${report.reportName} ${report.date.formatIso()}.csv`}
        />
        <TableScrollable>
          <Thead>
            <Tr>
              {Object.values(columns).map((columnHeading, columnIndex) => (
                <Th key={columnIndex}>{columnHeading}</Th>
              ))}
            </Tr>
          </Thead>
          <Tbody>
            {tableData.length > 0 ? (
              tableData.map((row, rowIndex) => (
                <Tr key={`${rowIndex}`}>
                  {Object.keys(columns).map((columnKey, columnIndex) => (
                    <Th key={columnIndex}>
                      {row[columnKey as keyof typeof columns]}
                    </Th>
                  ))}
                </Tr>
              ))
            ) : (
              <Tr>
                <Td colSpan={4}>{i18n.common.noResults}</Td>
              </Tr>
            )}
          </Tbody>
        </TableScrollable>
      </>
    )
  })
}
