package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.service.PlaywrightService;
import com.example.checkscamv2.util.FileCacheUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

        // 1. Check cache trước
        try {
            String cacheFileName = FileCacheUtil.getCacheFilename(normalizedUrl);
            if (FileCacheUtil.isFileValid(cacheFileName)) {
                log.info("Cache hit cho URL: {}", normalizedUrl);
                return "/" + cacheFileName;
            }
        } catch (Exception e) {
            log.warn("Lỗi khi kiểm tra cache: {}", e.getMessage());
        }

        log.info("Bắt đầu chụp ảnh cho URL: {}", normalizedUrl);
        long startTime = System.currentTimeMillis();

        try (Playwright playwright = Playwright.create()) {
            // 2. Launch browser với flags tối ưu và timeout ngắn
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(8000)
                    .setArgs(Arrays.asList(
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-web-security",
                        "--disable-features=VizDisplayCompositor",
                        "--disable-background-timer-throttling",
                        "--disable-backgrounding-occluded-windows",
                        "--disable-renderer-backgrounding",
                        "--disable-field-trial-config",
                        "--disable-ipc-flooding-protection",
                        "--disable-hang-monitor",
                        "--disable-prompt-on-repost",
                        "--disable-client-side-phishing-detection",
                        "--disable-component-extensions-with-background-pages",
                        "--disable-default-apps",
                        "--disable-extensions",
                        "--disable-sync",
                        "--disable-translate",
                        "--hide-scrollbars",
                        "--mute-audio",
                        "--no-first-run",
                        "--safebrowsing-disable-auto-update",
                        "--disable-blink-features=AutomationControlled",
                        "--disable-plugins"
                    ))
            );

            // 3. Context tối ưu
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(viewportWidth, viewportHeight);

            BrowserContext context = browser.newContext(contextOptions);
            Page page = context.newPage();

            // 4. Navigation với timeout ngắn
            page.navigate(normalizedUrl,
                new Page.NavigateOptions()
                    .setTimeout(7000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            // 5. Giảm thời gian chờ sau khi load
            page.waitForTimeout(200);

            // 6. Auto-scroll tối ưu
            autoScrollOptimized(page);

            // 7. Screenshot JPEG tối ưu tốc độ
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true)
                .setType(ScreenshotType.JPEG)
                .setQuality(75)
            );

            if (screenshot == null) {
                log.warn("Screenshot null cho URL: {}", normalizedUrl);
                return handleDefaultScreenshot(normalizedUrl);
            }

            String cacheFileName = FileCacheUtil.getCacheFilename(normalizedUrl);
            File cacheFile = new File(cacheFileName);
            java.nio.file.Files.write(cacheFile.toPath(), screenshot);

            long endTime = System.currentTimeMillis();
            log.info("Chụp ảnh hoàn thành cho URL: {} trong {}ms", normalizedUrl, endTime - startTime);

            return "/" + cacheFileName;

        } catch (Exception e) {
            log.error("Lỗi khi chụp ảnh URL {}: {}", normalizedUrl, e.getMessage());
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

    private void autoScrollOptimized(Page page) {
        // Auto-scroll tối đa 8 lần, mỗi lần 600px, timeout tổng 2s
        page.evaluate("async () => {" +
            "   await new Promise((resolve) => {" +
            "       const maxTime = 2000;" +
            "       const startTime = Date.now();" +
            "       let totalHeight = 0;" +
            "       const distance = 600;" +
            "       const delay = 20;" +
            "       const maxScrolls = 8;" +
            "       let scrollCount = 0;" +
            "       const timer = setInterval(() => {" +
            "           if (Date.now() - startTime > maxTime || scrollCount >= maxScrolls) {" +
            "               clearInterval(timer);" +
            "               resolve();" +
            "               return;" +
            "           }" +
            "           window.scrollBy(0, distance);" +
            "           totalHeight += distance;" +
            "           scrollCount++;" +
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
