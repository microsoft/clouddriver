/*
 * Copyright 2014 Pivotal Inc.
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

package com.netflix.spinnaker.clouddriver.cf.deploy.validators

import com.netflix.spinnaker.clouddriver.security.DefaultAccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.security.MapBackedAccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.cf.TestCredential
import com.netflix.spinnaker.clouddriver.cf.deploy.description.ResizeCloudFoundryServerGroupDescription
import org.springframework.validation.Errors
import spock.lang.Shared
import spock.lang.Specification

class ResizeCloudFoundryServerGroupDescriptionValidatorSpec extends Specification {

  private static final SERVER_GROUP_NAME = "spinnaker-test-v000"
  private static final ZONE = "us-central1-b"
  private static final ACCOUNT_NAME = "auto"
  private static final TARGET_SIZE = 4

  @Shared
  ResizeCloudFoundryServerGroupDescriptionValidator validator

  void setupSpec() {
    validator = new ResizeCloudFoundryServerGroupDescriptionValidator()
    def credentialsRepo = new MapBackedAccountCredentialsRepository()
    def credentialsProvider = new DefaultAccountCredentialsProvider(credentialsRepo)
    credentialsRepo.save(ACCOUNT_NAME, TestCredential.named(ACCOUNT_NAME))
    validator.accountCredentialsProvider = credentialsProvider
  }

  void "pass validation with proper description inputs"() {
    setup:
    def description = new ResizeCloudFoundryServerGroupDescription(serverGroupName: SERVER_GROUP_NAME,
        zone: ZONE,
        targetSize: TARGET_SIZE,
        credentials: TestCredential.named(ACCOUNT_NAME))
    def errors = Mock(Errors)

    when:
      validator.validate([], description, errors)

    then:
      0 * errors._
  }

  void "null input fails validation"() {
    setup:
      def description = new ResizeCloudFoundryServerGroupDescription()
      def errors = Mock(Errors)

    when:
      validator.validate([], description, errors)

    then:
      1 * errors.rejectValue('credentials', _)
      1 * errors.rejectValue('serverGroupName', _)
      1 * errors.rejectValue('zone', _)
      1 * errors.rejectValue('targetSize', _)
  }
}