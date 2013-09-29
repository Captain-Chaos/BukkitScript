/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.*;
import java.nio.charset.Charset;

/**
 *
 * @author pepijn
 */
public class FileUtils {
    public static String read(File file) throws IOException {
        return read(file, Charset.defaultCharset());
    }
    
    public static String read(File file, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder((int) file.length());
        Reader in = new InputStreamReader(new FileInputStream(file), charset);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }
    
    private static final int BUFFER_SIZE = 32768;
}