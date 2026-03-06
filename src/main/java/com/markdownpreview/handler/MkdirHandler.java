package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.markdownpreview.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /api/mkdir — creates a new directory.
 * Request body: { "path": "..." }
 */
public class MkdirHandler extends BaseHandler {

    public MkdirHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        if (!HttpUtils.requireMethod(exchange, "POST")) return;

        String body = HttpUtils.readRequestBody(exchange, config.getMaxRequestBody());
        String path = JsonUtils.extractString(body, "path");

        fileService.createDirectory(path);

        HttpUtils.sendSuccess(exchange, Map.of("path", path));
    }
}
