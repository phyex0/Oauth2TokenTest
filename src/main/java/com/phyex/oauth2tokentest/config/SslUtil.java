package com.phyex.oauth2tokentest.config;

import lombok.SneakyThrows;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SslUtil {

    @SneakyThrows
    public static void init() {
        String crtFilePath = "src/main/docker/server.crt"; // Sertifika yolun

        // 1. Sertifikayı Yükle
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (FileInputStream fis = new FileInputStream(crtFilePath)) {
            caCert = (X509Certificate) cf.generateCertificate(fis);
        }

        // 2. Boş bir KeyStore oluştur ve sertifikayı ekle
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("global-custom-server", caCert);

        // 3. TrustManagerFactory init et
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 4. SSLContext oluştur ve DEFAULT olarak ata
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // CRITICAL: Java'nın varsayılan SSLContext'ini kendi oluşturduğumuzla eziyoruz
        SSLContext.setDefault(sslContext);

        // 5. Global Hostname Verifier (Domain/IP uyuşmazlığını ezmek için)
        // Hangi domain gelirse gelsin 'true' dönerek doğrulamayı bypass eder
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println(">>> Global SSLContext ve HostnameVerifier başarıyla uygulandı! <<<");
    }
}
