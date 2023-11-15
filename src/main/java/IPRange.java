import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPRange {

    private final String startIP;
    private final List<InetAddress> ips = new ArrayList<>();

    public IPRange(String startIP) {
        this.startIP = startIP;
        getIps();
    }

    private void getIps() {
        String[] parts = this.startIP.split("/");
        String ipAddress = parts[0];
        int subnetMask = Integer.parseInt(parts[1]);

        try {
            InetAddress startAddress = InetAddress.getByName(ipAddress);

            byte[] startBytes = startAddress.getAddress();
            int numberOfAddresses = 1 << (32 - subnetMask);

            for (int i = 0; i < numberOfAddresses; i++) {
                int finalIp = bytesToInt(startBytes) + i;

                InetAddress currentAddress = InetAddress.getByAddress(intToBytes(finalIp));
                ips.add(currentAddress);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public int getIpCount() {
        return this.ips.size();
    }

    public List<String> getAddresses(int from, int to) {
        List<String> address = new ArrayList<>();
        for (int i = from; i < to; i++) {
            address.add(ips.get(i).getHostAddress());
        }
        return address;
    }

    public static int bytesToInt(byte[] bytes) {
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }

    public static byte[] intToBytes(int value) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return result;
    }
}