package com.box.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class BoxAPIResponse {
    private static final Logger LOGGER = Logger.getLogger(BoxFolder.class.getName());

    private final HttpURLConnection connection;
    private InputStream inputStream;
    private int responseCode;
    private String bodyString;

    public BoxAPIResponse(HttpURLConnection connection) {
        this.connection = connection;
        this.inputStream = null;

        try {
            this.responseCode = this.connection.getResponseCode();
        } catch (IOException e) {
            throw new BoxAPIException("Couldn't connect to the Box API due to a network error.", e);
        }

        if (!this.isSuccess()) {
            throw new BoxAPIException("The API returned an error code: " + this.responseCode);
        }

        this.logResponse();
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public InputStream getBody() {
        if (this.inputStream == null) {
            String contentEncoding = this.connection.getContentEncoding();
            try {
                if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                    this.inputStream = new GZIPInputStream(this.connection.getInputStream());
                } else {
                    this.inputStream = this.connection.getInputStream();
                }
            } catch (IOException e) {
                throw new BoxAPIException("Couldn't connect to the Box API due to a network error.", e);
            }
        }
        return this.inputStream;
    }

    public void disconnect() {
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                throw new BoxAPIException("Couldn't connect to the Box API due to a network error.", e);
            }
        }
        this.connection.disconnect();
    }

    @Override
    public String toString() {
        Map<String, List<String>> headers = this.connection.getHeaderFields();
        StringBuilder builder = new StringBuilder();
        builder.append(this.connection.getRequestMethod());
        builder.append(' ');
        builder.append(this.connection.getURL().toString());
        builder.append(System.lineSeparator());
        builder.append(headers.get(null).get(0));
        builder.append(System.lineSeparator());

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }

            builder.append(key);
            builder.append(": ");
            for (String value : entry.getValue()) {
                builder.append(value);
                builder.append(", ");
            }

            builder.delete(builder.length() - 2, builder.length());
            builder.append(System.lineSeparator());
        }

        String bodyString = this.bodyToString();
        if (bodyString != null) {
            builder.append(System.lineSeparator());
            builder.append(bodyString);
        }

        return builder.toString();
    }

    protected String bodyToString() {
        return null;
    }

    protected HttpURLConnection getConnection() {
        return this.connection;
    }

    private void logResponse() {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, this.toString());
        }
    }

    private boolean isSuccess() {
        return this.responseCode >= 200 && this.responseCode < 300;
    }
}
