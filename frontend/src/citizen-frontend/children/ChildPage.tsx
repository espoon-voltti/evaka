// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Failure, Success } from 'lib-common/api'
import { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'

import ChildHeader from './ChildHeader'
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

  const user = useUser()

  return (
    <>
      <Main>
        <Container>
          <Gap size="s" />
          {renderResult(child, (child) => (
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
          ))}
        </Container>
      </Main>
      <Footer />
    </>
  )
})
