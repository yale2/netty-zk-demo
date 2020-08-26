package com.yale.zkconfig.dao;

import com.yale.zkconfig.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

/**
 * @author yale
 */
@Component
public interface UserDao {

    User findById(@Param("id") Long id);
}
