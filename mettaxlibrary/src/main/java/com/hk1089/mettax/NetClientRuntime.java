package com.hk1089.mettax;

import android.content.Context;
import android.util.Log;

import com.babelstar.gviewer.NetClient;

/**
 * Process-wide guard for NetClient global native state.
 * Prevents repeated Initialize/SetDirSvr churn across video/talkback flows.
 */
public final class NetClientRuntime {
    private static final String TAG = "NetClientRuntime";
    private static final Object LOCK = new Object();

    private static NetClient sClient;
    private static boolean sInitialized = false;
    private static String sServer = null;
    private static int sPort = -1;

    private NetClientRuntime() { }

    public static boolean ensureInitialized(Context context) {
        synchronized (LOCK) {
            if (sInitialized && sClient != null) {
                return true;
            }
            try {
                String sdPath = context.getFilesDir().getAbsolutePath() + "/";
                sClient = new NetClient();
                sClient.Initialize(sdPath);
                sClient.SetJniEnv();
                sClient.SetSession("");
                sInitialized = true;
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "Failed to initialize NetClient runtime", t);
                sInitialized = false;
                sClient = null;
                return false;
            }
        }
    }

    public static boolean ensureThreadEnv() {
        synchronized (LOCK) {
            if (!sInitialized || sClient == null) {
                return false;
            }
            try {
                sClient.SetJniEnv();
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "Failed to bind JNI env", t);
                return false;
            }
        }
    }

    public static boolean updateServerIfNeeded(String server, int port) {
        synchronized (LOCK) {
            if (!sInitialized || sClient == null || server == null || server.trim().isEmpty() || port <= 0) {
                return false;
            }
            if (server.equals(sServer) && port == sPort) {
                return true;
            }
            try {
                NetClient.SetDirSvr(server, server, port, 0);
                sServer = server;
                sPort = port;
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "Failed to update server: " + server + ":" + port, t);
                return false;
            }
        }
    }
}
