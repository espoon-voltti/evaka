// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type {
  Daycare,
  DaycareGroup
} from 'lib-common/generated/api-types/daycare'
import type {
  AnsweredQuestion,
  Question
} from 'lib-common/generated/api-types/document'
import type {
  CitizenDocumentResponseReportRow,
  CitizenDocumentResponseReportTemplate
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { Strong } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import {
  getCitizenDocumentResponseReportGroupOptionsQuery,
  getCitizenDocumentResponseReportQuery,
  getCitizenDocumentResponseTemplateOptionsQuery
} from './queries'

const emptyAnswerString = ''

export default React.memo(function CitizenDocumentResponseReport() {
  const { i18n } = useTranslation()
  const today = LocalDate.todayInHelsinkiTz()

  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedTemplate, setSelectedTemplate] =
    useState<CitizenDocumentResponseReportTemplate | null>(null)
  const [selectedGroup, setSelectedGroup] = useState<DaycareGroup | null>(null)

  const units = useQueryResult(daycaresQuery({ includeClosed: false }))
  const sortedUnits = useMemo(
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
  const templates = useQueryResult(
    getCitizenDocumentResponseTemplateOptionsQuery()
  )
  const sortedTemplates = useMemo(
    () =>
      templates.map((templateResult) => orderBy(templateResult, (t) => t.name)),
    [templates]
  )

  const groups = useQueryResult(
    selectedUnit
      ? getCitizenDocumentResponseReportGroupOptionsQuery({
          unitId: selectedUnit.id,
          from: today,
          to: today
        })
      : constantQuery([])
  )
  const sortedGroups = useMemo(
    () => groups.map((groupRows) => orderBy(groupRows, (g) => g.name)),
    [groups]
  )

  const [activeSelection, setActiveSelection] =
    useState<ReportParamSelection | null>(null)
  const rowsResult = useQueryResult(
    activeSelection
      ? getCitizenDocumentResponseReportQuery({
          unitId: activeSelection.unit.id,
          groupId: activeSelection.group.id,
          documentTemplateId: activeSelection.documentTemplate.id
        })
      : constantQuery([])
  )

  const fetchResults = () => {
    if (selectedUnit && selectedTemplate && selectedGroup) {
      setActiveSelection({
        unit: selectedUnit,
        group: selectedGroup,
        documentTemplate: selectedTemplate
      })
    }
  }

  const pertinentQuestions = useMemo(
    () =>
      activeSelection?.documentTemplate.content.sections.flatMap((s) =>
        s.questions.filter(
          (q) => q.type === 'CHECKBOX' || q.type === 'RADIO_BUTTON_GROUP'
        )
      ) ?? [],
    [activeSelection]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>
          {i18n.reports.citizenDocumentResponseReport.title}
        </Title>
        {renderResult(
          combine(sortedUnits, sortedTemplates, sortedGroups),
          ([unitOptions, templateOptions, groupOptions]) => (
            <>
              <FilterRowWide>
                <FilterLabel>
                  {i18n.reports.citizenDocumentResponseReport.filters.template}
                </FilterLabel>
                <FlexGrow>
                  <Select
                    items={templateOptions}
                    selectedItem={selectedTemplate}
                    onChange={setSelectedTemplate}
                    getItemValue={(i) => i.name}
                    getItemLabel={(i) => i.name}
                    placeholder={i18n.common.select}
                    data-qa="template-select"
                  />
                </FlexGrow>
              </FilterRowWide>
              <FilterRowWide>
                <FilterLabel>
                  {i18n.reports.citizenDocumentResponseReport.filters.unit}
                </FilterLabel>
                <FlexGrow>
                  <Select
                    items={unitOptions}
                    selectedItem={selectedUnit}
                    onChange={(value) => {
                      setSelectedGroup(null)
                      setSelectedUnit(value)
                    }}
                    getItemValue={(i) => i.name}
                    getItemLabel={(i) => i.name}
                    placeholder={i18n.common.select}
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
                    items={groupOptions}
                    selectedItem={selectedGroup}
                    onChange={setSelectedGroup}
                    getItemValue={(i) => i.name}
                    getItemLabel={(i) => i.name}
                    placeholder={i18n.common.select}
                    disabled={groupOptions.length === 0}
                    data-qa="group-select"
                  />
                </FlexGrow>
              </FilterRowWide>
              <Gap />
              <FilterRow>
                <Button
                  primary
                  disabled={
                    !(selectedUnit && selectedTemplate && selectedGroup)
                  }
                  text={i18n.common.search}
                  onClick={fetchResults}
                  data-qa="send-button"
                />
              </FilterRow>
              {renderResult(rowsResult, (reportRows) => (
                <>
                  {activeSelection && (
                    <CitizenDocumentResponseReportTable
                      rowData={reportRows}
                      questions={pertinentQuestions}
                      fileName={`${i18n.reports.citizenDocumentResponseReport.title}-${activeSelection.unit.name}-${activeSelection.group.name}-${today.format()}.csv`}
                    />
                  )}
                </>
              ))}
            </>
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
type ReportQuestion =
  | Question.CheckboxQuestion
  | Question.RadioButtonGroupQuestion

type CitizenDocumentResponseReportRowWithAnswers =
  CitizenDocumentResponseReportRow & {
    checkboxAnswersByQuestion: Record<string, AnsweredQuestion.CheckboxAnswer[]>
    radioAnswersByQuestion: Record<
      string,
      AnsweredQuestion.RadioButtonGroupAnswer[]
    >
  }

interface ReportParamSelection {
  unit: Daycare
  documentTemplate: CitizenDocumentResponseReportTemplate
  group: DaycareGroup
}

const CitizenDocumentResponseReportTable = React.memo(
  function CitizenDocumentResponseReportTable({
    rowData,
    questions,
    fileName
  }: {
    rowData: CitizenDocumentResponseReportRow[]
    questions: ReportQuestion[]
    fileName: string
  }) {
    const { i18n } = useTranslation()
    const getAnswerString = useCallback(
      (
        row: CitizenDocumentResponseReportRowWithAnswers,
        question: ReportQuestion
      ): string => {
        if (row.documentStatus !== 'COMPLETED') return emptyAnswerString
        switch (question.type) {
          case 'CHECKBOX': {
            const ca = row.checkboxAnswersByQuestion?.[question.id] ?? []
            return ca.length > 0
              ? ca[0].answer
                ? i18n.common.yes
                : i18n.common.no
              : emptyAnswerString
          }
          case 'RADIO_BUTTON_GROUP': {
            const ra = row.radioAnswersByQuestion?.[question.id] ?? []
            return ra.length > 0
              ? (question.options.find((o) => ra[0].answer === o.id)?.label ??
                  emptyAnswerString)
              : emptyAnswerString
          }
          default: {
            return emptyAnswerString
          }
        }
      },
      [i18n]
    )

    const getAnsweredAtString = useCallback(
      (row: CitizenDocumentResponseReportRowWithAnswers): string => {
        switch (row.documentStatus) {
          case 'COMPLETED': {
            return row.answeredAt?.toLocalDate().format() ?? ''
          }
          case 'CITIZEN_DRAFT': {
            return i18n.reports.citizenDocumentResponseReport.noAnswer
          }
          default: {
            return i18n.reports.citizenDocumentResponseReport.noSentDocument
          }
        }
      },
      [i18n]
    )

    const [showBackupChildren, setShowBackupChildren] = useState<boolean>(true)
    const sortedRows = useMemo(
      () =>
        orderBy(
          rowData.filter((r) => !r.isBackup || showBackupChildren),
          [(r) => r.lastName, (r) => r.firstName]
        ),
      [rowData, showBackupChildren]
    )
    const reportQnARows: CitizenDocumentResponseReportRowWithAnswers[] =
      sortedRows.map((row) => ({
        ...row,
        checkboxAnswersByQuestion:
          groupBy(
            row.documentContent?.answers.filter((a) => a.type === 'CHECKBOX'),
            (a) => a.questionId
          ) ?? {},
        radioAnswersByQuestion:
          groupBy(
            row.documentContent?.answers.filter(
              (a) => a.type === 'RADIO_BUTTON_GROUP'
            ),
            (a) => a.questionId
          ) ?? {}
      }))

    return (
      <>
        <DisplayFilterBox justifyContent="space-between" fullWidth>
          <FlexGrow>
            <Checkbox
              checked={showBackupChildren}
              onChange={setShowBackupChildren}
              label={
                i18n.reports.citizenDocumentResponseReport.filters
                  .showBackupChildren
              }
            />
          </FlexGrow>
          <ReportDownload
            data={reportQnARows}
            columns={[
              {
                label: i18n.reports.citizenDocumentResponseReport.headers.name,
                value: (row) => formatPersonName(row, 'Last First')
              },
              {
                label:
                  i18n.reports.citizenDocumentResponseReport.headers.answeredAt,
                value: (row) => getAnsweredAtString(row)
              },
              ...questions.map((q) => {
                return {
                  label: q.label,
                  value: (row: CitizenDocumentResponseReportRowWithAnswers) => {
                    return getAnswerString(row, q)
                  }
                }
              })
            ]}
            filename={fileName}
          />
        </DisplayFilterBox>
        <TableScrollable>
          <Thead>
            <Tr>
              <Th>{i18n.reports.citizenDocumentResponseReport.headers.name}</Th>
              <Th>
                {i18n.reports.citizenDocumentResponseReport.headers.answeredAt}
              </Th>
              {questions.map((cq) => (
                <Th key={cq.id}>{cq.label}</Th>
              ))}
            </Tr>
          </Thead>
          <Tbody>
            {reportQnARows.map((row) => {
              return (
                <React.Fragment key={row.childId}>
                  <Tr data-qa={`child-row-${row.childId}`}>
                    <Td data-qa="name">
                      <Strong>
                        <PersonName person={row} format="Last First" />
                      </Strong>
                    </Td>
                    <Td data-qa="answered-at">{getAnsweredAtString(row)}</Td>
                    {questions.map((qc) => {
                      return (
                        <Td
                          key={`${row.childId}-${qc.id}`}
                          data-qa={`question-${qc.id}`}
                        >
                          {getAnswerString(row, qc)}
                        </Td>
                      )
                    })}
                  </Tr>
                </React.Fragment>
              )
            })}
          </Tbody>
        </TableScrollable>
      </>
    )
  }
)

const DisplayFilterBox = styled(FixedSpaceRow)`
  margin: 20px 0;
`
