package ysomap.common.exception;

public class PathNotFoundException extends BaseException {

    public PathNotFoundException(String message) {
        super(message + " path not exist");
    }
}
