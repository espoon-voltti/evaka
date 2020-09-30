// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { FeeAlteration } from '~types/fee-alteration'
import {
  AdditionalInformation,
  AssistanceAction,
  AssistanceNeed,
  ChildBackupCare,
  Placement,
  ServiceNeed
} from '~/types/child'
import { Loading, Result } from '~/api'
import { PersonDetails } from '~types/person'
import { Parentship } from '~types/fridge'
import { ApplicationSummary } from 'types/application'

export interface ChildState {
  person: Result<PersonDetails>
  setPerson: (request: Result<PersonDetails>) => void
  serviceNeeds: Result<ServiceNeed[]>
  setServiceNeeds: (request: Result<ServiceNeed[]>) => void
  assistanceNeeds: Result<AssistanceNeed[]>
  setAssistanceNeeds: (request: Result<AssistanceNeed[]>) => void
  assistanceActions: Result<AssistanceAction[]>
  setAssistanceActions: (request: Result<AssistanceAction[]>) => void
  additionalInformation: Result<AdditionalInformation>
  setAdditionalInformation: (request: Result<AdditionalInformation>) => void
  feeAlterations: Result<FeeAlteration[]>
  setFeeAlterations: (result: Result<FeeAlteration[]>) => void
  placements: Result<Placement[]>
  setPlacements: (request: Result<Placement[]>) => void
  parentships: Result<Parentship[]>
  setParentships: (request: Result<Parentship[]>) => void
  backupCares: Result<ChildBackupCare[]>
  setBackupCares: (request: Result<ChildBackupCare[]>) => void
  guardians: Result<PersonDetails[]>
  setGuardians: (request: Result<PersonDetails[]>) => void
  applications: Result<ApplicationSummary[]>
  setApplications: (r: Result<ApplicationSummary[]>) => void
}

const defaultState: ChildState = {
  person: Loading(),
  setPerson: () => undefined,
  serviceNeeds: Loading(),
  setServiceNeeds: () => undefined,
  assistanceNeeds: Loading(),
  setAssistanceNeeds: () => undefined,
  assistanceActions: Loading(),
  setAssistanceActions: () => undefined,
  additionalInformation: Loading(),
  setAdditionalInformation: () => undefined,
  feeAlterations: Loading(),
  setFeeAlterations: () => undefined,
  placements: Loading(),
  setPlacements: () => undefined,
  parentships: Loading(),
  setParentships: () => undefined,
  backupCares: Loading(),
  setBackupCares: () => undefined,
  guardians: Loading(),
  setGuardians: () => undefined,
  applications: Loading(),
  setApplications: () => undefined
}

export const ChildContext = createContext<ChildState>(defaultState)

export const ChildContextProvider = React.memo(function ChildContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [person, setPerson] = useState<Result<PersonDetails>>(
    defaultState.person
  )
  const [serviceNeeds, setServiceNeeds] = useState<Result<ServiceNeed[]>>(
    defaultState.serviceNeeds
  )
  const [assistanceNeeds, setAssistanceNeeds] = useState<
    Result<AssistanceNeed[]>
  >(defaultState.assistanceNeeds)
  const [assistanceActions, setAssistanceActions] = useState<
    Result<AssistanceAction[]>
  >(defaultState.assistanceActions)
  const [additionalInformation, setAdditionalInformation] = useState<
    Result<AdditionalInformation>
  >(defaultState.additionalInformation)
  const [feeAlterations, setFeeAlterations] = useState<Result<FeeAlteration[]>>(
    defaultState.feeAlterations
  )
  const [placements, setPlacements] = useState<Result<Placement[]>>(
    defaultState.placements
  )
  const [parentships, setParentships] = useState<Result<Parentship[]>>(
    defaultState.parentships
  )
  const [backupCares, setBackupCares] = useState<Result<ChildBackupCare[]>>(
    defaultState.backupCares
  )
  const [guardians, setGuardians] = useState<Result<PersonDetails[]>>(
    defaultState.guardians
  )
  const [applications, setApplications] = useState<
    Result<ApplicationSummary[]>
  >(defaultState.applications)

  const value = useMemo(
    () => ({
      person,
      setPerson,
      serviceNeeds,
      setServiceNeeds,
      assistanceNeeds,
      setAssistanceNeeds,
      assistanceActions,
      setAssistanceActions,
      additionalInformation,
      setAdditionalInformation,
      feeAlterations,
      setFeeAlterations,
      placements,
      setPlacements,
      parentships,
      setParentships,
      backupCares,
      setBackupCares,
      guardians,
      setGuardians,
      applications,
      setApplications
    }),
    [
      person,
      setPerson,
      serviceNeeds,
      setServiceNeeds,
      assistanceNeeds,
      setAssistanceNeeds,
      assistanceActions,
      setAssistanceActions,
      additionalInformation,
      setAdditionalInformation,
      feeAlterations,
      setFeeAlterations,
      placements,
      setPlacements,
      parentships,
      setParentships,
      backupCares,
      setBackupCares,
      guardians,
      setGuardians,
      applications,
      setApplications
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
