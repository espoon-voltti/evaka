// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { AssistanceNeedsReportRow } from '~types/reports'
import {
  AssistanceNeedsReportFilters,
  getAssistanceNeedsReport
} from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { DatePicker } from '~components/common/DatePicker'
import {
  FilterLabel,
  FilterRow,
  TableFooter,
  TableScrollable
} from '~components/reports/common'
import { distinct, reducePropertySum } from 'utils'
import LocalDate from '@evaka/lib-common/src/local-date'
import SelectWithIcon from 'components/common/Select'
import { Link } from 'react-router-dom'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

function AssistanceNeeds() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<AssistanceNeedsReportRow[]>>(
    Loading()
  )
  const [filters, setFilters] = useState<AssistanceNeedsReportFilters>({
    date: LocalDate.today()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: AssistanceNeedsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getAssistanceNeedsReport(filters).then(setRows)
  }, [filters])

  const basisTypes = i18n.childInformation.assistanceNeed.fields.basisTypes

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.assistanceNeeds.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePicker
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <SelectWithIcon
            options={[
              { id: '', label: '' },
              ...(isSuccess(rows)
                ? distinct(
                    rows.data.map((row) => row.careAreaName)
                  ).map((s) => ({ id: s, label: s }))
                : [])
            ]}
            value={displayFilters.careArea}
            onChange={(e) =>
              setDisplayFilters({ ...displayFilters, careArea: e.target.value })
            }
            fullWidth
          />
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={rows.data.map((row) => ({
                ...row,
                unitType: row.unitType
                  ? i18n.reports.common.unitTypes[row.unitType]
                  : '',
                unitProviderType:
                  i18n.reports.common.unitProviderTypes[row.unitProviderType]
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
                {
                  label: basisTypes.AUTISM,
                  key: 'autism'
                },
                {
                  label: basisTypes.DEVELOPMENTAL_DISABILITY_1,
                  key: 'developmentalDisability1'
                },
                {
                  label: basisTypes.DEVELOPMENTAL_DISABILITY_2,
                  key: 'developmentalDisability2'
                },
                {
                  label: basisTypes.FOCUS_CHALLENGE,
                  key: 'focusChallenge'
                },
                {
                  label: basisTypes.LINGUISTIC_CHALLENGE,
                  key: 'linguisticChallenge'
                },
                {
                  label: basisTypes.DEVELOPMENT_MONITORING,
                  key: 'developmentMonitoring'
                },
                {
                  label: basisTypes.DEVELOPMENT_MONITORING_PENDING,
                  key: 'developmentMonitoringPending'
                },
                {
                  label: basisTypes.MULTI_DISABILITY,
                  key: 'multiDisability'
                },
                {
                  label: basisTypes.LONG_TERM_CONDITION,
                  key: 'longTermCondition'
                },
                {
                  label: basisTypes.REGULATION_SKILL_CHALLENGE,
                  key: 'regulationSkillChallenge'
                },
                {
                  label: basisTypes.DISABILITY,
                  key: 'disability'
                },
                {
                  label: basisTypes.OTHER,
                  key: 'other'
                },
                {
                  label: i18n.reports.assistanceNeeds.basisMissing,
                  key: 'none'
                }
              ]}
              filename={`Lapsien tuentarpeet yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.groupName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitType}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitProviderType}</Table.Th>
                  <Table.Th>{basisTypes.AUTISM}</Table.Th>
                  <Table.Th>{basisTypes.DEVELOPMENTAL_DISABILITY_1}</Table.Th>
                  <Table.Th>{basisTypes.DEVELOPMENTAL_DISABILITY_2}</Table.Th>
                  <Table.Th>{basisTypes.FOCUS_CHALLENGE}</Table.Th>
                  <Table.Th>{basisTypes.LINGUISTIC_CHALLENGE}</Table.Th>
                  <Table.Th>{basisTypes.DEVELOPMENT_MONITORING}</Table.Th>
                  <Table.Th>
                    {basisTypes.DEVELOPMENT_MONITORING_PENDING}
                  </Table.Th>
                  <Table.Th>{basisTypes.MULTI_DISABILITY}</Table.Th>
                  <Table.Th>{basisTypes.LONG_TERM_CONDITION}</Table.Th>
                  <Table.Th>{basisTypes.REGULATION_SKILL_CHALLENGE}</Table.Th>
                  <Table.Th>{basisTypes.DISABILITY}</Table.Th>
                  <Table.Th>{basisTypes.OTHER}</Table.Th>
                  <Table.Th>
                    {i18n.reports.assistanceNeeds.basisMissing}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: AssistanceNeedsReportRow) => (
                  <Table.Row key={`${row.unitId}:${row.groupId}`}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Table.Td>
                    <Table.Td>{row.groupName}</Table.Td>
                    <Table.Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Table.Td>
                    <Table.Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Table.Td>
                    <Table.Td>{row.autism}</Table.Td>
                    <Table.Td>{row.developmentalDisability1}</Table.Td>
                    <Table.Td>{row.developmentalDisability2}</Table.Td>
                    <Table.Td>{row.focusChallenge}</Table.Td>
                    <Table.Td>{row.linguisticChallenge}</Table.Td>
                    <Table.Td>{row.developmentMonitoring}</Table.Td>
                    <Table.Td>{row.developmentMonitoringPending}</Table.Td>
                    <Table.Td>{row.multiDisability}</Table.Td>
                    <Table.Td>{row.longTermCondition}</Table.Td>
                    <Table.Td>{row.regulationSkillChallenge}</Table.Td>
                    <Table.Td>{row.disability}</Table.Td>
                    <Table.Td>{row.other}</Table.Td>
                    <Table.Td>{row.none}</Table.Td>
                  </Table.Row>
                ))}
              </Table.Body>
              <TableFooter>
                <Table.Row>
                  <Table.Td className="bold">
                    {i18n.reports.common.total}
                  </Table.Td>
                  <Table.Td />
                  <Table.Td />
                  <Table.Td />
                  <Table.Td />
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.autism)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentalDisability1
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentalDisability2
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.focusChallenge)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.linguisticChallenge
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentMonitoring
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentMonitoringPending
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.multiDisability)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.longTermCondition
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.regulationSkillChallenge
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.disability)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.none)}
                  </Table.Td>
                </Table.Row>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default AssistanceNeeds
