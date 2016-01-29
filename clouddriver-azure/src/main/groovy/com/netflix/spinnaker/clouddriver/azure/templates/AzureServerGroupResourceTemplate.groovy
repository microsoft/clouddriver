/*
 * Copyright 2016 The original authors.
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.netflix.spinnaker.clouddriver.azure.common.AzureUtilities
import com.netflix.spinnaker.clouddriver.azure.resources.servergroup.model.AzureServerGroupDescription

class AzureServerGroupResourceTemplate {

  private static String uniqueStorageNamesArrayVar
  private static String newStorageAccountsSuffixVar
  private static String vhdContainerNameVar

  protected static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)

  /**
   * Build the resource manager template that will create the Azure equivalent (VM Scale Set)
   * of the Spinnaker Server Group
   * @param description - Description object containing the values to be specified in the template
   * @return - JSON string representing the Resource Manager template for a Azure VM Scale Set (Server Group)
   */
  static String getTemplate(AzureServerGroupDescription description) {
    ServerGroupTemplate template = new ServerGroupTemplate(description)
    mapper.writeValueAsString(template)
  }

  /**
   *
   */
  static class ServerGroupTemplate {
    String $schema = "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#"
    String contentVersion = "1.0.0.0"

    ServerGroupTemplateParameters parameters
    ServerGroupTemplateVariables variables
    ArrayList<Resource> resources = []

    /**
     *
     * @param desrciption
     */
    ServerGroupTemplate(AzureServerGroupDescription desrciption) {
      parameters = new ServerGroupTemplateParameters()
      variables = new ServerGroupTemplateVariables(desrciption)

      resources.add(new StorageAccount(desrciption))
      resources.add(new VirtualMachineScaleSet(desrciption))
    }

  }

  /**
   *
   */
  static class ServerGroupTemplateVariables {
    String newStorageAccountSuffix
    String vhdContainerName
    OsType osType
    String imageReference
    ArrayList<String> uniqueStorageNameArray = []

    /**
     *
     * @param description
     */
    ServerGroupTemplateVariables(AzureServerGroupDescription description) {

      newStorageAccountSuffix = "sa"
      vhdContainerName = description.getIdenifier()
      osType = new OsType(description)
      imageReference = "[variables('osType')]"

      // for later use
      uniqueStorageNamesArrayVar = uniqueStorageNameArray.class.name
      vhdContainerNameVar = vhdContainerName.class.name
      newStorageAccountsSuffixVar = newStorageAccountSuffix.class.name

      for (int i = 0; i < description.getStorageAccountCount(); i++) {
        String uniqueName = String.format("[concast(uniqueString(concat(resourceGroup().id, variables('%s'), '%s')))]", newStorageAccountsSuffixVar, i)
        uniqueStorageNameArray.add(uniqueName)
      }
    }
  }

  /**
   *
   */
  static class ServerGroupTemplateParameters {
    LocationParameter location = new LocationParameter()
    SubnetParameter subnetId = new SubnetParameter()
  }

  static class SubnetParameter {
    String type = "string"
    Map<String, String> metadata = ["description":"ID for subnet to be used by scale set"]
  }
  /**
   *
   */
  static class LocationParameter {
    String type = "string"
    Map<String, String> metadata = ["description":"Location to deploy"]
  }

  /**
   *
   */
  static class OsType {
    String publisher
    String offer
    String sku
    String version

    /**
     *
     * @param description
     */
    OsType(AzureServerGroupDescription description)
    {
      publisher = description.image.publisher
      offer = description.image.offer
      sku = description.image.sku
      version = description.image.version
    }
  }

  /**
   *
   */
  static class StorageAccount extends Resource {
    CopyOperation copy
    StorageAccountProperties properties

    /**
     *
     * @param description
     */
    StorageAccount(AzureServerGroupDescription description) {
      apiVersion = "2015-06-015"
      name = String.format("[concat(variables('%s')[copyIndex()], variables('%s'))]", uniqueStorageNamesArrayVar, newStorageAccountsSuffixVar)
      type = "Microsoft.Storage/storageAccounts"
      location = "[parameters('location')]"

      copy = new CopyOperation("storageLoop", description.getStorageAccountCount())
      tags = ["appName":description.appName, "stack":description.stack, "detail":description.detail]
      properties = new StorageAccountProperties(description)
    }
  }

  /**
   *
   */
  static class StorageAccountProperties {
    String accountType

    /**
     *
     * @param description
     */
    StorageAccountProperties(AzureServerGroupDescription description) {
      accountType = "Standard_LRS" // TODO get this from the description
    }
  }

  /**
   *
   */
  static class CopyOperation {
    String name
    int count

    /**
     *
     * @param operationName
     * @param iterations
     */
    CopyOperation(String operationName, int iterations) {
      name = operationName
      count = iterations
    }
  }

  /**
   *
   */
  static class VirtualMachineScaleSet extends DependingResource {
    VirtualMachineScaleSetProperty properties
    ScaleSetSkuProperty sku
    ScaleSetVMProfileProperty virtualMachineProfile
    ScaleSetOsProfileProperty osProfile
    ScaleSetNetworkProfileProperty networkProfile

    /**
     *
     * @param description
     */
    VirtualMachineScaleSet(AzureServerGroupDescription description) {
      apiVersion = "2015-06-015"
      name = description.getIdenifier()
      type = "Microsoft.Compute/virtualMachineScaleSets"
      location = "[parameters('location')]"
      tags = ["appName":description.appName, "stack":description.stack, "detail":description.detail]

      description.getStorageAccountCount().times{ idx ->
        this.dependsOn.add(
          String.format("[concat('Microsoft.Storage/storageAccounts/', variables('%s')[%s], variables('%s'))]",
            uniqueStorageNamesArrayVar,
            idx,
            newStorageAccountsSuffixVar)
        )
      }

      properties = new VirtualMachineScaleSetProperty(description)
      sku = new ScaleSetSkuProperty(description)
      virtualMachineProfile = new ScaleSetVMProfileProperty(description)
      osProfile = new ScaleSetOsProfileProperty(description)
      networkProfile = new ScaleSetNetworkProfileProperty(description)
    }
  }

  static class VirtualMachineScaleSetProperty {
    Map<String, String> upgradePolicy = []

    VirtualMachineScaleSetProperty(AzureServerGroupDescription description) {
      upgradePolicy["mode"] = description.upgradePolicy.toString()
    }
  }

  // ***Scale Set SKU
  /**
   *
   */
  static class ScaleSetSkuProperty {
    String name
    String tier
    int capacity

    /**
     *
     * @param description
     */
    ScaleSetSkuProperty(AzureServerGroupDescription description) {
      name = description.sku.name
      tier = description.sku.tier
      capacity = description.sku.capacity
    }
  }

  // ***OSProfile
  static class ScaleSetOsProfileProperty {
    String computerNamePrefix
    String userName
    String password

    /**
     *
     * @param description
     */
    ScaleSetOsProfileProperty(AzureServerGroupDescription description) {
      computerNamePrefix = description.getIdenifier()
      userName = description.osConfig.adminUserName
      password = description.osConfig.adminPassword
    }
  }

  // ***Network Profile
  /**
   *
   */
  static class ScaleSetNetworkProfileProperty {
    ArrayList<NetworkInterfaceConfiguration> networkInterfaceConfigurations = []

    /**
     *
     * @param description
     */
    ScaleSetNetworkProfileProperty(AzureServerGroupDescription description) {
      networkInterfaceConfigurations.add(new NetworkInterfaceConfiguration(description))
    }
  }

  /**
   *
   */
  static class NetworkInterfaceConfiguration {
    String name
    NetworkInterfaceConfigurationProperty properties

    /**
     *
     * @param description
     */
    NetworkInterfaceConfiguration(AzureServerGroupDescription description) {
      name = AzureUtilities.NETWORK_INTERFACE_PREFIX + description.getIdenifier()
      properties = new NetworkInterfaceConfigurationProperty(description)
    }
  }

  /**
   *
   */
  static class NetworkInterfaceConfigurationProperty {
    Boolean primary
    ArrayList<NetworkInterfaceIPConfiguration> ipConfigurations = []

    /**
     *
     * @param description
     */
    NetworkInterfaceConfigurationProperty(AzureServerGroupDescription description) {
      primary = true
      ipConfigurations.add(new NetworkInterfaceIPConfiguration(description))
    }
  }

  /**
   *
   */
  static class NetworkInterfaceIPConfiguration {
    String name
    NetworkInterfaceIPConfigurationsProperty properties

    /**
     *
     * @param description
     */
    NetworkInterfaceIPConfiguration(AzureServerGroupDescription description) {
      name = AzureUtilities.IPCONFIG_NAME_PREFIX + description.getIdenifier()
      properties = new NetworkInterfaceIPConfigurationsProperty(description)
    }
  }

  /**
   *
   */
  static class NetworkInterfaceIPConfigurationsProperty {
    NetworkInterfaceIPConfigurationSubnet subnet

    /**
     *
     * @param description
     */
    NetworkInterfaceIPConfigurationsProperty(AzureServerGroupDescription description) {
      subnet = new NetworkInterfaceIPConfigurationSubnet(description)
    }
  }

  /**
   *
   */
  static class NetworkInterfaceIPConfigurationSubnet {
    String id
    NetworkInterfaceIPConfigurationSubnet(AzureServerGroupDescription description) {
      id = "[parameters('subnetID')]"
    }
  }

  // ***VM Profile
  /**
   *
   */
  static class ScaleSetVMProfileProperty {
    ScaleSetStorageProfile storageProfile
    String imageReference

    ScaleSetVMProfileProperty(AzureServerGroupDescription description) {
      storageProfile = new ScaleSetStorageProfile(description)
      imageReference = "[variables('imageReference')]"
    }
  }

  /**
   *
   */
  static class ScaleSetStorageProfile {

    VirtualMachineOSDisk osDisk
    /**
     *
     * @param serverGroupDescription
     */
    ScaleSetStorageProfile(AzureServerGroupDescription description) {
      osDisk = new VirtualMachineOSDisk(description)
    }
  }

  static class VirtualMachineOSDisk {
    String name
    String caching
    String createOption
    ArrayList<String> vhdContainers = []

    VirtualMachineOSDisk(AzureServerGroupDescription description) {
      name = "osdisk-" + description.getIdenifier()
      caching = "ReadOnly"
      createOption = "FromImage"
      description.getStorageAccountCount().times { idx ->
        vhdContainers.add(String.format("[concat('https://', variables('%s')[%s], variables('%s'), '.blob.core.windows.net/', variables('%s'))]",
          uniqueStorageNamesArrayVar,
          idx,
          newStorageAccountsSuffixVar,
          vhdContainerNameVar))
      }
    }
  }
}
