// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Loading, Result } from '~/api'
import {
  TableMode,
  Group,
  AbsenceType,
  defaultAbsenceType,
  defaultCareTypeCategory,
  Cell,
  CellPart,
  CareTypeCategory
} from '~types/absence'
import { UUID } from '~types'

export interface AbsencesState {
  absences: Result<Group>
  setAbsences: (request: Result<Group>) => void
  tableMode: TableMode
  setTableMode: (mode: TableMode) => void
  modalVisible: boolean
  setModalVisible: (modalVisible: boolean) => void
  selectedDate: LocalDate
  setSelectedDate: (date: LocalDate) => void
  selectedCells: Cell[]
  setSelectedCells: (cells: Cell[]) => void
  selectedAbsenceType: AbsenceType
  setSelectedAbsenceType: (type: AbsenceType) => void
  selectedCareTypeCategories: CareTypeCategory[]
  setSelectedCareTypeCategories: (type: CareTypeCategory[]) => void
  toggleCellSelection: (id: UUID) => (cellParts: CellPart[]) => void
}

const defaultState: AbsencesState = {
  absences: Loading(),
  setAbsences: () => undefined,
  tableMode: 'MONTH',
  setTableMode: () => undefined,
  modalVisible: false,
  setModalVisible: () => undefined,
  selectedDate: LocalDate.today(),
  setSelectedDate: () => undefined,
  selectedCells: [],
  setSelectedCells: () => undefined,
  selectedAbsenceType: defaultAbsenceType,
  setSelectedAbsenceType: () => undefined,
  selectedCareTypeCategories: defaultCareTypeCategory,
  setSelectedCareTypeCategories: () => undefined,
  toggleCellSelection: () => () => undefined
}

export const AbsencesContext = createContext<AbsencesState>(defaultState)

export const AbsencesContextProvider = React.memo(
  function AbsencesContextProvider({ children }: { children: JSX.Element }) {
    const [absences, setAbsences] = useState<Result<Group>>(Loading())
    const [tableMode, setTableMode] = useState(defaultState.tableMode)
    const [selectedDate, setSelectedDate] = useState<LocalDate>(
      defaultState.selectedDate
    )
    const [selectedCells, setSelectedCells] = useState<Cell[]>(
      defaultState.selectedCells
    )
    const [selectedAbsenceType, setSelectedAbsenceType] = useState(
      defaultState.selectedAbsenceType
    )
    const [
      selectedCareTypeCategories,
      setSelectedCareTypeCategories
    ] = useState(defaultState.selectedCareTypeCategories)
    const [modalVisible, setModalVisible] = useState(defaultState.modalVisible)

    const updateSelectedCells = ({ id, parts }: Cell) =>
      setSelectedCells((currentSelectedCells) => {
        const selectedIds = currentSelectedCells.map((item) => item.id)

        const included = selectedIds.includes(id)
        const without = currentSelectedCells.filter((item) => item.id !== id)

        return included ? without : [{ id, parts }, ...without]
      })

    const toggleCellSelection = (id: UUID) => (parts: CellPart[]) =>
      updateSelectedCells({ id, parts })

    const value = useMemo(
      () => ({
        absences,
        setAbsences,
        tableMode,
        setTableMode,
        modalVisible,
        setModalVisible,
        selectedDate,
        setSelectedDate,
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
        setAbsences,
        tableMode,
        setTableMode,
        modalVisible,
        setModalVisible,
        selectedDate,
        setSelectedDate,
        selectedCells,
        setSelectedCells,
        selectedAbsenceType,
        setSelectedAbsenceType,
        selectedCareTypeCategories,
        setSelectedCareTypeCategories
      ]
    )

    return (
      <AbsencesContext.Provider value={value}>
        {children}
      </AbsencesContext.Provider>
    )
  }
)
