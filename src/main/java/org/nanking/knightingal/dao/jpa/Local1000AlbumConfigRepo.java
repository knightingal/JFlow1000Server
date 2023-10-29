package org.nanking.knightingal.dao.jpa;

import org.nanking.knightingal.bean.AlbumConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface Local1000AlbumConfigRepo extends JpaRepository<AlbumConfig, Long>, JpaSpecificationExecutor<AlbumConfig> {
}