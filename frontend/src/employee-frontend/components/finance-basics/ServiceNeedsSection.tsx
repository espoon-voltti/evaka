// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { isLoading } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import PlacementTypeItem from './PlacementTypeItem'
import { serviceNeedsQuery } from './queries'

export default React.memo(function ServiceNeedsSection() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(true)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  const data = useQueryResult(serviceNeedsQuery())

  return (
    <CollapsibleContentArea
      opaque
      title={<H2 noMargin>{i18n.financeBasics.serviceNeeds.title}</H2>}
      open={open}
      toggleOpen={toggleOpen}
      data-qa="service-needs-section"
      data-isloading={isLoading(data)}
    >
      {renderResult(data, (serviceNeedsList) => (
        <>
          {serviceNeedsList
            .map((serviceNeed) => serviceNeed.validPlacementType)
            .filter((value, index, array) => array.indexOf(value) === index)
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
