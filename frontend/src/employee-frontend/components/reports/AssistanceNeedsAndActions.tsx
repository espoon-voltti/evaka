// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import {
  AssistanceNeedsAndActionsReportFilters,
  getAssistanceNeedsAndActionsReport
} from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'
import { distinct, reducePropertySum } from '../../utils'
import LocalDate from 'lib-common/local-date'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  AssistanceNeedsAndActionsReport,
  AssistanceNeedsAndActionsReportRow
} from '../../types/reports'
import { assistanceMeasures, featureFlags } from 'lib-customizations/employee'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

function AssistanceNeedsAndActions() {
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<AssistanceNeedsAndActionsReport>>(
    Loading.of()
  )
  const [filters, setFilters] =
    useState<AssistanceNeedsAndActionsReportFilters>({
      date: LocalDate.today()
    })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: AssistanceNeedsAndActionsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setReport(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getAssistanceNeedsAndActionsReport(filters).then(setReport)
  }, [filters])

  const filteredRows: AssistanceNeedsAndActionsReportRow[] = useMemo(
    () => report.map((rs) => rs.rows.filter(displayFilter)).getOrElse([]),
    [report, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

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
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              selectedItem={
                displayFilters.careArea !== ''
                  ? {
                      label: displayFilters.careArea,
                      value: displayFilters.careArea
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

        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload<Record<string, unknown>>
              data={filteredRows.map((row) => ({
                ...row,
                unitType: row.unitType
                  ? i18n.reports.common.unitTypes[row.unitType]
                  : '',
                unitProviderType:
                  i18n.reports.common.unitProviderTypes[row.unitProviderType],
                ...Object.fromEntries(
                  Object.entries(row.basisCounts).map(([value, count]) => [
                    `BASIS-${value}`,
                    count ?? 0
                  ])
                ),
                ...Object.fromEntries(
                  Object.entries(row.actionCounts).map(([value, count]) => [
                    `ACTION-${value}`,
                    count ?? 0
                  ])
                ),
                ...Object.fromEntries(
                  Object.entries(row.measureCounts).map(([value, count]) => [
                    `MEASURE-${value}`,
                    count ?? 0
                  ])
                )
              }))}
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
                {
                  label: i18n.reports.common.unitType,
                  key: 'unitType'
                },
                {
                  label: i18n.reports.common.unitProviderType,
                  key: 'unitProviderType'
                },
                ...report.value.bases.map((basis) => ({
                  label: basis.nameFi,
                  key: `BASIS-${basis.value}`
                })),
                ...(featureFlags.assistanceBasisOtherEnabled
                  ? [
                      {
                        label:
                          i18n.childInformation.assistanceNeed.fields.basisTypes
                            .OTHER,
                        key: 'otherBasisCount'
                      }
                    ]
                  : []),
                {
                  label: i18n.reports.assistanceNeedsAndActions.basisMissing,
                  key: 'noBasisCount'
                },
                ...report.value.actions.map((action) => ({
                  label: action.nameFi,
                  key: `ACTION-${action.value}`
                })),
                ...(featureFlags.assistanceActionOtherEnabled
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
                },
                ...assistanceMeasures.map((measure) => ({
                  label:
                    i18n.childInformation.assistanceAction.fields.measureTypes[
                      measure
                    ],
                  key: `MEASURE-${measure}`
                }))
              ]}
              filename={`Lapsien tuentarpeet ja tukitoimet yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  {report.value.bases.map((basis) => (
                    <Th key={basis.value}>{basis.nameFi}</Th>
                  ))}
                  {featureFlags.assistanceBasisOtherEnabled && (
                    <Th>
                      {
                        i18n.childInformation.assistanceNeed.fields.basisTypes
                          .OTHER
                      }
                    </Th>
                  )}
                  <Th>{i18n.reports.assistanceNeedsAndActions.basisMissing}</Th>
                  {report.value.actions.map((action) => (
                    <Th key={action.value}>{action.nameFi}</Th>
                  ))}
                  {featureFlags.assistanceActionOtherEnabled && (
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
                  {assistanceMeasures.map((measure) => (
                    <Th key={measure}>
                      {
                        i18n.childInformation.assistanceAction.fields
                          .measureTypes[measure]
                      }
                    </Th>
                  ))}
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
                    <Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Td>
                    <Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Td>
                    {report.value.bases.map((basis) => (
                      <Td key={basis.value}>
                        {row.basisCounts[basis.value] ?? 0}
                      </Td>
                    ))}
                    {featureFlags.assistanceBasisOtherEnabled && (
                      <Td>{row.otherBasisCount}</Td>
                    )}
                    <Td>{row.noBasisCount}</Td>
                    {report.value.actions.map((action) => (
                      <Td key={action.value}>
                        {row.actionCounts[action.value] ?? 0}
                      </Td>
                    ))}
                    {featureFlags.assistanceActionOtherEnabled && (
                      <Td>{row.otherActionCount}</Td>
                    )}
                    <Td>{row.noActionCount}</Td>
                    {assistanceMeasures.map((measure) => (
                      <Td key={measure}>{row.measureCounts[measure] ?? 0}</Td>
                    ))}
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  <Td />
                  <Td />
                  {report.value.bases.map((basis) => (
                    <Td key={basis.value}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.basisCounts[basis.value] ?? 0
                      )}
                    </Td>
                  ))}
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.otherBasisCount)}
                  </Td>
                  {featureFlags.assistanceBasisOtherEnabled && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.noBasisCount)}
                    </Td>
                  )}
                  {report.value.actions.map((action) => (
                    <Td key={action.value}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.actionCounts[action.value] ?? 0
                      )}
                    </Td>
                  ))}
                  {featureFlags.assistanceActionOtherEnabled && (
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
                  {assistanceMeasures.map((measure) => (
                    <Td key={measure}>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.measureCounts[measure] ?? 0
                      )}
                    </Td>
                  ))}
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default AssistanceNeedsAndActions
