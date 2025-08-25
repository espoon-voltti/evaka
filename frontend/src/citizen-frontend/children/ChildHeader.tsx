// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'

import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import { RoundImage } from 'lib-components/atoms/RoundImage'
import { desktopMin } from 'lib-components/breakpoints'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H1, Title } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farUser } from 'lib-icons'

import { getImageCitizen } from '../generated/api-clients/childimages'
import { useTranslation } from '../localization'

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
  child: { firstName, imageId, group, unit, lastName }
}: {
  child: ChildAndPermittedActions
}) {
  const t = useTranslation()
  const mainHeading = useRef<HTMLHeadingElement | null>(null)
  useEffect(() => {
    // focus on the accessibility heading
    if (mainHeading.current) {
      mainHeading.current.focus()
    }
  }, [firstName, lastName])
  return (
    <ChildHeaderContainer>
      <RoundImage
        size="XXL"
        src={imageId ? getImageCitizen({ imageId }).url.toString() : null}
        fallbackContent={farUser}
        fallbackColor={colors.grayscale.g15}
        alt={t.children.childPicture}
      />
      <div>
        <H1 noMargin data-qa="child-name" tabIndex={-1} ref={mainHeading}>
          <PersonName
            person={{ firstName, lastName }}
            format={childPageNameFormat}
          />
        </H1>
        {group && <Title>{group.name}</Title>}
        <br />
        {unit && <span>{unit.name}</span>}
      </div>
    </ChildHeaderContainer>
  )
})

export const childPageNameFormat = 'First Last'
