// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useLayoutEffect, useState } from 'react'
import { CSVLink } from 'react-csv'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFileSpreadsheet } from 'icon-set'
import { LabelKeyObject } from 'react-csv/components/CommonPropTypes'
import { useTranslation } from '~state/i18n'
import { EspooColours } from '~utils/colours'

const RowRightAligned = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
`

const DisabledLink = styled.span`
  color: ${EspooColours.grey};
`

const LinkText = styled.span`
  margin-left: 8px;
  font-weight: 600;
`

interface Props {
  // eslint-disable-next-line @typescript-eslint/ban-types
  data: object[] | string[][]
  headers?: LabelKeyObject[]
  filename: string
}

function ReportDownload({ data, headers, filename }: Props) {
  const { i18n } = useTranslation()
  const [reloadCSV, setReloadCSV] = useState(true)

  /*
    The CSVLink components needs to be unmounted every time its props change
    as it doesn't recreate the file after the component has been mounted.
  */
  useLayoutEffect(() => {
    if (!reloadCSV) setReloadCSV(true)
  }, [data, headers, filename])

  useEffect(() => {
    const handler = reloadCSV
      ? setTimeout(() => setReloadCSV(false), 200)
      : undefined
    return () => clearTimeout(handler)
  }, [reloadCSV])

  return (
    <RowRightAligned>
      {data.length > 0 && !reloadCSV ? (
        <CSVLink
          data={data}
          headers={headers}
          separator=";"
          filename={filename}
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
    </RowRightAligned>
  )
}

export default ReportDownload
