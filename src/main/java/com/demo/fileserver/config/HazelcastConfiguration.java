package com.demo.fileserver.config;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author firs
 */
@Configuration
public class HazelcastConfiguration {

    @Value("${hz.instance.name}")
    private String instanceName;

    @Bean
    public Config hazelCastConfig(){
        return new Config()
                .setInstanceName(instanceName)
                .addMapConfig(
                        new MapConfig()
                                .setName("cluster")
                                .setMaxSizeConfig(new MaxSizeConfig(200, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                .setTimeToLiveSeconds(5));
    }
}