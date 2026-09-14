package io.binarycodes.mcp.frankfurter.client;

/** Error reported by the Frankfurter API (e.g. 422 "invalid currency: XXX"). */
public class FrankfurterException extends RuntimeException {

    private final int status;

    public FrankfurterException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
