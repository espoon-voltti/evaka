// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { useTranslation } from 'citizen-frontend/localization'
import { combine, Failure, Result, Success, wrapResult } from 'lib-common/api'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Spinner from 'lib-components/atoms/state/Spinner'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { renderResult } from '../../../async-rendering'
import {
  getChildDailyServiceTimes,
  getChildServiceNeeds
} from '../../../generated/api-clients/children'
import { childrenQuery, childServiceApplicationsQuery } from '../../queries'

import AttendanceSummaryTable from './AttendanceSummaryTable'
import DailyServiceTimeTable from './DailyServiceTimeTable'
import ServiceApplications from './ServiceApplications'
import ServiceNeedTable from './ServiceNeedTable'

interface ServiceNeedProps {
  childId: ChildId
  showServiceTimes: boolean
}

const getChildServiceNeedsResult = wrapResult(getChildServiceNeeds)
const getChildDailyServiceTimesResult = wrapResult(getChildDailyServiceTimes)

export default React.memo(function ServiceNeedAndDailyServiceTimeSection({
  childId,
  showServiceTimes
}: ServiceNeedProps) {
  const t = useTranslation()
  const [open, setOpen] = useState(false)
  const [serviceNeedsResponse] = useApiState(
    () => getChildServiceNeedsResult({ childId }),
    [childId]
  )
  const [dailyServiceTimesResponse] = useApiState(
    () =>
      showServiceTimes
        ? getChildDailyServiceTimesResult({ childId })
        : Promise.resolve(Success.of([])),
    [childId, showServiceTimes]
  )

  const hasContractDays = serviceNeedsResponse
    .map((serviceNeeds) =>
      serviceNeeds.some(
        ({ contractDaysPerMonth }) => contractDaysPerMonth !== null
      )
    )
    .getOrElse(false)

  const serviceApplications = useQueryResult(
    childServiceApplicationsQuery({ childId })
  )

  const serviceApplicationCreationPossible: Result<boolean> = useQueryResult(
    childrenQuery()
  )
    .map((children) => children.find((child) => child.id === childId))
    .chain((child) =>
      child
        ? Success.of(child.serviceApplicationCreationPossible)
        : Failure.of({ message: 'Child not found' })
    )

  return (
    <ResponsiveWholePageCollapsible
      title={
        showServiceTimes
          ? t.children.serviceNeedAndDailyServiceTime.titleWithDailyServiceTime
          : t.children.serviceNeedAndDailyServiceTime.title
      }
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      data-qa="collapsible-service-need-and-daily-service-time"
    >
      <H3>{t.children.serviceNeed.title}</H3>
      {serviceNeedsResponse.mapAll({
        failure: () => <ErrorSegment title={t.common.errors.genericGetError} />,
        loading: () => <Spinner />,
        success: (serviceNeeds) => (
          <ServiceNeedTable serviceNeeds={serviceNeeds} />
        )
      })}
      {featureFlags.citizenAttendanceSummary && hasContractDays && (
        <>
          <Gap size="s" />
          <AttendanceSummaryTable
            childId={childId}
            serviceNeedsResponse={serviceNeedsResponse}
          />
        </>
      )}
      {showServiceTimes && (
        <>
          <TabletAndDesktop>
            <Gap size="m" />
          </TabletAndDesktop>
          <MobileOnly>
            <HorizontalLine slim />
          </MobileOnly>
          <H3>{t.children.dailyServiceTime.title}</H3>
          {dailyServiceTimesResponse.mapAll({
            failure: () => (
              <ErrorSegment title={t.common.errors.genericGetError} />
            ),
            loading: () => <Spinner />,
            success: (dailyServiceTimes) => (
              <DailyServiceTimeTable dailyServiceTimes={dailyServiceTimes} />
            )
          })}
        </>
      )}
      {featureFlags.serviceApplications &&
        renderResult(
          combine(serviceApplications, serviceApplicationCreationPossible),
          ([serviceApplications, serviceApplicationCreationPossible]) => {
            const hasApplications = serviceApplications.length > 0

            if (!hasApplications && !serviceApplicationCreationPossible)
              return null

            return (
              <>
                <TabletAndDesktop>
                  <Gap size="m" />
                </TabletAndDesktop>
                <MobileOnly>
                  <HorizontalLine slim />
                </MobileOnly>
                <H3>{t.children.serviceApplication.title}</H3>
                <ServiceApplications
                  childId={childId}
                  applications={serviceApplications}
                  canCreate={serviceApplicationCreationPossible}
                />
              </>
            )
          }
        )}
    </ResponsiveWholePageCollapsible>
  )
})
