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
package com.netflix.spinnaker.clouddriver.azure.resources.loadbalancer.deploy.templates

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.clouddriver.azure.security.AzureNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.azure.resources.loadbalancer.ops.converters.UpsertAzureLoadBalancerAtomicOperationConverter
import com.netflix.spinnaker.clouddriver.azure.templates.AzureLoadBalancerResourceTemplate
import com.netflix.spinnaker.clouddriver.azure.resources.loadbalancer.model.UpsertAzureLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import spock.lang.Shared
import spock.lang.Specification

class AzureLoadBalancerResourceTemplateSpec extends Specification {
  UpsertAzureLoadBalancerDescription description

  void setup(){
    description = createDescription()
  }

  def 'should generate correct LoadBalancer create template'(){
    String template = AzureLoadBalancerResourceTemplate.getTemplate(description)

    expect: template == expectedFullTemplate
  }

  UpsertAzureLoadBalancerDescription createNoRulesDescription(){
    new UpsertAzureLoadBalancerDescription()
  }

  UpsertAzureLoadBalancerDescription createDescription(){
    UpsertAzureLoadBalancerDescription description = new UpsertAzureLoadBalancerDescription()
    description.cloudProvider = 'azure'
    description.appName = 'azureMASM'
    description.loadBalancerName = 'azureMASM-st1-d11'
    description.stack = 'st1'
    description.detail = 'd11'
    description.region = 'West US'
    description.vnet = null
    description.probes = new ArrayList<UpsertAzureLoadBalancerDescription.AzureLoadBalancerProbe>()

    UpsertAzureLoadBalancerDescription.AzureLoadBalancerProbe probe = new UpsertAzureLoadBalancerDescription.AzureLoadBalancerProbe()
    probe.probeName = 'healthcheck1'
    probe.probeProtocol = 'HTTP'
    probe.probePort = 7001
    probe.probePath = '/healthcheck'
    probe.probeInterval = 10
    probe.unhealthyThreshold = 2

    description.probes.add(probe)
    description.securityGroups = null
    description.loadBalancingRules = new ArrayList<UpsertAzureLoadBalancerDescription.AzureLoadBalancingRule>()

    UpsertAzureLoadBalancerDescription.AzureLoadBalancingRule rule = new UpsertAzureLoadBalancerDescription.AzureLoadBalancingRule()
    rule.ruleName = 'lbrule1'
    rule.protocol = 'TCP'
    rule.externalPort = 80
    rule.backendPort = 80
    rule.probeName = probe.probeName
    rule.persistence = 'None'
    rule.idleTimeout = 4

    description.loadBalancingRules.add(rule)
    description.inboundNATRules = new ArrayList<UpsertAzureLoadBalancerDescription.AzureLoadBalancerInboundNATRule>()

    UpsertAzureLoadBalancerDescription.AzureLoadBalancerInboundNATRule natRule = new UpsertAzureLoadBalancerDescription.AzureLoadBalancerInboundNATRule()
    natRule.ruleName = 'inboundRule1'
    natRule.serviceType = UpsertAzureLoadBalancerDescription.AzureLoadBalancerInboundNATRule.AzureLoadBalancerInboundNATRulesServiceType.SSH
    natRule.protocol = UpsertAzureLoadBalancerDescription.AzureLoadBalancerInboundNATRule.AzureLoadBalancerInboundNATRulesProtocolType.TCP
    natRule.port = 80

    description.inboundNATRules.add(natRule)
    description.name = 'azureMASM-st1-d11'
    description.user = '[anonymous]'

    return description
  }

  static String expectedFullTemplate = '''{
  "$schema" : "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
  "contentVersion" : "1.0.0.0",
  "parameters" : {
    "location" : {
      "type" : "string",
      "allowedValues" : [ "East US", "eastus", "West US", "westus", "West Europe", "westeurope", "East Asia", "eastasia", "Southeast Asia", "southeastus" ],
      "metadata" : {
        "description" : "Location to deploy"
      }
    }
  },
  "variables" : {
    "loadBalancerName" : "azureMASM-st1-d11",
    "virtualNetworkName" : "vnet-westus-azureMASM-st1-d11",
    "publicIPAddressName" : "publicIp-westus-azureMASM-st1-d11",
    "publicIPAddressType" : "Dynamic",
    "loadBalancerFrontEnd" : "lbFrontEnd-westus-azureMASM-st1-d11",
    "dnsNameForLBIP" : "dns-westus-azuremasm-st1-d11",
    "ipConfigName" : "ipConfig-westus-azureMASM-st1-d11",
    "loadBalancerID" : "[resourceID('Microsoft.Network/loadBalancers',variables('loadBalancerName'))]",
    "publicIPAddressID" : "[resourceID('Microsoft.Network/publicIPAddresses',variables('publicIPAddressName'))]",
    "frontEndIPConfig" : "[concat(variables('loadBalancerID'),'/frontendIPConfigurations/',variables('loadBalancerFrontEnd'))]"
  },
  "resources" : [ {
    "apiVersion" : "2015-05-01-preview",
    "name" : "[variables('publicIPAddressName')]",
    "type" : "Microsoft.Network/publicIPAddresses",
    "location" : "[parameters('location')]",
    "properties" : {
      "publicIPAllocationMethod" : "[variables('publicIPAddressType')]",
      "dnsSettings" : {
        "domainNameLabel" : "[variables('dnsNameForLBIP')]"
      }
    }
  }, {
    "apiVersion" : "2015-05-01-preview",
    "name" : "[variables('loadBalancerName')]",
    "type" : "Microsoft.Network/loadBalancers",
    "location" : "[parameters('location')]",
    "dependsOn" : [ "[concat('Microsoft.Network/publicIPAddresses/',variables('publicIPAddressName'))]" ],
    "tags" : {
      "appName" : "azureMASM",
      "stack" : "st1",
      "detail" : "d11"
    },
    "properties" : {
      "frontEndIPConfigurations" : [ {
        "name" : "[variables('loadBalancerFrontEnd')]",
        "properties" : {
          "publicIPAddress" : {
            "id" : "[variables('publicIPAddressID')]"
          }
        }
      } ],
      "loadBalancingRules" : [ {
        "name" : "lbrule1",
        "properties" : {
          "frontendIPConfiguration" : {
            "id" : "[variables('frontEndIPConfig')]"
          },
          "protocol" : "tcp",
          "frontendPort" : 80,
          "backendPort" : 80,
          "probe" : {
            "id" : "[concat(variables('loadBalancerID'),'/probes/healthcheck1')]"
          }
        }
      } ],
      "probes" : [ {
        "properties" : {
          "protocol" : "http",
          "port" : 7001,
          "intervalInSeconds" : 10,
          "requestPath" : "/healthcheck",
          "numberOfProbes" : 2
        },
        "name" : "healthcheck1"
      } ]
    }
  } ]
}'''
}
