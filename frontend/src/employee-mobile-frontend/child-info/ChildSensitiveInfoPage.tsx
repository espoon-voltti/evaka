// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Result, Success } from 'lib-common/api'
import { ChildSensitiveInformation } from 'lib-common/generated/api-types/sensitive'
import { useQueryResult } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { useApiState } from 'lib-common/utils/useRestApi'

import { mapPinLoginRequiredError, PinLoginRequired } from '../auth/api'
import { renderPinRequiringResult } from '../auth/renderPinRequiringResult'
import { childrenQuery } from '../child-attendance/queries'
import { useChild } from '../child-attendance/utils'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { getSensitiveInfo } from '../generated/api-clients/sensitive'
import { TallContentArea } from '../pairing/components'

import ChildSensitiveInfo from './ChildSensitiveInfo'

const getSensitiveInfoResult = (
  req: Arg0<typeof getSensitiveInfo>
): Promise<Result<ChildSensitiveInformation | PinLoginRequired>> =>
  getSensitiveInfo(req)
    .then((v) => Success.of(v))
    .catch(mapPinLoginRequiredError)

export default React.memo(function ChildSensitiveInfoPage({
  unitId
}: {
  unitId: UUID
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { childId } = useRouteParams(['childId'])
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const childName = useMemo(
    () =>
      child
        .map((c) => (c ? `${c.firstName} ${c.lastName}` : null))
        .getOrElse(null),
    [child]
  )

  const [childSensitiveResult] = useApiState(
    () => getSensitiveInfoResult({ childId }),
    [childId]
  )

  return (
    <TallContentAreaNoOverflow
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopBar
        title={childName ?? i18n.common.back}
        onBack={() => navigate(-1)}
        unitId={unitId}
        invertedColors
      />
      {renderPinRequiringResult(childSensitiveResult, unitId, (child) => (
        <ChildSensitiveInfo child={child} />
      ))}
    </TallContentAreaNoOverflow>
  )
})

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`
