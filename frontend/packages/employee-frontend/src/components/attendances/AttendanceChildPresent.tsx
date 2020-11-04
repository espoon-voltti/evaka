import React, { Fragment, useState } from 'react'
import { useHistory } from 'react-router-dom'

import { childDeparts, ChildInGroup } from '~api/unit'
import InputField from '~components/shared/atoms/form/InputField'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { WideAsyncButton } from './AttendanceChildDeparted'
import { FlexLabel } from './AttendanceChildPage'

interface Props {
  child: ChildInGroup
  id: UUID
  groupid: UUID | 'all'
}

export default React.memo(function AttendanceChildPresent({
  child,
  id,
  groupid
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  const [time, setTime] = useState<string>(
    new Date().getHours() < 10
      ? new Date().getMinutes() < 10
        ? `0${new Date().getHours()}:0${new Date().getMinutes()}`
        : `0${new Date().getHours()}:${new Date().getMinutes()}`
      : new Date().getMinutes() < 10
      ? `${new Date().getHours()}:0${new Date().getMinutes()}`
      : `${new Date().getHours()}:${new Date().getMinutes()}`
  )

  function markDeparted() {
    const hours = parseInt(time.slice(0, 2))
    const minutes = parseInt(time.slice(3, 5))
    const today = new Date()
    today.setHours(hours)
    today.setMinutes(minutes)
    return childDeparts(child.childId, today)
  }

  return (
    <Fragment>
      <WideAsyncButton
        primary
        text={i18n.attendances.actions.markLeaving}
        onClick={markDeparted}
        onSuccess={() =>
          history.push(`/units/${id}/attendance/${groupid}/departed`)
        }
        data-qa="mark-departed"
      />
      <FlexLabel>
        <span>{i18n.attendances.timeLabel}</span>
        <InputField
          onChange={setTime}
          value={time}
          width="s"
          type="time"
          data-qa="set-time"
        />
      </FlexLabel>
    </Fragment>
  )
})
