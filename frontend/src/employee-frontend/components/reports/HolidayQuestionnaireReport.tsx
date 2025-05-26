import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { HolidayQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import {
  HolidayReportRow,
  ChildWithName
} from 'lib-common/generated/api-types/reports'
import {
  DaycareId,
  GroupId,
  HolidayQuestionnaireId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChevronDown, faChevronRight } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { questionnairesQuery } from '../holiday-term-periods/queries'
import { daycaresQuery, unitGroupsQuery } from '../unit/queries'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { holidayQuestionnaireReportQuery } from './queries'

interface ReportQueryParams {
  unitId: DaycareId
  questionnaireId: HolidayQuestionnaireId
  groupIds: GroupId[] | null
}

export default React.memo(function HolidayQuestionnaireReport() {
  const { i18n, lang } = useTranslation()
  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedQuestionnaire, setSelectedQuestionnaire] =
    useState<HolidayQuestionnaire | null>(null)
  const [selectedGroups, setSelectedGroups] = useState<GroupId[] | null>(null)

  const units = useQueryResult(daycaresQuery({ includeClosed: false }))
  const questionnaires = useQueryResult(questionnairesQuery())

  const firstPeriod = (questionnaire: HolidayQuestionnaire) => {
    return questionnaire.type === 'FIXED_PERIOD'
      ? questionnaire.periodOptions.at(0)!
      : questionnaire.period
  }

  const lastPeriod = (questionnaire: HolidayQuestionnaire) => {
    return questionnaire.type === 'FIXED_PERIOD'
      ? questionnaire.periodOptions.at(-1)!
      : questionnaire.period
  }

  const questionnaireOptions = useMemo(
    () =>
      questionnaires.map((g) =>
        orderBy(
          g.filter((q) => lastPeriod(q).end >= LocalDate.todayInHelsinkiTz()),
          (item) => lastPeriod(item).end
        )
      ),
    [questionnaires]
  )

  const daycareOptions = useMemo(
    () =>
      units.map((d) => {
        return orderBy(
          d.filter((u) => u.enabledPilotFeatures.includes('RESERVATIONS')),
          (item) => item.name
        )
      }),
    [units]
  )

  const groupsResult = useQueryResult(
    selectedUnit && selectedQuestionnaire
      ? unitGroupsQuery({
          daycareId: selectedUnit.id,
          from: firstPeriod(selectedQuestionnaire).start,
          to: lastPeriod(selectedQuestionnaire).end
        })
      : constantQuery([])
  )

  const groupOptions = useMemo(
    () =>
      groupsResult.map((g) => {
        return orderBy(g, (item) => item.name)
      }),
    [groupsResult]
  )

  const [activeParams, setActiveParams] = useState<ReportQueryParams | null>(
    null
  )

  const reportResult = useQueryResult(
    activeParams
      ? holidayQuestionnaireReportQuery(activeParams)
      : constantQuery([])
  )

  const fetchResults = () => {
    if (selectedUnit && selectedQuestionnaire) {
      setActiveParams({
        unitId: selectedUnit.id,
        groupIds: selectedGroups,
        questionnaireId: selectedQuestionnaire.id
      })
    }
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.holidayQuestionnaire.title}</Title>
        {renderResult(
          combine(daycareOptions, questionnaireOptions),
          ([unitResult, questionnaireResult]) => (
            <>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.holidayQuestionnaire.unitFilter}
                </FilterLabel>

                <Combobox
                  fullWidth
                  items={unitResult}
                  onChange={(item) => {
                    setSelectedGroups([])
                    setSelectedUnit(item)
                  }}
                  selectedItem={selectedUnit}
                  getItemLabel={(item) => item.name}
                  placeholder={i18n.filters.unitPlaceholder}
                  data-qa="unit-select"
                  getItemDataQa={({ id }) => `unit-${id}`}
                />
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.holidayQuestionnaire.questionnaireFilter}
                </FilterLabel>

                <Combobox
                  fullWidth
                  items={questionnaireResult}
                  onChange={(item) => {
                    setSelectedGroups([])
                    setSelectedQuestionnaire(item)
                  }}
                  selectedItem={selectedQuestionnaire}
                  getItemLabel={(item) => item.title[lang]}
                  placeholder={
                    i18n.reports.holidayQuestionnaire
                      .questionnaireFilterPlaceholder
                  }
                  data-qa="period-select"
                  getItemDataQa={({ id }) => `term-${id}`}
                />
              </FilterRow>
            </>
          )
        )}

        {renderResult(groupOptions, (groups) => (
          <FilterRow>
            <FilterLabel>
              {i18n.reports.holidayQuestionnaire.groupFilter}
            </FilterLabel>
            <div style={{ width: '100%' }}>
              <MultiSelect
                isClearable
                options={groups}
                getOptionId={(item) => item.id}
                getOptionLabel={(item) => item.name}
                placeholder={
                  i18n.reports.holidayQuestionnaire.groupFilterPlaceholder
                }
                onChange={(item) => setSelectedGroups(item.map((i) => i.id))}
                value={groups.filter(
                  (g) => selectedGroups?.includes(g.id) === true
                )}
                data-qa="group-select"
              />
            </div>
          </FilterRow>
        ))}
        <Gap />
        <FilterRow>
          <Button
            primary
            disabled={!(selectedUnit && selectedQuestionnaire)}
            text={i18n.common.search}
            onClick={fetchResults}
            data-qa="send-button"
          />
        </FilterRow>
        <Gap />
        {renderResult(reportResult, (report) => (
          <HolidayQuestionnaireReportGrid reportResult={report} />
        ))}
      </ContentArea>
    </Container>
  )
})

type ReportDisplayRow = {
  date: LocalDate
  presentChildren: ChildWithName[]
  assistanceChildren: ChildWithName[]
  coefficientSum: number
  requiredStaffCount: number
  absentCount: number
  noResponseChildren: ChildWithName[]
}

const HolidayQuestionnaireReportGrid = React.memo(
  function HolidayQuestionnaireReportGrid({
    reportResult
  }: {
    reportResult: HolidayReportRow[]
  }) {
    const { i18n } = useTranslation()

    const [expandingInfo, infoExpansion] = useBoolean(false)

    const sortedReportResult = useMemo(() => {
      const displayRows: ReportDisplayRow[] = reportResult.map((row) => ({
        date: row.date,
        presentChildren: orderChildren(row.presentChildren),
        assistanceChildren: row.assistanceChildren,
        coefficientSum: row.presentOccupancyCoefficient,
        requiredStaffCount: row.requiredStaff,
        absentCount: row.absentCount,
        noResponseChildren: row.noResponseChildren
      }))
      return orderBy(displayRows, [(r) => r.date], ['asc'])
    }, [reportResult])
    return (
      <>
        {expandingInfo && (
          <ExpandingInfoBox
            info={i18n.reports.holidayQuestionnaire.occupancyColumnInfo}
            close={() => infoExpansion.off()}
          />
        )}
        <TableScrollable>
          <Thead>
            <Tr>
              <TinyTh />
              <ShortTh>{i18n.reports.holidayQuestionnaire.dateColumn}</ShortTh>
              <Th>{i18n.reports.holidayQuestionnaire.presentColumn}</Th>
              <Th>{i18n.reports.holidayQuestionnaire.assistanceColumn}</Th>
              <ShortTh>
                {i18n.reports.holidayQuestionnaire.occupancyColumn}
                <InfoButton
                  onClick={() => infoExpansion.on()}
                  aria-label={i18n.common.openExpandingInfo}
                />
              </ShortTh>
              <ShortTh>{i18n.reports.holidayQuestionnaire.staffColumn}</ShortTh>
              <ShortTh>
                {i18n.reports.holidayQuestionnaire.absentColumn}
              </ShortTh>
              <Th>{i18n.reports.holidayQuestionnaire.noResponseColumn}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {sortedReportResult.length > 0 ? (
              sortedReportResult.map((row) => (
                <DailyQuestionnaireRow key={row.date.formatIso()} row={row} />
              ))
            ) : (
              <Tr>
                <Td colSpan={8}>{i18n.common.noResults}</Td>
              </Tr>
            )}
          </Tbody>
        </TableScrollable>
      </>
    )
  }
)

type TooltipProps = { fullText: string }
const TooltippedChildListItem = React.memo(function TooltippedChildListItem(
  props: TooltipProps
) {
  return (
    <div>
      <Tooltip tooltip={props.fullText}>
        <ChildListLabel data-qa="child-name">{props.fullText}</ChildListLabel>
      </Tooltip>
    </div>
  )
})

type StatRowProps = { row: ReportDisplayRow }
const DailyQuestionnaireRow = React.memo(function DailyQuestionnaireRow(
  props: StatRowProps
) {
  const [isExpanded, expansion] = useBoolean(false)
  const { i18n, lang } = useTranslation()
  const { row } = props
  return (
    <Tr data-qa="holiday-questionnaire-row">
      <TinyTd data-qa="resize-column">
        <Button
          text=""
          aria-label={i18n.reports.holidayQuestionnaire.showMoreButton}
          onClick={() => expansion.toggle()}
          icon={isExpanded ? faChevronDown : faChevronRight}
          appearance="link"
        />
      </TinyTd>
      <ShortTd data-qa="date-column">
        {capitalizeFirstLetter(row.date.format('EEEEEE dd.MM.yyyy', lang))}
      </ShortTd>
      <Td data-qa="present-children-column">
        <ChildList list={row.presentChildren} showFull={isExpanded} />
      </Td>
      <Td data-qa="assistance-children-column">
        <ChildList list={row.assistanceChildren} showFull={isExpanded} />
      </Td>
      <ShortTd data-qa="coefficient-sum-column">
        {row.coefficientSum.toFixed(2).replace('.', ',')}
      </ShortTd>
      <ShortTd data-qa="staff-count-column">{row.requiredStaffCount}</ShortTd>
      <ShortTd data-qa="absence-count-column">{row.absentCount}</ShortTd>
      <Td data-qa="no-response-children-column">
        <ChildList list={row.noResponseChildren} showFull={isExpanded} />
      </Td>
    </Tr>
  )
})

type ChildListProps = { list: ChildWithName[]; showFull: boolean }
const ChildList = React.memo(function ChildList({
  list,
  showFull
}: ChildListProps) {
  const { i18n } = useTranslation()
  const shortMaxLength = 5
  const extraCount = list.length - shortMaxLength
  const visibleList = useMemo(
    () =>
      !showFull && list.length > shortMaxLength
        ? list.slice(0, shortMaxLength)
        : list,
    [list, showFull]
  )

  return (
    <CellWrapper>
      {visibleList.map((c, i) => (
        <TooltippedChildListItem
          key={c.id}
          fullText={`${i + 1}. ${c.lastName} ${formatFirstName(c)}`}
        />
      ))}
      {!showFull && extraCount > 0 && (
        <P
          noMargin
        >{`+${extraCount} ${i18n.reports.holidayQuestionnaire.moreText}`}</P>
      )}
    </CellWrapper>
  )
})

const orderChildren = (children: ChildWithName[]) =>
  orderBy(children, [(c) => c.lastName, (c) => c.firstName, (c) => c.id])

const CellWrapper = styled.div`
  display: flex;
  flex-direction: column;
`

const ChildListLabel = styled.p`
  margin: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 200px;
  overflow: hidden;
`

const ShortTd = styled(Td)`
  max-width: 90px;
`

const ShortTh = styled(Th)`
  max-width: 90px;
`

const TinyTh = styled(Th)`
  max-width: 50px;
`
const TinyTd = styled(Td)`
  max-width: 50px;
`
