// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import {
  CareTypeCategory,
  defaultAbsenceType,
  defaultCareTypeCategory
} from '../types/absence'
import { AbsenceType } from 'lib-common/generated/enums'

export interface AbsencesState {
  selectedAbsenceType: AbsenceType | null
  setSelectedAbsenceType: (type: AbsenceType | null) => void
  selectedCareTypeCategories: CareTypeCategory[]
  setSelectedCareTypeCategories: (type: CareTypeCategory[]) => void
}

const defaultState: AbsencesState = {
  selectedAbsenceType: defaultAbsenceType,
  setSelectedAbsenceType: () => undefined,
  selectedCareTypeCategories: defaultCareTypeCategory,
  setSelectedCareTypeCategories: () => undefined
}

export const AbsencesContext = createContext<AbsencesState>(defaultState)

export const AbsencesContextProvider = React.memo(
  function AbsencesContextProvider({ children }: { children: JSX.Element }) {
    const [selectedAbsenceType, setSelectedAbsenceType] = useState(
      defaultState.selectedAbsenceType
    )
    const [selectedCareTypeCategories, setSelectedCareTypeCategories] =
      useState(defaultState.selectedCareTypeCategories)

    const value = useMemo(
      () => ({
        selectedAbsenceType,
        setSelectedAbsenceType,
        selectedCareTypeCategories,
        setSelectedCareTypeCategories
      }),
      [selectedAbsenceType, selectedCareTypeCategories]
    )

    return (
      <AbsencesContext.Provider value={value}>
        {children}
      </AbsencesContext.Provider>
    )
  }
)
