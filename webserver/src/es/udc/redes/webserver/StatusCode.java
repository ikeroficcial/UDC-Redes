package es.udc.redes.webserver;

public enum StatusCode {
    OK("200 OK\n"),
    NOT_MODIFIED("304 Not Modified\n"),
    BAD_REQUEST("400 Bad Request\n"),
    FORBIDDEN("403 Forbidden\n"),
    NOT_FOUND("404 Not Found\n");

    private String status;

    StatusCode(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}