// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'

import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

import ChildHeader from './ChildHeader'
import PlacementTerminationSection from './PlacementTerminationSection'
import { getChild } from './api'

export default React.memo(function ChildPage() {
  const t = useTranslation()
  const { childId } = useParams<{ childId: string }>()
  const [childResponse] = useApiState(() => getChild(childId), [childId])

  return (
    <>
      <Container>
        <ReturnButton label={t.common.return} />
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
