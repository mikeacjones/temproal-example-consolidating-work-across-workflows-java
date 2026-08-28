package com.example.shipping;

import com.example.shipping.config.DemoTimingProperties;
import com.example.shipping.config.TemporalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DemoTimingProperties.class, TemporalProperties.class})
public class ShippingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShippingDemoApplication.class, args);
    }
}
