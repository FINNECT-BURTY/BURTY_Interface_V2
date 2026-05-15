package com.burty.application.port.out;

import com.burty.domain.model.AssetSnapshot;

public interface MyDataPort {
    AssetSnapshot fetchAssetSnapshot(String userId);
}
