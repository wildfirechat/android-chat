package cn.wildfirechat.client;

/**
 * Created by heavyrainlee on 17/02/2018.
 */

public class NotInitializedExecption extends RuntimeException {
    public NotInitializedExecption() {
        super("Not init!!!");
    }
}
