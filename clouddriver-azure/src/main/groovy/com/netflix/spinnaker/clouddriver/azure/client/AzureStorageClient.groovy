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

package com.netflix.spinnaker.clouddriver.azure.client

import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.ListBlobItem
import com.microsoft.azure.storage.CloudStorageAccount
import com.netflix.spinnaker.clouddriver.azure.resources.vmimage.model.AzureCustomImageStorage
import com.netflix.spinnaker.clouddriver.azure.resources.vmimage.model.AzureCustomVMImage
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class AzureStorageClient {
  static final String AZURE_IMAGE_FILE_EXT = ".vhd"

  static List<AzureCustomVMImage> getCustomImages(List<AzureCustomImageStorage> imageStorageList) {
    def vmImages = new ArrayList<AzureCustomVMImage>()

    imageStorageList.each {AzureCustomImageStorage storage ->
      try {
        ArrayList<String> blobDirectoryList = new ArrayList<String>()

        // Retrieve storage account from connection-string.
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storage.scs)

        // retrieve the blob client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient()
        String dirDelimiter = blobClient.getDirectoryDelimiter()
        blobDirectoryList.addAll(storage.blobDir.split(dirDelimiter))
        def container = blobClient.getContainerReference(blobDirectoryList.remove(0))

        if (container) {
          if (blobDirectoryList.size() > 0) {
            def dir = blobDirectoryList.remove(0)
            def blob = container.getDirectoryReference(dir)

            while (blobDirectoryList.size() > 0) {
              dir = blobDirectoryList.remove(0)
              blob = blob.getDirectoryReference(dir)
            }

            if (blob) {
              getBlobsContent(blob, AZURE_IMAGE_FILE_EXT).each { URI uri ->
                vmImages.add(getAzureCustomVMImage(uri, dirDelimiter, storage.osType, storage.region))
              }
            }
          } else {
            getBlobsContent(container, AZURE_IMAGE_FILE_EXT).each { URI uri ->
              vmImages.add(getAzureCustomVMImage(uri, dirDelimiter, storage.osType, storage.region))
            }
          }
        }
      }
      catch (Exception e) {
        log.error("getCustomImages -> Unexpected exception ", e)
      }
    }

    vmImages
  }

  static List<URI> getBlobsContent(CloudBlobDirectory blobDir, String filter) {
    def uriList = new ArrayList<URI>()

    blobDir.listBlobs().each { ListBlobItem blob ->
      try {
        // try converting current blob item to a CloudBlobDirectory; if conversion fails an exception is thrown
        CloudBlobDirectory blobDirectory = blob as CloudBlobDirectory
        if (blobDirectory) {
          uriList.addAll(getBlobsContent(blobDirectory, filter))
        }
      } catch(Exception e) {
        // blob must be a regular item
        if (blob.uri.toString().toLowerCase().endsWith(filter)) {
          uriList.add(blob.uri)
        }
      }
    }

    uriList
  }

  static List<URI> getBlobsContent(CloudBlobContainer container, String filter) {
    def uriList = new ArrayList<URI>()

    container.listBlobs().each { ListBlobItem blob ->
      try {
        CloudBlobDirectory blobDirectory = blob as CloudBlobDirectory
        if (blobDirectory) {
          uriList.addAll(getBlobsContent(blobDirectory, filter))
        }
      } catch(Exception e) {
        // blob must be a regular item
        if (blob.uri.toString().toLowerCase().endsWith(filter)) {
          uriList.add(blob.uri)
        }
      }
    }

    uriList
  }

  static AzureCustomVMImage getAzureCustomVMImage(URI uri, String delimiter, String osType, String region) {
    String imageName = uri.toString()
    def idx = imageName.lastIndexOf(delimiter)
    if (idx > 0) {
      imageName = imageName.substring(idx+1)
    }

    new AzureCustomVMImage(
      name: imageName,
      uri: uri.toString(),
      osType: osType,
      region: region
    )
  }

}

/*
class AzureCustomImageStorage{
  String scs
  String blobDir
  String osType
}
*/
