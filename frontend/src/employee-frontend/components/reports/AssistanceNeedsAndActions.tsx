// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import {
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels
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
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { assistanceMeasures, featureFlags } from 'lib-customizations/employee'

import {
  AssistanceNeedsAndActionsReportFilters,
  getAssistanceNeedsAndActionsReport
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { distinct, reducePropertySum } from '../../utils'

import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

const OldModelOnly = (props: { children: ReactNode }) => {
  const { user } = useContext(UserContext)
  const useNewAssistanceModel =
    user?.accessibleFeatures.useNewAssistanceModel ?? false
  return useNewAssistanceModel ? undefined : props.children
}

function NewModelOnly(props: { children: ReactNode }) {
  const { user } = useContext(UserContext)
  const useNewAssistanceModel =
    user?.accessibleFeatures.useNewAssistanceModel ?? false
  return useNewAssistanceModel ? props.children : undefined
}

export default React.memo(function AssistanceNeedsAndActions() {
  const { user } = useContext(UserContext)
  const useNewAssistanceModel =
    user?.accessibleFeatures.useNewAssistanceModel ?? false
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<AssistanceNeedsAndActionsReport>>(
    Loading.of()
  )
  const [filters, setFilters] =
    useState<AssistanceNeedsAndActionsReportFilters>({
      date: LocalDate.todayInSystemTz()
    })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: AssistanceNeedsAndActionsReportRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

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
              data={filteredRows.map((row) =>
                /* eslint-disable-next-line @typescript-eslint/no-unsafe-return */
                ({
                  ...row,
                  ...Object.fromEntries([
                    ...(useNewAssistanceModel
                      ? [
                          ...daycareAssistanceLevels.map((level) => [
                            `DAYCARE-ASSISTANCE-${level}`,
                            row.daycareAssistanceCounts[level] ?? 0
                          ]),
                          ...preschoolAssistanceLevels.map((level) => [
                            `PRESCHOOL-ASSISTANCE-${level}`,
                            row.preschoolAssistanceCounts[level] ?? 0
                          ]),
                          ...otherAssistanceMeasureTypes.map((type) => [
                            `OTHER-ASSISTANCE-MEASURE-${type}`,
                            row.otherAssistanceMeasureCounts[type] ?? 0
                          ])
                        ]
                      : report.value.bases.map(({ value }) => [
                          `BASIS-${value}`,
                          row.basisCounts[value] ?? 0
                        ])),
                    ...report.value.actions.map(({ value }) => [
                      `ACTION-${value}`,
                      row.actionCounts[value] ?? 0
                    ]),
                    ...(useNewAssistanceModel
                      ? []
                      : assistanceMeasures.map((value) => [
                          `MEASURE-${value}`,
                          row.measureCounts[value] ?? 0
                        ]))
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
                ...(useNewAssistanceModel
                  ? [
                      ...daycareAssistanceLevels.map((level) => ({
                        label:
                          i18n.childInformation.assistance.types
                            .daycareAssistanceLevel[level],
                        key: `DAYCARE-ASSISTANCE-${level}`
                      })),
                      ...preschoolAssistanceLevels.map((level) => ({
                        label:
                          i18n.childInformation.assistance.types
                            .preschoolAssistanceLevel[level],
                        key: `PRESCHOOL-ASSISTANCE-${level}`
                      })),
                      ...otherAssistanceMeasureTypes.map((type) => ({
                        label:
                          i18n.childInformation.assistance.types
                            .otherAssistanceMeasureType[type],
                        key: `OTHER-ASSISTANCE-MEASURE-${type}`
                      }))
                    ]
                  : [
                      ...report.value.bases.map((basis) => ({
                        label: basis.nameFi,
                        key: `BASIS-${basis.value}`
                      })),
                      {
                        label:
                          i18n.reports.assistanceNeedsAndActions.basisMissing,
                        key: 'noBasisCount'
                      }
                    ]),
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
                },
                ...(useNewAssistanceModel
                  ? []
                  : assistanceMeasures.map((measure) => ({
                      label:
                        i18n.childInformation.assistanceAction.fields
                          .measureTypes[measure],
                      key: `MEASURE-${measure}`
                    })))
              ]}
              filename={`Lapsien tuentarpeet ja tukitoimet yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <OldModelOnly>
                    {report.value.bases.map((basis) => (
                      <Th key={basis.value}>{basis.nameFi}</Th>
                    ))}
                    <Th>
                      {i18n.reports.assistanceNeedsAndActions.basisMissing}
                    </Th>
                  </OldModelOnly>
                  <NewModelOnly>
                    {daycareAssistanceLevels.map((level) => (
                      <Th key={level}>
                        {
                          i18n.childInformation.assistance.types
                            .daycareAssistanceLevel[level]
                        }
                      </Th>
                    ))}
                    {preschoolAssistanceLevels.map((level) => (
                      <Th key={level}>
                        {
                          i18n.childInformation.assistance.types
                            .preschoolAssistanceLevel[level]
                        }
                      </Th>
                    ))}
                    {otherAssistanceMeasureTypes.map((type) => (
                      <Th key={type}>
                        {
                          i18n.childInformation.assistance.types
                            .otherAssistanceMeasureType[type]
                        }
                      </Th>
                    ))}
                  </NewModelOnly>
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
                  <OldModelOnly>
                    {assistanceMeasures.map((measure) => (
                      <Th key={measure}>
                        {
                          i18n.childInformation.assistanceAction.fields
                            .measureTypes[measure]
                        }
                      </Th>
                    ))}
                  </OldModelOnly>
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
                    <OldModelOnly>
                      {report.value.bases.map((basis) => (
                        <Td key={basis.value}>
                          {row.basisCounts[basis.value] ?? 0}
                        </Td>
                      ))}
                      <Td>{row.noBasisCount}</Td>
                    </OldModelOnly>
                    <NewModelOnly>
                      {daycareAssistanceLevels.map((level) => (
                        <Td key={level}>
                          {row.daycareAssistanceCounts[level] ?? 0}
                        </Td>
                      ))}
                      {preschoolAssistanceLevels.map((level) => (
                        <Td key={level}>
                          {row.preschoolAssistanceCounts[level] ?? 0}
                        </Td>
                      ))}
                      {otherAssistanceMeasureTypes.map((type) => (
                        <Td key={type}>
                          {row.otherAssistanceMeasureCounts[type] ?? 0}
                        </Td>
                      ))}
                    </NewModelOnly>
                    {report.value.actions.map((action) => (
                      <Td key={action.value}>
                        {row.actionCounts[action.value] ?? 0}
                      </Td>
                    ))}
                    {featureFlags.assistanceActionOther && (
                      <Td>{row.otherActionCount}</Td>
                    )}
                    <Td>{row.noActionCount}</Td>
                    <OldModelOnly>
                      {assistanceMeasures.map((measure) => (
                        <Td key={measure}>{row.measureCounts[measure] ?? 0}</Td>
                      ))}
                    </OldModelOnly>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  <OldModelOnly>
                    {report.value.bases.map((basis) => (
                      <Td key={basis.value}>
                        {reducePropertySum(
                          filteredRows,
                          (r) => r.basisCounts[basis.value] ?? 0
                        )}
                      </Td>
                    ))}
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.noBasisCount)}
                    </Td>
                  </OldModelOnly>
                  <NewModelOnly>
                    {daycareAssistanceLevels.map((level) => (
                      <Td key={level}>
                        {reducePropertySum(
                          filteredRows,
                          (r) => r.daycareAssistanceCounts[level] ?? 0
                        )}
                      </Td>
                    ))}
                    {preschoolAssistanceLevels.map((level) => (
                      <Td key={level}>
                        {reducePropertySum(
                          filteredRows,
                          (r) => r.preschoolAssistanceCounts[level] ?? 0
                        )}
                      </Td>
                    ))}
                    {otherAssistanceMeasureTypes.map((type) => (
                      <Td key={type}>
                        {reducePropertySum(
                          filteredRows,
                          (r) => r.otherAssistanceMeasureCounts[type] ?? 0
                        )}
                      </Td>
                    ))}
                  </NewModelOnly>
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
                  <OldModelOnly>
                    {assistanceMeasures.map((measure) => (
                      <Td key={measure}>
                        {reducePropertySum(
                          filteredRows,
                          (r) => r.measureCounts[measure] ?? 0
                        )}
                      </Td>
                    ))}
                  </OldModelOnly>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
