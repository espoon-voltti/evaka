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
import { reactSelectStyles } from '~components/common/Select'
import { useTranslation } from '~state/i18n'
import { Loading, Result } from '~api'
import { AssistanceActionsReportRow } from '~types/reports'
import {
  AssistanceActionsReportFilters,
  getAssistanceActionsReport
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

function AssistanceActions() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<AssistanceActionsReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<AssistanceActionsReportFilters>({
    date: LocalDate.today()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: AssistanceActionsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getAssistanceActionsReport(filters).then(setRows)
  }, [filters])

  const actionTypes = i18n.childInformation.assistanceAction.fields.actionTypes

  const filteredRows: AssistanceActionsReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.assistanceActions.title}</Title>
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
                ...rows
                  .map((rs) =>
                    distinct(rs.map((row) => row.careAreaName)).map((s) => ({
                      value: s,
                      label: s
                    }))
                  )
                  .getOrElse([])
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

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
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
                  label: actionTypes.ASSISTANCE_SERVICE_CHILD,
                  key: 'assistanceServiceChild'
                },
                {
                  label: actionTypes.ASSISTANCE_SERVICE_UNIT,
                  key: 'assistanceServiceUnit'
                },
                { label: actionTypes.SMALLER_GROUP, key: 'smallerGroup' },
                { label: actionTypes.SPECIAL_GROUP, key: 'specialGroup' },
                {
                  label: actionTypes.PERVASIVE_VEO_SUPPORT,
                  key: 'pervasiveVeoSupport'
                },
                { label: actionTypes.RESOURCE_PERSON, key: 'resourcePerson' },
                { label: actionTypes.RATIO_DECREASE, key: 'ratioDecrease' },
                {
                  label: actionTypes.PERIODICAL_VEO_SUPPORT,
                  key: 'periodicalVeoSupport'
                },
                {
                  label: actionTypes.OTHER,
                  key: 'other'
                },
                {
                  label: i18n.reports.assistanceActions.actionMissing,
                  key: 'none'
                }
              ]}
              filename={`Lapsien tukitoimet yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{actionTypes.ASSISTANCE_SERVICE_CHILD}</Th>
                  <Th>{actionTypes.ASSISTANCE_SERVICE_UNIT}</Th>
                  <Th>{actionTypes.SMALLER_GROUP}</Th>
                  <Th>{actionTypes.SPECIAL_GROUP}</Th>
                  <Th>{actionTypes.PERVASIVE_VEO_SUPPORT}</Th>
                  <Th>{actionTypes.RESOURCE_PERSON}</Th>
                  <Th>{actionTypes.RATIO_DECREASE}</Th>
                  <Th>{actionTypes.PERIODICAL_VEO_SUPPORT}</Th>
                  <Th>{actionTypes.OTHER}</Th>
                  <Th>{i18n.reports.assistanceActions.actionMissing}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: AssistanceActionsReportRow) => (
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
                    <Td>{row.assistanceServiceChild}</Td>
                    <Td>{row.assistanceServiceUnit}</Td>
                    <Td>{row.smallerGroup}</Td>
                    <Td>{row.specialGroup}</Td>
                    <Td>{row.pervasiveVeoSupport}</Td>
                    <Td>{row.resourcePerson}</Td>
                    <Td>{row.ratioDecrease}</Td>
                    <Td>{row.periodicalVeoSupport}</Td>
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
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.assistanceServiceChild
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.assistanceServiceUnit
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.smallerGroup)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.specialGroup)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.pervasiveVeoSupport
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.resourcePerson)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.ratioDecrease)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.periodicalVeoSupport
                    )}
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

export default AssistanceActions
