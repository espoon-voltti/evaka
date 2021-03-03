// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import {
  Th,
  Tr,
  Td,
  Thead,
  Tbody
} from '@evaka/lib-components/src/layout/Table'
import { reactSelectStyles } from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { AssistanceNeedsAndActionsReportRow } from '../../types/reports'
import {
  AssistanceNeedsAndActionsReportFilters,
  getAssistanceNeedsAndActionsReport
} from '../../api/reports'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { DatePickerDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'
import {
  FilterLabel,
  FilterRow,
  TableFooter,
  TableScrollable
} from '../../components/reports/common'
import { distinct, reducePropertySum } from '../../utils'
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

function AssistanceNeedsAndActions() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<
    Result<AssistanceNeedsAndActionsReportRow[]>
  >(Loading.of())
  const [
    filters,
    setFilters
  ] = useState<AssistanceNeedsAndActionsReportFilters>({
    date: LocalDate.today()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: AssistanceNeedsAndActionsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getAssistanceNeedsAndActionsReport(filters).then(setRows)
  }, [filters])

  const basisTypes = i18n.childInformation.assistanceNeed.fields.basisTypes
  const actionTypes = i18n.childInformation.assistanceAction.fields.actionTypes

  const filteredRows: AssistanceNeedsAndActionsReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters]
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
                  key: 'otherAssistanceNeed'
                },
                {
                  label: i18n.reports.assistanceNeedsAndActions.basisMissing,
                  key: 'noAssistanceNeeds'
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
                  key: 'otherAssistanceAction'
                },
                {
                  label: i18n.reports.assistanceNeedsAndActions.actionMissing,
                  key: 'noAssistanceActions'
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
                  <Th>{i18n.reports.assistanceNeedsAndActions.basisMissing}</Th>
                  <Th>{actionTypes.ASSISTANCE_SERVICE_CHILD}</Th>
                  <Th>{actionTypes.ASSISTANCE_SERVICE_UNIT}</Th>
                  <Th>{actionTypes.SMALLER_GROUP}</Th>
                  <Th>{actionTypes.SPECIAL_GROUP}</Th>
                  <Th>{actionTypes.PERVASIVE_VEO_SUPPORT}</Th>
                  <Th>{actionTypes.RESOURCE_PERSON}</Th>
                  <Th>{actionTypes.RATIO_DECREASE}</Th>
                  <Th>{actionTypes.PERIODICAL_VEO_SUPPORT}</Th>
                  <Th>{actionTypes.OTHER}</Th>
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
                    <Td>{row.otherAssistanceNeed}</Td>
                    <Td>{row.noAssistanceNeeds}</Td>
                    <Td>{row.assistanceServiceChild}</Td>
                    <Td>{row.assistanceServiceUnit}</Td>
                    <Td>{row.smallerGroup}</Td>
                    <Td>{row.specialGroup}</Td>
                    <Td>{row.pervasiveVeoSupport}</Td>
                    <Td>{row.resourcePerson}</Td>
                    <Td>{row.ratioDecrease}</Td>
                    <Td>{row.periodicalVeoSupport}</Td>
                    <Td>{row.otherAssistanceAction}</Td>
                    <Td>{row.noAssistanceActions}</Td>
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
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.otherAssistanceNeed
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.noAssistanceNeeds
                    )}
                  </Td>
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
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.otherAssistanceAction
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.noAssistanceActions
                    )}
                  </Td>
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
