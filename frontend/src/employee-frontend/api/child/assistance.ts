import { AssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getAssistanceData(
  childId: UUID
): Promise<AssistanceResponse> {
  return client
    .get<JsonOf<AssistanceResponse>>(
      `/children/${encodeURIComponent(childId)}/assistance`
    )
    .then(({ data }) => ({
      assistanceNeeds: data.assistanceNeeds.map((row) => ({
        need: {
          ...row.need,
          startDate: LocalDate.parseIso(row.need.startDate),
          endDate: LocalDate.parseIso(row.need.endDate)
        },
        permittedActions: row.permittedActions
      })),
      assistanceActions: data.assistanceActions.map((row) => ({
        action: {
          ...row.action,
          startDate: LocalDate.parseIso(row.action.startDate),
          endDate: LocalDate.parseIso(row.action.endDate)
        },
        permittedActions: row.permittedActions
      }))
    }))
}
