// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import {
  ChildDocumentsReportTemplate,
  GroupRow
} from 'lib-common/generated/api-types/reports'
import {
  DaycareId,
  DocumentTemplateId
} from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import TreeDropdown, {
  TreeNode
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { P, Strong } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { unitsQuery } from '../unit/queries'

import { FilterLabel, FilterRow } from './common'
import {
  childDocumentsReportQuery,
  childDocumentsReportTemplateOptionsQuery
} from './queries'

export default React.memo(function ChildDocumentsReport() {
  const { i18n } = useTranslation()

  const units = useQueryResult(unitsQuery({ includeClosed: false }))
  const unitOptions = useMemo(
    () =>
      units
        .map((res) =>
          res.filter((u) => u.enabledPilotFeatures.includes('VASU_AND_PEDADOC'))
        )
        .map((res) => orderBy(res, (u) => u.name)),
    [units]
  )
  const templates = useQueryResult(childDocumentsReportTemplateOptionsQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childDocuments.title}</Title>
        <P>{i18n.reports.childDocuments.description}</P>
        <P>
          {i18n.reports.childDocuments.info}
          <br />
          {i18n.reports.childDocuments.info2}
        </P>
        {renderResult(
          combine(unitOptions, templates),
          ([unitOptions, templates]) => (
            <ChildDocumentsReportInner
              units={unitOptions}
              templates={templates}
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

const ChildDocumentsReportInner = React.memo(
  function ChildDocumentsReportInner({
    units,
    templates
  }: {
    units: Daycare[]
    templates: ChildDocumentsReportTemplate[]
  }) {
    const { i18n } = useTranslation()

    const [selectedUnits, setSelectedUnits] = useState<Daycare[]>([])

    const sortedTemplates = useMemo(
      () => orderBy(templates, (t) => t.name),
      [templates]
    )
    const [templateTree, setTemplateTree] = useState<TreeNode[]>(
      [
        {
          key: 'VASU',
          text: i18n.reports.childDocuments.categories.VASU,
          checked: false,
          children: sortedTemplates
            .filter((t) => ['VASU', 'MIGRATED_VASU'].includes(t.type))
            .map((t) => ({
              key: t.id,
              text: t.name,
              checked: false,
              children: []
            }))
        },
        {
          key: 'LEOPS+HOJKS',
          text: i18n.reports.childDocuments.categories.LEOPS_HOJKS,
          checked: false,
          children: sortedTemplates
            .filter((t) =>
              ['LEOPS', 'MIGRATED_LEOPS', 'HOJKS'].includes(t.type)
            )
            .map((t) => ({
              key: t.id,
              text: t.name,
              checked: false,
              children: []
            }))
        },
        {
          key: 'OTHER',
          text: i18n.reports.childDocuments.categories.OTHER,
          checked: false,
          children: sortedTemplates
            .filter(
              (t) =>
                ![
                  'VASU',
                  'MIGRATED_VASU',
                  'LEOPS',
                  'MIGRATED_LEOPS',
                  'HOJKS'
                ].includes(t.type)
            )
            .map((t) => ({
              key: t.id,
              text: t.name,
              checked: false,
              children: []
            }))
        }
      ].filter((category) => category.children.length > 0)
    )

    const unitIds = useMemo(
      () => selectedUnits.map((u) => u.id),
      [selectedUnits]
    )
    const templateIds = useMemo(
      () =>
        templateTree.flatMap((category) =>
          category.children
            .filter((t) => t.checked)
            .map((t) => fromUuid<DocumentTemplateId>(t.key))
        ),
      [templateTree]
    )

    return (
      <>
        <FilterRowWide>
          <FilterLabel>{i18n.reports.childDocuments.filters.units}</FilterLabel>
          <FlexGrow>
            <MultiSelect
              options={units}
              value={selectedUnits}
              onChange={setSelectedUnits}
              getOptionLabel={(o) => o.name}
              getOptionId={(o) => o.id}
              placeholder={i18n.common.select}
              data-qa="unit-select"
            />
          </FlexGrow>
        </FilterRowWide>
        <FilterRowWide>
          <FilterLabel>
            {i18n.reports.childDocuments.filters.templates}
          </FilterLabel>
          <FlexGrow>
            <TreeDropdown
              tree={templateTree}
              onChange={setTemplateTree}
              placeholder={i18n.common.select}
              data-qa="template-select"
            />
          </FlexGrow>
        </FilterRowWide>
        {unitIds.length > 0 && templateIds.length > 0 && (
          <ChildDocumentsReportTable
            unitIds={unitIds}
            templateIds={templateIds}
          />
        )}
      </>
    )
  }
)

const ChildDocumentsReportTable = React.memo(
  function ChildDocumentsReportInner({
    unitIds,
    templateIds
  }: {
    unitIds: DaycareId[]
    templateIds: DocumentTemplateId[]
  }) {
    const { i18n } = useTranslation()

    const rowsResult = useQueryResult(
      childDocumentsReportQuery({ unitIds, templateIds })
    )
    const orderedRows = useMemo(
      () => rowsResult.map((res) => orderBy(res, (r) => r.unitName)),
      [rowsResult]
    )

    return renderResult(orderedRows, (unitRows) => (
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
          {unitRows.map((row) => (
            <React.Fragment key={row.unitId}>
              <Tr data-qa={`unit-row-${row.unitId}`}>
                <Td data-qa="name">
                  <Strong>{row.unitName}</Strong>
                </Td>
                <Td data-qa="drafts-count">{row.drafts}</Td>
                <Td data-qa="prepared-count">{row.prepared}</Td>
                <Td data-qa="completed-count">{row.completed}</Td>
                <Td data-qa="no-documents-count">{row.none}</Td>
                <Td data-qa="total-count">{row.total}</Td>
              </Tr>
              <GroupSection unitId={row.unitId} groupRows={row.groups} />
            </React.Fragment>
          ))}
        </Tbody>
      </Table>
    ))
  }
)

const TdIndented = styled(Td)`
  padding-left: ${defaultMargins.L};
`

const GroupSection = React.memo(function GroupSection({
  unitId,
  groupRows
}: {
  unitId: DaycareId
  groupRows: GroupRow[]
}) {
  const { i18n } = useTranslation()
  const t = i18n.reports.childDocuments

  const [expanded, { toggle: toggleExpanded }] = useBoolean(false)
  const orderedRows = useMemo(
    () => orderBy(groupRows, (r) => r.groupName),
    [groupRows]
  )

  return (
    <>
      {expanded &&
        orderedRows.map((row) => (
          <Tr key={row.groupId} data-qa={`group-row-${row.groupId}`}>
            <TdIndented data-qa="name">{row.groupName}</TdIndented>
            <TdIndented data-qa="drafts-count">{row.drafts}</TdIndented>
            <TdIndented data-qa="prepared-count">{row.prepared}</TdIndented>
            <TdIndented data-qa="completed-count">{row.completed}</TdIndented>
            <TdIndented data-qa="no-documents-count">{row.none}</TdIndented>
            <TdIndented data-qa="total-count">{row.total}</TdIndented>
          </Tr>
        ))}
      <Tr>
        <Td colSpan={6} align="center">
          <Button
            appearance="inline"
            text={expanded ? t.table.collapse : t.table.expand}
            icon={expanded ? faChevronUp : faChevronDown}
            onClick={toggleExpanded}
            data-qa={`unit-${unitId}-toggle-groups`}
          />
        </Td>
      </Tr>
    </>
  )
})
