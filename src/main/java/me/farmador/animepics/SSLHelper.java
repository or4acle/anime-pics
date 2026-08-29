package me.farmador.animepics;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class SSLHelper {

	private static SSLSocketFactory sslSocketFactory;
	private static HostnameVerifier trustAllHostnameVerifier;

	private SSLHelper() {
	}

	public static synchronized void configure(HttpURLConnection conn) {
		if (conn instanceof HttpsURLConnection httpsConn) {
			try {
				if (sslSocketFactory == null) {
					TrustManager[] trustAllCerts = new TrustManager[]{
							new X509TrustManager() {
								public X509Certificate[] getAcceptedIssuers() {
									return new X509Certificate[0];
								}

								public void checkClientTrusted(X509Certificate[] certs, String authType) {
								}

								public void checkServerTrusted(X509Certificate[] certs, String authType) {
								}
							}
					};

					SSLContext sc = SSLContext.getInstance("TLS");
					sc.init(null, trustAllCerts, new SecureRandom());
					sslSocketFactory = sc.getSocketFactory();
					trustAllHostnameVerifier = (hostname, session) -> true;
				}

				httpsConn.setSSLSocketFactory(sslSocketFactory);
				httpsConn.setHostnameVerifier(trustAllHostnameVerifier);
			} catch (Exception ignored) {
			}
		}
	}
}
