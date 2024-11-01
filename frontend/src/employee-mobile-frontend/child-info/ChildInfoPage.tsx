// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { childBasicInfoQuery } from '../child-attendance/queries'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { TallContentArea } from '../pairing/components'

import ChildBasicInfo from './ChildBasicInfo'
import ChildSensitiveInfo from './ChildSensitiveInfo'

export default React.memo(function ChildInfoPage({
  unitId,
  childId
}: {
  unitId: UUID
  childId: UUID
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const childBasicInfo = useQueryResult(childBasicInfoQuery({ childId }))
  const childName = useMemo(
    () =>
      childBasicInfo
        .map((c) => (c ? `${c.firstName} ${c.lastName}` : null))
        .getOrElse(null),
    [childBasicInfo]
  )

  const [showSensitiveInfo, setshowSensitiveInfo] = useState<boolean>(false)

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

      <FixedSpaceColumn spacing="m">
        <ContentArea shadow opaque paddingHorizontal="s" paddingVertical="xs">
          <FixedSpaceColumn alignItems="center" spacing="m">
            <Title noMargin>{i18n.childInfo.header}</Title>
          </FixedSpaceColumn>
        </ContentArea>

        {renderResult(childBasicInfo, (child) => (
          <ChildBasicInfo child={child} />
        ))}

        {showSensitiveInfo ? (
          <ChildSensitiveInfo childId={childId} unitId={unitId} />
        ) : (
          <ShowSensitiveInfoButtonContainer>
            <Button
              data-qa="show-sensitive-info-button"
              appearance="inline"
              text={i18n.childInfo.showSensitiveInfo}
              onClick={() => setshowSensitiveInfo(true)}
            />
            <Gap size="m" />
          </ShowSensitiveInfoButtonContainer>
        )}
      </FixedSpaceColumn>
    </TallContentAreaNoOverflow>
  )
})

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`
const ShowSensitiveInfoButtonContainer = styled.div`
  margin-left: 16px;
`
