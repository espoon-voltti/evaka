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
import SelectWithIcon from 'components/common/Select'
import { Link } from 'react-router-dom'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

function AssistanceActions() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<AssistanceActionsReportRow[]>>(
    Loading()
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
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getAssistanceActionsReport(filters).then(setRows)
  }, [filters])

  const actionTypes = i18n.childInformation.assistanceAction.fields.actionTypes

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
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
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.groupName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitType}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitProviderType}</Table.Th>
                  <Table.Th>{actionTypes.ASSISTANCE_SERVICE_CHILD}</Table.Th>
                  <Table.Th>{actionTypes.ASSISTANCE_SERVICE_UNIT}</Table.Th>
                  <Table.Th>{actionTypes.SMALLER_GROUP}</Table.Th>
                  <Table.Th>{actionTypes.SPECIAL_GROUP}</Table.Th>
                  <Table.Th>{actionTypes.PERVASIVE_VEO_SUPPORT}</Table.Th>
                  <Table.Th>{actionTypes.RESOURCE_PERSON}</Table.Th>
                  <Table.Th>{actionTypes.RATIO_DECREASE}</Table.Th>
                  <Table.Th>{actionTypes.PERIODICAL_VEO_SUPPORT}</Table.Th>
                  <Table.Th>{actionTypes.OTHER}</Table.Th>
                  <Table.Th>
                    {i18n.reports.assistanceActions.actionMissing}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: AssistanceActionsReportRow) => (
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
                    <Table.Td>{row.assistanceServiceChild}</Table.Td>
                    <Table.Td>{row.assistanceServiceUnit}</Table.Td>
                    <Table.Td>{row.smallerGroup}</Table.Td>
                    <Table.Td>{row.specialGroup}</Table.Td>
                    <Table.Td>{row.pervasiveVeoSupport}</Table.Td>
                    <Table.Td>{row.resourcePerson}</Table.Td>
                    <Table.Td>{row.ratioDecrease}</Table.Td>
                    <Table.Td>{row.periodicalVeoSupport}</Table.Td>
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
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.assistanceServiceChild
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.assistanceServiceUnit
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.smallerGroup)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.specialGroup)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.pervasiveVeoSupport
                    )}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.resourcePerson)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.ratioDecrease)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.periodicalVeoSupport
                    )}
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

export default AssistanceActions
