// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.typeOf
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.qualifier.Qualifier

inline fun <reified T> createQualifiedType(
    vararg annotations: KClass<out Annotation>
): QualifiedType<T> = createQualifiedType(typeOf<T>(), *annotations)

fun <T> createQualifiedType(
    type: KType,
    vararg annotations: KClass<out Annotation>,
): QualifiedType<T> =
    @Suppress("UNCHECKED_CAST")
    (QualifiedType.of(type.asJdbiJavaType()) as QualifiedType<T>).withAnnotationClasses(
        annotations.map { it.java }
    )

fun KType.asJdbiJavaType(): Type {
    return when (val classifier = this.classifier) {
        // Arrays need special handling, because Array<Byte>, ByteArray, and Array<Byte?> all behave
        // a bit
        // differently due to underlying JVM implementation details
        ByteArray::class ->
            if (this.arguments.isNotEmpty()) Array<Byte>::class.java else ByteArray::class.java
        CharArray::class ->
            if (this.arguments.isNotEmpty()) Array<Char>::class.java else CharArray::class.java
        ShortArray::class ->
            if (this.arguments.isNotEmpty()) Array<Short>::class.java else ShortArray::class.java
        IntArray::class ->
            if (this.arguments.isNotEmpty()) Array<Int>::class.java else IntArray::class.java
        LongArray::class ->
            if (this.arguments.isNotEmpty()) Array<Long>::class.java else LongArray::class.java
        FloatArray::class ->
            if (this.arguments.isNotEmpty()) Array<Float>::class.java else FloatArray::class.java
        DoubleArray::class ->
            if (this.arguments.isNotEmpty()) Array<Double>::class.java else DoubleArray::class.java
        BooleanArray::class ->
            if (this.arguments.isNotEmpty()) Array<Boolean>::class.java
            else BooleanArray::class.java
        is KClass<*> -> {
            val javaClass = classifier.javaObjectType
            if (
                classifier.typeParameters.isEmpty() ||
                    javaClass.isArray ||
                    this.arguments.all { it.type == null }
            ) {
                javaClass
            } else {
                GenericTypes.parameterizeClass(
                    javaClass,
                    *(this.arguments
                        .map {
                            it.type?.asJdbiJavaType() ?: error("Star projections are not supported")
                        }
                        .toTypedArray()),
                )
            }
        }
        else -> error("Unsupported type $this")
    }
}

inline fun <reified T> defaultQualifiers() =
    T::class
        .java
        .annotations
        .filter { it.annotationClass.hasAnnotation<Qualifier>() }
        .map { it.annotationClass }
        .toTypedArray()
