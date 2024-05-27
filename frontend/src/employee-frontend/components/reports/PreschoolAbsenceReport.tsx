// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'

import { combine } from 'lib-common/api'
import {
  Daycare,
  DaycareGroup,
  PreschoolTerm
} from 'lib-common/generated/api-types/daycare'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { preschoolTermsQuery } from '../holiday-term-periods/queries'
import { unitGroupsQuery, unitsQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { preschoolAbsenceReportQuery } from './queries'

export default React.memo(function PreschoolAbsenceReport() {
  const { i18n } = useTranslation()

  const allOption = { name: i18n.common.all, id: null }
  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedGroup, setSelectedGroup] = useState<
    DaycareGroup | null | { name: string; id: string | null }
  >(allOption)
  const [selectedTerm, setSelectedTerm] = useState<PreschoolTerm | null>(null)
  const units = useQueryResult(unitsQuery({ includeClosed: true }))
  const groups = useQueryResult(
    queryOrDefault(
      unitGroupsQuery,
      []
    )(selectedUnit?.id ? { daycareId: selectedUnit.id } : null)
  )
  const terms = useQueryResult(preschoolTermsQuery())


  const groupOptions = useMemo(
    () => groups.map((g) => [allOption, ...orderBy(g, (item) => item.name)]),
    [groups, allOption]
  )

  const daycareOptions = useMemo(
    () => units.map((d) => orderBy(d, (item) => item.name)),
    [units]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.preschoolAbsences.title}</Title>
        {renderResult(
          combine(daycareOptions, terms, groupOptions),
          ([unitResult, termResult, groupResult]) => (
            <>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.preschoolAbsences.filters.preschoolTerm.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={termResult}
                    onChange={setSelectedTerm}
                    selectedItem={selectedTerm}
                    getItemLabel={(item) => item.finnishPreschool.format()}
                    placeholder={
                      i18n.reports.preschoolAbsences.filters.preschoolTerm
                        .placeholder
                    }
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {
                    i18n.reports.preschoolAbsences.filters.daycareSelection
                      .label
                  }
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={unitResult}
                    onChange={setSelectedUnit}
                    selectedItem={selectedUnit}
                    getItemLabel={(item) => item.name}
                    placeholder={i18n.filters.unitPlaceholder}
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.preschoolAbsences.filters.groupSelection.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={groupResult}
                    onChange={setSelectedGroup}
                    selectedItem={selectedGroup}
                    getItemLabel={(item) => item.name}
                    placeholder={
                      i18n.reports.preschoolAbsences.filters.groupSelection
                        .placeholder
                    }
                  />
                </FlexRow>
              </FilterRow>
              {selectedTerm && selectedUnit && (
                <PreschoolAbsenceGrid
                  term={selectedTerm}
                  daycare={selectedUnit}
                  groupId={selectedGroup?.id}
                />
              )}
            </>
          )
        )}
      </ContentArea>
    </Container>
  )
})

const PreschoolAbsenceGrid = ({
  term,
  daycare,
  groupId
}: {
  term: PreschoolTerm
  daycare: Daycare
  groupId?: string | null
}) => {
  const { i18n } = useTranslation()
  const reportResult = useQueryResult(
    preschoolAbsenceReportQuery({
      termId: term.id,
      unitId: daycare.id,
      groupId: groupId ?? null
    })
  )
  return renderResult(reportResult, (report) => {
    const dataRows = report.map((row) => ({
      childId: row.childId,
      firstName: row.firstName,
      lastName: row.lastName,
      TOTAL: row.hourlyTypeResults.OTHER_ABSENCE + row.hourlyTypeResults.SICKLEAVE + row.hourlyTypeResults.UNKNOWN_ABSENCE,
      OTHER_ABSENCE: row.hourlyTypeResults.OTHER_ABSENCE,
      SICKLEAVE: row.hourlyTypeResults.SICKLEAVE,
      UNKNOWN_ABSENCE: row.hourlyTypeResults.UNKNOWN_ABSENCE
    }))

    return (
      <>
        <ReportDownload
          data={dataRows}
          headers={[
            { key: 'firstName', label: i18n.reports.preschoolAbsences.firstName },
            { key: 'lastName', label: i18n.reports.preschoolAbsences.lastName },
            { key: 'TOTAL', label: i18n.reports.preschoolAbsences.total },
            {
              key: 'OTHER_ABSENCE',
              label: i18n.absences.absenceTypes.OTHER_ABSENCE
            },
            { key: 'SICKLEAVE', label: i18n.absences.absenceTypes.SICKLEAVE },
            {
              key: 'UNKNOWN_ABSENCE',
              label: i18n.absences.absenceTypes.UNKNOWN_ABSENCE
            }
          ]}
          filename={`${i18n.reports.preschoolAbsences.title} ${daycare.name} ${term.finnishPreschool.format()}.csv`}
        />
        <TableScrollable>
          <Thead>
            <Tr>
              <Th>{i18n.reports.preschoolAbsences.firstName}</Th>
              <Th>{i18n.reports.preschoolAbsences.lastName}</Th>
              <Th>{`${i18n.reports.preschoolAbsences.total} ${i18n.reports.preschoolAbsences.hours}`}</Th>
              <Th>{`${i18n.absences.absenceTypes.OTHER_ABSENCE} ${i18n.reports.preschoolAbsences.hours}`}</Th>
              <Th>{`${i18n.absences.absenceTypes.SICKLEAVE} ${i18n.reports.preschoolAbsences.hours}`}</Th>
              <Th>{`${i18n.absences.absenceTypes.UNKNOWN_ABSENCE} ${i18n.reports.preschoolAbsences.hours}`}</Th>

            </Tr>
          </Thead>
          <Tbody>
            {dataRows.length > 0 ? (
              dataRows.map((row, rowIndex) => (
                <Tr key={`${rowIndex}`}>
                  <Td>{row.firstName}</Td>
                  <Td>{row.lastName}</Td>
                  <Td>{row.UNKNOWN_ABSENCE + row.OTHER_ABSENCE + row.SICKLEAVE}</Td>
                  <Td>{row.OTHER_ABSENCE}</Td>
                  <Td>{row.SICKLEAVE}</Td>
                  <Td>{row.UNKNOWN_ABSENCE}</Td>
                </Tr>
              ))
            ) : (
              <Tr>
                <Td colSpan={6}>{i18n.common.noResults}</Td>
              </Tr>
            )}
          </Tbody>
        </TableScrollable>
      </>
    )
  })
}
