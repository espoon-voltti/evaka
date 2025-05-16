// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import type { Action } from 'lib-common/generated/action'
import type { Daycare } from 'lib-common/generated/api-types/daycare'
import type { MealReportRow } from 'lib-common/generated/api-types/reports'
import type { MealReportData } from 'lib-common/generated/api-types/reports'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H2 } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'
import { faFileExport } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { mealReportByUnitQuery, sendJamixOrdersMutation } from './queries'

function getWeekDates(date: LocalDate) {
  return Array.from({ length: 7 }, (_, i) => date.startOfWeek().addDays(i))
}

export default React.memo(function MealReport() {
  const { lang, i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const units = useQueryResult(
    daycaresQuery({ includeClosed: false })
  ).getOrElse([])
  const [date, setDate] = useState<LocalDate | null>(
    LocalDate.todayInHelsinkiTz()
  )
  const [wholeWeek, setWholeWeek] = useState<boolean>(false)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.meals.title}</Title>
        <FixedSpaceRow justifyContent="space-between" alignItems="flex-start">
          <div>
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
              <FilterLabel>{i18n.reports.common.date}</FilterLabel>
              <FlexRow>
                <DatePicker date={date} onChange={setDate} locale={lang} />
              </FlexRow>
            </FilterRow>
            <FilterRow>
              <FilterLabel>{i18n.reports.meals.wholeWeekLabel}</FilterLabel>
              <FlexRow>
                <Checkbox
                  label={i18n.reports.meals.wholeWeekLabel}
                  hiddenLabel={true}
                  checked={wholeWeek}
                  onChange={setWholeWeek}
                />
              </FlexRow>
            </FilterRow>
          </div>
        </FixedSpaceRow>

        {date &&
          selectedUnit &&
          (wholeWeek ? getWeekDates(date) : [date]).map((adate, idx) => (
            <MealReportData
              key={idx}
              date={adate}
              unitId={selectedUnit.id}
              permittedGlobalActions={user?.permittedGlobalActions ?? []}
            />
          ))}
      </ContentArea>
    </Container>
  )
})

const MealReportData = ({
  date,
  unitId,
  permittedGlobalActions
}: {
  date: LocalDate
  unitId: DaycareId
  permittedGlobalActions: Action.Global[]
}) => {
  const { i18n } = useTranslation()
  const reportResult = useQueryResult(mealReportByUnitQuery({ date, unitId }))
  return renderResult(reportResult, (report) => {
    const tableData = report.meals.map((mealRow) => ({
      ...mealRow,
      mealName: i18n.reports.meals.mealName[mealRow.mealType]
    }))

    // Shared column definitions for both ReportDownload and HTML-table
    const columns = [
      {
        label: i18n.reports.meals.headings.mealName,
        value: (row: MealReportRow) => i18n.reports.meals.mealName[row.mealType]
      },
      {
        label: i18n.reports.meals.headings.mealId,
        value: (row: MealReportRow) => row.mealId
      },
      {
        label: i18n.reports.meals.headings.mealCount,
        value: (row: MealReportRow) => row.mealCount
      },
      {
        label: i18n.reports.meals.headings.dietId,
        value: (row: MealReportRow) => row.dietId
      },
      {
        label: i18n.reports.meals.headings.dietAbbreviation,
        value: (row: MealReportRow) => row.dietAbbreviation
      },
      {
        label: i18n.reports.meals.headings.mealTextureId,
        value: (row: MealReportRow) => row.mealTextureId
      },
      {
        label: i18n.reports.meals.headings.mealTextureName,
        value: (row: MealReportRow) => row.mealTextureName
      },
      {
        label: i18n.reports.meals.headings.additionalInfo,
        value: (row: MealReportRow) => row.additionalInfo
      }
    ]
    return (
      <>
        <H2>
          {i18n.common.datetime.weekdays[report.date.getIsoDayOfWeek() - 1]}{' '}
          {report.date.format()}
        </H2>
        <FixedSpaceRow alignItems="center" justifyContent="right">
          {featureFlags.jamixIntegration === true &&
            permittedGlobalActions.includes('SEND_JAMIX_ORDERS') &&
            date.isEqualOrAfter(LocalDate.todayInHelsinkiTz().addDays(2)) &&
            tableData.length > 0 && (
              <ConfirmedMutation
                buttonStyle="INLINE"
                buttonText={i18n.reports.meals.jamixSend.button}
                icon={faFileExport}
                mutation={sendJamixOrdersMutation}
                onClick={() => ({ unitId, date })}
                onSuccess={() => undefined}
                confirmationTitle={
                  i18n.reports.meals.jamixSend.confirmationTitle
                }
                confirmLabel={i18n.common.send}
              />
            )}
          <ReportDownload
            data={tableData}
            columns={columns}
            filename={`${i18n.reports.meals.title} ${report.reportName} ${report.date.formatIso()}.csv`}
          />
        </FixedSpaceRow>
        <TableScrollable>
          <Thead>
            <Tr>
              {columns.map((column, columnIndex) => (
                <Th key={columnIndex}>{column.label}</Th>
              ))}
            </Tr>
          </Thead>
          <Tbody>
            {tableData.length > 0 ? (
              tableData.map((row, rowIndex) => (
                <Tr key={`${rowIndex}`}>
                  {columns.map((column, columnIndex) => (
                    <Td key={columnIndex}>{column.value(row)}</Td>
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
