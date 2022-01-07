// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'lib-common/types'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { Incomes } from '../person-profile/PersonIncome'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function ChildIncome({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(startOpen)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.income.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="income-collapsible"
    >
      <Incomes personId={id} />
    </CollapsibleContentArea>
  )
})
