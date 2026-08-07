/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import java.util.Comparator;

public class AlphanumComparator
implements Comparator<String> {
    public static final AlphanumComparator INSTANCE = new AlphanumComparator();

    private final boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private final String getChunk(String s, int slength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        ++marker;
        if (this.isDigit(c)) {
            while (marker < slength && this.isDigit(c = s.charAt(marker))) {
                chunk.append(c);
                ++marker;
            }
        } else {
            while (marker < slength && !this.isDigit(c = s.charAt(marker))) {
                chunk.append(c);
                ++marker;
            }
        }
        return chunk.toString();
    }

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }
        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();
        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = this.getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();
            String thatChunk = this.getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();
            int result = 0;
            if (this.isDigit(thisChunk.charAt(0)) && this.isDigit(thatChunk.charAt(0))) {
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; ++i) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result == 0) continue;
                        return result;
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk);
            }
            if (result == 0) continue;
            return result;
        }
        return s1Length - s2Length;
    }
}

