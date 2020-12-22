import { PlacementType } from '~types/placementdraft'

const partDayPlacementTypes: readonly PlacementType[] = [
  'DAYCARE_PART_TIME',
  'PRESCHOOL',
  'PREPARATORY'
] as const

export function isPartDayPlacement(type: PlacementType): boolean {
  return partDayPlacementTypes.includes(type)
}
