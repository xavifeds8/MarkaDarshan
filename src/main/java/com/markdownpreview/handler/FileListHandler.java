package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * GET /api/files?path=... — returns JSON listing of files/directories.
 */
public class FileListHandler extends BaseHandler {

    public FileListHandler(FileService fileService, AppConfig config) {
        super(fileService, config);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String relativePath = HttpUtils.getQueryParam(exchange, "path");
        FileService.DirectoryListing listing = fileService.listDirectory(relativePath);
        HttpUtils.sendJson(exchange, 200, listing);
    }
}
