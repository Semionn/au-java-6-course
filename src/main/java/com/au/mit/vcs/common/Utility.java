package com.au.mit.vcs.common;

import com.au.mit.vcs.common.exceptions.CommandExecutionException;
import sun.misc.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class.
 * Allows build relative paths handling and hashing of files
 */
public class Utility {
    public static String getCurDir() {
        return System.getProperty("user.dir");
    }

    public static Path getCurDirPath() {
        return Paths.get(getCurDir());
    }

    public static Path getCurrentAbsolutePath(String path) {
        return getCurDirPath().resolve(path).toAbsolutePath();
    }

    public static String calcFileSHA1(String filePath) {
        try (FileInputStream fs = new FileInputStream(filePath)) {
            return Utility.calcSHA1(IOUtils.readFully(fs, -1, true));
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    public static String calcSHA1(String input) {
        return calcSHA1(input.getBytes());
    }

    public static String calcSHA1(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch(NoSuchAlgorithmException e) {
            throw new CommandExecutionException(e);
        }
        byte[] shaBytes = md.digest(bytes);
        String result = "";
        for (byte aByte : shaBytes) {
            result += Integer.toString((aByte & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
