// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import { combine, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AttendanceSummary } from 'lib-common/generated/api-types/children'
import { ServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import YearMonth from 'lib-common/year-month'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Spinner from 'lib-components/atoms/state/Spinner'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faChevronLeft, faChevronRight } from 'lib-icons'

import { attendanceSummaryQuery } from './queries'

interface AttendanceSummaryTableProps {
  childId: UUID
  serviceNeedsResponse: Result<ServiceNeedSummary[]>
}

export default React.memo(function AttendanceSummaryTable({
  childId,
  serviceNeedsResponse
}: AttendanceSummaryTableProps) {
  const t = useTranslation()
  const [attendanceSummaryMonth, setAttendanceSummaryMonth] = useState(() =>
    YearMonth.todayInHelsinkiTz()
  )
  const attendanceSummaryRange = useMemo(
    () => FiniteDateRange.ofMonth(attendanceSummaryMonth),
    [attendanceSummaryMonth]
  )
  const attendanceSummaryResponse = useQueryResult(
    attendanceSummaryQuery({ childId, yearMonth: attendanceSummaryMonth })
  )

  return (
    <>
      <ListGrid>
        <H3>{t.children.attendanceSummary.title}</H3>
        <FixedSpaceRow alignItems="center">
          <IconOnlyButton
            icon={faChevronLeft}
            onClick={() =>
              setAttendanceSummaryMonth(attendanceSummaryMonth.subMonths(1))
            }
            aria-label={t.calendar.previousMonth}
          />
          <div>
            {attendanceSummaryMonth.month.toString().padStart(2, '0')} /{' '}
            {attendanceSummaryMonth.year}
          </div>
          <IconOnlyButton
            icon={faChevronRight}
            onClick={() =>
              setAttendanceSummaryMonth(attendanceSummaryMonth.addMonths(1))
            }
            aria-label={t.calendar.nextMonth}
          />
        </FixedSpaceRow>
      </ListGrid>
      {attendanceSummaryResponse !== undefined &&
        combine(serviceNeedsResponse, attendanceSummaryResponse).mapAll({
          failure: () => (
            <ErrorSegment title={t.common.errors.genericGetError} />
          ),
          loading: () => <Spinner />,
          success: ([serviceNeeds, attendanceSummary]) => (
            <AttendanceSummary
              serviceNeeds={serviceNeeds.filter(
                (sn) =>
                  attendanceSummaryRange.overlaps(
                    new FiniteDateRange(sn.startDate, sn.endDate)
                  ) && sn.contractDaysPerMonth !== null
              )}
              attendanceSummary={attendanceSummary}
            />
          )
        })}
    </>
  )
})

interface AttendanceSummaryProps {
  serviceNeeds: ServiceNeedSummary[]
  attendanceSummary: AttendanceSummary
}

const AttendanceSummary = ({
  serviceNeeds,
  attendanceSummary: { attendanceDays }
}: AttendanceSummaryProps) => {
  const t = useTranslation()
  return (
    <>
      {serviceNeeds.length > 0 ? (
        serviceNeeds.map(({ startDate, contractDaysPerMonth }) => {
          const warning =
            contractDaysPerMonth !== null &&
            attendanceDays > contractDaysPerMonth
          return (
            <React.Fragment key={startDate.formatIso()}>
              <ListGrid>
                <Label>{t.children.attendanceSummary.attendanceDays}</Label>
                <span>
                  <Days warning={warning}>{attendanceDays}</Days> /{' '}
                  {contractDaysPerMonth} {t.common.datetime.dayShort}
                </span>
              </ListGrid>
              {warning && (
                <AlertBox message={t.children.attendanceSummary.warning} />
              )}
            </React.Fragment>
          )
        })
      ) : (
        <div>{t.children.attendanceSummary.empty}</div>
      )}
    </>
  )
}

const Days = styled.span<{ warning: boolean }>`
  ${(p) =>
    p.warning &&
    `
    color: ${colors.status.warning}
  `}
`
