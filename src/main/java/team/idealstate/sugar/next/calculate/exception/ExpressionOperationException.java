package team.idealstate.sugar.next.calculate.exception;

public class ExpressionOperationException extends ExpressionException {

    private static final long serialVersionUID = 4046007980833942987L;

    public ExpressionOperationException() {
        super();
    }

    public ExpressionOperationException(String message) {
        super(message);
    }

    public ExpressionOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpressionOperationException(Throwable cause) {
        super(cause);
    }

    protected ExpressionOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
