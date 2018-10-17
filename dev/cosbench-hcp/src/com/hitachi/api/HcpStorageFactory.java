/** 
 
Copyright 2018 Hitachi Vantara, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/ 
package com.hitachi.api;

import com.intel.cosbench.api.storage.*;

public class HcpStorageFactory implements StorageAPIFactory {

    private static final String STORAGE_NAME = "hcp";

    @Override
    public String getStorageName() {
        return STORAGE_NAME;
    }

    @Override
    public StorageAPI getStorageAPI() {
        return new HcpS3Storage();
    }
}