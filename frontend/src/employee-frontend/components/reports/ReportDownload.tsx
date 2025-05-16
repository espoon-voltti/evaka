// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { Button } from 'lib-components/atoms/buttons/Button'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faFileSpreadsheet } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import type { Column } from './csv'
import { toCsv } from './csv'

const RowRightAligned = styled.div`
  display: flex;
  justify-content: flex-end;
`

const DisabledLink = styled.span`
  color: ${colors.grayscale.g35};
`

const LinkText = styled.span`
  margin-left: 8px;
  font-weight: ${fontWeights.semibold};
`

interface Props<T> {
  data: T[]
  columns: Column<T>[]
  filename: string | (() => string)
  'data-qa'?: string
}

function ReportDownload<T>({
  data,
  columns,
  filename,
  'data-qa': dataQa
}: Props<T>) {
  const { i18n } = useTranslation()

  const downloadCsv = useCallback(() => {
    const link = document.createElement('a')
    link.download = typeof filename === 'function' ? filename() : filename
    link.href = `data:text/csv;charset=utf-8,${encodeURIComponent(toCsv(data, columns))}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }, [data, filename, columns])

  return (
    <RowRightAligned>
      <Button
        data-qa={dataQa}
        icon={faFileSpreadsheet}
        text={i18n.reports.downloadButton}
        appearance="inline"
        onClick={downloadCsv}
        disabled={data.length === 0}
      />
    </RowRightAligned>
  )
}

export default ReportDownload

export const BackendReportDownload = ({
  href,
  text,
  enabled
}: {
  href: string
  text: string
  enabled: boolean
}) => {
  return (
    <RowRightAligned>
      {enabled ? (
        <a href={href}>
          <FontAwesomeIcon icon={faFileSpreadsheet} size="lg" />
          <LinkText>{text}</LinkText>
        </a>
      ) : (
        <DisabledLink>
          <FontAwesomeIcon icon={faFileSpreadsheet} size="lg" />
          <LinkText>{text}</LinkText>
        </DisabledLink>
      )}
    </RowRightAligned>
  )
}
