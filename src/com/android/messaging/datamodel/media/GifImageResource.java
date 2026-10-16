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
package com.android.messaging.datamodel.media;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import com.android.messaging.util.Assert;
import com.android.messaging.util.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GifImageResource extends ImageResource {
    private Object mImageDecoderSource;
    private byte[] mBytes;

    public GifImageResource(String key, Object imageDecoderSource, byte[] bytes) {
        // GIF does not support exif tags
        super(key, ExifInterface.ORIENTATION_NORMAL);
        mImageDecoderSource = imageDecoderSource;
        mBytes = bytes;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public static GifImageResource createGifImageResource(String key, InputStream inputStream) {
        final byte[] bytes;
        try {
            bytes = readAllBytes(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // ImageDecoder.createSource(byte[]) only exists from API 31, so fall back to
        // the Bitmap decoding path on anything older.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Object source = ImageDecoderHelper.createSource(bytes);
            if (source == null) {
                return null;
            }
            return new GifImageResource(key, source, bytes);
        } else {
            return new GifImageResource(key, null, bytes);
        }
    }

    @Override
    public Drawable getDrawable(Resources resources) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mImageDecoderSource != null) {
                return ImageDecoderHelper.decodeDrawable(mImageDecoderSource);
            } else if (mBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(mBytes, 0, mBytes.length);
                if (bitmap != null) {
                    return new BitmapDrawable(resources, bitmap);
                }
            }
            return null;
        } catch (final Throwable t) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Error getting drawable for GIF", t);
            return null;
        }
    }

    @Override
    public Bitmap getBitmap() {
        if (mBytes != null) {
            try {
                return BitmapFactory.decodeByteArray(mBytes, 0, mBytes.length);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public byte[] getBytes() {
        return mBytes;
    }

    @Override
    public Bitmap reuseBitmap() {
        return null;
    }

    @Override
    public boolean supportsBitmapReuse() {
        return false;
    }

    @Override
    public int getMediaSize() {
        return mBytes != null ? mBytes.length : 0;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    protected void close() {
        acquireLock();
        try {
            mImageDecoderSource = null;
            mBytes = null;
        } finally {
            releaseLock();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static class ImageDecoderHelper {
        static Object createSource(byte[] bytes) {
            return ImageDecoder.createSource(bytes);
        }

        static Drawable decodeDrawable(Object source) throws IOException {
            if (source instanceof ImageDecoder.Source) {
                return ImageDecoder.decodeDrawable((ImageDecoder.Source) source);
            }
            return null;
        }
    }
}
