package com.nuri.application.port.out;

import com.nuri.domain.model.AssetSnapshot;

public interface MyDataPort {
    AssetSnapshot fetchAssetSnapshot(String userId);
}
