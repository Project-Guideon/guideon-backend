package com.guideon.core.domain.admin.repository;

import com.guideon.core.domain.admin.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
}
