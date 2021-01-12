import LocalDate from '@evaka/lib-common/src/local-date'

export interface GuardianApplications {
  childId: string
  childName: string
  applicationSummaries: ApplicationSummary[]
}

export interface ApplicationSummary {
  id: string
  sentDate: LocalDate | null
}
