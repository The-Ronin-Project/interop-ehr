package com.projectronin.interop.fhir.ronin.util

import mu.KLogger
import mu.KotlinLogging

fun <T : Any> T.logger(): Lazy<KLogger> =
    lazy {
        KotlinLogging.logger { }
    }
