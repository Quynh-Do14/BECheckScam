package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.service.PlaywrightService;
import com.example.checkscamv2.util.FileCacheUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class PlaywrightServiceImpl implements PlaywrightService {

    @Value("${playwright.timeout:1000}")
    private long timeout;

    @Value("${playwright.default-protocol:https://}")
    private String defaultProtocol;

    @Value("${playwright.viewport.width:720}")
    private int viewportWidth;

    @Value("${playwright.viewport.height:1080}")
    private int viewportHeight;

    private static final byte[] DEFAULT_ERROR_SCREENSHOT;
    static {
        DEFAULT_ERROR_SCREENSHOT = loadDefaultErrorScreenshot();
        FileCacheUtil.ensureCacheDirExists();
    }

    @Override
    public String captureScreenshotAsUrl(String url) {
        String normalizedUrl = normalizeUrl(url);
        String cacheFileName = null;

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setTimeout(30000)
            );

            // Dùng context để set user-agent và viewport
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(viewportWidth, viewportHeight);

            BrowserContext context = browser.newContext(contextOptions);
            Page page = context.newPage();
            page.navigate(normalizedUrl,
                    new Page.NavigateOptions()
                            .setTimeout(20000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            page.waitForTimeout(timeout / 2);
            autoScroll(page);
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            if (screenshot == null) {
                return handleDefaultScreenshot(normalizedUrl);
            }

            cacheFileName = FileCacheUtil.getCacheFilename(normalizedUrl);
            File cacheFile = new File(cacheFileName);
            java.nio.file.Files.write(cacheFile.toPath(), screenshot);

            if (!FileCacheUtil.isFileValid(cacheFileName)) {
                java.nio.file.Files.write(cacheFile.toPath(), screenshot);
            }
            return "/" + cacheFileName;

        } catch (Exception e) {
            return handleDefaultScreenshot(normalizedUrl);
        }
    }

    private String handleDefaultScreenshot(String normalizedUrl) {
        if (DEFAULT_ERROR_SCREENSHOT != null) {
            try {
                String cacheFileName = FileCacheUtil.getCacheFilename(normalizedUrl + "_default");
                File cacheFile = new File(cacheFileName);
                java.nio.file.Files.write(cacheFile.toPath(), DEFAULT_ERROR_SCREENSHOT);
                return "/" + cacheFileName;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL không được để trống");
        }
        url = url.trim();
        if (!url.matches("^(https?://).*")) {
            url = defaultProtocol + url;
        }
        if (!url.matches("^(https?://)([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(/.*)?$")) {
            throw new IllegalArgumentException("URL không hợp lệ: " + url);
        }
        return url;
    }

    private void autoScroll(Page page) {
        page.evaluate("async () => {" +
                "   await new Promise((resolve) => {" +
                "       let totalHeight = 0;" +
                "       const distance = 300;" +
                "       const delay = 0;" +
                "       const timer = setInterval(() => {" +
                "           window.scrollBy(0, distance);" +
                "           totalHeight += distance;" +
                "           if (totalHeight >= document.body.scrollHeight) {" +
                "               clearInterval(timer);" +
                "               resolve();" +
                "           }" +
                "       }, delay);" +
                "   });" +
                "}");
    }

    private static byte[] loadDefaultErrorScreenshot() {
        try {
            ClassPathResource resource = new ClassPathResource("static/page_unavailable.jpeg");
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed to load default error screenshot from file: {}", e.getMessage(), e);
            return null;
        }
    }
}
