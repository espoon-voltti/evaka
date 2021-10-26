// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useCallback } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { PersonWithChildren, PersonDetails } from '../types/person'
import { Parentship, Partnership } from '../types/fridge'
import { Income } from '../types/income'
import { Loading, Paged, Result } from 'lib-common/api'
import { ApplicationSummary } from '../types/application'
import { Decision } from '../types/decision'
import { Invoice } from '../types/invoicing'
import { FamilyOverview } from '../types/family-overview'
import { getFamilyOverview } from '../api/family-overview'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { Action } from 'lib-common/generated/action'

export interface PersonState {
  person: Result<PersonDetails>
  permittedActions: Set<Action.Person>
  parentships: Result<Parentship[]>
  partnerships: Result<Partnership[]>
  incomes: Result<Income[]>
  incomeStatements: Result<Paged<IncomeStatement>>
  applications: Result<ApplicationSummary[]>
  dependants: Result<PersonWithChildren[]>
  decisions: Result<Decision[]>
  family: Result<FamilyOverview>
  invoices: Result<Invoice[]>
  setPerson: (request: Result<PersonDetails>) => void
  setPermittedActions: React.Dispatch<React.SetStateAction<Set<Action.Person>>>
  setParentships: (request: Result<Parentship[]>) => void
  setPartnerships: (request: Result<Partnership[]>) => void
  setIncomes: (r: Result<Income[]>) => void
  setIncomeStatements: (r: Result<Paged<IncomeStatement>>) => void
  setApplications: (r: Result<ApplicationSummary[]>) => void
  setDependants: (r: Result<PersonWithChildren[]>) => void
  setDecisions: (r: Result<Decision[]>) => void
  setFamily: (r: Result<FamilyOverview>) => void
  setInvoices: (r: Result<Invoice[]>) => void
  reloadFamily: (id: string) => void
}

const defaultState: PersonState = {
  person: Loading.of(),
  permittedActions: new Set(),
  parentships: Loading.of(),
  partnerships: Loading.of(),
  incomes: Loading.of(),
  incomeStatements: Loading.of(),
  applications: Loading.of(),
  dependants: Loading.of(),
  decisions: Loading.of(),
  family: Loading.of(),
  invoices: Loading.of(),
  setPerson: () => undefined,
  setPermittedActions: () => undefined,
  setParentships: () => undefined,
  setPartnerships: () => undefined,
  setIncomes: () => undefined,
  setIncomeStatements: () => undefined,
  setApplications: () => undefined,
  setDependants: () => undefined,
  setDecisions: () => undefined,
  setFamily: () => undefined,
  setInvoices: () => undefined,
  reloadFamily: (_id: string) => undefined
}

export const PersonContext = createContext<PersonState>(defaultState)

export const PersonContextProvider = React.memo(function PersonContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [person, setPerson] = useState(defaultState.person)
  const [permittedActions, setPermittedActions] = useState(
    defaultState.permittedActions
  )
  const [parentships, setParentships] = useState(defaultState.parentships)
  const [partnerships, setPartnerships] = useState(defaultState.partnerships)
  const [applications, setApplications] = useState(defaultState.applications)
  const [incomes, setIncomes] = useState(defaultState.incomes)
  const [incomeStatements, setIncomeStatements] = useState(
    defaultState.incomeStatements
  )
  const [dependants, setDependants] = useState(defaultState.dependants)
  const [decisions, setDecisions] = useState(defaultState.decisions)
  const [family, setFamily] = useState(defaultState.family)
  const [invoices, setInvoices] = useState(defaultState.invoices)

  const loadFamily = useRestApi(getFamilyOverview, setFamily)
  const reloadFamily = useCallback((id: string) => loadFamily(id), [loadFamily])

  const value = useMemo(
    () => ({
      person,
      setPerson,
      permittedActions,
      setPermittedActions,
      parentships,
      setParentships,
      partnerships,
      setPartnerships,
      applications,
      setApplications,
      dependants,
      setDependants,
      incomes,
      setIncomes,
      incomeStatements,
      setIncomeStatements,
      decisions,
      setDecisions,
      family,
      setFamily,
      invoices,
      setInvoices,
      reloadFamily
    }),
    [
      person,
      permittedActions,
      parentships,
      partnerships,
      applications,
      dependants,
      incomes,
      incomeStatements,
      decisions,
      family,
      invoices,
      reloadFamily
    ]
  )

  return (
    <PersonContext.Provider value={value}>{children}</PersonContext.Provider>
  )
})
