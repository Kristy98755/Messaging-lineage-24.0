/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.messaging.util;

import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.regex.Pattern;

/*
 * Helper class to support Linkify-ing the geo uri scheme, see
 * https://en.wikipedia.org/wiki/Geo_URI_scheme
 */
public class LinkifyHelper {
    public final static String TAG = "LinkifyHelper";
    public final static boolean VERBOSE = false;

    private final static Pattern GEO_URL_PATTERN =
            Pattern.compile("geo:([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?(.*))?",
                    Pattern.CASE_INSENSITIVE);


    // This could have been simpler, but Linkify.addLinks() removes existing links
    // and PHONE_NUMBERS also matches parts of the geo-url
    public static boolean addLinks(TextView text) {
        boolean ret;

        // We need to add and know the geo spans first since Linkify will replace them with
        // phone numbers - therefore we remove those later and replace them with the geo ones
        // again
        Linkify.addLinks(text, GEO_URL_PATTERN, null);
        SpannableString geoSpannable = SpannableString.valueOf(text.getText());
        final URLSpan[] geoSpans = geoSpannable.getSpans(0, geoSpannable.length(), URLSpan.class);

        ArrayList<Pair<Integer, Integer>> geoSpanPairs = new ArrayList<>();
        for (URLSpan geoSpan : geoSpans) {
            geoSpanPairs.add(new Pair<>(geoSpannable.getSpanStart(geoSpan),
                    geoSpannable.getSpanEnd(geoSpan)));
        }

        // We want "ALL" but that's deprecated due to Linkify.MAP_ADDRESSES
        int mask = Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS;

        // This will remove our existing spans
        ret = Linkify.addLinks(text, mask);
        SpannableString s = SpannableString.valueOf(text.getText());

        if (geoSpans.length > 0) {
            ret = true;
            final URLSpan[] allSpans = s.getSpans(0, s.length(), URLSpan.class);
            for (int i = allSpans.length - 1; i >= 0; i--) {
                int spanStart = s.getSpanStart(allSpans[i]);
                int spanEnd = s.getSpanEnd(allSpans[i]);
                for (Pair<Integer, Integer> pair : geoSpanPairs) {
                    if (spanStart >= pair.first && spanStart <= pair.second) {
                        // We have found a span within a geo span
                        if (VERBOSE) {
                            Log.d(TAG, "Removing span between " + spanStart + " and " + spanEnd +
                                    " since it's in range of a geo span (" + pair.first + ", " +
                                    pair.second + ")");
                        }
                        s.removeSpan(allSpans[i]);
                    }
                }
            }
            text.setText(s);
            // Add the geo spans again
            Linkify.addLinks(text, GEO_URL_PATTERN, null);
        }

        return ret;
    }
}
