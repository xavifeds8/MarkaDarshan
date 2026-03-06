package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.markdownpreview.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /api/rename — renames a file or directory.
 * Request body: { "oldPath": "...", "newName": "..." }
 */
public class RenameHandler extends BaseHandler {

    public RenameHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        if (!HttpUtils.requireMethod(exchange, "POST")) return;

        String body = HttpUtils.readRequestBody(exchange, config.getMaxRequestBody());
        String oldPath = JsonUtils.extractString(body, "oldPath");
        String newName = JsonUtils.extractString(body, "newName");

        String newPath = fileService.rename(oldPath, newName);

        HttpUtils.sendSuccess(exchange, Map.of("newPath", newPath));
    }
}
