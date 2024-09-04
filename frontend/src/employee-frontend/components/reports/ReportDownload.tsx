// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useLayoutEffect, useState } from 'react'
import { CSVLink } from 'react-csv'
import styled from 'styled-components'

import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faFileSpreadsheet } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

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
  headers?: { label: string; key: keyof T }[]
  filename: string | (() => string)
  'data-qa'?: string
}

function ReportDownload<T extends object>({
  data,
  headers,
  filename,
  'data-qa': dataQa
}: Props<T>) {
  const { i18n } = useTranslation()
  const filenameStr = typeof filename === 'function' ? filename() : filename
  const [reloadCSV, setReloadCSV] = useState(true)

  /*
    The CSVLink components needs to be unmounted every time its props change
    as it doesn't recreate the file after the component has been mounted.
  */
  useLayoutEffect(() => {
    if (!reloadCSV) setReloadCSV(true)
  }, [data, headers, filenameStr]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handler = reloadCSV
      ? setTimeout(() => setReloadCSV(false), 200)
      : undefined
    return () => {
      if (handler) clearTimeout(handler)
    }
  }, [reloadCSV])

  return (
    <RowRightAligned>
      <div data-qa={dataQa}>
        {data.length > 0 && !reloadCSV ? (
          <CSVLink
            data={data}
            headers={headers?.map(({ label, key }) => ({
              label,
              key: String(key)
            }))}
            separator=";"
            filename={filenameStr}
          >
            <FontAwesomeIcon icon={faFileSpreadsheet} size="lg" />
            <LinkText>{i18n.reports.downloadButton}</LinkText>
          </CSVLink>
        ) : (
          <DisabledLink>
            <FontAwesomeIcon icon={faFileSpreadsheet} size="lg" />
            <LinkText>{i18n.reports.downloadButton}</LinkText>
          </DisabledLink>
        )}
      </div>
    </RowRightAligned>
  )
}

export default ReportDownload
