import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '@evaka/lib-common/src/types'

export interface ApplicationDecisions {
  applicationId: UUID
  childName: string
  decisions: DecisionSummary[]
}

export interface DecisionSummary {
  decisionId: UUID
  type: DecisionType
  status: DecisionStatus
  sentDate: LocalDate
  resolved: LocalDate | null
}

export type DecisionType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export interface Decision {
  id: UUID
  type: DecisionType
  startDate: LocalDate
  endDate: LocalDate
  unit: DecisionUnit
  applicationId: UUID
  childId: UUID
  childName: string
  documentUri: string
  decisionNumber: number
  sentDate: LocalDate
  status: DecisionStatus
}

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface DecisionUnit {
  id: UUID
  name: string
  daycareDecisionName: string
  preschoolDecisionName: string
  manager: string | null
  streetAddress: string
  postalCode: string
  postOffice: string
  approverName: string
  decisionHandler: string
  decisionHandlerAddress: string
}
