// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { Result } from 'lib-common/api'
import {
  ApplicationAddress,
  ApplicationDetails,
  ApplicationFutureAddress,
  ApplicationPersonBasics
} from 'lib-common/api-types/application/ApplicationDetails'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField, { InputInfo } from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import Radio from 'lib-components/atoms/form/Radio'
import colors from 'lib-customizations/common'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ListGrid from 'lib-components/layout/ListGrid'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import FileUpload from 'lib-components/molecules/FileUpload'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faChild,
  faExclamationTriangle,
  faFileAlt,
  faInfo,
  faMapMarkerAlt,
  faTimes,
  faUserFriends,
  faUsers
} from 'lib-icons'
import { flow, set } from 'lodash/fp'
import React, { useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import Combobox from 'lib-components/atoms/form/Combobox'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveApplicationAttachment
} from '../../api/attachments'
import ApplicationStatusSection from '../../components/application-page/ApplicationStatusSection'
import ApplicationTitle from '../../components/application-page/ApplicationTitle'
import VTJGuardian from '../../components/application-page/VTJGuardian'
import { Translations, useTranslation } from '../../state/i18n'
import { PersonDetails } from '../../types/person'
import { formatName } from '../../utils'
import { InputWarning } from '../common/InputWarning'
import { ServiceNeedOptionPublicInfo } from 'lib-common/api-types/serviceNeed/common'
import { featureFlags } from 'lib-customizations/citizen'
import { AttachmentType } from 'lib-common/generated/enums'

interface PreschoolApplicationProps {
  application: ApplicationDetails
  setApplication: React.Dispatch<
    React.SetStateAction<ApplicationDetails | undefined>
  >
  errors: Record<string, string>
  units: Result<PublicUnit[]>
  guardians: PersonDetails[]
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

export default React.memo(function ApplicationEditView({
  application,
  setApplication,
  errors,
  units,
  guardians,
  serviceNeedOptions
}: PreschoolApplicationProps) {
  const { i18n } = useTranslation()

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
        urgent,
        serviceNeed,
        siblingBasis,
        preparatory
      },
      otherInfo,
      maxFeeAccepted,
      clubDetails
    },
    childId,
    guardianId,
    childRestricted,
    guardianRestricted,
    attachments
  } = application

  const fullTimeOptions = useMemo(
    () =>
      serviceNeedOptions?.filter(
        (opt) => opt.validPlacementType === 'DAYCARE'
      ) ?? [],
    [serviceNeedOptions]
  )
  const partTimeOptions = useMemo(
    () =>
      serviceNeedOptions?.filter(
        (opt) => opt.validPlacementType === 'DAYCARE_PART_TIME'
      ) ?? [],
    [serviceNeedOptions]
  )

  useEffect(() => {
    if (
      featureFlags.daycareApplication.serviceNeedOptionsEnabled &&
      application.form.preferences.serviceNeed?.serviceNeedOption === null
    ) {
      setApplication(
        set(
          'form.preferences.serviceNeed.serviceNeedOption',
          application.form.preferences.serviceNeed.partTime
            ? partTimeOptions[0] ?? null
            : fullTimeOptions[0] ?? null
        )
      )
    }
  }, [
    partTimeOptions,
    fullTimeOptions,
    application.form.preferences.serviceNeed?.serviceNeedOption,
    application.form.preferences.serviceNeed?.partTime,
    setApplication
  ])

  const preferencesInUnitsList = units
    .map((us) =>
      preferredUnits.filter(({ id }) => us.find((unit) => unit.id === id))
    )
    .getOrElse(preferredUnits)

  const connectedDaycare = type === 'PRESCHOOL' && serviceNeed !== null
  const paid = type === 'DAYCARE' || connectedDaycare

  const otherGuardian = guardians.find((guardian) => guardian.id !== guardianId)

  const formatPersonName = (person: ApplicationPersonBasics) =>
    formatName(person.firstName, person.lastName, i18n, true)
  const formatAddress = (a: ApplicationAddress) =>
    `${a.street}, ${a.postalCode} ${a.postOffice}`

  const onUploadAttachment =
    (type: AttachmentType) =>
    (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ): Promise<Result<UUID>> =>
      saveApplicationAttachment(
        application.id,
        file,
        type,
        onUploadProgress
      ).then((res) => {
        res.isSuccess &&
          setApplication(
            (prev) =>
              prev && {
                ...prev,
                attachments: [
                  ...prev.attachments,
                  {
                    contentType: file.type,
                    id: res.value,
                    name: file.name,
                    type,
                    updated: new Date(),
                    receivedAt: new Date()
                  }
                ]
              }
          )
        return res
      })

  const onDeleteAttachment = (id: UUID) =>
    deleteAttachment(id).then((res) => {
      res.isSuccess &&
        setApplication(
          (prev) =>
            prev && {
              ...prev,
              attachments: prev.attachments.filter((a) => a.id !== id)
            }
        )
      return res
    })

  const validationErrorInfo = (field: string): InputInfo | undefined => {
    return errors[field]
      ? {
          text: errors[field],
          status: 'warning'
        }
      : undefined
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
          <div data-qa="datepicker-start-date">
            <HorizontalContainer>
              <DatePickerDeprecated
                type="short"
                date={preferredStartDate ?? undefined}
                onChange={(value) => {
                  setApplication(
                    set('form.preferences.preferredStartDate', value)
                  )
                }}
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

              {urgent && featureFlags.urgencyAttachmentsEnabled && (
                <FileUploadGridContainer>
                  <FileUpload
                    onUpload={onUploadAttachment('URGENCY')}
                    onDelete={onDeleteAttachment}
                    onDownloadFile={getAttachmentBlob}
                    i18n={i18n.fileUpload}
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
                        setApplication(
                          flow(
                            set('form.preferences.serviceNeed.partTime', true),
                            set(
                              'form.preferences.serviceNeed.serviceNeedOption',
                              partTimeOptions[0] ?? null
                            )
                          )
                        )
                      }}
                    />
                    {featureFlags.daycareApplication
                      .serviceNeedOptionsEnabled &&
                      serviceNeed.partTime && (
                        <SubRadios>
                          <FixedSpaceColumn spacing={'xs'}>
                            {partTimeOptions.map((opt) => (
                              <Radio
                                key={opt.id}
                                label={opt.name}
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
                    {featureFlags.daycareApplication
                      .serviceNeedOptionsEnabled &&
                      !serviceNeed.partTime && (
                        <SubRadios>
                          <FixedSpaceColumn spacing={'xs'}>
                            {fullTimeOptions.map((opt) => (
                              <Radio
                                key={opt.id}
                                label={opt.name}
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
                    : setApplication(set('form.preferences.serviceNeed', null))
                }
                data-qa="checkbox-service-need-connected"
              />
            </>
          )}

          {serviceNeed !== null && (
            <>
              {((type === 'DAYCARE' &&
                featureFlags.daycareApplication.dailyTimesEnabled) ||
                type === 'PRESCHOOL') && (
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
                    onUpload={onUploadAttachment('EXTENDED_CARE')}
                    onDelete={onDeleteAttachment}
                    onDownloadFile={getAttachmentBlob}
                    i18n={i18n.fileUpload}
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

          {type === 'PRESCHOOL' && (
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
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                  setApplication(
                    set('form.child.assistanceDescription', e.target.value)
                  )
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
              <HorizontalContainer key={unit.id}>
                <Link to={`/units/${unit.id}`}>{`${i + 1}. ${unit.name}`}</Link>
                <Gap size="s" horizontal />
                <InlineButton
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
                    <Gap size="s" horizontal />
                    <FontAwesomeIcon
                      size="sm"
                      icon={faExclamationTriangle}
                      color={colors.accents.orange}
                    />
                  </>
                ) : null}
              </HorizontalContainer>
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
                      siblingSsn: ''
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
                          movingDate: LocalDate.today()
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
                    path={'child'}
                  />
                  <Label>{i18n.application.person.movingDate}</Label>
                  <DatePickerDeprecated
                    type="short"
                    date={child.futureAddress.movingDate ?? undefined}
                    onChange={(value) => {
                      setApplication(
                        set('form.child.futureAddress.movingDate', value)
                      )
                    }}
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
                              movingDate: LocalDate.today()
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
                        path={'guardian'}
                      />
                      <Label>{i18n.application.person.movingDate}</Label>
                      <DatePickerDeprecated
                        type="short"
                        date={guardian.futureAddress.movingDate ?? undefined}
                        onChange={(value) => {
                          setApplication(
                            set('form.guardian.futureAddress.movingDate', value)
                          )
                        }}
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
                value={guardian.email}
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
                      {(
                        [
                          'AGREED',
                          'NOT_AGREED',
                          'RIGHT_TO_GET_NOTIFIED',
                          null
                        ] as const
                      ).map((id, index) => (
                        <React.Fragment key={id ?? 'NOT_SET'}>
                          {index !== 0 ? <Gap size="xxs" /> : null}
                          <Radio
                            label={
                              i18n.application.person
                                .otherGuardianAgreementStatuses[id ?? 'NOT_SET']
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
                      ))}
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
                        <InlineButton
                          icon={faTimes}
                          text={i18n.common.remove}
                          onClick={() =>
                            setApplication(
                              set(
                                'form.otherChildren',
                                otherChildren.filter((c, i) => i !== index)
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
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
              setApplication(set('form.otherInfo', e.target.value))
            }}
          />

          {type !== 'CLUB' && (
            <>
              <Label>{i18n.application.additionalInfo.allergies}</Label>
              <TextArea
                value={child.allergies}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                  setApplication(set('form.child.allergies', e.target.value))
                }}
              />

              <Label>{i18n.application.additionalInfo.diet}</Label>
              <TextArea
                value={application.form.child.diet}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                  setApplication(set('form.child.diet', e.target.value))
                }}
              />
            </>
          )}
        </ListGrid>

        {type !== 'CLUB' && (
          <>
            <Gap />
            <Checkbox
              label={i18n.application.additionalInfo.maxFeeAccepted}
              checked={maxFeeAccepted}
              onChange={(value) =>
                setApplication(set('form.maxFeeAccepted', value))
              }
              data-qa="checkbox-maxFeeAccepted"
              disabled={application.origin !== 'PAPER'}
            />
          </>
        )}
      </CollapsibleSection>

      <ApplicationStatusSection
        application={application}
        dueDateEditor={
          <DatePickerDeprecated
            date={application.dueDate || undefined}
            onChange={(value) => setApplication(set('dueDate', value))}
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
  futureAddress: ApplicationFutureAddress
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
