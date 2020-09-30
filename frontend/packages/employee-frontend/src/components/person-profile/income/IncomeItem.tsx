// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Income, PartialIncome } from '~types/income'
import IncomeItemEditor from './IncomeItemEditor'
import IncomeItemBody from './IncomeItemBody'

const Container = styled.div`
  margin-top: 20px;
  margin-bottom: 40px;
`

type NotEditing = { income: Income }

type Editing = {
  income?: Income
  editing: true
  cancel: () => void
  createIncome: (v: PartialIncome) => void
  updateIncome: (v: Income) => void
}

type Props = NotEditing | Editing

const IncomeItem = React.memo(function IncomeItem(props: Props) {
  return (
    <Container>
      <div>
        {'editing' in props ? (
          <IncomeItemEditor
            baseIncome={props.income}
            cancel={props.cancel}
            create={props.createIncome}
            update={props.updateIncome}
          />
        ) : (
          <IncomeItemBody income={props.income} />
        )}
      </div>
    </Container>
  )
})

export default IncomeItem
