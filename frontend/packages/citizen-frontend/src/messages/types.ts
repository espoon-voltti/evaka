import { JsonOf } from '@evaka/lib-common/src/json'

export type ReceivedBulletin = {
  id: string
  sentAt: Date
  sender: string
  title: string
  content: string
  isRead: boolean
}

export const deserializeReceivedBulletin = (
  json: JsonOf<ReceivedBulletin>
): ReceivedBulletin => ({
  ...json,
  sentAt: new Date(json.sentAt)
})
