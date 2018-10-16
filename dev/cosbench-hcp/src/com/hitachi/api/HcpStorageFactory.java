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