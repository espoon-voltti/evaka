// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H4 } from 'lib-components/typography'

export type PlacementTypeItemProps = {
  serviceNeed: string
}
export default React.memo(function ServiceNeedItem({
  serviceNeed
}: PlacementTypeItemProps) {
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  return (
    <>
      <CollapsibleContentArea
        opaque
        title={<H4>{serviceNeed}</H4>}
        open={open}
        toggleOpen={toggleOpen}
        paddingHorizontal="0"
        paddingVertical="0"
      >
        <>{/* TODO: voucher value listing here */}</>
      </CollapsibleContentArea>
      <div className="separator small" />
    </>
  )
})
