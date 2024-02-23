// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationDetails as ApplicationDetailsGen } from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'

import HelsinkiDateTime from '../../helsinki-date-time'
import { JsonOf } from '../../json'

export type ApplicationDetails = ApplicationDetailsGen

export const deserializeApplicationDetails = (
  json: JsonOf<ApplicationDetails>
): ApplicationDetails => ({
  ...json,
  form: {
    ...json.form,
    child: {
      ...json.form.child,
      dateOfBirth: LocalDate.parseNullableIso(json.form.child.dateOfBirth),
      futureAddress: json.form.child.futureAddress
        ? {
            ...json.form.child.futureAddress,
            movingDate: LocalDate.parseNullableIso(
              json.form.child.futureAddress.movingDate
            )
          }
        : null
    },
    guardian: {
      ...json.form.guardian,
      futureAddress: json.form.guardian.futureAddress
        ? {
            ...json.form.guardian.futureAddress,
            movingDate: LocalDate.parseNullableIso(
              json.form.guardian.futureAddress.movingDate
            )
          }
        : null
    },
    preferences: {
      ...json.form.preferences,
      preferredStartDate: LocalDate.parseNullableIso(
        json.form.preferences.preferredStartDate
      ),
      connectedDaycarePreferredStartDate: LocalDate.parseNullableIso(
        json.form.preferences.connectedDaycarePreferredStartDate
      )
    }
  },
  guardianDateOfDeath: LocalDate.parseNullableIso(json.guardianDateOfDeath),
  createdDate: json.createdDate
    ? HelsinkiDateTime.parseIso(json.createdDate)
    : null,
  modifiedDate: json.modifiedDate
    ? HelsinkiDateTime.parseIso(json.modifiedDate)
    : null,
  sentDate: LocalDate.parseNullableIso(json.sentDate),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  dueDateSetManuallyAt: json.dueDateSetManuallyAt
    ? HelsinkiDateTime.parseIso(json.dueDateSetManuallyAt)
    : null,
  attachments: json.attachments.map(({ updated, receivedAt, ...rest }) => ({
    ...rest,
    updated: HelsinkiDateTime.parseIso(updated),
    receivedAt: HelsinkiDateTime.parseIso(receivedAt)
  }))
})
