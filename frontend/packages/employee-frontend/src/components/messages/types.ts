import { JsonOf } from '@evaka/lib-common/src/json'
import { UUID } from '~types'

export type Bulletin = {
  id: UUID
  title: string
  content: string
  createdByEmployee: UUID
  groupId: UUID | null
  sentAt: Date | null
}

export function deserializeBulletin(json: JsonOf<Bulletin>): Bulletin {
  return {
    ...json,
    sentAt: json.sentAt ? new Date(json.sentAt) : null
  }
}

export type IdAndName = {
  id: UUID
  name: string
}
