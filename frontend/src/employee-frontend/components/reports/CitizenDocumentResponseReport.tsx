// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { Daycare, DaycareGroup } from 'lib-common/generated/api-types/daycare'
import {
  CitizenDocumentResponseReportTemplate,
} from 'lib-common/generated/api-types/reports'
import {
  DaycareId,
  DocumentTemplateId,
  GroupId
} from 'lib-common/generated/api-types/shared'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'

import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Strong } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { daycaresQuery } from '../unit/queries'

import { FilterLabel, FilterRow } from './common'
import {
  getCitizenDocumentResponseReportQuery,
  getCitizenDocumentResponseTemplateOptionsQuery
} from './queries'
import Select from 'lib-components/atoms/dropdowns/Select'

export default React.memo(function CitizenDocumentResponseReport() {
  const { i18n } = useTranslation()

  const units = useQueryResult(daycaresQuery({ includeClosed: false }))
  const unitOptions = useMemo(
    () =>
      units.map((res) =>
        orderBy(
          res.filter((u) =>
            u.enabledPilotFeatures.includes('VASU_AND_PEDADOC')
          ),
          (u) => u.name
        )
      ),
    [units]
  )
  const templates = useQueryResult(getCitizenDocumentResponseTemplateOptionsQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.citizenDocumentResponseReport.title}</Title>
        {renderResult(
          combine(unitOptions, templates),
          ([unitOptions, templates]) => (
            <CitizenDocumentResponseReportInner
              units={unitOptions}
              templates={templates}
              groups={[]}
            />
          )
        )}
      </ContentArea>
    </Container>
  )
})

const FilterRowWide = styled(FilterRow)`
  width: 750px;
`

const FlexGrow = styled.div`
  flex-grow: 1;
`

const CitizenDocumentResponseReportInner = React.memo(
  function CitizenDocumentResponseReportInner({
    units,
    templates,
    groups
  }: {
    units: Daycare[]
    templates: CitizenDocumentResponseReportTemplate[]
    groups: DaycareGroup[]
  }) {
    const { i18n } = useTranslation()

    const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
    const [selectedTemplate, setSelectedTemplate] = useState<CitizenDocumentResponseReportTemplate | null>(null)
    const [selectedGroup, setSelectedGroup] = useState<DaycareGroup | null>(null)

    const sortedTemplates = useMemo(
      () => orderBy(templates, (t) => t.name),
      [templates]
    )

    const sortedUnits = useMemo(
      () => orderBy(units, (u) => u.name),
      [units]
    )

    const sortedGroups = useMemo(
      () => orderBy(groups, (g) => g.name),
      [groups]
    )

    return (
      <>
        <FilterRowWide>
          <FilterLabel>{i18n.reports.citizenDocumentResponseReport.filters.unit}</FilterLabel>
          <FlexGrow>
            <Select
              items={sortedUnits}
              selectedItem={selectedUnit}
              onChange={setSelectedUnit}
              getItemValue={i => i.name}
              data-qa="unit-select"
            />
          </FlexGrow>
        </FilterRowWide>
        <FilterRowWide>
          <FilterLabel>
            {i18n.reports.citizenDocumentResponseReport.filters.group}
          </FilterLabel>
          <FlexGrow>
            <Select
              items={sortedGroups}
              selectedItem={selectedGroup}
              onChange={setSelectedGroup}
              getItemValue={i => i.name}
              data-qa="group-select"
            />
          </FlexGrow>
        </FilterRowWide>
        <FilterRowWide>
          <FilterLabel>
            {i18n.reports.citizenDocumentResponseReport.filters.template}
          </FilterLabel>
          <FlexGrow>
            <Select
              items={sortedTemplates}
              selectedItem={selectedTemplate}
              onChange={setSelectedTemplate}
              getItemValue={i => i.name}
              data-qa="template-select"
            />
          </FlexGrow>
        </FilterRowWide>
        <CitizenDocumentResponseReportTable
          unitId={selectedUnit?.id}
          groupId={selectedGroup?.id}
          templateId={selectedTemplate?.id}
        />
      </>
    )
  }
)

const CitizenDocumentResponseReportTable = React.memo(
  function CitizenDocumentResponseReportTable({
    unitId,
    templateId,
    groupId
  }: {
    unitId?: DaycareId
    templateId?: DocumentTemplateId
    groupId?: GroupId
  }) {
    const { i18n } = useTranslation()

    const rowsResult = useQueryResult(unitId && groupId && templateId ?
      getCitizenDocumentResponseReportQuery({ unitId, groupId, documentTemplateId: templateId }) : constantQuery([])
    )
    const orderedRows = useMemo(
      () => rowsResult.map((res) => orderBy(res, [(r) => r.lastName, (r) => r.firstName])),
      [rowsResult]
    )

    return renderResult(orderedRows, (reportRows) => (
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.reports.childDocuments.table.unitOrGroup}</Th>
            <Th>{i18n.reports.childDocuments.table.draft}</Th>
            <Th>{i18n.reports.childDocuments.table.prepared}</Th>
            <Th>{i18n.reports.childDocuments.table.completed}</Th>
            <Th>{i18n.reports.childDocuments.table.none}</Th>
            <Th>{i18n.reports.childDocuments.table.total}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {reportRows.map((row) => (
            <React.Fragment key={row.childId}>
              <Tr data-qa={`child-row-${row.childId}`}>
                <Td data-qa="name">
                  <Strong>{`${row.lastName} ${row.firstName}`}</Strong>
                </Td>
                <Td data-qa="answer-date">{row.responseDate.format()}</Td>
              </Tr>
            </React.Fragment>
          ))}
        </Tbody>
      </Table>
    ))
  }
)