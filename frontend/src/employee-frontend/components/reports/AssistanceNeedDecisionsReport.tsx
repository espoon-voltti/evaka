// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { SortDirection } from 'lib-common/generated/api-types/invoicing'
import { AssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'

import { getAssistanceNeedDecisionsReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow } from './common'

const getAssistanceNeedDecisionsReportResult = wrapResult(
  getAssistanceNeedDecisionsReport
)

const Wrapper = styled.div`
  width: 100%;
`

const BorderedTbody = styled(Tbody)`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
`

const Highlight = styled.div`
  background-color: ${(p) => p.theme.colors.status.success};
  width: 6px;
  position: absolute;
  left: 0.5px;
  top: 0.5px;
  bottom: 0.5px;
`

const RelativeTr = styled(Tr)`
  position: relative;
`

export default React.memo(function AssistanceNeedDecisionsReport() {
  const { i18n } = useTranslation()
  const [report] = useApiState(getAssistanceNeedDecisionsReportResult, [])
  const [careAreaFilter, setCareAreaFilter] = useState<string>()
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    const selectedCareArea = searchParams.get('careArea')
    if (selectedCareArea) {
      setCareAreaFilter(selectedCareArea)
    }
  }, [searchParams])

  const [sortColumn, setSortColumn] =
    useState<keyof AssistanceNeedDecisionsReportRow>('sentForDecision')
  const [sortDirection, setSortDirection] = useState<SortDirection>('DESC')

  const sortBy = useCallback(
    (column: keyof AssistanceNeedDecisionsReportRow) => {
      if (sortColumn === column) {
        setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
      } else {
        setSortColumn(column)
        setSortDirection(column === 'sentForDecision' ? 'DESC' : 'ASC')
      }
    },
    [sortColumn, sortDirection]
  )

  const decisionRows = useMemo(
    () =>
      report.map((rs) =>
        orderBy(
          rs.filter(
            (row) => !careAreaFilter || row.careAreaName === careAreaFilter
          ),
          [sortColumn],
          [sortDirection === 'ASC' ? 'asc' : 'desc']
        )
      ),
    [report, careAreaFilter, sortColumn, sortDirection]
  )

  const navigate = useNavigate()

  const changeCareArea = useCallback(
    (option: { value: string; label: string } | null) => {
      if (option) {
        setCareAreaFilter(option.value)
        setSearchParams({ careArea: option.value })
      }
    },
    [setSearchParams]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.assistanceNeedDecisions.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                { value: '', label: i18n.common.all },
                ...report
                  .map((reportRows) =>
                    distinct(reportRows.map((row) => row.careAreaName)).map(
                      (s) => ({
                        value: s,
                        label: s
                      })
                    )
                  )
                  .getOrElse([])
              ]}
              onChange={changeCareArea}
              selectedItem={
                careAreaFilter
                  ? {
                      label: careAreaFilter,
                      value: careAreaFilter
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

        {renderResult(decisionRows, (rows) => (
          <Table>
            <Thead>
              <Tr>
                <SortableTh
                  sorted={
                    sortColumn === 'sentForDecision' ? sortDirection : undefined
                  }
                  onClick={() => sortBy('sentForDecision')}
                >
                  {i18n.reports.assistanceNeedDecisions.sentToDecisionMaker}
                </SortableTh>
                <SortableTh
                  sorted={
                    sortColumn === 'childName' ? sortDirection : undefined
                  }
                  onClick={() => sortBy('childName')}
                >
                  {i18n.reports.common.child}
                </SortableTh>
                <SortableTh
                  sorted={
                    sortColumn === 'careAreaName' ? sortDirection : undefined
                  }
                  onClick={() => sortBy('careAreaName')}
                >
                  {i18n.reports.common.careAreaName}
                </SortableTh>
                <SortableTh
                  sorted={sortColumn === 'unitName' ? sortDirection : undefined}
                  onClick={() => sortBy('unitName')}
                >
                  {i18n.reports.common.unitName}
                </SortableTh>
                <Th>{i18n.reports.assistanceNeedDecisions.decisionMade}</Th>
                <SortableTh
                  sorted={sortColumn === 'status' ? sortDirection : undefined}
                  onClick={() => sortBy('status')}
                >
                  {i18n.reports.assistanceNeedDecisions.status}
                </SortableTh>
              </Tr>
            </Thead>
            <BorderedTbody>
              {rows.map((row) => (
                <RelativeTr
                  key={row.id}
                  onClick={() =>
                    navigate(
                      `/reports/assistance-need${
                        row.preschool ? '-preschool' : ''
                      }-decisions/${row.id}`
                    )
                  }
                  data-qa="assistance-need-decision-row"
                >
                  <Td data-qa="sent-for-decision">
                    {row.isOpened === false && (
                      <Highlight data-qa="unopened-indicator" />
                    )}

                    {row.sentForDecision.format()}
                  </Td>
                  <Td data-qa="child-name">{row.childName}</Td>
                  <Td data-qa="care-area-name">{row.careAreaName}</Td>
                  <Td data-qa="unit-name">{row.unitName}</Td>
                  <Td data-qa="decision-made">
                    {row.decisionMade?.format() ?? '-'}
                  </Td>
                  <Td>
                    <AssistanceNeedDecisionStatusChip
                      decisionStatus={row.status}
                      texts={
                        i18n.childInformation.assistanceNeedDecision.statuses
                      }
                      data-qa="decision-chip"
                    />
                  </Td>
                </RelativeTr>
              ))}
            </BorderedTbody>
          </Table>
        ))}
      </ContentArea>
    </Container>
  )
})
