/**
 * 
 */
package com.template.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.template.model.UserEntity;

/**
 * @author sunil
 *
 */
@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
	UserEntity findByMobileNumber(Long mobileNumber);

	UserEntity findByEmail(String email);
}
