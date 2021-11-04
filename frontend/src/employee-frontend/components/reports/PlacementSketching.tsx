// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import { PlacementSketchingRow } from '../../types/reports'
import {
  getPlacementSketchingReport,
  PlacementSketchingReportFilters
} from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import LocalDate from 'lib-common/local-date'
import { distinct } from '../../utils'
import { faFileAlt } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

function PlacementSketching() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<PlacementSketchingRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<PlacementSketchingReportFilters>({
    placementStartDate: LocalDate.of(LocalDate.today().year, 1, 1),
    earliestPreferredStartDate: LocalDate.of(LocalDate.today().year, 8, 1)
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: PlacementSketchingRow): boolean => {
    return !(
      displayFilters.careArea && row.areaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getPlacementSketchingReport(filters).then(setRows)
  }, [filters])

  const filteredRows: PlacementSketchingRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const yesNo = (b: boolean) => (b ? i18n.common.yes : i18n.common.no)

  const formatOtherPreferredUnits = ({
    otherPreferredUnits,
    requestedUnitName
  }: PlacementSketchingRow) =>
    otherPreferredUnits
      ? otherPreferredUnits
          .filter((unit) => unit !== requestedUnitName)
          .join(',')
      : ''

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.placementSketching.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.placementSketching.placementStartDate}
          </FilterLabel>
          <DatePickerDeprecated
            date={filters.placementStartDate}
            onChange={(placementStartDate) =>
              setFilters({ ...filters, placementStartDate })
            }
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.placementSketching.earliestPreferredStartDate}
          </FilterLabel>
          <DatePickerDeprecated
            date={filters.earliestPreferredStartDate}
            onChange={(earliestPreferredStartDate) =>
              setFilters({ ...filters, earliestPreferredStartDate })
            }
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                { value: '', label: i18n.common.all },
                ...rows
                  .map((rs) =>
                    distinct(rs.map((row) => row.areaName)).map((s) => ({
                      value: s,
                      label: s
                    }))
                  )
                  .getOrElse([])
              ]}
              onChange={(option) =>
                option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              selectedItem={
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
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
              getItemLabel={(item) => item.label}
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
                childName: `${row.childLastName} ${row.childFirstName}`,
                contact: `${
                  row.guardianPhoneNumber ? row.guardianPhoneNumber : ''
                } / ${row.guardianEmail ? row.guardianEmail : ''}`,
                assistanceNeeded: row.assistanceNeeded ? 'k' : 'e',
                preparatoryEducation: row.preparatoryEducation ? 'k' : 'e',
                siblingBasis: row.siblingBasis ? 'k' : 'e',
                connectedDaycare: row.connectedDaycare ? 'k' : 'e',
                otherPreferredUnits: formatOtherPreferredUnits(row)
              }))}
              headers={[
                { label: 'Haettu yksikkö', key: 'requestedUnitName' },
                { label: 'Nykyinen yksikkö', key: 'currentUnitName' },
                { label: 'Lapsi', key: 'childName' },
                { label: 'Lapsen syntymäpäivä', key: 'childDob' },
                { label: 'Lapsen osoite', key: 'childStreetAddr' },
                { label: 'Yhteystiedot', key: 'contact' },
                { label: 'Tuen tarve', key: 'assistanceNeeded' },
                { label: 'Valmistava', key: 'preparatoryEducation' },
                { label: 'Sisarusperuste', key: 'siblingBasis' },
                { label: 'Liittyvä vaka', key: 'connectedDaycare' },
                { label: 'Toivottu aloituspäivä', key: 'preferredStartDate' },
                { label: 'Lähetyspäivä', key: 'sentDate' },
                { label: 'Palvelualue', key: 'areaName' },
                { label: 'Muut toiveet', key: 'otherPreferredUnits' }
              ]}
              filename={`sijoitushahmottelu_${filters.placementStartDate.formatIso()}-${
                filters.earliestPreferredStartDate?.formatIso() ?? ''
              }.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.placementSketching.preferredUnit}</Th>
                  <Th>{i18n.reports.placementSketching.currentUnit}</Th>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>{i18n.reports.placementSketching.dob}</Th>
                  <Th>{i18n.reports.placementSketching.streetAddress}</Th>
                  <Th>
                    {i18n.reports.placementSketching.tel} /{' '}
                    {i18n.reports.placementSketching.email}
                  </Th>
                  <Th>{i18n.reports.placementSketching.assistanceNeed}</Th>
                  <Th>{i18n.reports.placementSketching.preparatory}</Th>
                  <Th>{i18n.reports.placementSketching.siblingBasis}</Th>
                  <Th>{i18n.reports.placementSketching.connected}</Th>
                  <Th>{i18n.reports.placementSketching.sentDate}</Th>
                  <Th>{i18n.application.tabTitle}</Th>
                  <Th>{i18n.reports.placementSketching.preferredStartDate}</Th>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.placementSketching.otherPreferredUnits}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: PlacementSketchingRow) => (
                  <Tr
                    key={`${row.applicationId}`}
                    data-qa={`${row.applicationId}`}
                  >
                    <Td data-qa={'requested-unit'}>
                      <Link to={`/units/${row.requestedUnitId}`}>
                        {row.requestedUnitName}
                      </Link>
                    </Td>
                    <Td data-qa={'current-unit'}>
                      {row.currentUnitId && (
                        <Link to={`/units/${row.currentUnitId}`}>
                          {row.currentUnitName}
                        </Link>
                      )}
                    </Td>
                    <Td data-qa={'child-name'}>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childLastName} {row.childFirstName}
                      </Link>
                    </Td>
                    <Td>{row.childDob.format()}</Td>
                    <Td>{row.childStreetAddr}</Td>
                    <Td>
                      {row.guardianPhoneNumber} / {row.guardianEmail}
                    </Td>
                    <Td>{yesNo(row.assistanceNeeded)}</Td>
                    <Td>{yesNo(row.preparatoryEducation)}</Td>
                    <Td>{yesNo(row.siblingBasis)}</Td>
                    <Td>{yesNo(row.connectedDaycare)}</Td>
                    <Td>{row.sentDate.format()}</Td>
                    <Td data-qa={'application'}>
                      <Link to={`/applications/${row.applicationId}`}>
                        <FontAwesomeIcon icon={faFileAlt} />
                      </Link>
                    </Td>
                    <Td>{row.preferredStartDate.format()}</Td>
                    <Td data-qa={'area-name'}>{row.areaName}</Td>
                    <Td data-qa={'other-preferred-units'}>
                      {formatOtherPreferredUnits(row)}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default PlacementSketching
