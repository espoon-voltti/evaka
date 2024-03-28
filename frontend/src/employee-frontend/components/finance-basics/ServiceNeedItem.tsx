// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useBoolean } from 'lib-common/form/hooks'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H4 } from 'lib-components/typography'

export type PlacementTypeItemProps = {
  serviceNeed: string
}
export default React.memo(function ServiceNeedItem({
  serviceNeed
}: PlacementTypeItemProps) {
  const [open, useOpen] = useBoolean(false)

  return (
    <>
      <CollapsibleContentArea
        opaque
        title={<H4>{serviceNeed}</H4>}
        open={open}
        toggleOpen={useOpen.toggle}
        paddingHorizontal="0"
        paddingVertical="0"
      >
        <>{/* TODO: voucher value listing here */}</>
      </CollapsibleContentArea>
      <HorizontalLine dashed={true} slim={true} />
    </>
  )
})
