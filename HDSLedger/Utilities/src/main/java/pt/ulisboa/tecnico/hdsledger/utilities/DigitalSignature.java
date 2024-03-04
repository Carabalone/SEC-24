package pt.ulisboa.tecnico.hdsledger.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class DigitalSignature {

    private static byte[] readFile(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        byte[] content = new byte[fis.available()];
        fis.read(content);
        fis.close();

        return content;
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String key) {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PublicKey readPublicKey(String publicKeyPath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] pubEncoded = readFile(publicKeyPath);
        // Convert PEM to X.509 format
        String publicKeyPEM = new String(pubEncoded);
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(pubSpec);

        return publicKey;
    }

    public static PrivateKey readPrivateKey(String privateKeyPath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] privEncoded = readFile(privateKeyPath);
        // Convert PEM to PKCS8 format
        String privateKeyPEM = new String(privEncoded);
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return privateKey;
    }

    public static byte[] encrypt(byte[] data, String pathToPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        PrivateKey privateKey = readPrivateKey(pathToPrivateKey);
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedData = encryptCipher.doFinal(data);

        return encryptedData;
    }

    public static byte[] decrypt(byte[] data, String pathToPublicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        System.out.println("DECRYPT: ENTROU");
        PublicKey publicKey = readPublicKey(pathToPublicKey);
        System.out.println("DECRYPT: LEU CHAVE PUBLICA");
        Cipher decryptCipher = Cipher.getInstance("RSA");
        System.out.println("DECRYPT: INSTANCIOU CIPHER");
        decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);
        System.out.println("DECRYPT: INICIALIZOU CIPHER");
        byte[] decryptedData = decryptCipher.doFinal(data);
        System.out.println("DECRYPT: DECRYPTOU");

        return decryptedData;
    }

    public static String digest(String data) throws NoSuchAlgorithmException {
        byte[] dataBytes = data.getBytes();
        final String DIGEST_ALGO = "SHA-256";
        MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);
        messageDigest.update(dataBytes);
        byte[] digestBytes = messageDigest.digest();

        return Base64.getEncoder().encodeToString(digestBytes);
    }

    public static String sign(String data, String pathToPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {

        String digest = digest(data);
        System.out.printf("SIGN: Digest: %s\n", digest);

        byte[] digestEncrypted = encrypt(digest.getBytes(), pathToPrivateKey);

        String digestBase64 = Base64.getEncoder().encodeToString(digestEncrypted);
        System.out.printf("SIGN: DigestBase64 / Signature: %s\n", digestBase64);

        return digestBase64;
    }

    public static boolean verifySignature(String data, String signature, String pathToPublicKey) {
        try {
            String hash = digest(data);
            System.out.printf("VERIFY: Hash: %s\n", hash);

            System.out.println("VERIFY: Signature: " + signature);
            System.out.printf("VERIFY: Signature bytes: %s\n", Base64.getDecoder().decode(signature));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            System.out.println("VERIFY: IDK bytes: " + signatureBytes);

            String decryptedHash = new String(decrypt(Base64.getDecoder().decode(signature), pathToPublicKey));
            System.out.println("VERIFY: Decrypted Hash: " + decryptedHash);
            System.out.printf("Hash equals Decrypted Hash: %s\n", hash.equals(decryptedHash));
            return hash.equals(decryptedHash);
        } catch (Exception e) {
            return false;
        }
    }
}