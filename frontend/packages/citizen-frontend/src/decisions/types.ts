import LocalDate from '@evaka/lib-common/src/local-date'

export interface ApplicationDecisions {
  applicationId: string
  childName: string
  decisions: DecisionSummary[]
}

export interface DecisionSummary {
  decisionId: string
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

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'
