package com.guideon.guideonbackend.domain.admin.repository;

import com.guideon.guideonbackend.domain.admin.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
}
