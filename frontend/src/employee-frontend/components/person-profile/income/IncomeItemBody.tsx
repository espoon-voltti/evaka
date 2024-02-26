// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Attachment } from 'lib-common/api-types/attachment'
import {
  Income,
  IncomeTypeOptions
} from 'lib-common/generated/api-types/invoicing'
import Title from 'lib-components/atoms/Title'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fileIcon } from 'lib-components/molecules/FileUpload'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { getAttachmentUrl } from '../../../api/attachments'
import { useTranslation } from '../../../state/i18n'
import { UserContext } from '../../../state/user'

import IncomeTable, { tableDataFromIncomeFields } from './IncomeTable'

interface Props {
  incomeTypeOptions: IncomeTypeOptions
  income: Income
}

const IncomeItemBody = React.memo(function IncomeItemBody({
  incomeTypeOptions,
  income
}: Props) {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)

  const applicationLinkVisible = roles.find((r) => ['ADMIN'].includes(r))

  const tableData = useMemo(
    () => tableDataFromIncomeFields(income.data),
    [income.data]
  )

  return (
    <>
      <ListGrid labelWidth="fit-content(40%)" rowGap="xs" columnGap="L">
        <Label>{i18n.personProfile.income.details.dateRange}</Label>
        <span>
          {`${income.validFrom.format()} - ${
            income.validTo ? income.validTo.format() : ''
          }`}
        </span>
        <Label>{i18n.personProfile.income.details.effect}</Label>
        <span>
          {i18n.personProfile.income.details.effectOptions[income.effect]}
        </span>
        <Label>{i18n.personProfile.income.details.echa}</Label>
        <span>{income.worksAtECHA ? i18n.common.yes : i18n.common.no}</span>
        <Label>{i18n.personProfile.income.details.entrepreneur}</Label>
        <span>{income.isEntrepreneur ? i18n.common.yes : i18n.common.no}</span>
        <Label>{i18n.personProfile.income.details.notes}</Label>
        <span>{income.notes}</span>
        <Label>{i18n.personProfile.income.details.updated}</Label>
        <span>{income.updatedAt?.toLocalDate().format()}</span>
        <Label>{i18n.personProfile.income.details.handler}</Label>
        <span>
          {income.applicationId
            ? i18n.personProfile.income.details.originApplication
            : income.updatedBy}
        </span>
        {income.applicationId !== null && (
          <>
            <Label>{i18n.personProfile.income.details.source}</Label>
            <span>
              {applicationLinkVisible ? (
                <Link to={`/applications/${income.applicationId}`}>
                  {i18n.personProfile.income.details.application}
                </Link>
              ) : (
                i18n.personProfile.income.details.createdFromApplication
              )}
            </span>
          </>
        )}
      </ListGrid>
      {income.effect === 'INCOME' ? (
        <>
          <div className="separator" />
          <Title size={4}>
            {i18n.personProfile.income.details.incomeTitle}
          </Title>
          <IncomeTable
            incomeTypeOptions={incomeTypeOptions}
            data={tableData}
            type="income"
            total={income.totalIncome}
          />
          <Title size={4}>
            {i18n.personProfile.income.details.expensesTitle}
          </Title>
          <IncomeTable
            incomeTypeOptions={incomeTypeOptions}
            data={tableData}
            type="expenses"
            total={income.totalExpenses}
          />
        </>
      ) : null}
      <IncomeAttachments attachments={income.attachments} />
    </>
  )
})

function IncomeAttachments({ attachments }: { attachments: Attachment[] }) {
  const { i18n } = useTranslation()
  return (
    <>
      <Title size={4}>{i18n.personProfile.income.details.attachments}</Title>
      {attachments.length === 0 ? (
        <p data-qa="no-attachments">
          {i18n.incomeStatement.citizenAttachments.noAttachments}
        </p>
      ) : (
        <FixedSpaceColumn>
          {attachments.map((file) => (
            <div key={file.id} data-qa="attachment">
              <FileIcon icon={fileIcon(file)} />
              <FileDownloadButton file={file} getFileUrl={getAttachmentUrl} />
            </div>
          ))}
        </FixedSpaceColumn>
      )}
    </>
  )
}

const FileIcon = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
  margin-right: ${defaultMargins.s};
`

export default IncomeItemBody
