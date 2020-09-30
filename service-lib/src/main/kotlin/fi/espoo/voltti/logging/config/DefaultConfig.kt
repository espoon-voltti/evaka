// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.config

import fi.espoo.voltti.logging.filter.BasicMdcFilter
import fi.espoo.voltti.logging.filter.SpringSecurityMdcFilter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

@Configuration
@Import(BasicMdcFilter::class, SpringSecurityMdcFilter::class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class DefaultConfig
