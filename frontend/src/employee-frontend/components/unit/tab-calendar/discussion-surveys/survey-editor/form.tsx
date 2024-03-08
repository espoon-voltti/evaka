import { boolean, requiredLocalTimeRange, string } from 'lib-common/form/fields'
import { array, object, recursive, required, value } from 'lib-common/form/form'
import { Form } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { TreeNodeInfo } from './DiscussionTimesForm'

export const treeNodeInfo = (): Form<
  TreeNodeInfo,
  never,
  TreeNodeInfo,
  unknown
> =>
  object({
    text: string(),
    key: string(),
    checked: boolean(),
    children: array(recursive(treeNodeInfo)),
    firstName: string(),
    lastName: string()
  })

export const basicInfoForm = object({
  title: required(string()),
  description: required(string())
})

export const calendarEventTimeForm = object({
  id: value<UUID>(),
  childId: value<UUID | null>(),
  date: required(value<LocalDate>()),
  timeRange: required(requiredLocalTimeRange())
})

export const timesForm = object({
  times: array(calendarEventTimeForm)
})

export const surveyForm = object({
  title: required(string()),
  description: required(string()),
  attendees: array(recursive(treeNodeInfo))
})
