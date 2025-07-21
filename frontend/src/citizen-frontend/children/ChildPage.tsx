// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Failure, Success } from 'lib-common/api'
import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import ChildHeader, { childPageNameFormat } from './ChildHeader'
import { childrenQuery } from './queries'
import ChildDocumentsSection from './sections/ChildDocumentsSection'
import { AbsenceApplicationsSection } from './sections/absence-applications/AbsenceApplicationsSection'
import PedagogicalDocumentsSection from './sections/pedagogical-documents/PedagogicalDocumentsSection'
import PlacementTerminationSection from './sections/placement-termination/PlacementTerminationSection'
import ServiceNeedAndDailyServiceTimeSection from './sections/service-need-and-daily-service-time/ServiceNeedAndDailyServiceTimeSection'

export default React.memo(function ChildPage() {
  const childId = useIdRouteParam<ChildId>('childId')
  const children = useQueryResult(childrenQuery())
  const child = children.chain<ChildAndPermittedActions>((children) => {
    const child = children.find((child) => child.id === childId)
    return child ? Success.of(child) : Failure.of({ message: 'Not found' })
  })

  return (
    <>
      <Main>
        <Container>
          <Gap size="s" />
          {renderResult(child, (child) => (
            <ChildData child={child} />
          ))}
        </Container>
      </Main>
      <Footer />
    </>
  )
})

const ChildData = ({ child }: { child: ChildAndPermittedActions }) => {
  const i18n = useTranslation()
  const { id: childId, firstName, lastName } = child
  useTitle(i18n, formatPersonName({ firstName, lastName }, childPageNameFormat))
  const user = useUser()

  return (
    <>
      <ContentArea opaque>
        <ChildHeader child={child} />
      </ContentArea>
      <Gap size="s" />
      <ServiceNeedAndDailyServiceTimeSection
        childId={childId}
        showServiceTimes={child.permittedActions.includes(
          'READ_DAILY_SERVICE_TIMES'
        )}
      />
      {user?.accessibleFeatures.childDocumentation && (
        <>
          <Gap size="s" />
          <PedagogicalDocumentsSection childId={childId} />
          <Gap size="s" />
          <ChildDocumentsSection childId={childId} />
        </>
      )}
      {featureFlags.absenceApplications &&
        child.absenceApplicationCreationPossible && (
          <>
            <Gap size="s" />
            <AbsenceApplicationsSection childId={childId} />
          </>
        )}
      <Gap size="s" />
      <PlacementTerminationSection childId={childId} />
    </>
  )
}
