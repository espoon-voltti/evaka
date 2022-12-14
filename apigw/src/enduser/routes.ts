// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { createProxy } from '../shared/proxy-utils'

const router = Router()
const proxy = createProxy()

const multipartProxy = createProxy({ multipart: true })

router.post('/citizen/attachments/applications/:applicationId', multipartProxy)
router.post(
  '/citizen/attachments/income-statements/:incomeStatementId?',
  multipartProxy
)

router.all('/citizen/*', createProxy())

// deprecated
router.get('/decisions2/:decisionId/download', proxy)

// deprecated
router.get('/attachments/:applicationId/download/:filename', proxy)
router.get('/attachments/:attachmentId/download/:filename', proxy)

// deprecated
router.delete('/attachments/citizen/:id', proxy)

// deprecated
router.post('/attachments/citizen/applications/:applicationId', multipartProxy)
router.post(
  '/attachments/citizen/income-statements/:incomeStatementId?',
  multipartProxy
)

export default router
