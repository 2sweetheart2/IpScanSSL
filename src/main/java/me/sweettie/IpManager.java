package me.sweettie;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class IpManager {

    private final int threadCount;
    private final List<List<String>> ipsForThreads;

    public IpManager(IPRange ipRange, int threadCount) {
        this.threadCount = threadCount;
        int ipForOneThred = ipRange.getIpCount() / threadCount;
        int countForLastThread = ipRange.getIpCount() - (ipForOneThred * threadCount);
        ipsForThreads = new ArrayList<>();
        int offset = 0;

        for (int i = 0; i < threadCount; i++) {
            if (i == threadCount - 1 && countForLastThread != 0)
                ipsForThreads.add(ipRange.getAddresses(offset, offset + ipForOneThred + countForLastThread));
            ipsForThreads.add(ipRange.getAddresses(offset, offset + ipForOneThred));
            offset += ipForOneThred;
        }

    }

    public Map<String, List<String>> getIpAndDns() {
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            scanIps(ipsForThreads.get(i), latch::countDown);
        }

        try {
            latch.await();
            return sortByIPAddress(ipAndDns);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    HashMap<String, List<String>> ipAndDns = new HashMap<>();

    private void scanIps(List<String> ips, IScanEnd scanEnd) {
        new Thread(() -> {
            for (String ip : ips) {
                scanIp(ip);
            }
            scanEnd.end();
        }).start();
    }

    private void scanIp(String domain) {
        try {
            URL url = new URL("https://" + domain + ":443");

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);

            connection.connect();
            Certificate[] certificates = connection.getServerCertificates();

            List<String> dns = new ArrayList<>();

            for (Certificate cert : certificates) {
                if (!(cert instanceof X509Certificate))
                    continue;

                Collection<List<?>> subjectAlternativeNames = ((X509Certificate) cert).getSubjectAlternativeNames();

                if (subjectAlternativeNames == null)
                    continue;

                for (List<?> san : subjectAlternativeNames)
                    dns.add(san.get(1).toString());

            }
            ipAndDns.put(domain, dns);
            connection.disconnect();

        } catch (Exception ignored) {
        }
    }

    public interface IScanEnd {
        void end();
    }

    static Map<String, List<String>> sortByIPAddress(Map<String, List<String>> unsortedMap) {
        TreeMap<String, List<String>> sortedMap = new TreeMap<>(new IPAddressComparator());
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    static class IPAddressComparator implements Comparator<String> {
        @Override
        public int compare(String ipAddress1, String ipAddress2) {
            String[] octets1 = ipAddress1.split("\\.");
            String[] octets2 = ipAddress2.split("\\.");
            for (int i = 0; i < 4; i++) {
                int octet1 = Integer.parseInt(octets1[i]);
                int octet2 = Integer.parseInt(octets2[i]);

                if (octet1 != octet2) {
                    return Integer.compare(octet1, octet2);
                }
            }
            return 0;
        }
    }


}
