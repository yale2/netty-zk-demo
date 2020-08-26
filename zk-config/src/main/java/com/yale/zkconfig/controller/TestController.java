package com.yale.zkconfig.controller;

import com.yale.zkconfig.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yale
 */
@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public String findUserById(Long id){
        return userService.findById(id).toString();
    }
}
