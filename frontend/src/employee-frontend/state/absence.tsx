// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useMemo, useState } from 'react'
import { Loading, Result } from 'lib-common/api'
import {
  TableMode,
  Group,
  defaultAbsenceType,
  defaultCareTypeCategory,
  Cell,
  CellPart,
  CareTypeCategory
} from '../types/absence'
import { AbsenceType } from 'lib-common/generated/enums'
import { UUID } from 'lib-common/types'

export interface AbsencesState {
  absences: Result<Group>
  setAbsences: (request: Result<Group>) => void
  tableMode: TableMode
  setTableMode: (mode: TableMode) => void
  modalVisible: boolean
  setModalVisible: (modalVisible: boolean) => void
  selectedCells: Cell[]
  setSelectedCells: (cells: Cell[]) => void
  selectedAbsenceType: AbsenceType | null
  setSelectedAbsenceType: (type: AbsenceType | null) => void
  selectedCareTypeCategories: CareTypeCategory[]
  setSelectedCareTypeCategories: (type: CareTypeCategory[]) => void
  toggleCellSelection: (id: UUID, cellParts: CellPart[]) => void
}

const defaultState: AbsencesState = {
  absences: Loading.of(),
  setAbsences: () => undefined,
  tableMode: 'MONTH',
  setTableMode: () => undefined,
  modalVisible: false,
  setModalVisible: () => undefined,
  selectedCells: [],
  setSelectedCells: () => undefined,
  selectedAbsenceType: defaultAbsenceType,
  setSelectedAbsenceType: () => undefined,
  selectedCareTypeCategories: defaultCareTypeCategory,
  setSelectedCareTypeCategories: () => undefined,
  toggleCellSelection: () => undefined
}

export const AbsencesContext = createContext<AbsencesState>(defaultState)

export const AbsencesContextProvider = React.memo(
  function AbsencesContextProvider({ children }: { children: JSX.Element }) {
    const [absences, setAbsences] = useState<Result<Group>>(Loading.of())
    const [tableMode, setTableMode] = useState(defaultState.tableMode)
    const [selectedCells, setSelectedCells] = useState<Cell[]>(
      defaultState.selectedCells
    )
    const [selectedAbsenceType, setSelectedAbsenceType] = useState(
      defaultState.selectedAbsenceType
    )
    const [selectedCareTypeCategories, setSelectedCareTypeCategories] =
      useState(defaultState.selectedCareTypeCategories)
    const [modalVisible, setModalVisible] = useState(defaultState.modalVisible)

    const updateSelectedCells = ({ id, parts }: Cell) =>
      setSelectedCells((currentSelectedCells) => {
        const selectedIds = currentSelectedCells.map((item) => item.id)

        const included = selectedIds.includes(id)
        const without = currentSelectedCells.filter((item) => item.id !== id)

        return included ? without : [{ id, parts }, ...without]
      })

    const toggleCellSelection = useCallback(
      (id: UUID, parts: CellPart[]) => updateSelectedCells({ id, parts }),
      []
    )

    const value = useMemo(
      () => ({
        absences,
        setAbsences,
        tableMode,
        setTableMode,
        modalVisible,
        setModalVisible,
        selectedCells,
        setSelectedCells,
        selectedAbsenceType,
        setSelectedAbsenceType,
        selectedCareTypeCategories,
        setSelectedCareTypeCategories,
        toggleCellSelection
      }),
      [
        absences,
        tableMode,
        modalVisible,
        selectedCells,
        selectedAbsenceType,
        selectedCareTypeCategories,
        toggleCellSelection
      ]
    )

    return (
      <AbsencesContext.Provider value={value}>
        {children}
      </AbsencesContext.Provider>
    )
  }
)
