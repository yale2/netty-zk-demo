package com.yale.zkconfig.properties;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

/**
 * @author yale
 */
@Component
@Data
public class DatasourceProperties implements ApplicationListener<ContextRefreshedEvent> {

    private static final String PARENT_PATH="/zk-config";

    @Autowired
    private DruidDataSource druidDataSource;

    private String url;

    private String username;

    private String password;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("139.155.234.189:2181", retryPolicy);
        //启动curator
        curatorFramework.start();
        List<String> strings = null;
        try {
            strings = curatorFramework.getChildren().forPath(PARENT_PATH);
            //创建子节点监听
            PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorFramework, PARENT_PATH, true);
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    ChildData data = pathChildrenCacheEvent.getData();
                    String[] split = data.getPath().split("\\/");
                    switch (pathChildrenCacheEvent.getType()){
                        case CHILD_UPDATED:
                            changeDataSourceProperties(split[split.length-1],new String(data.getData()));
                            druidDataSource.restart();
                    }
                }
            });
            //启动监听
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            for (String string : strings) {
                switch (string) {
                    case "url":
                        String url = new String(curatorFramework.getData().forPath(PARENT_PATH + "/" + string));
                        this.setUrl(url);
                        druidDataSource.setUrl(url);
                        break;
                    case "username":
                        String username = new String(curatorFramework.getData().forPath(PARENT_PATH + "/" + string));
                        this.setUsername(new String(curatorFramework.getData().forPath(PARENT_PATH + "/" + string)));
                        druidDataSource.setUsername(username);
                        break;
                    case "password":
                        String password = new String(curatorFramework.getData().forPath(PARENT_PATH + "/" + string));
                        this.setPassword(new String(curatorFramework.getData().forPath(PARENT_PATH + "/" + string)));
                        druidDataSource.setPassword(password);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void changeDataSourceProperties(String variable, String data) throws SQLException {
        switch (variable) {
            case "url":
                this.url=data;
                druidDataSource.setUrl(data);
                druidDataSource.restart();
                break;
            case "username":
                this.username=data;
                druidDataSource.setUsername(data);
                druidDataSource.setUsername(data);
                break;
            case "password":
                this.password=data;
                druidDataSource.setPassword(data);
                druidDataSource.setPassword(data);
                break;
        }
    }
}
