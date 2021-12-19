package com.leo.ipcsocket.util;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
    /**
     * 关闭
     *
     * @param closeableArray 关闭
     */
    public static void close(Closeable... closeableArray) {
        for (Closeable closeable : closeableArray) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
