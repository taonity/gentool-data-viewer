package org.taonity.gentooldataviewer.config.repository

import org.taonity.gentooldataviewer.config.entity.ConfigOverrideEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConfigOverrideRepository : JpaRepository<ConfigOverrideEntity, String>
