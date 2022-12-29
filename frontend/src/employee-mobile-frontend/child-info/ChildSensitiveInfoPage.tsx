// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'

import { renderPinRequiringResult } from '../auth/renderPinRequiringResult'
import { childrenQuery } from '../child-attendance/queries'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { TallContentArea } from '../pairing/components'

import ChildSensitiveInfo from './ChildSensitiveInfo'
import { getChildSensitiveInformation } from './api'

export default React.memo(function ChildSensitiveInfoPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { childId, unitId } = useNonNullableParams<{
    childId: UUID
    unitId: UUID
  }>()
  const unitChildren = useQueryResult(childrenQuery(unitId))

  const childName = useMemo(
    () =>
      unitChildren
        .map((children) => children.find((ac) => ac.id === childId))
        .map((c) => (c ? `${c.firstName} ${c.lastName}` : null))
        .getOrElse(null),
    [unitChildren, childId]
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
      <TopBar
        title={childName ?? i18n.common.back}
        onBack={() => navigate(-1)}
        invertedColors
      />
      {renderPinRequiringResult(childSensitiveResult, (child) => (
        <ChildSensitiveInfo child={child} />
      ))}
    </TallContentAreaNoOverflow>
  )
})

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`
