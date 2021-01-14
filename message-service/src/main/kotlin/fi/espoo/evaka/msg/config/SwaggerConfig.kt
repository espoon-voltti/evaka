// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfig {

    fun apiInfo(): ApiInfo =
        ApiInfoBuilder()
            .title("Messages service")
            .description("")
            .termsOfServiceUrl("")
            .version("1.0.0")
            .contact(Contact("", "", "temporary-evaka@espoo.fi"))
            .build()

    @Bean
    fun customImplementation(): Docket =
        Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.springframework.boot").negate())
            .build()
            .apiInfo(apiInfo())
}
