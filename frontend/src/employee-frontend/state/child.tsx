// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import { Recipient } from 'lib-common/generated/api-types/messaging'
import { FeeAlteration } from '../types/fee-alteration'
import { ChildBackupCare } from '../types/child'
import { Loading, Result } from 'lib-common/api'
import { Parentship, PersonJSON } from 'lib-common/generated/api-types/pis'
import { VasuDocumentSummary } from '../components/vasu/api'
import { Action } from 'lib-common/generated/action'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import { PersonApplicationSummary } from 'lib-common/generated/api-types/application'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import {
  ServiceNeed,
  ServiceNeedOption
} from 'lib-common/generated/api-types/serviceneed'
import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'

export interface ChildState {
  person: Result<PersonJSON>
  setPerson: (request: Result<PersonJSON>) => void
  permittedActions: Set<Action.Child | Action.Person>
  setPermittedActions: (r: Set<Action.Child | Action.Person>) => void
  serviceNeeds: Result<ServiceNeed[]>
  setServiceNeeds: (request: Result<ServiceNeed[]>) => void
  serviceNeedOptions: Result<ServiceNeedOption[]>
  setServiceNeedOptions: (request: Result<ServiceNeedOption[]>) => void
  additionalInformation: Result<AdditionalInformation>
  setAdditionalInformation: (request: Result<AdditionalInformation>) => void
  feeAlterations: Result<FeeAlteration[]>
  setFeeAlterations: (result: Result<FeeAlteration[]>) => void
  placements: Result<DaycarePlacementWithDetails[]>
  setPlacements: (request: Result<DaycarePlacementWithDetails[]>) => void
  parentships: Result<Parentship[]>
  setParentships: (request: Result<Parentship[]>) => void
  backupCares: Result<ChildBackupCare[]>
  setBackupCares: (request: Result<ChildBackupCare[]>) => void
  guardians: Result<PersonJSON[]>
  setGuardians: (request: Result<PersonJSON[]>) => void
  applications: Result<PersonApplicationSummary[]>
  setApplications: (r: Result<PersonApplicationSummary[]>) => void
  recipients: Result<Recipient[]>
  setRecipients: (r: Result<Recipient[]>) => void
  vasus: Result<VasuDocumentSummary[]>
  setVasus: (r: Result<VasuDocumentSummary[]>) => void
  pedagogicalDocuments: Result<PedagogicalDocument[]>
  setPedagogicalDocuments: (r: Result<PedagogicalDocument[]>) => void
}

const defaultState: ChildState = {
  person: Loading.of(),
  setPerson: () => undefined,
  permittedActions: new Set(),
  setPermittedActions: () => undefined,
  serviceNeeds: Loading.of(),
  setServiceNeeds: () => undefined,
  serviceNeedOptions: Loading.of(),
  setServiceNeedOptions: () => undefined,
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
  setRecipients: () => undefined,
  vasus: Loading.of(),
  setVasus: () => undefined,
  pedagogicalDocuments: Loading.of(),
  setPedagogicalDocuments: () => undefined
}

export const ChildContext = createContext<ChildState>(defaultState)

export const ChildContextProvider = React.memo(function ChildContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [person, setPerson] = useState<Result<PersonJSON>>(defaultState.person)
  const [permittedActions, setPermittedActions] = useState(
    defaultState.permittedActions
  )
  const [serviceNeeds, setServiceNeeds] = useState<Result<ServiceNeed[]>>(
    defaultState.serviceNeeds
  )
  const [serviceNeedOptions, setServiceNeedOptions] = useState<
    Result<ServiceNeedOption[]>
  >(defaultState.serviceNeedOptions)
  const [additionalInformation, setAdditionalInformation] = useState<
    Result<AdditionalInformation>
  >(defaultState.additionalInformation)
  const [feeAlterations, setFeeAlterations] = useState<Result<FeeAlteration[]>>(
    defaultState.feeAlterations
  )
  const [placements, setPlacements] = useState<
    Result<DaycarePlacementWithDetails[]>
  >(defaultState.placements)
  const [parentships, setParentships] = useState<Result<Parentship[]>>(
    defaultState.parentships
  )
  const [backupCares, setBackupCares] = useState<Result<ChildBackupCare[]>>(
    defaultState.backupCares
  )
  const [guardians, setGuardians] = useState<Result<PersonJSON[]>>(
    defaultState.guardians
  )
  const [applications, setApplications] = useState<
    Result<PersonApplicationSummary[]>
  >(defaultState.applications)
  const [vasus, setVasus] = useState<Result<VasuDocumentSummary[]>>(
    defaultState.vasus
  )

  const [recipients, setRecipients] = useState<Result<Recipient[]>>(
    defaultState.recipients
  )

  const [pedagogicalDocuments, setPedagogicalDocuments] = useState<
    Result<PedagogicalDocument[]>
  >(defaultState.pedagogicalDocuments)

  const value = useMemo(
    () => ({
      person,
      setPerson,
      permittedActions,
      setPermittedActions,
      serviceNeeds,
      setServiceNeeds,
      serviceNeedOptions,
      setServiceNeedOptions,
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
      vasus,
      setVasus,
      applications,
      setApplications,
      recipients,
      setRecipients,
      pedagogicalDocuments,
      setPedagogicalDocuments
    }),
    [
      person,
      permittedActions,
      serviceNeeds,
      serviceNeedOptions,
      feeAlterations,
      placements,
      parentships,
      backupCares,
      guardians,
      vasus,
      applications,
      recipients,
      pedagogicalDocuments
    ]
  )

  return <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
})
