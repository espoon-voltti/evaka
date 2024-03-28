// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { isLoading } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { useQueryResult } from 'lib-common/query'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import PlacementTypeItem from './PlacementTypeItem'
import { serviceNeedsQuery } from './queries'

export default React.memo(function ServiceNeedsSection() {
  const { i18n } = useTranslation()

  const [open, useOpen] = useBoolean(false)

  const data = useQueryResult(serviceNeedsQuery())

  const stableDistinct = (
    value: PlacementType,
    index: number,
    array: PlacementType[]
  ): boolean => array.indexOf(value) === index

  return (
    <CollapsibleContentArea
      opaque
      title={<H2 noMargin>{i18n.financeBasics.serviceNeeds.title}</H2>}
      open={open}
      toggleOpen={useOpen.toggle}
      data-qa="service-needs-section"
      data-isloading={isLoading(data)}
    >
      {renderResult(data, (serviceNeedsList) => (
        <>
          {serviceNeedsList
            .map((serviceNeed) => serviceNeed.validPlacementType)
            .filter(stableDistinct)
            .map((placementType, i) => (
              <PlacementTypeItem
                placementType={placementType}
                serviceNeedsList={serviceNeedsList}
                key={i}
              />
            ))}
        </>
      ))}
    </CollapsibleContentArea>
  )
})
