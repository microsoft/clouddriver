/*
 * Copyright 2015 The original authors.
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

package com.netflix.spinnaker.clouddriver.azure.templates

import com.netflix.spinnaker.clouddriver.azure.common.AzureUtilities
import com.netflix.spinnaker.clouddriver.azure.resources.securitygroup.model.AzureSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.azure.resources.securitygroup.model.UpsertAzureSecurityGroupDescription
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig

class AzureSecurityGroupResourceTemplate {
  static ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true)

  static String getTemplate(UpsertAzureSecurityGroupDescription description) {
    SecurityGroupTemplate template = new SecurityGroupTemplate(description)

    // For the moment return a hardcoded string reprezenting the deployment template
    //  corresponding to the Azure Network Security Group

    "template value here"

// TODO:
//    mapper.writeValueAsString(template)
  }

  static class SecurityGroupTemplate{
    String $schema = "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#"
    String contentVersion = "1.0.0.0"

    SecurityGroupParameters parameters
    SecurityGroupTemplateVariables variables
    ArrayList<Resource> resources = []

    SecurityGroupTemplate(UpsertAzureSecurityGroupDescription description) {
      parameters = new SecurityGroupParameters()
      variables = new SecurityGroupTemplateVariables(description)

      SecurityGroup sg = new SecurityGroup(description)

      resources.add(sg)
    }
  }

  static class SecurityGroupTemplateVariables{
    String securityGroupName

    SecurityGroupTemplateVariables(UpsertAzureSecurityGroupDescription description){
      String regionName = description.region.replace(' ', '').toLowerCase()
      String resourceGroupName = AzureUtilities.getResourceGroupName(description)

      securityGroupName = description.securityGroupName.toLowerCase()
    }
  }

  static class SecurityGroupParameters{
    Location location = new Location()
  }

  static class Location{
    String type = "string"
    Map<String, String> metadata = ["description":"Location to deploy"]
  }

  static class SecurityGroup extends DependingResource{
    Map<String, String> tags
    SecurityGroupProperties properties

    SecurityGroup(UpsertAzureSecurityGroupDescription description) {
      apiVersion = "2015-05-01-preview"
      name = "[variables('securityGroupName')]"
      type = "Microsoft.Network/networkSecurityGroups"
      location = "[parameters('location')]"
      tags = ["appName":description.appName, "stack":description.stack, "detail":description.detail]

      properties = new SecurityGroupProperties(description)
    }
  }

  static class SecurityGroupProperties{
    SecurityGroupProperties(UpsertAzureSecurityGroupDescription description){
    }
  }
}

