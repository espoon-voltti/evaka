import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'
import { client } from '~api-client'
import { Application } from '~applications/types'

export async function getApplication(
  applicationId: string
): Promise<Result<Application>> {
  try {
    const { data } = await client.get<JsonOf<Application>>(
      `/citizen/applications/${applicationId}`
    )
    return Success.of(deserializeApplication(data))
  } catch (e) {
    return Failure.fromError(e)
  }
}

const deserializeApplication = (json: JsonOf<Application>): Application => ({
  ...json,
  form: {
    ...json.form,
    child: {
      ...json.form.child,
      dateOfBirth: LocalDate.parseNullableIso(json.form.child.dateOfBirth),
      futureAddress: json.form.child.futureAddress
        ? {
            ...json.form.child.futureAddress,
            movingDate: json.form.child.futureAddress.movingDate
              ? new Date(json.form.child.futureAddress.movingDate)
              : null
          }
        : null
    },
    guardian: {
      ...json.form.guardian,
      futureAddress: json.form.guardian.futureAddress
        ? {
            ...json.form.guardian.futureAddress,
            movingDate: json.form.guardian.futureAddress.movingDate
              ? new Date(json.form.guardian.futureAddress.movingDate)
              : null
          }
        : null
    },
    preferences: {
      ...json.form.preferences,
      preferredStartDate: LocalDate.parseNullableIso(
        json.form.preferences.preferredStartDate
      )
    }
  },
  createdDate: json.createdDate ? new Date(json.createdDate) : null,
  modifiedDate: json.modifiedDate ? new Date(json.modifiedDate) : null,
  sentDate: LocalDate.parseNullableIso(json.sentDate),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  attachments: json.attachments.map(({ updated, ...rest }) => ({
    ...rest,
    updated: new Date(updated)
  }))
})
