import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'

export const ItemTypes = {
  APPLICATION_CARD: 'application-card'
}

export interface DropResult {
  daycareId: DaycareId | null
}

export type OnDropApplication = (
  applicationId: ApplicationId,
  daycareId: DaycareId | null
) => void

export type OnClickApplication = (applicationId: ApplicationId) => void

export type OnLockPlacement = (applicationId: ApplicationId) => void

export type OnUnlockPlacement = (applicationId: ApplicationId) => void
