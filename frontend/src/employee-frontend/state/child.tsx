// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { FeeAlteration } from '../types/fee-alteration'
import {
  AdditionalInformation,
  AssistanceAction,
  AssistanceNeed,
  ChildBackupCare,
  Placement,
  ServiceNeed
} from '../types/child'
import { Loading, Result } from '@evaka/lib-common/api'
import { PersonDetails } from '../types/person'
import { Parentship } from '../types/fridge'
import { ApplicationSummary } from '../types/application'
import { Recipient } from '@evaka/employee-frontend/components/messages/types'

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
  recipients: Result<Recipient[]>
  setRecipients: (r: Result<Recipient[]>) => void
}

const defaultState: ChildState = {
  person: Loading.of(),
  setPerson: () => undefined,
  serviceNeeds: Loading.of(),
  setServiceNeeds: () => undefined,
  assistanceNeeds: Loading.of(),
  setAssistanceNeeds: () => undefined,
  assistanceActions: Loading.of(),
  setAssistanceActions: () => undefined,
  additionalInformation: Loading.of(),
  setAdditionalInformation: () => undefined,
  feeAlterations: Loading.of(),
  setFeeAlterations: () => undefined,
  placements: Loading.of(),
  setPlacements: () => undefined,
  parentships: Loading.of(),
  setParentships: () => undefined,
  backupCares: Loading.of(),
  setBackupCares: () => undefined,
  guardians: Loading.of(),
  setGuardians: () => undefined,
  applications: Loading.of(),
  setApplications: () => undefined,
  recipients: Loading.of(),
  setRecipients: () => undefined
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

  const [recipients, setRecipients] = useState<Result<Recipient[]>>(
    defaultState.recipients
  )

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
      setApplications,
      recipients,
      setRecipients
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
      setApplications,
      recipients,
      setRecipients
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
