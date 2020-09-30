// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~types'
import { PlacementType } from '~types/placementdraft'
import LocalDate from '@evaka/lib-common/src/local-date'

export interface CareArea {
  id: UUID
  name: string
  shortName: string
}

export type UnitTypes =
  | 'CLUB'
  | 'FAMILY'
  | 'CENTRE'
  | 'GROUP_FAMILY'
  | 'PRESCHOOL'
  | 'PREPARATORY_EDUCATION'

export type ProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'

export type UnitLanguage = 'fi' | 'sv'

export interface Unit {
  id: UUID
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  area: CareArea
  type: UnitTypes[]
  canApplyDaycare: boolean
  canApplyPreschool: boolean
  canApplyClub: boolean
  providerType: ProviderType
  roundTheClock: boolean
  capacity: number
  language: UnitLanguage
  uploadToVarda: boolean
  uploadToKoski: boolean
  invoicedByMunicipality: boolean
  costCenter: string | null
  additionalInfo: string | null
  phone: string | null
  email: string | null
  url: string | null
  visitingAddress: VisitingAddress
  location: Coordinate | null
  mailingAddress: MailingAddress
  unitManager: UnitManager | null
  decisionCustomization: DecisionCustomization
  ophUnitOid: string | null
  ophOrganizerOid: string | null
  ophOrganizationOid: string | null
}

export interface DecisionCustomization {
  daycareName: string
  preschoolName: string
  handler: string
  handlerAddress: string
}

export interface UnitManager {
  name: string | null
  email: string | null
  phone: string | null
}

export interface VisitingAddress {
  streetAddress: string
  postalCode: string
  postOffice: string
}

export interface MailingAddress {
  streetAddress: string | null
  poBox: string | null
  postalCode: string | null
  postOffice: string | null
}

export interface Coordinate {
  lat: number
  lon: number
}

export interface UnitFiltersType {
  startDate: LocalDate
  endDate: LocalDate
}

export interface DaycarePlacement {
  id: UUID
  child: ChildBasics
  daycare: {
    id: UUID
    name: string
  }
  groupPlacements: DaycareGroupPlacement[]
  type: PlacementType
  missingServiceNeedDays: number
  startDate: LocalDate
  endDate: LocalDate
}

interface ChildBasics {
  id: UUID
  socialSecurityNumber: string | null
  firstName: string | null
  lastName: string | null
  dateOfBirth: LocalDate
}

interface ClosedPeriod {
  start: LocalDate
  end: LocalDate
}

export type PlacementPlanConfirmationStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'

export type PlacementPlanRejectReason = 'OTHER' | 'REASON_1' | 'REASON_2'

export interface DaycarePlacementPlan {
  id: UUID
  unitId: UUID
  applicationId: UUID
  type: PlacementType
  period: ClosedPeriod
  preschoolDaycarePeriod: ClosedPeriod | null
  child: ChildBasics
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectReason: PlacementPlanRejectReason | null
  unitRejectOtherReason: string | null
}

export interface DaycareGroupPlacement {
  id: UUID | null
  groupId: UUID | null
  daycarePlacementId: UUID
  type: PlacementType
  startDate: LocalDate
  endDate: LocalDate
}

export interface DaycareGroupPlacementDetailed extends DaycareGroupPlacement {
  daycarePlacementStartDate: LocalDate
  daycarePlacementEndDate: LocalDate
  daycarePlacementMissingServiceNeedDays: number
  child: ChildBasics
}

export const flatMapGroupPlacements = (
  daycarePlacements: DaycarePlacement[]
): DaycareGroupPlacementDetailed[] =>
  daycarePlacements.reduce((groupPlacements, daycarePlacement) => {
    daycarePlacement.groupPlacements
      .map<DaycareGroupPlacementDetailed>((groupPlacement) => ({
        ...groupPlacement,
        child: daycarePlacement.child,
        daycarePlacementStartDate: daycarePlacement.startDate,
        daycarePlacementEndDate: daycarePlacement.endDate,
        daycarePlacementId: daycarePlacement.id,
        daycarePlacementMissingServiceNeedDays:
          daycarePlacement.missingServiceNeedDays
      }))
      .forEach((groupPlacement) => groupPlacements.push(groupPlacement))

    return groupPlacements
  }, [] as DaycareGroupPlacementDetailed[])

export interface DaycareGroup {
  id: UUID
  daycareId: UUID
  name: string
  startDate: LocalDate
  endDate: LocalDate | null
  deletable: boolean
}

export interface DaycareGroupWithPlacements extends DaycareGroup {
  placements: DaycareGroupPlacementDetailed[]
}

export interface Stats {
  minimum: number
  maximum: number
}

export interface DaycareCapacityStats {
  unitTotalCaretakers: Stats
  groupCaretakers: {
    [groupId: string]: Stats
  }
}

export type OccupancyType = 'CONFIRMED' | 'PLANNED' | 'REALIZED'

export interface Occupancy {
  period: {
    start: LocalDate
    end: LocalDate
  }
  sum: number
  headcount: number
  caretakers?: number
  percentage?: number
}
