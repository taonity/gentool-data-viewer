package org.taonity.gentooldataviewer.common.demo

interface DemoDataContributor {
    val feature: String

    fun seed(): Int
}
