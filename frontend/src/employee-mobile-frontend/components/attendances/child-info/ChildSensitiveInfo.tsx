import React from 'react'
import { Child } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { ContentAreaWithShadow } from '../../mobile/components'
import { FixedSpaceColumn } from '../../../../lib-components/layout/flex-helpers'
import Title from '../../../../lib-components/atoms/Title'
import CollapsibleSection from '../../../../lib-components/molecules/CollapsibleSection'
import { P, Label } from 'lib-components/typography'

interface Props {
  child: Child | null
}

const render = (label: string, value: string | null, dataQa: string) => {
  return value ? (
    <>
      <Label>{label}</Label>
      <P data-qa={dataQa}>{value}</P>
    </>
  ) : null
}

export default React.memo(function ChildSensitiveInfo({ child }: Props) {
  const { i18n } = useTranslation()

  return (
    <>
      {child && (
        <FixedSpaceColumn spacing={'m'}>
          <ContentAreaWithShadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <FixedSpaceColumn alignItems={'center'} spacing={'m'}>
              <Title>{i18n.attendances.childInfo.header}</Title>
            </FixedSpaceColumn>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.personalInfoHeader}
            >
              <Label>{i18n.attendances.childInfo.childName}</Label>
              <P
                data-qa={'child-info-name'}
              >{`${child.firstName} ${child.lastName}`}</P>

              {render(
                i18n.attendances.childInfo.preferredName,
                child.preferredName,
                'child-info-preferred-name'
              )}

              {render(
                i18n.attendances.childInfo.address,
                child.childAddress,
                'child-info-child-address'
              )}
            </CollapsibleSection>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.allergiesHeader}
            >
              {render(
                i18n.attendances.childInfo.allergies,
                child.allergies,
                'child-info-allergies'
              )}

              {render(
                i18n.attendances.childInfo.diet,
                child.diet,
                'child-info-diet'
              )}

              {render(
                i18n.attendances.childInfo.medication,
                child.medication,
                'child-info-medication'
              )}
            </CollapsibleSection>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.contactInfoHeader}
            >
              {child.contact1 && (
                <FixedSpaceColumn>
                  <Title>{i18n.attendances.childInfo.contact1}</Title>

                  <Label>{i18n.attendances.childInfo.name}</Label>
                  <P data-qa={'child-info-contact1-name'}>{`${
                    child.contact1.firstName || ''
                  } ${child.contact1.lastName || ''}`}</P>

                  {render(
                    i18n.attendances.childInfo.phone,
                    child.contact1.phone,
                    'child-info-contact1-phone'
                  )}

                  {render(
                    i18n.attendances.childInfo.backupPhone,
                    child.contact1.backupPhone,
                    'child-info-contact1-backup-phone'
                  )}

                  {render(
                    i18n.attendances.childInfo.email,
                    child.contact1.email,
                    'child-info-contact1-email'
                  )}
                </FixedSpaceColumn>
              )}

              {child.contact2 && (
                <FixedSpaceColumn>
                  <Title>{i18n.attendances.childInfo.contact2}</Title>

                  <Label>{i18n.attendances.childInfo.name}</Label>
                  <P data-qa={'child-info-contact2-name'}>{`${
                    child.contact2.firstName || ''
                  } ${child.contact2.lastName || ''}`}</P>

                  {render(
                    i18n.attendances.childInfo.phone,
                    child.contact2.phone,
                    'child-info-contact2-phone'
                  )}

                  {render(
                    i18n.attendances.childInfo.backupPhone,
                    child.contact2.backupPhone,
                    'child-info-contact2-backup-phone'
                  )}

                  {render(
                    i18n.attendances.childInfo.email,
                    child.contact2.email,
                    'child-info-contact2-email'
                  )}
                </FixedSpaceColumn>
              )}

              {child.backupPickup1 && (
                <FixedSpaceColumn>
                  <Title>{i18n.attendances.childInfo.backupPicker1}</Title>

                  <Label>{i18n.attendances.childInfo.backupPickerName}</Label>
                  {render(
                    i18n.attendances.childInfo.backupPickerName,
                    child.backupPickup1.firstName,
                    'child-info-backup-pickup1-name'
                  )}

                  {render(
                    i18n.attendances.childInfo.phone,
                    child.backupPickup1.phone,
                    'child-info-backup-pickup1-phone'
                  )}
                </FixedSpaceColumn>
              )}

              {child.backupPickup2 && (
                <FixedSpaceColumn>
                  <Title>{i18n.attendances.childInfo.backupPicker2}</Title>

                  <Label>{i18n.attendances.childInfo.backupPickerName}</Label>
                  {render(
                    i18n.attendances.childInfo.backupPickerName,
                    child.backupPickup2.firstName,
                    'child-info-backup-pickup2-name'
                  )}

                  {render(
                    i18n.attendances.childInfo.phone,
                    child.backupPickup2.phone,
                    'child-info-backup-pickup2-name'
                  )}
                </FixedSpaceColumn>
              )}
            </CollapsibleSection>
          </ContentAreaWithShadow>
        </FixedSpaceColumn>
      )}
    </>
  )
})
