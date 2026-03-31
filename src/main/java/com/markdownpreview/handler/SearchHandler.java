package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.markdownpreview.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /api/search — searches for text across all files in a directory.
 * Request body: { "path": "...", "query": "..." }
 */
public class SearchHandler extends BaseHandler {

    public SearchHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        if (!HttpUtils.requireMethod(exchange, "POST")) return;

        String body = HttpUtils.readRequestBody(exchange, config.getMaxRequestBody());
        String path = JsonUtils.extractString(body, "path");
        String query = JsonUtils.extractString(body, "query");

        var results = fileService.searchFiles(path, query);

        HttpUtils.sendSuccess(exchange, Map.of("results", results));
    }
}
