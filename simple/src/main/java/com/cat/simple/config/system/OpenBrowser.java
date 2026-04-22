package com.cat.simple.config.system;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Component
public class OpenBrowser {


    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.context-path}")
    private String contextPath;



    public void open() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String url = "http://localhost:"+port+contextPath+"/doc.html";

            try {
                // 检查Desktop API是否支持
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    // 备用方案（针对Linux等环境）
                    openBrowserFallback(url);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void openBrowserFallback(String url) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        if (os.contains("win")) {
            // Windows
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.contains("mac")) {
            // macOS
            rt.exec(new String[] { "open", url });
        } else if (os.contains("nix") || os.contains("nux")) {
            // Linux/Unix
            String[] browsers = { "xdg-open", "gnome-open", "kde-open", "x-www-browser", "firefox", "chrome", "chromium" };
            for (String browser : browsers) {
                if (rt.exec(new String[] { "which", browser }).waitFor() == 0) {
                    rt.exec(new String[] { browser, url });
                    break;
                }
            }
        }
    }

}
