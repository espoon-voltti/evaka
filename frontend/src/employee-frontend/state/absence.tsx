// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

export interface AbsencesState {}

const defaultState: AbsencesState = {}

export const AbsencesContext = createContext<AbsencesState>(defaultState)

export const AbsencesContextProvider = React.memo(
  function AbsencesContextProvider({ children }: { children: JSX.Element }) {
    const value = useMemo(() => ({}), [])

    return (
      <AbsencesContext.Provider value={value}>
        {children}
      </AbsencesContext.Provider>
    )
  }
)
