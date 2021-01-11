import React from 'react'
import { useTranslation } from '~localization'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'

export default React.memo(function Footer() {
  const t = useTranslation()
  return (
    <FooterContainer>
      <FooterItem>{t.footer.espooLabel}</FooterItem>
      <FooterItem>
        <FooterLink href={t.footer.privacyPolicyLink} data-qa={'nav-old-map'}>
          {t.footer.privacyPolicy}
        </FooterLink>
      </FooterItem>
      <FooterItem>
        <FooterLink href={t.footer.sendFeedbackLink} data-qa={'nav-old-map'}>
          {t.footer.sendFeedback}
        </FooterLink>
      </FooterItem>
    </FooterContainer>
  )
})

const FooterContainer = styled.footer`
  display: flex;
  flex-direction: row;
  justify-content: center;
  flex-wrap: wrap;
  padding: 20px 0 20px 0;
  font-size: 12px;
  font-weight: 400;
`

const FooterItem = styled.div`
  display: inline-block;
  margin: 0 40px 0 40px;
`

const FooterLink = styled.a`
  color: ${colors.brandEspoo.espooBlue};
  text-decoration: none;
`
