// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import { CareTypeCategory, defaultCareTypeCategory } from '../types/absence'

export interface AbsencesState {
  selectedCareTypeCategories: CareTypeCategory[]
  setSelectedCareTypeCategories: (type: CareTypeCategory[]) => void
}

const defaultState: AbsencesState = {
  selectedCareTypeCategories: defaultCareTypeCategory,
  setSelectedCareTypeCategories: () => undefined
}

export const AbsencesContext = createContext<AbsencesState>(defaultState)

export const AbsencesContextProvider = React.memo(
  function AbsencesContextProvider({ children }: { children: JSX.Element }) {
    const [selectedCareTypeCategories, setSelectedCareTypeCategories] =
      useState(defaultState.selectedCareTypeCategories)

    const value = useMemo(
      () => ({
        selectedCareTypeCategories,
        setSelectedCareTypeCategories
      }),
      [selectedCareTypeCategories]
    )

    return (
      <AbsencesContext.Provider value={value}>
        {children}
      </AbsencesContext.Provider>
    )
  }
)
