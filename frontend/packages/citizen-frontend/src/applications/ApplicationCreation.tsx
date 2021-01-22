import React, { useEffect, useMemo, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { H1, H2, P } from '@evaka/lib-components/src/typography'
import { Gap, defaultMargins } from '@evaka/lib-components/src/white-space'
import ButtonContainer from '@evaka/lib-components/src/layout/ButtonContainer'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { useUser } from '~auth'
import { useTranslation } from '~localization'
import { ApplicationType } from '~applications/types'
import { createApplication, getDuplicateApplications } from '~applications/api'
import InfoBallWrapper from '~applications/InfoBallWrapper'

export default React.memo(function ApplicationCreation() {
  const history = useHistory()
  const { childId } = useParams<{ childId: string }>()
  const t = useTranslation()
  const user = useUser()
  const child = useMemo(() => user?.children.find(({ id }) => childId === id), [
    childId,
    user
  ])
  const [selectedType, setSelectedType] = useState<ApplicationType>()

  const [duplicates, setDuplicates] = useState<
    Record<ApplicationType, boolean>
  >(duplicatesDefault)
  useEffect(() => {
    void getDuplicateApplications(childId).then(setDuplicates)
  }, [])

  const duplicateExists =
    selectedType !== undefined && duplicates[selectedType] === true

  if (child === undefined) {
    return <Redirect to="/applications" />
  }

  return (
    <Container>
      <ReturnButton label={t.common.return} />
      <ContentArea opaque paddingVertical="L">
        <H1 noMargin>{t.applications.creation.title}</H1>
        <Gap size="m" />
        <H2 noMargin>
          {child.firstName} {child.lastName}
        </H2>
        <Gap size="XL" />
        <InfoBallWrapper infoText={t.applications.creation.daycareInfo}>
          <Radio
            checked={selectedType === 'daycare'}
            onChange={() => setSelectedType('daycare')}
            label={t.applications.creation.daycareLabel}
          />
        </InfoBallWrapper>
        <Gap size="s" />
        <InfoBallWrapper infoText={t.applications.creation.preschoolInfo}>
          <Radio
            checked={selectedType === 'preschool'}
            onChange={() => setSelectedType('preschool')}
            label={t.applications.creation.preschoolLabel}
          />
        </InfoBallWrapper>
        <PreschoolDaycareInfo>
          {t.applications.creation.preschoolDaycareInfo}
        </PreschoolDaycareInfo>
        <Gap size="s" />
        <InfoBallWrapper infoText={t.applications.creation.clubInfo}>
          <Radio
            checked={selectedType === 'club'}
            onChange={() => setSelectedType('club')}
            label={t.applications.creation.clubLabel}
          />
        </InfoBallWrapper>
        {duplicateExists ? (
          <>
            <Gap size="L" />
            <AlertBox thin message={t.applications.creation.duplicateWarning} />
          </>
        ) : null}
        <Gap size="s" />
        <P
          dangerouslySetInnerHTML={{
            __html: t.applications.creation.applicationInfo
          }}
        />
      </ContentArea>
      <ContentArea opaque={false} paddingVertical="L">
        <ButtonContainer justify="center">
          <AsyncButton
            primary
            text={t.applications.creation.create}
            disabled={selectedType === undefined || duplicateExists}
            onClick={() =>
              selectedType !== undefined
                ? createApplication(
                    childId,
                    selectedType
                  ).then((applicationId) =>
                    history.push(`/applications/${applicationId}/edit`)
                  )
                : Promise.resolve()
            }
            onSuccess={() => undefined}
          />
          <Button
            text={t.common.cancel}
            onClick={() => history.push('/applications')}
          />
        </ButtonContainer>
      </ContentArea>
    </Container>
  )
})

const duplicatesDefault: Record<ApplicationType, boolean> = {
  club: false,
  daycare: false,
  preschool: false
}

const PreschoolDaycareInfo = styled.p`
  margin: 0;
  margin-left: calc(
    36px + ${defaultMargins.s}
  ); // width of the radio input's icon + the margin on label
  font-weight: 600;
  font-size: 0.875em;
`
