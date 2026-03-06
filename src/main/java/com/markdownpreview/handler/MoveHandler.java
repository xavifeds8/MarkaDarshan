package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.markdownpreview.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /api/move — moves a file or directory into a target directory.
 * Request body: { "sourcePath": "...", "targetPath": "..." }
 */
public class MoveHandler extends BaseHandler {

    public MoveHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        if (!HttpUtils.requireMethod(exchange, "POST")) return;

        String body = HttpUtils.readRequestBody(exchange, config.getMaxRequestBody());
        String sourcePath = JsonUtils.extractString(body, "sourcePath");
        String targetPath = JsonUtils.extractString(body, "targetPath");

        String newPath = fileService.move(sourcePath, targetPath);

        HttpUtils.sendSuccess(exchange, Map.of("newPath", newPath));
    }
}
