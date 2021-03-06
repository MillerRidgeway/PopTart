package peer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
    /**
     * This method converts a set of bytes into a Hexadecimal representation.
     *
     * @param buf
     * @return
     */
    public static String convertBytesToHex(byte[] buf) {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            int byteValue = (int) buf[i] & 0xff;
            if (byteValue <= 15) {
                strBuf.append("0");
            }
            strBuf.append(Integer.toString(byteValue, 16));
        }
        return strBuf.toString();
    }

    /**
     * This method converts a specified hexadecimal String into a set of bytes.
     *
     * @param hexString
     * @return
     */
    public static byte[] convertHexToBytes(String hexString) {
        int size = hexString.length();
        byte[] buf = new byte[size / 2];
        int j = 0;
        for (int i = 0; i < size; i++) {
            String a = hexString.substring(i, i + 2);
            int valA = Integer.parseInt(a, 16);
            i++;
            buf[j] = (byte) valA;
            j++;
        }
        return buf;
    }

    public static int getIdMatchingDigits(String id1, String id2) {
        for (int i = 0; i < id1.length(); i++) {
            if (id1.charAt(i) != id2.charAt(i))
                return i;
        }
        return -1;
    }

    public static int getNumericalDifference(String id1, String id2) {
        int parsedId1 = Integer.parseInt(id1, 16);
        int parsedId2 = Integer.parseInt(id2, 16);

        return parsedId1 - parsedId2;
    }

    public static String getTimestampId() throws NoSuchAlgorithmException {
        long timeStamp = System.currentTimeMillis();
        String hex = Long.toHexString(timeStamp);

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] buf = messageDigest.digest(hex.getBytes());
        String hashedHex = convertBytesToHex(buf);

        return hashedHex.substring(hashedHex.length() - 4);
    }

    public static String getFilenameHash(String filename) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] buf = messageDigest.digest(filename.getBytes());
        String hashedHex = convertBytesToHex(buf);

        return hashedHex.substring(hashedHex.length() - 4);
    }

}
