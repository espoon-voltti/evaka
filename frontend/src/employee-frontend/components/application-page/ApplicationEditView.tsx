// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { flow, set } from 'lodash/fp'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type { SelectableOtherGuardianAgreementStatus } from 'lib-common/api-types/application/ApplicationFormData'
import { swapElements } from 'lib-common/array'
import DateRange from 'lib-common/date-range'
import type {
  Address,
  ApplicationAttachmentType,
  ApplicationDetails,
  FutureAddress,
  PersonBasics
} from 'lib-common/generated/api-types/application'
import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type { AttachmentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import type { InputInfo } from 'lib-components/atoms/form/InputField'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import FileUpload from 'lib-components/molecules/FileUpload'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faArrowDown,
  faArrowUp,
  faChild,
  faExclamationTriangle,
  faFileAlt,
  faInfo,
  faMapMarkerAlt,
  faTimes,
  faUserFriends,
  faUsers
} from 'lib-icons'

import { getAttachmentUrl, applicationAttachment } from '../../api/attachments'
import ApplicationStatusSection from '../../components/application-page/ApplicationStatusSection'
import ApplicationTitle from '../../components/application-page/ApplicationTitle'
import VTJGuardian from '../../components/application-page/VTJGuardian'
import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { InputWarning } from '../common/InputWarning'

interface PreschoolApplicationProps {
  application: ApplicationDetails
  setApplication: React.Dispatch<
    React.SetStateAction<ApplicationDetails | undefined>
  >
  errors: Record<string, string>
  units: Result<PublicUnit[]>
  guardians: PersonJSON[]
  serviceNeedOptions: ServiceNeedOptionPublicInfo[]
}

const FileUploadGridContainer = styled.div`
  grid-column: 1 / span 2;
  margin: 8px 0;
`
const SubRadios = styled.div`
  margin-bottom: ${defaultMargins.s};
  margin-left: ${defaultMargins.XL};
`
const PreferredUnitGridContainer = styled.div`
  display: inline-grid;
  grid-column-gap: ${defaultMargins.s};
  grid-template-columns: 1fr auto auto auto auto;
`

const selectableOtherGuardianAgreementStatuses: (SelectableOtherGuardianAgreementStatus | null)[] =
  ['AGREED', 'NOT_AGREED', 'RIGHT_TO_GET_NOTIFIED', null]

export default React.memo(function ApplicationEditView({
  application,
  setApplication,
  errors,
  units,
  guardians,
  serviceNeedOptions
}: PreschoolApplicationProps) {
  const { i18n } = useTranslation()
  const [placementType, setPlacementType] = useState<PlacementType | null>(
    application.form.preferences.serviceNeed?.serviceNeedOption
      ?.validPlacementType ?? null
  )

  const {
    type,
    form: {
      child,
      guardian,
      secondGuardian,
      otherPartner,
      otherChildren,
      preferences: {
        preferredUnits,
        preferredStartDate,
        connectedDaycarePreferredStartDate,
        urgent,
        serviceNeed,
        siblingBasis,
        preparatory
      },
      otherInfo,
      clubDetails
    },
    childId,
    guardianId,
    childRestricted,
    guardianRestricted,
    attachments
  } = application
  const serviceNeedPreferredStartDate =
    type === 'PRESCHOOL' &&
    featureFlags.preschoolApplication.connectedDaycarePreferredStartDate
      ? connectedDaycarePreferredStartDate
      : preferredStartDate
  const serviceNeedOptionsByType = useMemo(
    () =>
      serviceNeedOptions?.reduce<
        Map<PlacementType, ServiceNeedOptionPublicInfo[]>
      >((map, item) => {
        if (
          serviceNeedPreferredStartDate === null ||
          !new DateRange(item.validFrom, item.validTo).includes(
            serviceNeedPreferredStartDate
          )
        ) {
          return map
        }
        const key = item.validPlacementType
        const list = map.get(key) ?? []
        list.push(item)
        map.set(key, list)
        return map
      }, new Map<PlacementType, ServiceNeedOptionPublicInfo[]>()) ??
      new Map<PlacementType, ServiceNeedOptionPublicInfo[]>(),
    [serviceNeedOptions, serviceNeedPreferredStartDate]
  )

  const fullTimeOptions = useMemo(
    () => serviceNeedOptionsByType.get('DAYCARE') ?? [],
    [serviceNeedOptionsByType]
  )
  const partTimeOptions = useMemo(
    () => serviceNeedOptionsByType.get('DAYCARE_PART_TIME') ?? [],
    [serviceNeedOptionsByType]
  )

  const preferencesInUnitsList = units
    .map((us) =>
      preferredUnits.filter(({ id }) => us.find((unit) => unit.id === id))
    )
    .getOrElse(preferredUnits)

  const connectedDaycare = type === 'PRESCHOOL' && serviceNeed !== null
  const paid = type === 'DAYCARE' || connectedDaycare

  const otherGuardian = guardians.find((guardian) => guardian.id !== guardianId)

  const formatPersonName = (person: PersonBasics) =>
    formatName(person.firstName, person.lastName, i18n, true)
  const formatAddress = (a: Address) =>
    `${a.street}, ${a.postalCode} ${a.postOffice}`

  const onAttachmentUploaded = useCallback(
    (type: ApplicationAttachmentType) => (attachment: Attachment) =>
      setApplication(
        (prev) =>
          prev && {
            ...prev,
            attachments: [
              ...prev.attachments,
              {
                ...attachment,
                type,
                updated: HelsinkiDateTime.now(),
                receivedAt: HelsinkiDateTime.now(),
                uploadedByEmployee: null,
                uploadedByPerson: null
              }
            ]
          }
      ),
    [setApplication]
  )

  const handleAttachmentDeleted = useCallback(
    (id: AttachmentId) =>
      setApplication(
        (prev) =>
          prev && {
            ...prev,
            attachments: prev.attachments.filter((a) => a.id !== id)
          }
      ),
    [setApplication]
  )

  const validationErrorInfo = (field: string): InputInfo | undefined =>
    errors[field]
      ? {
          text: errors[field],
          status: 'warning'
        }
      : undefined

  const updateServiceNeed = (partTime: boolean) => {
    let serviceNeedOption = serviceNeed?.serviceNeedOption
    if (partTime && partTimeOptions.length > 0) {
      serviceNeedOption = partTimeOptions[0]
    } else if (!partTime && fullTimeOptions.length > 0) {
      serviceNeedOption = fullTimeOptions[0]
    }
    setApplication(
      flow(
        set('form.preferences.serviceNeed.partTime', partTime),
        set('form.preferences.serviceNeed.serviceNeedOption', serviceNeedOption)
      )
    )
  }

  return (
    <div data-qa="application-edit-view">
      <ApplicationTitle application={application} />
      <Gap />

      <CollapsibleSection
        title={i18n.application.serviceNeed.title}
        icon={faFileAlt}
      >
        <ListGrid>
          <Label>{i18n.application.serviceNeed.startDate}</Label>
          <div>
            <HorizontalContainer>
              <DatePicker
                date={preferredStartDate}
                onChange={(value) => {
                  setApplication(
                    set('form.preferences.preferredStartDate', value)
                  )
                }}
                locale="fi"
                data-qa="datepicker-start-date"
              />
              {errors['form.preferences.preferredStartDate'] ? (
                <>
                  <Gap size="s" horizontal />
                  <InputWarning
                    text={errors['form.preferences.preferredStartDate']}
                  />
                </>
              ) : null}
            </HorizontalContainer>
          </div>

          {type === 'DAYCARE' && (
            <>
              <Label>{i18n.application.serviceNeed.urgentLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.urgentLabel}
                checked={urgent}
                onChange={(value) =>
                  setApplication(set('form.preferences.urgent', value))
                }
                data-qa="checkbox-urgent"
              />

              {urgent && featureFlags.urgencyAttachments && (
                <FileUploadGridContainer>
                  <FileUpload
                    uploadHandler={applicationAttachment(
                      application.id,
                      'URGENCY'
                    )}
                    onUploaded={onAttachmentUploaded('URGENCY')}
                    onDeleted={handleAttachmentDeleted}
                    getDownloadUrl={getAttachmentUrl}
                    files={attachments.filter((a) => a.type === 'URGENCY')}
                    data-qa="file-upload-urgent"
                  />
                </FileUploadGridContainer>
              )}

              {serviceNeed !== null && (
                <>
                  <Label>{i18n.application.serviceNeed.partTimeLabel}</Label>
                  <FixedSpaceColumn>
                    <Radio
                      label={i18n.application.serviceNeed.partTime}
                      checked={serviceNeed.partTime === true}
                      onChange={() => {
                        updateServiceNeed(true)
                      }}
                    />
                    {partTimeOptions.length > 0 && serviceNeed.partTime && (
                      <SubRadios>
                        <FixedSpaceColumn spacing="xs">
                          {partTimeOptions.map((opt) => (
                            <Radio
                              key={opt.id}
                              label={opt.nameFi}
                              checked={
                                serviceNeed.serviceNeedOption?.id === opt.id
                              }
                              onChange={() => {
                                setApplication(
                                  set(
                                    'form.preferences.serviceNeed.serviceNeedOption',
                                    opt
                                  )
                                )
                              }}
                            />
                          ))}
                        </FixedSpaceColumn>
                      </SubRadios>
                    )}
                    <Radio
                      label={i18n.application.serviceNeed.fullTime}
                      checked={serviceNeed.partTime === false}
                      onChange={() => {
                        setApplication(
                          flow(
                            set('form.preferences.serviceNeed.partTime', false),
                            set(
                              'form.preferences.serviceNeed.serviceNeedOption',
                              fullTimeOptions[0] ?? null
                            )
                          )
                        )
                      }}
                    />
                    {!serviceNeed.partTime && fullTimeOptions.length > 0 && (
                      <SubRadios>
                        <FixedSpaceColumn spacing="xs">
                          {fullTimeOptions.map((opt) => (
                            <Radio
                              key={opt.id}
                              label={opt.nameFi}
                              checked={
                                serviceNeed.serviceNeedOption?.id === opt.id
                              }
                              onChange={() => {
                                setApplication(
                                  set(
                                    'form.preferences.serviceNeed.serviceNeedOption',
                                    opt
                                  )
                                )
                              }}
                            />
                          ))}
                        </FixedSpaceColumn>
                      </SubRadios>
                    )}
                  </FixedSpaceColumn>
                </>
              )}
            </>
          )}

          {type === 'PRESCHOOL' && (
            <>
              <Label>{i18n.application.serviceNeed.connectedLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.connectedValue}
                checked={serviceNeed !== null}
                onChange={(value) =>
                  value
                    ? setApplication(
                        set('form.preferences.serviceNeed', {
                          startTime: '',
                          endTime: '',
                          shiftCare: false,
                          partTime: false
                        })
                      )
                    : setApplication(
                        flow(
                          set('form.preferences.serviceNeed', null),
                          set(
                            'form.preferences.connectedDaycarePreferredStartDate',
                            null
                          )
                        )
                      )
                }
                data-qa="checkbox-service-need-connected"
              />
            </>
          )}

          {serviceNeed !== null && (
            <>
              {((type === 'DAYCARE' &&
                featureFlags.daycareApplication.dailyTimes) ||
                (type === 'PRESCHOOL' &&
                  !featureFlags.preschoolApplication.serviceNeedOption)) && (
                <>
                  <Label>{i18n.application.serviceNeed.dailyTime}</Label>
                  <div>
                    <HorizontalContainer>
                      <InputField
                        width="m"
                        placeholder={
                          i18n.application.serviceNeed.startTimePlaceholder
                        }
                        value={serviceNeed.startTime}
                        onChange={(value) =>
                          setApplication(
                            set('form.preferences.serviceNeed.startTime', value)
                          )
                        }
                        info={validationErrorInfo(
                          'form.preferences.serviceNeed.startTime'
                        )}
                        data-qa="start-time"
                      />
                      <Gap size="s" horizontal />
                      <InputField
                        width="m"
                        placeholder={
                          i18n.application.serviceNeed.endTimePlaceholder
                        }
                        value={serviceNeed.endTime}
                        onChange={(value) =>
                          setApplication(
                            set('form.preferences.serviceNeed.endTime', value)
                          )
                        }
                        info={validationErrorInfo(
                          'form.preferences.serviceNeed.endTime'
                        )}
                        data-qa="end-time"
                      />
                    </HorizontalContainer>
                    <Gap size="m" />
                  </div>
                </>
              )}

              {type === 'PRESCHOOL' &&
                featureFlags.preschoolApplication
                  .connectedDaycarePreferredStartDate && (
                  <>
                    <Label>
                      {
                        i18n.application.serviceNeed
                          .connectedDaycarePreferredStartDateLabel
                      }
                    </Label>
                    <div>
                      <DatePicker
                        date={connectedDaycarePreferredStartDate}
                        onChange={(value) => {
                          setApplication(
                            set(
                              'form.preferences.connectedDaycarePreferredStartDate',
                              value
                            )
                          )
                        }}
                        locale="fi"
                        data-qa="datepicker-connected-daycare-preferred-start-date"
                      />
                      {errors[
                        'form.preferences.connectedDaycarePreferredStartDate'
                      ] ? (
                        <>
                          <Gap size="s" horizontal />
                          <InputWarning
                            text={
                              errors[
                                'form.preferences.connectedDaycarePreferredStartDate'
                              ]
                            }
                            data-qa="input-warning-connected-daycare-preferred-start-date"
                          />
                        </>
                      ) : null}
                    </div>
                  </>
                )}

              {type === 'PRESCHOOL' &&
                featureFlags.preschoolApplication.serviceNeedOption &&
                serviceNeedPreferredStartDate && (
                  <>
                    <Label>
                      {
                        i18n.application.serviceNeed
                          .connectedDaycareServiceNeedOptionLabel
                      }
                    </Label>
                    <FixedSpaceColumn>
                      {[...serviceNeedOptionsByType].map(([type, options]) => (
                        <React.Fragment key={type}>
                          <Radio
                            data-qa={`preschool-placement-type-${type}`}
                            label={i18n.placement.type[type]}
                            checked={placementType === type}
                            onChange={() => {
                              setPlacementType(type)
                              setApplication(
                                set(
                                  'form.preferences.serviceNeed.serviceNeedOption',
                                  null
                                )
                              )
                            }}
                          />
                          {placementType === type && (
                            <SubRadios>
                              <FixedSpaceColumn spacing="xs">
                                {options.map((option) => (
                                  <Radio
                                    data-qa={`preschool-service-need-option-${option.nameFi}`}
                                    key={option.id}
                                    label={option.nameFi}
                                    checked={
                                      serviceNeed.serviceNeedOption?.id ===
                                      option.id
                                    }
                                    onChange={() =>
                                      setApplication(
                                        set(
                                          'form.preferences.serviceNeed.serviceNeedOption',
                                          option
                                        )
                                      )
                                    }
                                  />
                                ))}
                              </FixedSpaceColumn>
                            </SubRadios>
                          )}
                        </React.Fragment>
                      ))}
                      {errors[
                        'form.preferences.serviceNeed.serviceNeedOption'
                      ] ? (
                        <>
                          <Gap size="s" horizontal />
                          <InputWarning
                            text={
                              errors[
                                'form.preferences.serviceNeed.serviceNeedOption'
                              ]
                            }
                          />
                        </>
                      ) : null}
                    </FixedSpaceColumn>
                  </>
                )}

              <Label>{i18n.application.serviceNeed.shiftCareLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.shiftCareNeeded}
                checked={serviceNeed.shiftCare}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.serviceNeed.shiftCare', value)
                  )
                }
                data-qa="checkbox-service-need-shift-care"
              />

              {serviceNeed.shiftCare && (
                <FileUploadGridContainer>
                  <FileUpload
                    uploadHandler={applicationAttachment(
                      application.id,
                      'EXTENDED_CARE'
                    )}
                    onUploaded={onAttachmentUploaded('EXTENDED_CARE')}
                    onDeleted={handleAttachmentDeleted}
                    getDownloadUrl={getAttachmentUrl}
                    files={attachments.filter(
                      (a) => a.type === 'EXTENDED_CARE'
                    )}
                    data-qa="file-upload-shift-care"
                  />
                </FileUploadGridContainer>
              )}
            </>
          )}

          {type === 'CLUB' && (
            <>
              <Label>{i18n.application.clubDetails.wasOnClubCareLabel}</Label>
              <Checkbox
                label={i18n.application.clubDetails.wasOnClubCareValue}
                checked={!!clubDetails?.wasOnClubCare}
                onChange={(value) =>
                  setApplication(set('form.clubDetails.wasOnClubCare', value))
                }
                data-qa="checkbox-club-details-club-care"
              />
              <Label>{i18n.application.clubDetails.wasOnDaycareLabel}</Label>
              <Checkbox
                label={i18n.application.clubDetails.wasOnDaycareValue}
                checked={!!clubDetails?.wasOnDaycare}
                onChange={(value) =>
                  setApplication(set('form.clubDetails.wasOnDaycare', value))
                }
                data-qa="checkbox-club-details-day-care"
              />
            </>
          )}

          {type === 'PRESCHOOL' && featureFlags.preparatory && (
            <>
              <Label>{i18n.application.serviceNeed.preparatoryLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.preparatoryValue}
                checked={preparatory}
                onChange={(value) =>
                  setApplication(set('form.preferences.preparatory', value))
                }
                data-qa="checkbox-service-need-preparatory"
              />
            </>
          )}

          <Label>{i18n.application.serviceNeed.assistanceLabel}</Label>
          <Checkbox
            label={i18n.application.serviceNeed.assistanceValue}
            checked={child.assistanceNeeded}
            onChange={(value) =>
              setApplication(set('form.child.assistanceNeeded', value))
            }
            data-qa="checkbox-service-need-assistance-needed"
          />

          {child.assistanceNeeded && (
            <>
              <Label>{i18n.application.serviceNeed.assistanceDesc}</Label>
              <TextArea
                value={child.assistanceDescription}
                onChange={(value) => {
                  setApplication(set('form.child.assistanceDescription', value))
                }}
                info={validationErrorInfo('form.child.assistanceDescription')}
                required={true}
              />
            </>
          )}
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection
        title={i18n.application.preferences.title}
        icon={faMapMarkerAlt}
      >
        <ListGrid>
          <Label>{i18n.application.preferences.preferredUnits}</Label>
          <VerticalContainer data-qa="preferred-unit">
            <Combobox
              placeholder={i18n.common.search}
              isLoading={units.isLoading}
              selectedItem={null}
              items={units
                .map((us) =>
                  us
                    .filter(
                      ({ id }) => !preferredUnits.some((unit) => unit.id === id)
                    )
                    .map(({ id, name }) => ({ id, name }))
                )
                .getOrElse([])}
              getItemLabel={({ name }) => name}
              disabled={preferredUnits.length >= 3}
              onChange={(option) => {
                if (option) {
                  setApplication(
                    set('form.preferences.preferredUnits', [
                      ...preferredUnits,
                      { id: option.id, name: option.name }
                    ])
                  )
                }
              }}
              menuEmptyLabel={i18n.common.noResults}
              data-qa="select-preferred-unit"
            />
            <Gap size="s" />
            {preferredUnits.length === 0 ? (
              <InputWarning
                text={i18n.application.preferences.missingPreferredUnits}
              />
            ) : null}
            {preferencesInUnitsList.length !== preferredUnits.length ? (
              <InputWarning text={i18n.application.preferences.unitMismatch} />
            ) : null}
            {preferredUnits.map((unit, i) => (
              <PreferredUnitGridContainer key={unit.id}>
                <Link to={`/units/${unit.id}`}>{`${i + 1}. ${unit.name}`}</Link>
                <Button
                  appearance="inline"
                  icon={faArrowUp}
                  text={i18n.application.preferences.moveUp}
                  onClick={() =>
                    setApplication(
                      set(
                        'form.preferences.preferredUnits',
                        swapElements(preferredUnits, i, i - 1)
                      )
                    )
                  }
                  disabled={i === 0}
                  data-qa="button-move-up-preferred-unit"
                />
                <Button
                  appearance="inline"
                  icon={faArrowDown}
                  text={i18n.application.preferences.moveDown}
                  onClick={() =>
                    setApplication(
                      set(
                        'form.preferences.preferredUnits',
                        swapElements(preferredUnits, i, i + 1)
                      )
                    )
                  }
                  disabled={i === preferredUnits.length - 1}
                  data-qa="button-move-down-preferred-unit"
                />
                <Button
                  appearance="inline"
                  icon={faTimes}
                  text={i18n.common.remove}
                  onClick={() =>
                    setApplication(
                      set(
                        'form.preferences.preferredUnits',
                        preferredUnits.filter((_, index) => i !== index)
                      )
                    )
                  }
                  data-qa="button-select-preferred-unit"
                />
                {!preferencesInUnitsList.some(({ id }) => id === unit.id) ? (
                  <>
                    <FontAwesomeIcon
                      size="sm"
                      icon={faExclamationTriangle}
                      color={colors.status.warning}
                    />
                  </>
                ) : null}
              </PreferredUnitGridContainer>
            ))}
          </VerticalContainer>
        </ListGrid>
        <Gap size="s" />
        <ListGrid>
          <Label>{i18n.application.preferences.siblingBasisLabel}</Label>
          <Checkbox
            label={i18n.application.preferences.siblingBasisValue}
            checked={siblingBasis !== null}
            onChange={(value) =>
              value
                ? setApplication(
                    set('form.preferences.siblingBasis', {
                      siblingName: '',
                      siblingSsn: '',
                      siblingUnit: ''
                    })
                  )
                : setApplication(set('form.preferences.siblingBasis', null))
            }
            data-qa="checkbox-sibling-basis"
          />
          {siblingBasis !== null && (
            <>
              <Label>{i18n.application.preferences.siblingName}</Label>
              <InputField
                width="L"
                value={siblingBasis.siblingName}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.siblingBasis.siblingName', value)
                  )
                }
                data-qa="input-sibling-name"
              />
              <Label>{i18n.application.preferences.siblingSsn}</Label>
              <InputField
                width="L"
                value={siblingBasis.siblingSsn}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.siblingBasis.siblingSsn', value)
                  )
                }
                data-qa="input-sibling-ssn"
              />
              {type === 'PRESCHOOL' && (
                <>
                  <Label>{i18n.application.preferences.siblingUnit}</Label>
                  <InputField
                    width="L"
                    value={siblingBasis.siblingUnit}
                    onChange={(value) =>
                      setApplication(
                        set('form.preferences.siblingBasis.siblingUnit', value)
                      )
                    }
                    data-qa="input-sibling-unit"
                  />
                </>
              )}
            </>
          )}
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection title={i18n.application.child.title} icon={faChild}>
        <ListGrid>
          <Label>{i18n.application.person.name}</Label>
          <Link to={`/child-information/${childId}`}>
            <span data-qa="link-child-name">
              {formatPersonName(child.person)}
            </span>
          </Link>

          <Label>{i18n.application.person.ssn}</Label>
          <span data-qa="child-ssn">{child.person.socialSecurityNumber}</span>

          {!child.person.socialSecurityNumber && (
            <>
              <Label>{i18n.application.person.dob}</Label>
              <span data-qa="child-dob">{child.dateOfBirth?.format()}</span>
            </>
          )}

          {childRestricted ? (
            <>
              <Label>{i18n.application.person.address}</Label>
              <span data-qa="child-restricted">
                {i18n.application.person.restricted}
              </span>
            </>
          ) : (
            <>
              <Label>{i18n.application.person.address}</Label>
              <div>
                <span data-qa="child-address">
                  {child.address && formatAddress(child.address)}
                </span>
                <Gap size="xs" />
                <Checkbox
                  label={i18n.application.person.hasFutureAddress}
                  checked={child.futureAddress !== null}
                  onChange={(value) => {
                    if (value) {
                      setApplication(
                        set('form.child.futureAddress', {
                          street: '',
                          postalCode: '',
                          postOffice: '',
                          movingDate: LocalDate.todayInSystemTz()
                        })
                      )
                    } else {
                      setApplication(set('form.child.futureAddress', null))
                    }
                  }}
                />
              </div>
              {child.futureAddress && (
                <>
                  <Label>{i18n.application.person.futureAddress}</Label>
                  <FutureAddressInputs
                    futureAddress={child.futureAddress}
                    setApplication={setApplication}
                    i18n={i18n}
                    path="child"
                  />
                  <Label>{i18n.application.person.movingDate}</Label>
                  <DatePicker
                    date={child.futureAddress.movingDate}
                    onChange={(value) => {
                      setApplication(
                        set('form.child.futureAddress.movingDate', value)
                      )
                    }}
                    locale="fi"
                  />
                </>
              )}
            </>
          )}

          <Label>{i18n.application.person.nationality}</Label>
          <span data-qa="child-nationality">{child.nationality}</span>
          <Label>{i18n.application.person.language}</Label>
          <span data-qa="child-language">{child.language}</span>
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection
        title={i18n.application.guardians.title}
        icon={faUserFriends}
      >
        <FixedSpaceColumn spacing="L">
          <div>
            <H4>{i18n.application.guardians.appliedGuardian}</H4>
            <ListGrid>
              <Label>{i18n.application.person.name}</Label>
              <Link to={`/profile/${guardianId}`}>
                <span data-qa="guardian-name">
                  {formatPersonName(guardian.person)}
                </span>
              </Link>
              <Label>{i18n.application.person.ssn}</Label>
              <span data-qa="guardian-ssn">
                {guardian.person.socialSecurityNumber}
              </span>

              {guardianRestricted ? (
                <>
                  <Label>{i18n.application.person.address}</Label>
                  <span data-qa="guardian-restricted">
                    {i18n.application.person.restricted}
                  </span>
                </>
              ) : (
                <>
                  <Label>{i18n.application.person.address}</Label>
                  <div>
                    <span data-qa="guardian-address">
                      {guardian.address && formatAddress(guardian.address)}
                    </span>
                    <Gap size="xs" />
                    <Checkbox
                      label="Väestorekisterissä oleva osoite on muuttunut / muuttumassa"
                      checked={guardian.futureAddress !== null}
                      onChange={(value) => {
                        if (value) {
                          setApplication(
                            set('form.guardian.futureAddress', {
                              street: '',
                              postalCode: '',
                              postOffice: '',
                              movingDate: LocalDate.todayInSystemTz()
                            })
                          )
                        } else {
                          setApplication(
                            set('form.guardian.futureAddress', null)
                          )
                        }
                      }}
                    />
                  </div>
                  {guardian.futureAddress && (
                    <>
                      <Label>{i18n.application.person.futureAddress}</Label>
                      <FutureAddressInputs
                        futureAddress={guardian.futureAddress}
                        setApplication={setApplication}
                        i18n={i18n}
                        path="guardian"
                      />
                      <Label>{i18n.application.person.movingDate}</Label>
                      <DatePicker
                        date={guardian.futureAddress.movingDate}
                        onChange={(value) => {
                          setApplication(
                            set('form.guardian.futureAddress.movingDate', value)
                          )
                        }}
                        locale="fi"
                      />
                    </>
                  )}
                </>
              )}

              <Label>{i18n.application.person.phone}</Label>
              <InputField
                width="m"
                value={guardian.phoneNumber}
                onChange={(value) => {
                  setApplication(set('form.guardian.phoneNumber', value))
                }}
                data-qa="application-person-phone"
              />
              <Label>{i18n.application.person.email}</Label>
              <InputField
                width="m"
                value={guardian.email ?? ''}
                onChange={(value) => {
                  setApplication(set('form.guardian.email', value))
                }}
                data-qa="application-person-email"
              />
            </ListGrid>
          </div>

          {type !== 'CLUB' && (
            <div>
              <H4>{i18n.application.guardians.secondGuardian.title}</H4>
              <Checkbox
                checked={!!secondGuardian}
                label={i18n.application.guardians.secondGuardian.checkboxLabel}
                onChange={(value) =>
                  setApplication(
                    set(
                      'form.secondGuardian',
                      value
                        ? {
                            phoneNumber: '',
                            email: '',
                            agreementStatus: null
                          }
                        : undefined
                    )
                  )
                }
                data-qa="application-second-guardian-toggle"
              />

              {secondGuardian && (
                <>
                  <Gap size="s" />
                  <ListGrid>
                    <Label>{i18n.application.person.phone}</Label>
                    <InputField
                      width="m"
                      value={secondGuardian?.phoneNumber ?? ''}
                      onChange={(value) => {
                        setApplication(
                          set('form.secondGuardian.phoneNumber', value)
                        )
                      }}
                      data-qa="application-second-guardian-phone"
                    />
                    <Label>{i18n.application.person.email}</Label>
                    <InputField
                      width="m"
                      value={secondGuardian?.email ?? ''}
                      onChange={(value) => {
                        setApplication(set('form.secondGuardian.email', value))
                      }}
                      data-qa="application-second-guardian-email"
                    />
                    <Label data-qa="agreement-status-label">
                      {i18n.application.person.agreementStatus}
                    </Label>
                    <div>
                      {selectableOtherGuardianAgreementStatuses.map(
                        (id, index) => (
                          <React.Fragment key={id ?? 'NOT_SET'}>
                            {index !== 0 ? <Gap size="xxs" /> : null}
                            <Radio
                              label={
                                i18n.application.person
                                  .otherGuardianAgreementStatuses[
                                  id ?? 'NOT_SET'
                                ]
                              }
                              checked={
                                id === (secondGuardian?.agreementStatus ?? null)
                              }
                              onChange={() => {
                                setApplication(
                                  set('form.secondGuardian.agreementStatus', id)
                                )
                              }}
                              data-qa={`radio-other-guardian-agreement-status-${
                                id ?? 'null'
                              }`}
                            />
                          </React.Fragment>
                        )
                      )}
                    </div>
                  </ListGrid>
                </>
              )}
            </div>
          )}

          <VTJGuardian guardianId={otherGuardian?.id} />
        </FixedSpaceColumn>
      </CollapsibleSection>

      {paid && (
        <>
          <CollapsibleSection
            title={i18n.application.otherPeople.title}
            icon={faUsers}
          >
            <FixedSpaceColumn spacing="L">
              <div>
                <H4>{i18n.application.otherPeople.adult}</H4>
                <Checkbox
                  label={i18n.application.otherPeople.spouse}
                  checked={otherPartner !== null}
                  onChange={(value) => {
                    if (value) {
                      setApplication(
                        set('form.otherPartner', {
                          firstName: '',
                          lastName: '',
                          socialSecurityNumber: ''
                        })
                      )
                    } else {
                      setApplication(set('form.otherPartner', null))
                    }
                  }}
                />
                {otherPartner && (
                  <>
                    <Gap size="s" />
                    <HorizontalContainer>
                      <WithLabel label={i18n.common.form.firstName}>
                        <InputField
                          width="m"
                          value={otherPartner.firstName}
                          onChange={(value) => {
                            setApplication(
                              set('form.otherPartner.firstName', value)
                            )
                          }}
                        />
                      </WithLabel>
                      <Gap size="s" horizontal />
                      <WithLabel label={i18n.common.form.lastName}>
                        <InputField
                          width="m"
                          value={otherPartner.lastName}
                          onChange={(value) => {
                            setApplication(
                              set('form.otherPartner.lastName', value)
                            )
                          }}
                        />
                      </WithLabel>
                      <Gap size="s" horizontal />
                      <WithLabel label={i18n.application.person.ssn}>
                        <InputField
                          width="m"
                          value={otherPartner.socialSecurityNumber ?? ''}
                          onChange={(value) => {
                            setApplication(
                              set(
                                'form.otherPartner.socialSecurityNumber',
                                value
                              )
                            )
                          }}
                          info={validationErrorInfo(
                            'form.otherPartner.socialSecurityNumber'
                          )}
                        />
                      </WithLabel>
                    </HorizontalContainer>
                  </>
                )}
              </div>

              <div>
                <H4>{i18n.application.otherPeople.children}</H4>
                <AddButton
                  text={i18n.application.otherPeople.addChild}
                  onClick={() => {
                    setApplication(
                      set('form.otherChildren', [
                        ...otherChildren,
                        {
                          firstName: '',
                          lastName: '',
                          socialSecurityNumber: ''
                        }
                      ])
                    )
                  }}
                />
                {otherChildren.map(
                  ({ firstName, lastName, socialSecurityNumber }, index) => (
                    <React.Fragment key={index}>
                      <Gap size="s" />
                      <HorizontalContainer>
                        <WithLabel label={i18n.common.form.firstName}>
                          <InputField
                            width="m"
                            value={firstName}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  ['form', 'otherChildren', index, 'firstName'],
                                  value
                                )
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.common.form.lastName}>
                          <InputField
                            width="m"
                            value={lastName}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  ['form', 'otherChildren', index, 'lastName'],
                                  value
                                )
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.application.person.ssn}>
                          <InputField
                            width="m"
                            value={socialSecurityNumber ?? ''}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  [
                                    'form',
                                    'otherChildren',
                                    index,
                                    'socialSecurityNumber'
                                  ],
                                  value
                                )
                              )
                            }}
                            info={validationErrorInfo(
                              `form.otherChildren.${index}.socialSecurityNumber`
                            )}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <Button
                          appearance="inline"
                          icon={faTimes}
                          text={i18n.common.remove}
                          onClick={() =>
                            setApplication(
                              set(
                                'form.otherChildren',
                                otherChildren.filter((_, i) => i !== index)
                              )
                            )
                          }
                        />
                      </HorizontalContainer>
                    </React.Fragment>
                  )
                )}
              </div>
            </FixedSpaceColumn>
          </CollapsibleSection>
        </>
      )}

      <CollapsibleSection
        title={i18n.application.additionalInfo.title}
        icon={faInfo}
      >
        <ListGrid>
          <Label>{i18n.application.additionalInfo.applicationInfo}</Label>
          <TextArea
            value={otherInfo}
            onChange={(value) => {
              setApplication(set('form.otherInfo', value))
            }}
          />

          {type !== 'CLUB' && (
            <>
              <Label>{i18n.application.additionalInfo.allergies}</Label>
              <TextArea
                value={child.allergies}
                onChange={(value) => {
                  setApplication(set('form.child.allergies', value))
                }}
              />

              <Label>{i18n.application.additionalInfo.diet}</Label>
              <TextArea
                value={application.form.child.diet}
                onChange={(value) => {
                  setApplication(set('form.child.diet', value))
                }}
              />
            </>
          )}
          <Label>
            {i18n.application.additionalInfo.serviceWorkerAttachmentsTitle}
          </Label>

          <FileUploadGridContainer>
            <FileUpload
              uploadHandler={applicationAttachment(
                application.id,
                'SERVICE_WORKER_ATTACHMENT'
              )}
              onUploaded={onAttachmentUploaded('SERVICE_WORKER_ATTACHMENT')}
              onDeleted={handleAttachmentDeleted}
              getDownloadUrl={getAttachmentUrl}
              files={attachments.filter(
                (a) => a.type === 'SERVICE_WORKER_ATTACHMENT'
              )}
              data-qa="file-upload-service-worker"
            />
          </FileUploadGridContainer>
        </ListGrid>
      </CollapsibleSection>

      <ApplicationStatusSection
        application={application}
        dueDateEditor={
          <DatePicker
            date={application.dueDate}
            onChange={(value) => setApplication(set('dueDate', value))}
            locale="fi"
            data-qa="due-date"
          />
        }
      />
    </div>
  )
})

function WithLabel({
  label,
  children
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <Label>{label}</Label>
      {children}
    </div>
  )
}

function FutureAddressInputs({
  futureAddress,
  setApplication,
  i18n,
  path
}: {
  futureAddress: FutureAddress
  setApplication: React.Dispatch<
    React.SetStateAction<ApplicationDetails | undefined>
  >
  i18n: Translations
  path: 'child' | 'guardian'
}) {
  return (
    <div>
      <InputField
        width="L"
        placeholder={i18n.common.form.streetAddress}
        value={futureAddress.street}
        onChange={(value) => {
          setApplication(set(['form', path, 'futureAddress', 'street'], value))
        }}
      />
      <Gap size="xs" />
      <HorizontalContainer>
        <InputField
          width="s"
          placeholder={i18n.common.form.postalCode}
          value={futureAddress.postalCode}
          onChange={(value) => {
            setApplication(
              set(['form', path, 'futureAddress', 'postalCode'], value)
            )
          }}
        />
        <Gap size="xs" horizontal />
        <InputField
          width="m"
          placeholder={i18n.common.form.postOffice}
          value={futureAddress.postOffice}
          onChange={(value) => {
            setApplication(
              set(['form', path, 'futureAddress', 'postOffice'], value)
            )
          }}
        />
      </HorizontalContainer>
    </div>
  )
}

const HorizontalContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

const VerticalContainer = styled.div`
  display: flex;
  flex-direction: column;
`
