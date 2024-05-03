// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { ChildContext } from 'employee-frontend/state'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'lib-common/types'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { Incomes, IncomeStatements } from '../person-profile/PersonIncome'

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function ChildIncome({ childId, startOpen }: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const { permittedActions } = useContext(ChildContext)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.income.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="income-collapsible"
    >
      <H4>{i18n.personProfile.incomeStatement.title}</H4>
      <IncomeStatements personId={childId} />
      <Gap size="L" />
      <H3>{i18n.personProfile.income.title}</H3>
      <Incomes personId={childId} permittedActions={permittedActions} />
    </CollapsibleContentArea>
  )
})
