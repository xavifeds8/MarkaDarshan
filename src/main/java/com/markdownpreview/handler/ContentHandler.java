package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * GET /api/content?path=... — returns the raw content of a file as JSON.
 */
public class ContentHandler extends BaseHandler {

    public ContentHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String relativePath = HttpUtils.getQueryParam(exchange, "path");
        FileService.FileContent content = fileService.readFile(relativePath);
        HttpUtils.sendJson(exchange, 200, content);
    }
}
