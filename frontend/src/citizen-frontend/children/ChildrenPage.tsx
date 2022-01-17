// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'
import Footer from 'citizen-frontend/Footer'
import { Child } from 'lib-common/generated/api-types/children'
import { useApiState } from 'lib-common/utils/useRestApi'
import { RoundImage } from 'lib-components/atoms/RoundImage'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { desktopMin } from 'lib-components/breakpoints'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  Desktop,
  MobileAndTablet
} from 'lib-components/layout/responsive-layout'
import {
  Bold,
  H1,
  H2,
  InformationText,
  Label,
  P
} from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronRight, faUser } from 'lib-icons'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { getChildren } from './api'

const Children = styled.div`
  display: flex;
  flex-direction: column;
  @media (min-width: ${desktopMin}) {
    gap: ${defaultMargins.s};
  }
`

const ChildContainer = styled(ContentArea)`
  cursor: pointer;
  display: flex;
  align-items: center;

  border-bottom: 1px solid ${colors.grayscale.g15};
  gap: ${defaultMargins.s};
  min-height: 80px;
  @media (min-width: ${desktopMin}) {
    border-bottom-color: transparent;
    gap: ${defaultMargins.L};
    min-height: 192px;
  }
`

const NameAndGroup = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`

const ChevronContainer = styled.div`
  flex: 1 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
`

const ChildItem = React.memo(function ChildItem({ child }: { child: Child }) {
  const history = useHistory()
  const t = useTranslation()
  const navigateToChild = useCallback(
    () => history.push(`/children/${child.id}`),
    [history, child.id]
  )

  const name = `${child.firstName} ${child.lastName}`
  return (
    <ChildContainer onClick={navigateToChild} opaque data-qa="child">
      <RoundImage
        size="XL"
        sizeDesktop="XXL"
        src={
          child.imageId
            ? `/api/application/citizen/child-images/${child.imageId}`
            : null
        }
        fallbackContent={faUser}
        fallbackColor={colors.grayscale.g15}
        alt={t.children.childPicture}
      />
      <MobileAndTablet>
        <NameAndGroup>
          <Label primary>{name}</Label>
          {child.group && <InformationText>{child.group.name}</InformationText>}
        </NameAndGroup>
      </MobileAndTablet>
      <Desktop>
        <NameAndGroup>
          <H2 primary noMargin>
            {name}
          </H2>
          {child.group && <Bold>{child.group.name}</Bold>}
        </NameAndGroup>
      </Desktop>
      <ChevronContainer>
        <IconButton icon={faChevronRight} />
      </ChevronContainer>
    </ChildContainer>
  )
})

export default React.memo(function ChildrenPage() {
  const [childrenResponse] = useApiState(getChildren, [])
  const t = useTranslation()

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque>
          <H1 noMargin>{t.children.title}</H1>
          <P>{t.children.pageDescription}</P>
        </ContentArea>
        <Gap size="s" />
        {renderResult(childrenResponse, ({ children }) => (
          <Children>
            {children.length > 0 ? (
              children.map((c) => <ChildItem key={c.id} child={c} />)
            ) : (
              <P>{t.children.noChildren}</P>
            )}
          </Children>
        ))}
      </Container>
      <Footer />
    </>
  )
})
