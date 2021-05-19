import { UUID } from 'lib-common/types'
import { PlacementType } from '../application/enums'

export interface ServiceNeedOptionPublicInfo {
  id: UUID
  name: string
  validPlacementType: PlacementType
}

export interface ServiceNeedOptionSummary {
  id: UUID
  name: string
}
