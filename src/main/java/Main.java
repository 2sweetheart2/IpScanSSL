import io.javalin.Javalin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.defaultContentType = "text/html; charset=UTF-8";
        }).start(7000);
        app.get("/", ctx -> ctx.html(getHtmlForm()));

        app.post("/process", ctx -> {
            String startIP = ctx.formParam("startIP");
            int threadCount = Integer.parseInt(ctx.formParam("threadCount"));
            IPRange ipRange = new IPRange(startIP);
            System.out.println("START SCAN " + ipRange.getIpCount() + " ips");
            IpManager ipManager = new IpManager(ipRange, threadCount);
            Map<String, List<String>> data = ipManager.getIpAndDns();
            saveToFile(data);
            ctx.html(getEndForm(data));
        });
    }

    private static void saveToFile(Map<String, List<String>> data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("ipAndDnsInRange"))) {
            for(Map.Entry<String,List<String>> entry : data.entrySet()){
                if(entry.getValue().size()==0)
                    continue;
                writer.write("IP: " + entry.getKey());
                writer.newLine();
                for(String dns : entry.getValue()){
                    writer.write("- " +dns);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getEndForm(Map<String, List<String>> data) {
        String s = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>IP to Domain Converter</title>\n" +
                "</head>\n" +
                "<body><h1>What dns we found in your ip range</h1>\n<p>";
        for (Map.Entry<String, List<String>> ip : data.entrySet()) {
            if (ip.getValue().size() == 0)
                continue;
            s += "<br>" + ip.getKey();
            for (String dns : ip.getValue())
                s += "<br>- " + dns;
        }
        s += "</p></body></html>";
        return s;
    }

    private static String getHtmlForm() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>IP to Domain Converter</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>IPs to Domain Converter</h1>\n" +
                "    <form action=\"/process\" method=\"post\">\n" +
                "        <label for=\"startIP\">Enter a range of masked IP addresses (for example, 192.168.1.0/24):</label>\n" +
                "        <input type=\"text\" id=\"startIP\" name=\"startIP\" required><br>\n" +
                "\n" +
                "        <label for=\"threadCount\">Thread Count:</label>\n" +
                "        <input type=\"number\" id=\"threadCount\" name=\"threadCount\" required><br>\n" +
                "\n" +
                "        <button type=\"submit\">Submit</button>\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>";
    }


}
