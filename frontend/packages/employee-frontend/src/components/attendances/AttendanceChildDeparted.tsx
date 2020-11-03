import React, { Fragment } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'

import { ChildInGroup, deleteAttendance } from '~api/unit'
import Button from '~components/shared/atoms/buttons/Button'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'

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

export default React.memo(function AttendanceChildDeparted({
  child,
  id,
  groupid
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  async function returnToComing() {
    if (child.childAttendanceId) {
      await deleteAttendance(child.childAttendanceId)
      history.push(`/units/${id}/attendance/${groupid}/coming`)
    }
  }

  return (
    <Fragment>
      <WideButton
        disabled={!child.childAttendanceId}
        primary
        text={i18n.attendances.actions.returnToComing}
        onClick={returnToComing}
      />
    </Fragment>
  )
})
