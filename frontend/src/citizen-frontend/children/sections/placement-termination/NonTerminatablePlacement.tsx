// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { TerminatablePlacementGroup } from 'lib-common/generated/api-types/placement'
import { H3, InformationText, P } from 'lib-components/typography'

import { useTranslation } from '../../../localization'

interface Props {
  group: TerminatablePlacementGroup
}

export default React.memo(function NonTerminatablePlacement({ group }: Props) {
  const t = useTranslation()
  return (
    <div>
      <H3>{t.placement.type[group.type]}</H3>
      <P noMargin data-qa="non-terminatable-placement">
        {group.unitName},{' '}
        {t.children.placementTermination.until(group.endDate.format())}
      </P>
      <InformationText>
        {t.children.placementTermination.nonTerminatablePlacement}
      </InformationText>
    </div>
  )
})
