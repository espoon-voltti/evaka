// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H3 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

import ServiceNeedItem from './ServiceNeedItem'

export type PlacementTypeItemProps = {
  placementType: PlacementType
  serviceNeedsList: ServiceNeedOption[]
}

export default React.memo(function PlacementTypeItem({
  placementType,
  serviceNeedsList
}: PlacementTypeItemProps) {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  return (
    <CollapsibleContentArea
      opaque
      title={<H3>{i18n.placement.type[placementType]}</H3>}
      open={open}
      toggleOpen={toggleOpen}
      paddingHorizontal="0"
    >
      <>
        {serviceNeedsList
          .filter(
            (value) =>
              value.validPlacementType == placementType && value.defaultOption
          )
          .map((serviceNeed) => (
            <ServiceNeedItem
              key={serviceNeed.nameFi + ' (oletus)'}
              serviceNeed={serviceNeed.nameFi + ' (oletus)'}
            />
          ))}
      </>
      <>
        {serviceNeedsList
          .filter(
            (value) =>
              value.validPlacementType == placementType && !value.defaultOption
          )
          .map((serviceNeed) => (
            <ServiceNeedItem
              key={serviceNeed.nameFi}
              serviceNeed={serviceNeed.nameFi}
            />
          ))}
      </>
    </CollapsibleContentArea>
  )
})
