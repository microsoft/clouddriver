package com.netflix.spinnaker.clouddriver.azure.resources.vmimage.model

/**
 * Created by larry on 3/10/16.
 */
class AzureNamedImage {
  String imageName
  Boolean iscustom = false
  String publisher
  String offer
  String sku
  String version
  String account
  String region
  String uri
  String ostype
}
