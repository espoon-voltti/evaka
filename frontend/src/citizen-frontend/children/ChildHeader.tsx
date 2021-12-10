// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Child } from 'lib-common/generated/api-types/children'
import { RoundImage } from 'lib-components/atoms/RoundImage'
import { desktopMin } from 'lib-components/breakpoints'
import { H1, Strong } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { farUser } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'

const ChildHeaderContainer = styled.div`
  display: flex;
  align-items: center;

  // values below are changed by media query
  flex-direction: column;
  justify-content: center;
  gap: ${defaultMargins.xs};
  text-align: center;

  @media (min-width: ${desktopMin}) {
    flex-direction: row;
    justify-content: flex-start;
    gap: ${defaultMargins.L};
    text-align: start;
  }
`

export default React.memo(function ChildHeader({
  child: { firstName, imageId, group, lastName }
}: {
  child: Child
}) {
  return (
    <ChildHeaderContainer>
      <RoundImage
        size="XXL"
        src={
          imageId ? `/api/application/citizen/child-images/${imageId}` : null
        }
        fallbackContent={farUser}
        fallbackColor={colors.greyscale.lighter}
      />
      <div>
        <H1 noMargin>
          {firstName} {lastName}
        </H1>
        <Strong>{group?.name}</Strong>
      </div>
    </ChildHeaderContainer>
  )
})
