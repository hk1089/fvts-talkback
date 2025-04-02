package com.hk1089.mettax.listener;

import java.util.List;

public interface PermissionResultListener {
    void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList);
}
