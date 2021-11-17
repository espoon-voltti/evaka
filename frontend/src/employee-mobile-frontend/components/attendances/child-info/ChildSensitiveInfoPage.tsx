// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useApiState } from 'lib-common/utils/useRestApi'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'
import React, { useContext, useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getChildSensitiveInformation } from '../../../api/sensitive'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { renderPinRequiringResult } from '../../auth/renderPinRequiringResult'
import { TallContentArea } from '../../mobile/components'
import { BackButtonInline } from '../components'
import ChildSensitiveInfo from './ChildSensitiveInfo'

export default React.memo(function ChildSensitiveInfoPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const { childId } = useParams<{ childId: string }>()

  const childName = useMemo(
    () =>
      attendanceResponse
        .map(({ children }) => children.find((ac) => ac.id === childId))
        .map((c) => (c ? `${c.firstName} ${c.lastName}` : null))
        .getOrElse(null),
    [attendanceResponse, childId]
  )

  const [childSensitiveResult] = useApiState(
    () => getChildSensitiveInformation(childId),
    [childId]
  )

  return (
    <TallContentAreaNoOverflow
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopBarContainer>
        <TopRow>
          <BackButtonInline
            onClick={() => history.goBack()}
            icon={faArrowLeft}
            text={childName ?? i18n.common.back}
            data-qa="go-back"
          />
        </TopRow>
      </TopBarContainer>
      {renderPinRequiringResult(childSensitiveResult, (child) => (
        <ChildSensitiveInfo child={child} />
      ))}
    </TallContentAreaNoOverflow>
  )
})

const TopBarContainer = styled.div`
  display: grid;
  grid-template-columns: auto 50px;
`

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;
  max-width: 100vw;

  button {
    margin-right: ${defaultMargins.s};
  }
`
