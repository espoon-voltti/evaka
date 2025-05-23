// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import type { SetStateAction } from 'react'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import { combine } from 'lib-common/api'
import type { ApplicationStatus } from 'lib-common/generated/api-types/application'
import type { PlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { placementSketchingQuery } from './queries'

interface PlacementSketchingReportFilters {
  placementStartDate: LocalDate | null
  earliestPreferredStartDate: LocalDate | null
  applicationStatus: ApplicationStatus[] | null
  earliestApplicationSentDate: LocalDate | null
  latestApplicationSentDate: LocalDate | null
}

const selectableApplicationStatuses: ApplicationStatus[] = [
  'SENT',
  'WAITING_PLACEMENT'
]

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function PlacementSketching() {
  const { i18n, lang } = useTranslation()
  const [filters, _setFilters] = useState<PlacementSketchingReportFilters>({
    placementStartDate: LocalDate.of(LocalDate.todayInSystemTz().year, 1, 1),
    earliestPreferredStartDate: LocalDate.of(
      LocalDate.todayInSystemTz().year,
      8,
      1
    ),
    applicationStatus: [],
    earliestApplicationSentDate: null,
    latestApplicationSentDate: null
  })
  const rowsResult = useQueryResult(
    filters.placementStartDate !== null
      ? placementSketchingQuery({
          ...filters,
          placementStartDate: filters.placementStartDate
        })
      : constantQuery([])
  )
  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const setFilters = useCallback(
    (filters: SetStateAction<PlacementSketchingReportFilters>) => {
      _setFilters(filters)
      setDisplayFilters(emptyDisplayFilters)
    },
    []
  )
  const displayFilter = useCallback(
    (row: PlacementSketchingReportRow): boolean =>
      !(displayFilters.careArea && row.areaName !== displayFilters.careArea),
    [displayFilters.careArea]
  )

  const filteredRowsResult = useMemo(
    () => rowsResult.map((rs) => rs.filter(displayFilter)),
    [rowsResult, displayFilter]
  )

  const yesNo = useCallback(
    (b: boolean) => (b ? i18n.common.yes : i18n.common.no),
    [i18n.common.no, i18n.common.yes]
  )

  const formatOtherPreferredUnits = ({
    otherPreferredUnits,
    requestedUnitName
  }: PlacementSketchingReportRow) =>
    otherPreferredUnits
      ? otherPreferredUnits
          .filter((unit) => unit !== requestedUnitName)
          .join(',')
      : ''

  const currentLocalDate = LocalDate.todayInSystemTz()

  const showServiceNeedOptionResult = useMemo(
    () =>
      filteredRowsResult.map((rows) =>
        rows.some((item) => item.serviceNeedOption !== null)
      ),
    [filteredRowsResult]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.placementSketching.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.placementSketching.placementStartDate}
          </FilterLabel>
          <DatePicker
            date={filters.placementStartDate}
            onChange={(placementStartDate) =>
              setFilters({ ...filters, placementStartDate })
            }
            locale="fi"
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.placementSketching.earliestPreferredStartDate}
          </FilterLabel>
          <DatePicker
            date={filters.earliestPreferredStartDate}
            onChange={(earliestPreferredStartDate) =>
              setFilters({ ...filters, earliestPreferredStartDate })
            }
            locale="fi"
          />
        </FilterRow>

        <FilterRow>
          <FlexRow>
            <FilterLabel>
              {i18n.reports.placementSketching.applicationStatus}
            </FilterLabel>
            <Wrapper>
              <MultiSelect
                options={selectableApplicationStatuses}
                onChange={(selectedItems) =>
                  setFilters({
                    ...filters,
                    applicationStatus: selectedItems.map(
                      (selectedItem) => selectedItem
                    )
                  })
                }
                value={selectableApplicationStatuses.filter(
                  (status) =>
                    filters.applicationStatus?.includes(status) ?? true
                )}
                getOptionId={(status) => status}
                getOptionLabel={(status) => i18n.application.statuses[status]}
                placeholder=""
                data-qa="select-application-status"
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FlexRow>
            <FilterLabel>
              {i18n.reports.placementSketching.applicationSentDateRange}
            </FilterLabel>
            <DatePicker
              id="earliest-sent-date"
              date={filters.earliestApplicationSentDate}
              onChange={(earliestApplicationSentDate) => {
                setFilters({ ...filters, earliestApplicationSentDate })
              }}
              hideErrorsBeforeTouched
              locale={lang}
              isInvalidDate={(d) =>
                d.isAfter(currentLocalDate)
                  ? i18n.validationErrors.dateTooEarly
                  : null
              }
            />
            <span>{' - '}</span>

            <DatePicker
              id="latest-sent-date"
              date={filters.latestApplicationSentDate}
              onChange={(latestApplicationSentDate) => {
                setFilters({ ...filters, latestApplicationSentDate })
              }}
              hideErrorsBeforeTouched
              locale={lang}
            />
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                { value: '', label: i18n.common.all },
                ...rowsResult
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

        {renderResult(
          combine(filteredRowsResult, showServiceNeedOptionResult),
          ([filteredRows, showServiceNeedOption], isReloading) =>
            isReloading ? null : (
              <>
                <ReportDownload
                  data={filteredRows.map((row) => ({
                    ...row,
                    childName: `${row.childLastName} ${row.childFirstName}`,
                    contact: `${
                      row.guardianPhoneNumber ? row.guardianPhoneNumber : ''
                    } / ${row.guardianEmail ? row.guardianEmail : ''}`,
                    serviceNeedOption: row.serviceNeedOption?.nameFi,
                    assistanceNeeded: row.assistanceNeeded ? 'k' : 'e',
                    preparatoryEducation: row.preparatoryEducation ? 'k' : 'e',
                    siblingBasis: row.siblingBasis ? 'k' : 'e',
                    connectedDaycare: row.connectedDaycare ? 'k' : 'e',
                    applicationStatus:
                      i18n.application.statuses[row.applicationStatus],
                    otherPreferredUnits: formatOtherPreferredUnits(row),
                    additionalInfo: row.additionalInfo
                  }))}
                  columns={[
                    {
                      label: i18n.reports.placementSketching.preferredUnit,
                      value: (row) => row.requestedUnitName
                    },
                    {
                      label: i18n.reports.placementSketching.currentUnit,
                      value: (row) => row.currentUnitName
                    },
                    {
                      label: i18n.reports.common.childName,
                      value: (row) => row.childName
                    },
                    {
                      label: i18n.reports.placementSketching.dob,
                      value: (row) => row.childDob.format()
                    },
                    {
                      label: i18n.reports.placementSketching.streetAddress,
                      value: (row) => row.childStreetAddr
                    },
                    {
                      label: i18n.reports.placementSketching.postalCode,
                      value: (row) => row.childPostalCode
                    },
                    {
                      label: `${i18n.reports.placementSketching.tel} / ${i18n.reports.placementSketching.email}`,
                      value: (row) => row.contact
                    },
                    {
                      label: i18n.reports.placementSketching.serviceNeedOption,
                      value: (row) => row.serviceNeedOption,
                      exclude: !showServiceNeedOption
                    },
                    {
                      label: i18n.reports.placementSketching.assistanceNeed,
                      value: (row) => row.assistanceNeeded
                    },
                    {
                      label: i18n.reports.placementSketching.preparatory,
                      value: (row) => row.preparatoryEducation
                    },
                    {
                      label: i18n.reports.placementSketching.siblingBasis,
                      value: (row) => row.siblingBasis
                    },
                    {
                      label: i18n.reports.placementSketching.connected,
                      value: (row) => row.connectedDaycare
                    },
                    {
                      label: i18n.reports.placementSketching.applicationStatus,
                      value: (row) => row.applicationStatus
                    },
                    {
                      label: i18n.reports.placementSketching.preferredStartDate,
                      value: (row) => row.preferredStartDate.format()
                    },
                    {
                      label: i18n.reports.placementSketching.sentDate,
                      value: (row) => row.sentDate.format()
                    },
                    {
                      label: i18n.reports.common.careAreaName,
                      value: (row) => row.areaName
                    },
                    {
                      label:
                        i18n.reports.placementSketching.otherPreferredUnits,
                      value: (row) => row.otherPreferredUnits
                    },
                    {
                      label: i18n.reports.placementSketching.additionalInfo,
                      value: (row) => row.additionalInfo
                    },
                    {
                      label: i18n.reports.placementSketching.childMovingDate,
                      value: (row) => row.childMovingDate?.format()
                    },
                    {
                      label:
                        i18n.reports.placementSketching
                          .childCorrectedStreetAddress,
                      value: (row) => row.childCorrectedStreetAddress
                    },
                    {
                      label:
                        i18n.reports.placementSketching
                          .childCorrectedPostalCode,
                      value: (row) => row.childCorrectedPostalCode
                    },
                    {
                      label: i18n.reports.placementSketching.childCorrectedCity,
                      value: (row) => row.childCorrectedCity
                    }
                  ]}
                  filename={`sijoitushahmottelu_${filters.placementStartDate?.formatIso()}-${
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
                      <Th>{i18n.reports.placementSketching.postalCode}</Th>
                      <Th>
                        {i18n.reports.placementSketching.tel} /{' '}
                        {i18n.reports.placementSketching.email}
                      </Th>
                      {showServiceNeedOption ? (
                        <Th>
                          {i18n.reports.placementSketching.serviceNeedOption}
                        </Th>
                      ) : null}
                      <Th>{i18n.reports.placementSketching.assistanceNeed}</Th>
                      <Th>{i18n.reports.placementSketching.preparatory}</Th>
                      <Th>{i18n.reports.placementSketching.siblingBasis}</Th>
                      <Th>{i18n.reports.placementSketching.connected}</Th>
                      <Th>{i18n.reports.placementSketching.sentDate}</Th>
                      <Th>{i18n.application.tabTitle}</Th>
                      <Th>
                        {i18n.reports.placementSketching.applicationStatus}
                      </Th>
                      <Th>
                        {i18n.reports.placementSketching.preferredStartDate}
                      </Th>
                      <Th>{i18n.reports.common.careAreaName}</Th>
                      <Th>
                        {i18n.reports.placementSketching.otherPreferredUnits}
                      </Th>
                      <Th>{i18n.reports.placementSketching.additionalInfo}</Th>
                      <Th>{i18n.reports.placementSketching.childMovingDate}</Th>
                      <Th>
                        {
                          i18n.reports.placementSketching
                            .childCorrectedStreetAddress
                        }
                      </Th>
                      <Th>
                        {
                          i18n.reports.placementSketching
                            .childCorrectedPostalCode
                        }
                      </Th>
                      <Th>
                        {i18n.reports.placementSketching.childCorrectedCity}
                      </Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {filteredRows.map((row) => (
                      <Tr key={row.applicationId} data-qa={row.applicationId}>
                        <Td data-qa="requested-unit">
                          <Link to={`/units/${row.requestedUnitId}`}>
                            {row.requestedUnitName}
                          </Link>
                        </Td>
                        <Td data-qa="current-unit">
                          {!!row.currentUnitId && (
                            <Link to={`/units/${row.currentUnitId}`}>
                              {row.currentUnitName}
                            </Link>
                          )}
                        </Td>
                        <Td data-qa="child-name">
                          <Link to={`/child-information/${row.childId}`}>
                            {row.childLastName} {row.childFirstName}
                          </Link>
                        </Td>
                        <Td>{row.childDob.format()}</Td>
                        <Td>{row.childStreetAddr}</Td>
                        <Td>{row.childPostalCode}</Td>
                        <Td>
                          {row.guardianPhoneNumber} / {row.guardianEmail}
                        </Td>
                        {showServiceNeedOption ? (
                          <Td>{row.serviceNeedOption?.nameFi}</Td>
                        ) : null}
                        <Td>{yesNo(row.assistanceNeeded ?? false)}</Td>
                        <Td>{yesNo(row.preparatoryEducation ?? false)}</Td>
                        <Td>{yesNo(row.siblingBasis ?? false)}</Td>
                        <Td>{yesNo(row.connectedDaycare ?? false)}</Td>
                        <Td>{row.sentDate.format()}</Td>
                        <Td data-qa="application">
                          <Link to={`/applications/${row.applicationId}`}>
                            <FontAwesomeIcon icon={faFileAlt} />
                          </Link>
                        </Td>
                        <Td>
                          {i18n.application.statuses[row.applicationStatus]}
                        </Td>
                        <Td>{row.preferredStartDate.format()}</Td>
                        <Td data-qa="area-name">{row.areaName}</Td>
                        <Td data-qa="other-preferred-units">
                          {formatOtherPreferredUnits(row)}
                        </Td>
                        <Td>{row.additionalInfo}</Td>
                        <Td>{row.childMovingDate?.format()}</Td>
                        <Td>{row.childCorrectedStreetAddress}</Td>
                        <Td>{row.childCorrectedPostalCode}</Td>
                        <Td>{row.childCorrectedCity}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </TableScrollable>
                <RowCountInfo rowCount={filteredRows.length} />
              </>
            )
        )}
      </ContentArea>
    </Container>
  )
})
