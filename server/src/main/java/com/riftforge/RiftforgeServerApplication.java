package com.riftforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RiftforgeServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(RiftforgeServerApplication.class, args);
  }
}
