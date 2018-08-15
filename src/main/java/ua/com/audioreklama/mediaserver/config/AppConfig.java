package ua.com.audioreklama.mediaserver.config;

import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public CIFSContext getBaseSmbContext() throws Exception{
        return new BaseContext(new PropertyConfiguration(System.getProperties()));
    }
}
