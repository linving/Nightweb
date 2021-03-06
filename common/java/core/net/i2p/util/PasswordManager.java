package net.i2p.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.i2p.I2PAppContext;
import net.i2p.data.Base64;
import net.i2p.data.DataHelper;
import net.i2p.data.SessionKey;

/**
 *  Manage both plaintext and salted/hashed password storage in
 *  router.config.
 *
 *  There's no state here, so instantiate at will.
 *
 *  @since 0.9.4
 */
public class PasswordManager {
    private final I2PAppContext _context;

    protected static final int SALT_LENGTH = 16;
    /** 48 */
    protected static final int SHASH_LENGTH = SALT_LENGTH + SessionKey.KEYSIZE_BYTES;

    /** stored as plain text */
    protected static final String PROP_PW = ".password";
    /** stored obfuscated as b64 of the UTF-8 bytes */
    protected static final String PROP_B64 = ".b64";
    /** stored as the hex of the MD5 hash of the ISO-8859-1 bytes. Compatible with Jetty. */
    protected static final String PROP_MD5 = ".md5";
    /** stored as a Unix crypt string */
    protected static final String PROP_CRYPT = ".crypt";
    /** stored as the b64 of the 16 byte salt + the 32 byte hash of the UTF-8 bytes */
    protected static final String PROP_SHASH = ".shash";

    public PasswordManager(I2PAppContext ctx) {
        _context = ctx;
    }
    
    /**
     *  Checks both plaintext and hash
     *
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @param pw plain text, already trimmed
     *  @return if pw verified
     */
    public boolean check(String realm, String user, String pw) {
        return checkPlain(realm, user, pw) ||
               checkB64(realm, user, pw) ||
               checkHash(realm, user, pw);
    }
    
    /**
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @param pw plain text, already trimmed
     *  @return if pw verified
     */
    public boolean checkPlain(String realm, String user, String pw) {
        String pfx = realm;
        if (user != null && user.length() > 0)
            pfx += '.' + user;
        return pw.equals(_context.getProperty(pfx + PROP_PW));
    }
    
    /**
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @param pw plain text, already trimmed
     *  @return if pw verified
     */
    public boolean checkB64(String realm, String user, String pw) {
        String pfx = realm;
        if (user != null && user.length() > 0)
            pfx += '.' + user;
        String b64 = _context.getProperty(pfx + PROP_B64);
        if (b64 == null)
            return false;
        return b64.equals(Base64.encode(DataHelper.getUTF8(pw)));
    }
    
    /**
     *  With random salt
     *
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @param pw plain text, already trimmed
     *  @return if pw verified
     */
    public boolean checkHash(String realm, String user, String pw) {
        String pfx = realm;
        if (user != null && user.length() > 0)
            pfx += '.' + user;
        String shash = _context.getProperty(pfx + PROP_SHASH);
        if (shash == null)
            return false;
        byte[] shashBytes = Base64.decode(shash);
        if (shashBytes == null || shashBytes.length != SHASH_LENGTH)
            return false;
        byte[] salt = new byte[SALT_LENGTH];
        byte[] hash = new byte[SessionKey.KEYSIZE_BYTES];
        System.arraycopy(shashBytes, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(shashBytes, SALT_LENGTH, hash, 0, SessionKey.KEYSIZE_BYTES);
        byte[] pwHash = _context.keyGenerator().generateSessionKey(salt, DataHelper.getUTF8(pw)).getData();
        return DataHelper.eq(hash, pwHash);
    }
    
    /**
     *  Either plain or b64
     *
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @return the pw or null
     */
    public String get(String realm, String user) {
        String rv = getPlain(realm, user);
        if (rv != null)
            return rv;
        return getB64(realm, user);
    }

    /**
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @return the pw or null
     */
    public String getPlain(String realm, String user) {
        String pfx = realm;
        if (user != null && user.length() > 0)
            pfx += '.' + user;
        return _context.getProperty(pfx + PROP_PW);
    }

    /**
     *  @param realm e.g. i2cp, routerconsole, etc.
     *  @param user null or "" for no user, already trimmed
     *  @return the decoded pw or null
     */
    public String getB64(String realm, String user) {
        String pfx = realm;
        if (user != null && user.length() > 0)
            pfx += '.' + user;
        String b64 = _context.getProperty(pfx + PROP_B64);
        if (b64 == null)
            return null;
        return Base64.decodeToString(b64);
    }

    /**
     *  Straight MD5, no salt
     *  Will return the MD5 sum of "user:subrealm:pw", compatible with Jetty
     *  and RFC 2617.
     *
     *  @param subrealm to be used in creating the checksum
     *  @param user non-null, non-empty, already trimmed
     *  @param pw non-null, plain text, already trimmed
     *  @return lower-case hex with leading zeros, 32 chars, or null on error
     */
    public static String md5Hex(String subrealm, String user, String pw) {
        String fullpw = user + ':' + subrealm + ':' + pw;
        return md5Hex(fullpw);
    }

    /**
     *  Straight MD5, no salt
     *  Will return the MD5 sum of the data, compatible with Jetty
     *  and RFC 2617.
     *
     *  @param fullpw non-null, plain text, already trimmed
     *  @return lower-case hex with leading zeros, 32 chars, or null on error
     */
    public static String md5Hex(String fullpw) {
        try {
            byte[] data = fullpw.getBytes("ISO-8859-1");
            byte[] sum = md5Sum(data);
            if (sum != null)
                // adds leading zeros if necessary
                return DataHelper.toString(sum);
        } catch (UnsupportedEncodingException uee) {}
        return null;
    }

    /**
     *  Standard MD5 checksum
     *
     *  @param data non-null
     *  @return 16 bytes, or null on error
     */
    public static byte[] md5Sum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return md.digest();
        } catch (NoSuchAlgorithmException nsae) {}
        return null;
    }
}
