// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import { useParams } from 'react-router-dom'
import { renderResult } from '../async-rendering'
import Footer from '../Footer'
import { getChild } from './api'
import ChildHeader from './ChildHeader'
import PlacementTerminationSection from './PlacementTerminationSection'

export default React.memo(function ChildPage() {
  const { childId } = useParams<{ childId: string }>()
  const [childResponse] = useApiState(() => getChild(childId), [childId])

  return (
    <>
      <Container>
        <Gap size="s" />
        {renderResult(childResponse, (child) => (
          <ContentArea opaque>
            <ChildHeader child={child} />
            <HorizontalLine slim dashed />
            <PlacementTerminationSection childId={childId} />
          </ContentArea>
        ))}
      </Container>
      <Footer />
    </>
  )
})
