// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState, useCallback } from 'react'

export interface UnitState {
  scrollToPosition: () => void
  savePosition: () => void
}

const defaultState: UnitState = {
  scrollToPosition: () => undefined,
  savePosition: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [position, setPosition] = useState<number>(-1)
  const savePosition = useCallback(() => void setPosition(window.scrollY), [
    setPosition
  ])
  const scrollToPosition = useCallback(() => {
    if (position > -1) {
      window.scrollTo(0, position)
      setPosition(-1)
    }
  }, [position, setPosition])

  const value = useMemo(() => ({ scrollToPosition, savePosition }), [
    scrollToPosition,
    savePosition
  ])

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
