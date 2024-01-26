// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Failure, Success } from 'lib-common/api'
import { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'

import ChildHeader from './ChildHeader'
import { childrenQuery } from './queries'
import PedagogicalDocumentsSection from './sections/pedagogical-documents/PedagogicalDocumentsSection'
import PlacementTerminationSection from './sections/placement-termination/PlacementTerminationSection'
import ServiceNeedAndDailyServiceTimeSection from './sections/service-need-and-daily-service-time/ServiceNeedAndDailyServiceTimeSection'
import VasuAndLeopsSection from './sections/vasu-and-leops/VasuAndLeopsSection'

export default React.memo(function ChildPage() {
  const { childId } = useNonNullableParams<{ childId: string }>()
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
                  <VasuAndLeopsSection childId={childId} />
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
