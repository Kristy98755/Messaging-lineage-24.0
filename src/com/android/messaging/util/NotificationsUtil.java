/*
 * Copyright (C) 2019-2025 The LineageOS Project
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

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;

import java.util.List;

public final class NotificationsUtil {
    public static final String DEFAULT_CHANNEL_ID = "messaging_channel";
    public static final String CONVERSATION_GROUP_NAME = "conversation_group";

    private NotificationsUtil() {
    }

    public static void createNotificationChannel(Context context, String id,
            int titleResId, int priority, String groupId) {
        String title = context.getString(titleResId);
        createNotificationChannel(context, id, title, priority, groupId);
    }

    public static void createNotificationChannel(Context context, String id,
            String title, int priority, String groupId) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel existing = manager.getNotificationChannel(id);
        if (existing != null) {
            return;
        }

        NotificationChannel newChannel = new NotificationChannel(id, title, priority);
        newChannel.enableLights(true);
        if (groupId != null) {
            newChannel.setGroup(groupId);
        }
        manager.createNotificationChannel(newChannel);
    }

    public static void deleteNotificationChannel(Context context, String id) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.deleteNotificationChannel(id);
    }

    public static void createNotificationChannelGroup(Context context, String id,
            int titleResId) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        // getNotificationChannelGroup(String) only exists from API 28, so look the
        // group up through the API 26 list getter instead.
        final List<NotificationChannelGroup> existing = manager.getNotificationChannelGroups();
        if (existing != null) {
            for (final NotificationChannelGroup group : existing) {
                if (group != null && id.equals(group.getId())) {
                    return;
                }
            }
        }

        String title = context.getString(titleResId);
        NotificationChannelGroup newChannelGroup = new NotificationChannelGroup(id, title);
        manager.createNotificationChannelGroup(newChannelGroup);
    }

    public static NotificationChannel getNotificationChannel(Context context, String id) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager.getNotificationChannel(id);
    }
}
