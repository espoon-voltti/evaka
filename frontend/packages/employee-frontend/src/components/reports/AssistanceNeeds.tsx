// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
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

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

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
          <Wrapper>
            <ReactSelect
              options={[
                { value: '', label: i18n.common.all },
                ...(isSuccess(rows)
                  ? distinct(
                      rows.data.map((row) => row.careAreaName)
                    ).map((s) => ({ value: s, label: s }))
                  : [])
              ]}
              onChange={(option) =>
                option && 'value' in option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              value={
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
              styles={reactSelectStyles}
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
            />
          </Wrapper>
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
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
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{basisTypes.AUTISM}</Th>
                  <Th>{basisTypes.DEVELOPMENTAL_DISABILITY_1}</Th>
                  <Th>{basisTypes.DEVELOPMENTAL_DISABILITY_2}</Th>
                  <Th>{basisTypes.FOCUS_CHALLENGE}</Th>
                  <Th>{basisTypes.LINGUISTIC_CHALLENGE}</Th>
                  <Th>{basisTypes.DEVELOPMENT_MONITORING}</Th>
                  <Th>{basisTypes.DEVELOPMENT_MONITORING_PENDING}</Th>
                  <Th>{basisTypes.MULTI_DISABILITY}</Th>
                  <Th>{basisTypes.LONG_TERM_CONDITION}</Th>
                  <Th>{basisTypes.REGULATION_SKILL_CHALLENGE}</Th>
                  <Th>{basisTypes.DISABILITY}</Th>
                  <Th>{basisTypes.OTHER}</Th>
                  <Th>{i18n.reports.assistanceNeeds.basisMissing}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: AssistanceNeedsReportRow) => (
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
                    <Td>{row.autism}</Td>
                    <Td>{row.developmentalDisability1}</Td>
                    <Td>{row.developmentalDisability2}</Td>
                    <Td>{row.focusChallenge}</Td>
                    <Td>{row.linguisticChallenge}</Td>
                    <Td>{row.developmentMonitoring}</Td>
                    <Td>{row.developmentMonitoringPending}</Td>
                    <Td>{row.multiDisability}</Td>
                    <Td>{row.longTermCondition}</Td>
                    <Td>{row.regulationSkillChallenge}</Td>
                    <Td>{row.disability}</Td>
                    <Td>{row.other}</Td>
                    <Td>{row.none}</Td>
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
                  <Td>{reducePropertySum(filteredRows, (r) => r.autism)}</Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentalDisability1
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentalDisability2
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.focusChallenge)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.linguisticChallenge
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentMonitoring
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.developmentMonitoringPending
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.multiDisability)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.longTermCondition
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.regulationSkillChallenge
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.disability)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.none)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default AssistanceNeeds
