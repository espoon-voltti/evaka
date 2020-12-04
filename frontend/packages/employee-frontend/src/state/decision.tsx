// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import { Loading, Result } from '~/api'
import { DecisionDraftGroup } from '~types/decision'

export interface DecisionDraftState {
  decisionDraftGroup: Result<DecisionDraftGroup>
  setDecisionDraftGroup: (request: Result<DecisionDraftGroup>) => void
}

const defaultState: DecisionDraftState = {
  decisionDraftGroup: Loading.of(),
  setDecisionDraftGroup: () => undefined
}

export const DecisionDraftContext = createContext<DecisionDraftState>(
  defaultState
)

export const DecisionDraftContextProvider = React.memo(
  function DecisionDraftContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [decisionDraftGroup, setDecisionDraftGroup] = useState<
      Result<DecisionDraftGroup>
    >(defaultState.decisionDraftGroup)

    const value = useMemo(
      () => ({
        decisionDraftGroup,
        setDecisionDraftGroup
      }),
      [decisionDraftGroup, setDecisionDraftGroup]
    )

    return (
      <DecisionDraftContext.Provider value={value}>
        {children}
      </DecisionDraftContext.Provider>
    )
  }
)
