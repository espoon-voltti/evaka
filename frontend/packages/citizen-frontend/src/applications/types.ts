import LocalDate from '@evaka/lib-common/src/local-date'

export interface GuardianApplications {
  childId: string
  childName: string
  applicationSummaries: ApplicationSummary[]
}

export interface ApplicationSummary {
  applicationId: string
  sentDate: LocalDate | null
}
