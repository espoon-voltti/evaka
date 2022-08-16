// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import Main from 'lib-components/atoms/Main'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

import ChildHeader from './ChildHeader'
import { getChild } from './api'
import AssistanceNeedSection from './sections/assistance-need-decision/AssistanceNeedSection'
import ChildConsentsSection from './sections/consents/ChildConsentsSection'
import PedagogicalDocumentsSection from './sections/pedagogical-documents/PedagogicalDocumentsSection'
import PlacementTerminationSection from './sections/placement-termination/PlacementTerminationSection'
import VasuAndLeopsSection from './sections/vasu-and-leops/VasuAndLeopsSection'

export default React.memo(function ChildPage() {
  const t = useTranslation()
  const { childId } = useNonNullableParams<{ childId: string }>()
  const [childResponse] = useApiState(() => getChild(childId), [childId])

  const user = useUser()

  return (
    <>
      <Main>
        <Container>
          <Gap size="s" />
          <ReturnButton label={t.common.return} />
          <Gap size="s" />
          {renderResult(childResponse, (child) => (
            <>
              <ContentArea opaque>
                <ChildHeader child={child} />
              </ContentArea>
              {user?.accessibleFeatures.childDocumentation && (
                <>
                  <Gap size="s" />
                  <PedagogicalDocumentsSection childId={childId} />
                  <Gap size="s" />
                  <VasuAndLeopsSection childId={childId} />
                </>
              )}
              <Gap size="s" />
              <ChildConsentsSection childId={childId} />
              <Gap size="s" />
              <AssistanceNeedSection childId={childId} />
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
