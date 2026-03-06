package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.markdownpreview.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * POST /api/delete — deletes a file or directory.
 * Request body: { "path": "..." }
 */
public class DeleteHandler extends BaseHandler {

    public DeleteHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        if (!HttpUtils.requireMethod(exchange, "POST")) return;

        String body = HttpUtils.readRequestBody(exchange, config.getMaxRequestBody());
        String path = JsonUtils.extractString(body, "path");

        fileService.delete(path);

        HttpUtils.sendSuccess(exchange, null);
    }
}
