// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import {
  DaycareAssistanceLevel,
  OtherAssistanceMeasureType,
  PreschoolAssistanceLevel
} from 'lib-common/generated/api-types/assistance'
import {
  AssistanceNeedsAndActionsReport,
  AssistanceNeedsAndActionsReportRow
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import {
  daycareAssistanceLevels,
  featureFlags,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels
} from 'lib-customizations/employee'

import {
  AssistanceNeedsAndActionsReportFilters,
  getAssistanceNeedsAndActionsReport
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { distinct, reducePropertySum } from '../../utils'

import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'

const types = ['DAYCARE', 'PRESCHOOL'] as const
type Type = (typeof types)[number]

const daycareColumns = [
  ...daycareAssistanceLevels,
  ...otherAssistanceMeasureTypes
]
const preschoolColumns = [
  ...preschoolAssistanceLevels,
  ...otherAssistanceMeasureTypes
]

interface RowFilters {
  careArea: string
  unit: string
}

const emptyRowFilters: RowFilters = {
  careArea: '',
  unit: ''
}

interface ColumnFilters {
  type: Type
  daycareColumns: (DaycareAssistanceLevel | OtherAssistanceMeasureType)[]
  preschoolColumns: (PreschoolAssistanceLevel | OtherAssistanceMeasureType)[]
}

const emptyColumnFilters: ColumnFilters = {
  type: 'DAYCARE',
  daycareColumns: [],
  preschoolColumns: []
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function AssistanceNeedsAndActions() {
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<AssistanceNeedsAndActionsReport>>(
    Loading.of()
  )
  const [filters, setFilters] =
    useState<AssistanceNeedsAndActionsReportFilters>({
      date: LocalDate.todayInSystemTz()
    })

  const [rowFilters, setRowFilters] = useState<RowFilters>(emptyRowFilters)
  const rowFilter = (row: AssistanceNeedsAndActionsReportRow): boolean =>
    !(rowFilters.careArea && row.careAreaName !== rowFilters.careArea) &&
    !(rowFilters.unit && row.unitName !== rowFilters.unit)
  const [columnFilters, setColumnFilters] =
    useState<ColumnFilters>(emptyColumnFilters)

  useEffect(() => {
    setReport(Loading.of())
    setRowFilters(emptyRowFilters)
    void getAssistanceNeedsAndActionsReport(filters).then(setReport)
  }, [filters])

  const filteredRows: AssistanceNeedsAndActionsReportRow[] = useMemo(
    () => report.map((rs) => rs.rows.filter(rowFilter)).getOrElse([]),
    [report, rowFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const selectedDaycareColumns = useMemo(
    () =>
      columnFilters.type === 'DAYCARE'
        ? columnFilters.daycareColumns.length === 0
          ? daycareAssistanceLevels
          : daycareAssistanceLevels.filter((level) =>
              columnFilters.daycareColumns.includes(level)
            )
        : [],
    [columnFilters.daycareColumns, columnFilters.type]
  )
  const selectedPreschoolColumns = useMemo(
    () =>
      columnFilters.type === 'PRESCHOOL'
        ? columnFilters.preschoolColumns.length === 0
          ? preschoolAssistanceLevels
          : preschoolAssistanceLevels.filter((level) =>
              columnFilters.preschoolColumns.includes(level)
            )
        : [],
    [columnFilters.preschoolColumns, columnFilters.type]
  )
  const selectedOtherColumns = useMemo(
    () =>
      otherAssistanceMeasureTypes.filter(
        (type) =>
          (columnFilters.type === 'DAYCARE' &&
            (columnFilters.daycareColumns.length === 0 ||
              columnFilters.daycareColumns.includes(type))) ||
          (columnFilters.type === 'PRESCHOOL' &&
            (columnFilters.preschoolColumns.length === 0 ||
              columnFilters.preschoolColumns.includes(type)))
      ),
    [
      columnFilters.daycareColumns,
      columnFilters.preschoolColumns,
      columnFilters.type
    ]
  )

  const daycareColumnTexts = {
    ...i18n.childInformation.assistance.types.daycareAssistanceLevel,
    ...i18n.childInformation.assistance.types.otherAssistanceMeasureType
  }
  const preschoolColumnTexts = {
    ...i18n.childInformation.assistance.types.preschoolAssistanceLevel,
    ...i18n.childInformation.assistance.types.otherAssistanceMeasureType
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.assistanceNeedsAndActions.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePickerDeprecated
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                { value: '', label: i18n.common.all },
                ...report
                  .map((rs) =>
                    distinct(rs.rows.map((row) => row.careAreaName)).map(
                      (s) => ({
                        value: s,
                        label: s
                      })
                    )
                  )
                  .getOrElse([])
              ]}
              onChange={(option) =>
                option
                  ? setRowFilters({
                      ...rowFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              selectedItem={
                rowFilters.careArea !== ''
                  ? {
                      label: rowFilters.careArea,
                      value: rowFilters.careArea
                    }
                  : {
                      label: i18n.common.all,
                      value: ''
                    }
              }
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
              getItemLabel={(item) => item.label}
            />
          </Wrapper>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                { value: '', label: i18n.common.all },
                ...report
                  .map((rs) =>
                    distinct(rs.rows.map((row) => row.unitName)).map((s) => ({
                      value: s,
                      label: s
                    }))
                  )
                  .getOrElse([])
              ]}
              onChange={(option) =>
                option
                  ? setRowFilters({
                      ...rowFilters,
                      unit: option.value
                    })
                  : undefined
              }
              selectedItem={
                rowFilters.unit !== ''
                  ? {
                      label: rowFilters.unit,
                      value: rowFilters.unit
                    }
                  : {
                      label: i18n.common.all,
                      value: ''
                    }
              }
              getItemLabel={(item) => item.label}
            />
          </Wrapper>
        </FilterRow>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.assistanceNeedsAndActions.type}
          </FilterLabel>
          <Wrapper>
            <Combobox
              items={types}
              onChange={(option) =>
                option
                  ? setColumnFilters({
                      ...columnFilters,
                      type: option
                    })
                  : undefined
              }
              selectedItem={columnFilters.type}
              getItemLabel={(item) =>
                i18n.reports.assistanceNeedsAndActions.types[item]
              }
            />
          </Wrapper>
        </FilterRow>

        {columnFilters.type === 'DAYCARE' && (
          <FilterRow>
            <FilterLabel>
              {i18n.reports.assistanceNeedsAndActions.level}
            </FilterLabel>
            <Wrapper>
              <MultiSelect
                options={daycareColumns}
                onChange={(selectedItems) =>
                  setColumnFilters({
                    ...columnFilters,
                    daycareColumns: selectedItems.map(
                      (selectedItem) => selectedItem
                    )
                  })
                }
                value={columnFilters.daycareColumns}
                getOptionId={(level) => level}
                getOptionLabel={(level) => daycareColumnTexts[level]}
                placeholder={i18n.common.all}
              />
            </Wrapper>
          </FilterRow>
        )}

        {columnFilters.type === 'PRESCHOOL' && (
          <FilterRow>
            <FilterLabel>
              {i18n.reports.assistanceNeedsAndActions.level}
            </FilterLabel>
            <Wrapper>
              <MultiSelect
                options={preschoolColumns}
                onChange={(selectedItems) =>
                  setColumnFilters({
                    ...columnFilters,
                    preschoolColumns: selectedItems.map(
                      (selectedItem) => selectedItem
                    )
                  })
                }
                value={columnFilters.preschoolColumns}
                getOptionId={(level) => level}
                getOptionLabel={(level) => preschoolColumnTexts[level]}
                placeholder={i18n.common.all}
              />
            </Wrapper>
          </FilterRow>
        )}

        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload<Record<string, unknown>>
              data={filteredRows.map((row) =>
                /* eslint-disable-next-line @typescript-eslint/no-unsafe-return */
                ({
                  ...row,
                  ...Object.fromEntries([
                    ...selectedDaycareColumns.map((level) => [
                      `DAYCARE-ASSISTANCE-${level}`,
                      row.daycareAssistanceCounts[level] ?? 0
                    ]),
                    ...selectedPreschoolColumns.map((level) => [
                      `PRESCHOOL-ASSISTANCE-${level}`,
                      row.preschoolAssistanceCounts[level] ?? 0
                    ]),
                    ...selectedOtherColumns.map((type) => [
                      `OTHER-ASSISTANCE-MEASURE-${type}`,
                      row.otherAssistanceMeasureCounts[type] ?? 0
                    ]),
                    ...report.value.actions.map(({ value }) => [
                      `ACTION-${value}`,
                      row.actionCounts[value] ?? 0
                    ])
                  ])
                })
              )}
              headers={[
                {
                  label: i18n.reports.common.careAreaName,
                  key: 'careAreaName'
                },
                {
                  label: i18n.reports.common.unitName,
                  key: 'unitName'
                },
                {
                  label: i18n.reports.common.groupName,
                  key: 'groupName'
                },
                ...selectedDaycareColumns.map((level) => ({
                  label:
                    i18n.childInformation.assistance.types
                      .daycareAssistanceLevel[level],
                  key: `DAYCARE-ASSISTANCE-${level}`
                })),
                ...selectedPreschoolColumns.map((level) => ({
                  label:
                    i18n.childInformation.assistance.types
                      .preschoolAssistanceLevel[level],
                  key: `PRESCHOOL-ASSISTANCE-${level}`
                })),
                ...selectedOtherColumns.map((type) => ({
                  label:
                    i18n.childInformation.assistance.types
                      .otherAssistanceMeasureType[type],
                  key: `OTHER-ASSISTANCE-MEASURE-${type}`
                })),
                ...report.value.actions.map((action) => ({
                  label: action.nameFi,
                  key: `ACTION-${action.value}`
                })),
                ...(featureFlags.assistanceActionOther
                  ? [
                      {
                        label:
                          i18n.childInformation.assistanceAction.fields
                            .actionTypes.OTHER,
                        key: 'otherActionCount'
                      }
                    ]
                  : []),
                {
                  label: i18n.reports.assistanceNeedsAndActions.actionMissing,
                  key: 'noActionCount'
                }
              ]}
              filename={`Lapsien tuentarpeet ja tukitoimet yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  {selectedDaycareColumns.map((level) => (
                    <Th key={level}>
                      {
                        i18n.childInformation.assistance.types
                          .daycareAssistanceLevel[level]
                      }
                    </Th>
                  ))}
                  {selectedPreschoolColumns.map((level) => (
                    <Th key={level}>
                      {
                        i18n.childInformation.assistance.types
                          .preschoolAssistanceLevel[level]
                      }
                    </Th>
                  ))}
                  {selectedOtherColumns.map((type) => (
                    <Th key={type}>
                      {
                        i18n.childInformation.assistance.types
                          .otherAssistanceMeasureType[type]
                      }
                    </Th>
                  ))}
                  {report.value.actions.map((action) => (
                    <Th key={action.value}>{action.nameFi}</Th>
                  ))}
                  {featureFlags.assistanceActionOther && (
                    <Th>
                      {
                        i18n.childInformation.assistanceAction.fields
                          .actionTypes.OTHER
                      }
                    </Th>
                  )}
                  <Th>
                    {i18n.reports.assistanceNeedsAndActions.actionMissing}
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: AssistanceNeedsAndActionsReportRow) => (
                  <Tr key={`${row.unitId}:${row.groupId}`}>
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td>{row.groupName}</Td>
                    {selectedDaycareColumns.map((level) => (
                      <Td key={level}>
                        {row.daycareAssistanceCounts[level] ?? 0}
                      </Td>
                    ))}
                    {selectedPreschoolColumns.map((level) => (
                      <Td key={level}>
                        {row.preschoolAssistanceCounts[level] ?? 0}
                      </Td>
                    ))}
                    {selectedOtherColumns.map((type) => (
                      <Td key={type}>
                        {row.otherAssistanceMeasureCounts[type] ?? 0}
                      </Td>
                    ))}
                    {report.value.actions.map((action) => (
                      <Td key={action.value}>
                        {row.actionCounts[action.value] ?? 0}
                      </Td>
                    ))}
                    {featureFlags.assistanceActionOther && (
                      <Td>{row.otherActionCount}</Td>
                    )}
                    <Td>{row.noActionCount}</Td>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  {selectedDaycareColumns.map((level) => (
                    <Td key={level}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.daycareAssistanceCounts[level] ?? 0
                      )}
                    </Td>
                  ))}
                  {selectedPreschoolColumns.map((level) => (
                    <Td key={level}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.preschoolAssistanceCounts[level] ?? 0
                      )}
                    </Td>
                  ))}
                  {selectedOtherColumns.map((type) => (
                    <Td key={type}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.otherAssistanceMeasureCounts[type] ?? 0
                      )}
                    </Td>
                  ))}
                  {report.value.actions.map((action) => (
                    <Td key={action.value}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.actionCounts[action.value] ?? 0
                      )}
                    </Td>
                  ))}
                  {featureFlags.assistanceActionOther && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.otherActionCount
                      )}
                    </Td>
                  )}
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.noActionCount)}
                  </Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
