package com.hk1089.mettax.utils;

import androidx.fragment.app.FragmentActivity;

import com.hk1089.mettax.listener.PermissionResultListener;
import com.permissionx.guolindev.PermissionX;

import java.util.List;

public class Extensions {



    public void requestPermissions(
            FragmentActivity activity,
            List<String> permissionsList,
            PermissionResultListener listener
    ) {
        PermissionX.init(activity)
                .permissions(permissionsList)
                .explainReasonBeforeRequest()
                .onForwardToSettings((scope, deniedList) ->
                        scope.showForwardToSettingsDialog(
                                deniedList,
                                "You need to allow necessary permissions in Settings manually",
                                "OK"
                        )
                )
                .request((allGranted, grantedList, deniedList) ->
                        listener.onResult(allGranted, grantedList, deniedList)
                );
    }
}
