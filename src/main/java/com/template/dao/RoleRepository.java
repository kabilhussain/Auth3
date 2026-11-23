/**
 * 
 */
package com.template.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.template.model.RoleEntity;

/**
 * @author sunil
 *
 */
@Repository
public interface RoleRepository extends CrudRepository<RoleEntity, Integer> {

}
