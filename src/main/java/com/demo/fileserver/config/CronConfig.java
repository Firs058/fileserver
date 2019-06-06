package com.demo.fileserver.config;

import com.demo.fileserver.model.ClusterMember;
import com.hazelcast.core.HazelcastInstance;

import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.file.Path;


@Configuration
public class CronConfig {

    @Value("${hz.instance.name}")
    private String instanceName;
    @Value("${server.port}")
    private String port;
    @Value("${server.address}")
    private String address;
    @Value("${paths.upload}")
    private Path uploadPath;

    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public CronConfig(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Scheduled(fixedDelay = 5000)
    public void clearCache() {
        IMap<Object, Object> map = hazelcastInstance.getMap("cluster-info");
        ClusterMember clusterMember = new ClusterMember()
                .address(address + ":" + port)
                .name(instanceName)
                .freeSpace(new File(String.valueOf(uploadPath.getRoot())).getFreeSpace());
        map.put(instanceName, clusterMember);
    }
}