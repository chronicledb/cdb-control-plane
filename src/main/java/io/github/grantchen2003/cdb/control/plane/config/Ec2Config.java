package io.github.grantchen2003.cdb.control.plane.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
public class Ec2Config {
    @Bean
    public Ec2Client ec2Client() {
        return Ec2Client.create();
    }
}
