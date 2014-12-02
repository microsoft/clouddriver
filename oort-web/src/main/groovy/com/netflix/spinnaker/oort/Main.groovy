/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.oort

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.web.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.ShallowEtagHeaderFilter

import javax.servlet.Filter

@Configuration
@ComponentScan(["com.netflix.spinnaker.oort.config", "com.netflix.spinnaker.oort.controllers", "com.netflix.spinnaker.oort.filters"])
@EnableAutoConfiguration
@EnableScheduling
@EnableAsync
class Main extends SpringBootServletInitializer {

  static {
    imposeSpinnakerFileConfig("oort-internal.yml")
    imposeSpinnakerFileConfig("oort-local.yml")
    imposeSpinnakerClasspathConfig("oort-internal.yml")
    imposeSpinnakerClasspathConfig("oort-local.yml")
  }

  static void main(String... args) {
    System.setProperty("netflix.environment", System.getProperty("netflix.environment", "test"))
    SpringApplication.run this, args
  }

  static void imposeSpinnakerFileConfig(String file) {
    def internalConfig = new File("${System.properties['user.home']}/.spinnaker/${file}")
    if (internalConfig.exists()) {
      System.setProperty("spring.config.location", "${System.properties["spring.config.location"]},${internalConfig.canonicalPath}")
    }
  }

  static void imposeSpinnakerClasspathConfig(String resource) {
    def internalConfig = getClass().getResourceAsStream("/${resource}")
    if (internalConfig) {
      System.setProperty("spring.config.location", "${System.properties["spring.config.location"]},classpath:/${resource}")
    }
  }

  @Override
  SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    System.setProperty("netflix.environment", System.getProperty("netflix.environment", "test"))
    application.sources Main
  }

  @Bean
  Filter eTagFilter() {
    new ShallowEtagHeaderFilter()
  }
}