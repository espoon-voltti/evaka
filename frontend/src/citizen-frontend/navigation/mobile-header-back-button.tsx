// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useContext,
  useLayoutEffect,
  useMemo,
  useState
} from 'react'

export interface MobileHeaderBackButton {
  text: string
  onClick: () => void
}

export interface MobileHeaderBackButtonState {
  backButton: MobileHeaderBackButton | undefined
  setBackButton: (backButton: MobileHeaderBackButton | undefined) => void
}

const defaultState: MobileHeaderBackButtonState = {
  backButton: undefined,
  setBackButton: () => undefined
}

export const MobileHeaderBackButtonContext =
  createContext<MobileHeaderBackButtonState>(defaultState)

export const MobileHeaderBackButtonProvider = React.memo(
  function MobileHeaderBackButtonProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [backButton, setBackButton] = useState<MobileHeaderBackButton>()

    const value = useMemo(() => ({ backButton, setBackButton }), [backButton])

    return (
      <MobileHeaderBackButtonContext.Provider value={value}>
        {children}
      </MobileHeaderBackButtonContext.Provider>
    )
  }
)

/**
 * Replaces the logos of the mobile header with a back button while the calling
 * component is mounted.
 *
 * `onClick` must be wrapped in `useCallback` to avoid infinite loop.
 */
export function useMobileHeaderBackButton(text: string, onClick: () => void) {
  const { setBackButton } = useContext(MobileHeaderBackButtonContext)
  useLayoutEffect(() => {
    setBackButton({ text, onClick })
    return () => setBackButton(undefined)
  }, [setBackButton, text, onClick])
}
