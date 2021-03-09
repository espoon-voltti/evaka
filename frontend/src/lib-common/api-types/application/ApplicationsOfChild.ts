import { ApplicationStatus } from './enums'
import LocalDate from '../../local-date'
import { JsonOf } from '../../json'

export interface ApplicationsOfChild {
  childId: string
  childName: string
  applicationSummaries: CitizenApplicationSummary[]
}

export const deserializeApplicationsOfChild = (
  json: JsonOf<ApplicationsOfChild>
): ApplicationsOfChild => ({
  ...json,
  applicationSummaries: json.applicationSummaries.map((json2) => ({
    ...json2,
    sentDate: LocalDate.parseNullableIso(json2.sentDate),
    startDate: LocalDate.parseNullableIso(json2.startDate),
    createdDate: new Date(json2.createdDate),
    modifiedDate: new Date(json2.modifiedDate)
  }))
})

export interface CitizenApplicationSummary {
  applicationId: string
  type: string
  childId: string
  childName: string | null
  preferredUnitName: string | null
  allPreferredUnitNames: string[]
  applicationStatus: ApplicationStatus
  startDate: LocalDate | null
  sentDate: LocalDate | null
  createdDate: Date
  modifiedDate: Date
  transferApplication: boolean
}
