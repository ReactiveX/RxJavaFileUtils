/*
 * Forked from: https://github.com/gjoseph/BarbaryWatchService
 * Waiting to see what the license is - https://github.com/gjoseph/BarbaryWatchService/issues/6
 */
package com.barbarysoftware.jna;

import com.sun.jna.NativeLong;

public class CFIndex extends NativeLong {
    private static final long serialVersionUID = 0;

    public static CFIndex valueOf(int i) {
        CFIndex idx = new CFIndex();
        idx.setValue(i);
        return idx;
    }
}
