import { UUID } from '../../types'
import { CareType, ProviderType, UnitLanguage } from './enums'
import { Coordinate } from './Coordinate'

export type PublicUnit = {
  id: UUID
  name: string
  type: CareType[]
  providerType: ProviderType
  language: UnitLanguage
  streetAddress: string
  postalCode: string
  postOffice: string
  phone: string | undefined
  email: string | undefined
  url: string | undefined
  location: Coordinate | undefined
  ghostUnit: boolean | undefined
}
