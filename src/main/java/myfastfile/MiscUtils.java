package myfastfile;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiscUtils {

    private String osName = "linux";

    public MiscUtils() {
        Properties props = System.getProperties(); //鑾峰緱绯荤粺灞炴�ч泦    
        osName = props.getProperty("os.name"); //鎿嶄綔绯荤粺鍚嶇О   
    }

    public static byte[] toByteArray(long value) {
        // Note that this code needs to stay compatible with GWT, which has known
        // bugs when narrowing byte casts of long values occur.
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xffL);
            value >>= 8;
        }
        return result;
    }

    public static byte[] toByteArray(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value};
    }

    public static int fromByteArray(byte[] bytes) {

        return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    public static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    public static long fromBytes(byte b1, byte b2, byte b3, byte b4,
            byte b5, byte b6, byte b7, byte b8) {
        return (b1 & 0xFFL) << 56
                | (b2 & 0xFFL) << 48
                | (b3 & 0xFFL) << 40
                | (b4 & 0xFFL) << 32
                | (b5 & 0xFFL) << 24
                | (b6 & 0xFFL) << 16
                | (b7 & 0xFFL) << 8
                | (b8 & 0xFFL);
    }

    public void createDir(String dstPath) {

        Path newdir = FileSystems.getDefault().getPath(dstPath);

        boolean pathExists = Files.exists(newdir,
                new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
        if (!pathExists) {
            Set<PosixFilePermission> perms = PosixFilePermissions
                    .fromString("rwxrwxrwx");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                    .asFileAttribute(perms);
            try {
                if (osName.indexOf("Windows") == -1) {
                    Files.createDirectories(newdir, attr);
                } else {
                    Files.createDirectories(newdir);
                }
            } catch (Exception e) {
                System.err.println(e);

            }
        }
    }

    public static synchronized boolean moveFile(String src, String dst) {
        boolean ret = true;
        try {
            Path source = Paths.get(src);
            Path target = Paths.get(dst);
            Files.move(source, target, REPLACE_EXISTING, ATOMIC_MOVE);
            ret = true;
        } catch (IOException ex) {
            Logger.getLogger(MiscUtils.class.getName()).log(Level.SEVERE, null, ex);
            ret = false;
        }
        return ret;
    }

    public static long fileSize(String src) {
        long ret = 0L;
        try {
            Path source = Paths.get(src);

            ret = Files.size(source);

        } catch (IOException ex) {
            Logger.getLogger(MiscUtils.class.getName()).log(Level.SEVERE, null, ex);
            ret = 0L;
        }
        return ret;
    }

}
