package Shared;

/**
 * Utility class to read encrypted PEM files and generate a
 * SSL Socket Factory based on the provided certificates.
 * 
 * Note:
 * Java use jks (Java KeyStore) format, but openssl usual use pem format.
 * We can convert it by keytool (jdk build-in tool), or use BouncyCastle
 * library to handle pem.
 * 
 * The original code is by Sharon Asher (link below). I have modified
 * it to use a newer version of the BouncyCastle Library (v1.52)
 * 
 * Reference - https://gist.github.com/sharonbn/4104301"
 * Also - https://gist.github.com/10gic/b204dd9016f0b348797d94861d7962b9 - Accessed 07.11.2023
 */

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class SslUtil {
    /**
     * Create an SslSocketFactory using PEM encrypted certificate files. Mutual SSL Authentication is supported.
     *
     * @param caCrtFile
     *            CA certificate of remote server.
     * @param crtFile
     *            certificate file of client.
     * @param keyFile
     *            key file of client.
     * @param password
     *            password of key file.
     * @return
     */
    public static SSLSocketFactory getSSLSocketFactory(final String caCrtFile, final String crtFile,
            final String keyFile) {
        return getSSLSocketFactory(caCrtFile, crtFile, keyFile, "");
    }

    public static SSLSocketFactory getSSLSocketFactory(final String caCrtFile, final String crtFile,
            final String keyFile, final String password) {
        return getSSLContext(caCrtFile, crtFile, keyFile, password).getSocketFactory();
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(final String caCrtFile, final String crtFile,
            final String keyFile) {
        return getSSLServerSocketFactory(caCrtFile, crtFile, keyFile, "");
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(final String caCrtFile, final String crtFile,
            final String keyFile, final String password) {
        return getSSLContext(caCrtFile, crtFile, keyFile, password).getServerSocketFactory();
    }

    public static SSLContext getSSLContext(final String caCrtFile, final String crtFile,
            final String keyFile, final String password) {
        try {

            /**
             * Add BouncyCastle as a Security Provider
             */
            Security.addProvider(new BouncyCastleProvider());

            JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");

            /**
             * Load Certificate Authority (CA) certificate
             */
            PEMParser reader = new PEMParser(new FileReader(caCrtFile));
            X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
            reader.close();

            X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);

            /**
             * Load client certificate
             */
            reader = new PEMParser(new FileReader(crtFile));
            X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
            reader.close();

            X509Certificate cert = certificateConverter.getCertificate(certHolder);

            /**
             * Load client private key
             */
            reader = new PEMParser(new FileReader(keyFile));
            Object keyObject = reader.readObject();
            reader.close();

            JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

            PrivateKey privateKey = null;

            if (keyObject instanceof PEMEncryptedKeyPair) {
                PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                KeyPair keyPair = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
                privateKey = keyPair.getPrivate();
            } else if (keyObject instanceof PEMKeyPair) {
                KeyPair keyPair = keyConverter.getKeyPair((PEMKeyPair) keyObject);
                privateKey = keyPair.getPrivate();
            } else if (keyObject instanceof PrivateKeyInfo) {
                privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) keyObject);
            } else {
                throw new Exception(String.format("Unsported type of keyFile %s", keyFile));
            }

            /**
             * CA certificate is used to authenticate server
             */
            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            caKeyStore.setCertificateEntry("ca-certificate", caCert);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);

            /**
             * Client key and certificates are sent to server so it can authenticate the client. (server send
             * CertificateRequest message in TLS handshake step).
             */
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(null, null);
            clientKeyStore.setCertificateEntry("certificate", cert);
            clientKeyStore.setKeyEntry("private-key", privateKey, password.toCharArray(), new Certificate[] { cert });

            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, password.toCharArray());

            /**
             * Create SSL socket factory
             */
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return context;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}