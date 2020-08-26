package com.yale.rpc.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author yale
 */
@Component
public class MyApplicationContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MyApplicationContext.applicationContext=applicationContext;
    }

    public static ApplicationContext getContext(){
        return applicationContext;
    }

    public static Object getBean(String beanName){
        return applicationContext.getBean(beanName);
    }

    public static Map<String,?> getBeanOfType(Class<?> clazz){
        return applicationContext.getBeansOfType(clazz);
    }
}
