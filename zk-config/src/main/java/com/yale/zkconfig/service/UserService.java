package com.yale.zkconfig.service;

import com.yale.zkconfig.dao.UserDao;
import com.yale.zkconfig.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yale
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    public User findById(Long id){
        return userDao.findById(id);
    }
}
