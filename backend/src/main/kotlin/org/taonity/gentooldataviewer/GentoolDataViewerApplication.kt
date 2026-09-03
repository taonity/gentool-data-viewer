package org.taonity.gentooldataviewer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class GentoolDataViewerApplication

fun main(args: Array<String>) {
    runApplication<GentoolDataViewerApplication>(*args)
}