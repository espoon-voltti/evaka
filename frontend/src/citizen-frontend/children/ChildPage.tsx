// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine } from 'lib-common/api'
import { Child } from 'lib-common/generated/api-types/children'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import { useParams } from 'react-router-dom'
import { RoundImage } from 'lib-components/atoms/RoundImage'
import colors from 'lib-customizations/common'
import { faUser } from 'lib-icons'
import { renderResult } from '../async-rendering'
import Footer from '../Footer'
import { useTranslation } from '../localization'
import { getChild, getPlacements } from './api'

function ChildHeader({
  child: { firstName, imageId, group, lastName }
}: {
  child: Child
}) {
  return (
    <div>
      <RoundImage
        size="XL"
        sizeDesktop="XXL"
        src={
          imageId ? `/api/application/citizen/child-images/${imageId}` : null
        }
        fallbackContent={faUser}
        fallbackColor={colors.greyscale.lighter}
      />
      <div>
        {firstName} {lastName}
      </div>
      <div>{group?.name}</div>
    </div>
  )
}

export default React.memo(function ChildPage() {
  const { childId } = useParams<{ childId: string }>()
  const [childResponse] = useApiState(() => getChild(childId), [childId])
  const [placementsResponse] = useApiState(
    () => getPlacements(childId),
    [childId]
  )
  const t = useTranslation()

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque>
          {renderResult(
            combine(childResponse, placementsResponse),
            ([child, _placements]) => (
              <>
                <ChildHeader child={child} />
                <HorizontalLine slim dashed />
                <div>{t.children.placementTermination.title} chevron here</div>
              </>
            )
          )}
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})
