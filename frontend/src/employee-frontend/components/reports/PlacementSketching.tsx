// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import { PlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { Arg0 } from 'lib-common/types'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { faFileAlt } from 'lib-icons'

import ReportDownload from '../../components/reports/ReportDownload'
import { getPlacementSketchingReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'

const getPlacementSketchingReportResult = wrapResult(
  getPlacementSketchingReport
)

type PlacementSketchingReportFilters = Required<
  Arg0<typeof getPlacementSketchingReport>
>

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
  const [rows, setRows] = useState<Result<PlacementSketchingReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<PlacementSketchingReportFilters>({
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

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: PlacementSketchingReportRow): boolean =>
    !(displayFilters.careArea && row.areaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getPlacementSketchingReportResult(filters).then(setRows)
  }, [filters])

  const filteredRows: PlacementSketchingReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const yesNo = (b: boolean) => (b ? i18n.common.yes : i18n.common.no)

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

  const showServiceNeedOption = filteredRows.some(
    (item) => item.serviceNeedOption !== null
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
            date={filters.earliestPreferredStartDate ?? undefined}
            onChange={(earliestPreferredStartDate) =>
              setFilters({ ...filters, earliestPreferredStartDate })
            }
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
              headers={[
                {
                  label: i18n.reports.placementSketching.preferredUnit,
                  key: 'requestedUnitName'
                },
                {
                  label: i18n.reports.placementSketching.currentUnit,
                  key: 'currentUnitName'
                },
                { label: i18n.reports.common.childName, key: 'childName' },
                { label: i18n.reports.placementSketching.dob, key: 'childDob' },
                {
                  label: i18n.reports.placementSketching.streetAddress,
                  key: 'childStreetAddr'
                },
                {
                  label: i18n.reports.placementSketching.postalCode,
                  key: 'childPostalCode'
                },
                {
                  label: `${i18n.reports.placementSketching.tel} / ${i18n.reports.placementSketching.email}`,
                  key: 'contact'
                },
                ...(showServiceNeedOption
                  ? ([
                      {
                        label:
                          i18n.reports.placementSketching.serviceNeedOption,
                        key: 'serviceNeedOption'
                      }
                    ] as const)
                  : []),
                {
                  label: i18n.reports.placementSketching.assistanceNeed,
                  key: 'assistanceNeeded'
                },
                {
                  label: i18n.reports.placementSketching.preparatory,
                  key: 'preparatoryEducation'
                },
                {
                  label: i18n.reports.placementSketching.siblingBasis,
                  key: 'siblingBasis'
                },
                {
                  label: i18n.reports.placementSketching.connected,
                  key: 'connectedDaycare'
                },
                {
                  label: i18n.reports.placementSketching.applicationStatus,
                  key: 'applicationStatus'
                },
                {
                  label: i18n.reports.placementSketching.preferredStartDate,
                  key: 'preferredStartDate'
                },
                {
                  label: i18n.reports.placementSketching.sentDate,
                  key: 'sentDate'
                },
                { label: i18n.reports.common.careAreaName, key: 'areaName' },
                {
                  label: i18n.reports.placementSketching.otherPreferredUnits,
                  key: 'otherPreferredUnits'
                },
                {
                  label: i18n.reports.placementSketching.additionalInfo,
                  key: 'additionalInfo'
                },
                {
                  label: i18n.reports.placementSketching.childMovingDate,
                  key: 'childMovingDate'
                },
                {
                  label:
                    i18n.reports.placementSketching.childCorrectedStreetAddress,
                  key: 'childCorrectedStreetAddress'
                },
                {
                  label:
                    i18n.reports.placementSketching.childCorrectedPostalCode,
                  key: 'childCorrectedPostalCode'
                },
                {
                  label: i18n.reports.placementSketching.childCorrectedCity,
                  key: 'childCorrectedCity'
                }
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
                  <Th>{i18n.reports.placementSketching.postalCode}</Th>
                  <Th>
                    {i18n.reports.placementSketching.tel} /{' '}
                    {i18n.reports.placementSketching.email}
                  </Th>
                  {showServiceNeedOption ? (
                    <Th>{i18n.reports.placementSketching.serviceNeedOption}</Th>
                  ) : null}
                  <Th>{i18n.reports.placementSketching.assistanceNeed}</Th>
                  <Th>{i18n.reports.placementSketching.preparatory}</Th>
                  <Th>{i18n.reports.placementSketching.siblingBasis}</Th>
                  <Th>{i18n.reports.placementSketching.connected}</Th>
                  <Th>{i18n.reports.placementSketching.sentDate}</Th>
                  <Th>{i18n.application.tabTitle}</Th>
                  <Th>{i18n.reports.placementSketching.applicationStatus}</Th>
                  <Th>{i18n.reports.placementSketching.preferredStartDate}</Th>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.placementSketching.otherPreferredUnits}</Th>
                  <Th>{i18n.reports.placementSketching.additionalInfo}</Th>
                  <Th>{i18n.reports.placementSketching.childMovingDate}</Th>
                  <Th>
                    {
                      i18n.reports.placementSketching
                        .childCorrectedStreetAddress
                    }
                  </Th>
                  <Th>
                    {i18n.reports.placementSketching.childCorrectedPostalCode}
                  </Th>
                  <Th>{i18n.reports.placementSketching.childCorrectedCity}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: PlacementSketchingReportRow) => (
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
                    <Td>{i18n.application.statuses[row.applicationStatus]}</Td>
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
        )}
      </ContentArea>
    </Container>
  )
})
