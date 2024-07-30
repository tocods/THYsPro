package api;

public class Result {
    public static int SUCCESS_CODE=200;
    public static int ERROR_CODE=500;

    private int code;
    private String message;
    private Object data;

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public boolean ifSuccess() {
        return code == SUCCESS_CODE;
    }

    public static Result success(String message) {
        return new Result(SUCCESS_CODE, message, null);
    }

    public boolean ifError() {
        return code == ERROR_CODE;
    }

    public static Result error(Object data, String message) {
        return new Result(ERROR_CODE, message, data);
    }

    public static Result error(Object data) {
        return new Result(ERROR_CODE, "error", data);
    }
}
