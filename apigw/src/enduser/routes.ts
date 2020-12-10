// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { createProxy } from '../shared/proxy-utils'

const router = Router()
const proxy = createProxy()

router.get('/decisions', createProxy({ path: '/enduser/decisions' }))
router.get('/decisions2/:decisionId/download', proxy)
router.get('/attachments/:attachmentId/download', proxy)
router.get('/attachments/:attachmentId/pre-download', proxy)

router.post('/enduser/v2/applications', proxy)
router.get('/enduser/v2/applications', proxy)

router.get('/enduser/v2/applications/:applicationId', proxy)
router.put('/enduser/v2/applications/:applicationId', proxy)
router.delete('/enduser/v2/applications/:applicationId', proxy)

router.post(
  '/enduser/v2/applications/:applicationId/actions/send-application',
  proxy
)
router.post(
  '/enduser/v2/applications/:applicationId/actions/accept-decision',
  proxy
)
router.post(
  '/enduser/v2/applications/:applicationId/actions/reject-decision',
  proxy
)
router.delete('/attachments/enduser/:id', proxy)

router.get('/attachments/:applicationId/download', proxy)

const multipartProxy = createProxy({ multipart: true })
router.post('/attachments/enduser/applications/:applicationId', multipartProxy)

export default router
