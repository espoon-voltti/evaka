// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { Incomes, IncomeStatements } from '../person-profile/PersonIncome'

interface Props {
  childId: ChildId
}

export default React.memo(function ChildIncome({ childId }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  return (
    <>
      <H4>{i18n.personProfile.incomeStatement.title}</H4>
      <IncomeStatements personId={childId} />
      <Gap size="L" />
      <H3>{i18n.personProfile.income.title}</H3>
      <Incomes personId={childId} permittedActions={permittedActions} />
    </>
  )
})
