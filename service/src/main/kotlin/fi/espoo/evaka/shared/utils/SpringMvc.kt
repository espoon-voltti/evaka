// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import org.springframework.core.MethodParameter
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

inline fun <reified T> asArgumentResolver(
    crossinline f: (parameter: MethodParameter, webRequest: NativeWebRequest) -> T
): HandlerMethodArgumentResolver =
    object : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean =
            T::class.java.isAssignableFrom(parameter.parameterType)

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?,
        ) = f(parameter, webRequest)
    }

inline fun <reified I, reified O> convertFrom(crossinline f: (source: I) -> O): GenericConverter =
    object : GenericConverter {
        override fun getConvertibleTypes(): Set<GenericConverter.ConvertiblePair> =
            setOf(GenericConverter.ConvertiblePair(I::class.java, O::class.java))

        override fun convert(
            source: Any?,
            sourceType: TypeDescriptor,
            targetType: TypeDescriptor,
        ): Any? = (source as? I)?.let(f)
    }
