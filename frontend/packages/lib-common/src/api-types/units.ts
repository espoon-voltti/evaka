import { UUID } from '../types'

export type CareType =
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

export type Coordinate = {
  lat: number
  lon: number
}

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
