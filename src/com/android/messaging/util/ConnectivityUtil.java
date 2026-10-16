/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * ConnectivityUtil listens to the network service state changes.
 */
public class ConnectivityUtil {
    // Assume not connected until informed differently
    private volatile int mCurrentServiceState = ServiceState.STATE_POWER_OFF;

    private final TelephonyManager mTelephonyManager;
    private final Handler mHandler;

    private ConnectivityListener mListener;
    private Object mPhoneStateListener;

    public interface ConnectivityListener {
        void onPhoneStateChanged(int serviceState);
    }

    public ConnectivityUtil(final Context context, final int subId) {
        mTelephonyManager =
                ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                        .createForSubscriptionId(subId);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void onPhoneStateChanged(final int serviceState) {
        final ConnectivityListener listener = mListener;
        if (listener != null) {
            listener.onPhoneStateChanged(serviceState);
        }
    }

    public void register(final ConnectivityListener listener) {
        Assert.isTrue(mListener == null || mListener == listener);
        if (mListener == null) {
            if (mTelephonyManager != null) {
                mCurrentServiceState = (PhoneUtils.getDefault().isAirplaneModeOn() ?
                        ServiceState.STATE_POWER_OFF : ServiceState.STATE_IN_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mPhoneStateListener = TelephonyCallbackHelper.register(
                            mTelephonyManager, mHandler, this);
                } else {
                    final android.telephony.PhoneStateListener legacyListener =
                            new android.telephony.PhoneStateListener() {
                                @Override
                                public void onDataConnectionStateChanged(int state, int networkType) {
                                    mCurrentServiceState = (state == TelephonyManager.DATA_DISCONNECTED) ?
                                            ServiceState.STATE_OUT_OF_SERVICE : ServiceState.STATE_IN_SERVICE;
                                }

                                @Override
                                public void onServiceStateChanged(ServiceState serviceState) {
                                    if (serviceState != null && mCurrentServiceState != serviceState.getState()) {
                                        mCurrentServiceState = serviceState.getState();
                                        ConnectivityUtil.this.onPhoneStateChanged(mCurrentServiceState);
                                    }
                                }
                            };
                    mPhoneStateListener = legacyListener;
                    mTelephonyManager.listen(legacyListener,
                            android.telephony.PhoneStateListener.LISTEN_SERVICE_STATE |
                            android.telephony.PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                }
            }
        }
        mListener = listener;
    }

    public void unregister() {
        if (mListener != null) {
            if (mTelephonyManager != null && mPhoneStateListener != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TelephonyCallbackHelper.unregister(mTelephonyManager, mPhoneStateListener);
                } else {
                    mTelephonyManager.listen(
                            (android.telephony.PhoneStateListener) mPhoneStateListener,
                            android.telephony.PhoneStateListener.LISTEN_NONE);
                }
                mPhoneStateListener = null;
                mCurrentServiceState = ServiceState.STATE_POWER_OFF;
            }
        }
        mListener = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static class TelephonyCallbackHelper {
        static Object register(TelephonyManager telephonyManager, Handler handler,
                               ConnectivityUtil util) {
            TelephonyCallbackImpl callback = new TelephonyCallbackImpl(util);
            telephonyManager.registerTelephonyCallback(handler::post, callback);
            return callback;
        }

        static void unregister(TelephonyManager telephonyManager, Object callback) {
            if (callback instanceof TelephonyCallback) {
                telephonyManager.unregisterTelephonyCallback((TelephonyCallback) callback);
            }
        }

        private static class TelephonyCallbackImpl extends TelephonyCallback implements
                TelephonyCallback.DataConnectionStateListener, TelephonyCallback.ServiceStateListener {
            private final ConnectivityUtil mUtil;

            TelephonyCallbackImpl(ConnectivityUtil util) {
                mUtil = util;
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                mUtil.mCurrentServiceState = (state == TelephonyManager.DATA_DISCONNECTED) ?
                        ServiceState.STATE_OUT_OF_SERVICE : ServiceState.STATE_IN_SERVICE;
            }

            @Override
            public void onServiceStateChanged(@NonNull ServiceState serviceState) {
                if (mUtil.mCurrentServiceState != serviceState.getState()) {
                    mUtil.mCurrentServiceState = serviceState.getState();
                    mUtil.onPhoneStateChanged(mUtil.mCurrentServiceState);
                }
            }
        }
    }
}
