import React from 'react'
import { Child } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { ContentAreaWithShadow } from '../../mobile/components'
import { FixedSpaceColumn } from '../../../../lib-components/layout/flex-helpers'
import Title from '../../../../lib-components/atoms/Title'
import CollapsibleSection from '../../../../lib-components/molecules/CollapsibleSection'
import { P, Label } from 'lib-components/typography'
import { PlacementType } from '../../../types'
import styled from 'styled-components'

interface Props {
  child: Child | null
}

const KeyValue = styled.div`
  display: flex;
  flex-direction: column;
`

const Divider = styled.div`
  border-bottom: 1px solid #d8d8d8;
`

const renderKeyValue = (
  label: string,
  value: string | null,
  dataQa: string
) => {
  return value ? (
    <KeyValue>
      <Label>{label}</Label>
      <span data-qa={dataQa}>{value}</span>
    </KeyValue>
  ) : null
}

export default React.memo(function ChildSensitiveInfo({ child }: Props) {
  const { i18n } = useTranslation()

  const placementTypesToText = (types: PlacementType[]): string => {
    return types.map((type) => i18n.common.placement[type]).join(',')
  }

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
              <FixedSpaceColumn>
                <KeyValue>
                  <Label>{i18n.attendances.childInfo.childName}</Label>
                  <P
                    fitted
                    data-qa={'child-info-name'}
                  >{`${child.firstName} ${child.lastName}`}</P>
                </KeyValue>

                {renderKeyValue(
                  i18n.attendances.childInfo.preferredName,
                  child.preferredName,
                  'child-info-preferred-name'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.address,
                  child.childAddress,
                  'child-info-child-address'
                )}

                {child.placementType && child.placementType.length > 0 && (
                  <KeyValue>
                    <Label>{i18n.attendances.childInfo.type}</Label>
                    <P fitted>{placementTypesToText(child.placementType)}</P>
                  </KeyValue>
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.allergiesHeader}
            >
              <FixedSpaceColumn>
                {renderKeyValue(
                  i18n.attendances.childInfo.allergies,
                  child.allergies,
                  'child-info-allergies'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.diet,
                  child.diet,
                  'child-info-diet'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.medication,
                  child.medication,
                  'child-info-medication'
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.contactInfoHeader}
            >
              {child.contact1 && (
                <>
                  <Title size={4}>{i18n.attendances.childInfo.contact1}</Title>
                  <FixedSpaceColumn>
                    <KeyValue>
                      <Label>{i18n.attendances.childInfo.name}</Label>
                      <P fitted data-qa={'child-info-contact1-name'}>{`${
                        child.contact1.firstName || ''
                      } ${child.contact1.lastName || ''}`}</P>
                    </KeyValue>

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      child.contact1.phone,
                      'child-info-contact1-phone'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPhone,
                      child.contact1.backupPhone,
                      'child-info-contact1-backup-phone'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.email,
                      child.contact1.email,
                      'child-info-contact1-email'
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </>
              )}

              {child.contact2 && (
                <>
                  <Title size={4}>{i18n.attendances.childInfo.contact2}</Title>
                  <FixedSpaceColumn>
                    <KeyValue>
                      <Label>{i18n.attendances.childInfo.name}</Label>
                      <P fitted data-qa={'child-info-contact2-name'}>{`${
                        child.contact2.firstName || ''
                      } ${child.contact2.lastName || ''}`}</P>
                    </KeyValue>

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      child.contact2.phone,
                      'child-info-contact2-phone'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPhone,
                      child.contact2.backupPhone,
                      'child-info-contact2-backup-phone'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.email,
                      child.contact2.email,
                      'child-info-contact2-email'
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </>
              )}

              {child.backupPickup1 && (
                <>
                  <Title size={4}>
                    {i18n.attendances.childInfo.backupPicker1}
                  </Title>
                  <FixedSpaceColumn>
                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPickerName,
                      child.backupPickup1.firstName,
                      'child-info-backup-pickup1-name'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      child.backupPickup1.phone,
                      'child-info-backup-pickup1-phone'
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </>
              )}

              {child.backupPickup2 && (
                <>
                  <Title size={4}>
                    {i18n.attendances.childInfo.backupPicker2}
                  </Title>
                  <FixedSpaceColumn>
                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPickerName,
                      child.backupPickup2.firstName,
                      'child-info-backup-pickup2-name'
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      child.backupPickup2.phone,
                      'child-info-backup-pickup2-phone'
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </>
              )}
            </CollapsibleSection>
          </ContentAreaWithShadow>
        </FixedSpaceColumn>
      )}
    </>
  )
})
