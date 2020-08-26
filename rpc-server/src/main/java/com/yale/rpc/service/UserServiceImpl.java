package com.yale.rpc.service;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService {

    //将来客户单要远程调用的方法
    @Override
    public String sayHello(String msg) {
        System.out.println("are you ok ? "+msg);
        return "success";
    }
}
