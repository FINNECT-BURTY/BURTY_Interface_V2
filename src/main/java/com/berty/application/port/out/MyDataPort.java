package com.berty.application.port.out;

import com.berty.domain.model.AssetSnapshot;

public interface MyDataPort {
    AssetSnapshot fetchAssetSnapshot(String userId);
}
