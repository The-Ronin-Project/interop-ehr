package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.test.data.generator.DataGenerator
import com.projectronin.test.data.generator.collection.ListDataGenerator

fun <T> generateWithDefault(
    generator: ListDataGenerator<T>,
    defaultValue: List<T>,
): List<T> = generator.generate().ifEmpty { defaultValue }

fun <T> generateWithDefault(
    generator: DataGenerator<T>,
    defaultValue: T,
): T = generator.generate() ?: defaultValue
