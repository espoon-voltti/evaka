// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { PersonWithChildren, PersonDetails } from '../types/person'
import { Parentship, Partnership } from '../types/fridge'
import { Income } from '../types/income'
import { Loading, Result } from 'lib-common/api'
import { ApplicationSummary } from '../types/application'
import { Decision } from '../types/decision'
import { Invoice } from '../types/invoicing'
import { FamilyOverview } from '../types/family-overview'
import { getFamilyOverview } from '../api/family-overview'

export interface PersonState {
  person: Result<PersonDetails>
  parentships: Result<Parentship[]>
  partnerships: Result<Partnership[]>
  incomes: Result<Income[]>
  applications: Result<ApplicationSummary[]>
  dependants: Result<PersonWithChildren[]>
  decisions: Result<Decision[]>
  family: Result<FamilyOverview>
  invoices: Result<Invoice[]>
  setPerson: (request: Result<PersonDetails>) => void
  setParentships: (request: Result<Parentship[]>) => void
  setPartnerships: (request: Result<Partnership[]>) => void
  setIncomes: (r: Result<Income[]>) => void
  setApplications: (r: Result<ApplicationSummary[]>) => void
  setDependants: (r: Result<PersonWithChildren[]>) => void
  setDecisions: (r: Result<Decision[]>) => void
  setFamily: (r: Result<FamilyOverview>) => void
  setInvoices: (r: Result<Invoice[]>) => void
  reloadFamily: (id: string) => void
}

const defaultState: PersonState = {
  person: Loading.of(),
  parentships: Loading.of(),
  partnerships: Loading.of(),
  incomes: Loading.of(),
  applications: Loading.of(),
  dependants: Loading.of(),
  decisions: Loading.of(),
  family: Loading.of(),
  invoices: Loading.of(),
  setPerson: () => undefined,
  setParentships: () => undefined,
  setPartnerships: () => undefined,
  setIncomes: () => undefined,
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
  const [person, setPerson] = useState<Result<PersonDetails>>(
    defaultState.person
  )
  const [parentships, setParentships] = useState<Result<Parentship[]>>(
    defaultState.parentships
  )
  const [partnerships, setPartnerships] = useState<Result<Partnership[]>>(
    defaultState.partnerships
  )
  const [applications, setApplications] = useState<
    Result<ApplicationSummary[]>
  >(defaultState.applications)
  const [incomes, setIncomes] = useState<Result<Income[]>>(defaultState.incomes)
  const [dependants, setDependants] = useState<Result<PersonWithChildren[]>>(
    defaultState.dependants
  )
  const [decisions, setDecisions] = useState<Result<Decision[]>>(
    defaultState.decisions
  )
  const [family, setFamily] = useState<Result<FamilyOverview>>(
    defaultState.family
  )
  const [invoices, setInvoices] = useState<Result<Invoice[]>>(
    defaultState.invoices
  )

  // TODO: fix the deps
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reloadFamily = (id: string) => {
    setFamily(Loading.of())
    void getFamilyOverview(id).then(setFamily)
  }

  const value = useMemo(
    () => ({
      person,
      setPerson,
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
      setPerson,
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
      decisions,
      setDecisions,
      family,
      setFamily,
      invoices,
      setInvoices,
      reloadFamily
    ]
  )

  return (
    <PersonContext.Provider value={value}>{children}</PersonContext.Provider>
  )
})
