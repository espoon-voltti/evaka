import React, { Fragment, useState } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'

import { childDeparts, ChildInGroup } from '~api/unit'
import Button from '~components/shared/atoms/buttons/Button'
import InputField from '~components/shared/atoms/form/InputField'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { FlexLabel } from './AttendanceChildPage'

const WideButton = styled(Button)`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: 100%;
    white-space: normal;
    height: 64px;
  }
`

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

  async function markDeparted() {
    const hours = parseInt(time.slice(0, 2))
    const minutes = parseInt(time.slice(3, 5))
    const today = new Date()
    today.setHours(hours)
    today.setMinutes(minutes)
    await childDeparts(child.childId, today)
    history.push(`/units/${id}/attendance/${groupid}/departed`)
  }

  return (
    <Fragment>
      <WideButton
        primary
        text={i18n.attendances.actions.markLeaving}
        onClick={markDeparted}
      />
      <FlexLabel>
        <span>{i18n.attendances.timeLabel}</span>
        <InputField onChange={setTime} value={time} width="s" type="time" />
      </FlexLabel>
    </Fragment>
  )
})
