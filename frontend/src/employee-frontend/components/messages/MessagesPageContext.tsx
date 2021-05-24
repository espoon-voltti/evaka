import React, { createContext, useMemo, useState } from 'react'
import { DraftContent } from './types'
import { AccountView } from './types-view'

export interface MessagesPageState {
  selectedDraft: DraftContent | undefined
  setSelectedDraft: (draft: DraftContent | undefined) => void
  view: AccountView | undefined
  setView: (view: AccountView) => void
}

const defaultState: MessagesPageState = {
  selectedDraft: undefined,
  setSelectedDraft: () => undefined,
  view: undefined,
  setView: () => undefined
}

export const MessagesPageContext = createContext<MessagesPageState>(
  defaultState
)

export const MessagesPageContextProvider = React.memo(
  function MessagesPageContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [selectedDraft, setSelectedDraft] = useState(
      defaultState.selectedDraft
    )
    const [view, setView] = useState<AccountView>()

    const value = useMemo(
      () => ({
        selectedDraft,
        setSelectedDraft,
        view,
        setView
      }),
      [selectedDraft, view]
    )

    return (
      <MessagesPageContext.Provider value={value}>
        {children}
      </MessagesPageContext.Provider>
    )
  }
)
