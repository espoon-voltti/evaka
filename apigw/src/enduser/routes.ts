// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { createProxy } from '../shared/proxy-utils'

const router = Router()
const proxy = createProxy()

router.all('/citizen/*', createProxy())

router.get('/decisions2/:decisionId/download', proxy)

router.get('/attachments/:applicationId/download', proxy)
router.get('/attachments/:attachmentId/download', proxy)
router.get('/attachments/:attachmentId/pre-download', proxy)

router.delete('/attachments/citizen/:id', proxy)

const multipartProxy = createProxy({ multipart: true })
router.post('/attachments/citizen/applications/:applicationId', multipartProxy)

export default router
