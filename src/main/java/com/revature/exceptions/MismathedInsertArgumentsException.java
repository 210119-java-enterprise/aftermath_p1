package com.revature.exceptions;

public class MismathedInsertArgumentsException extends RuntimeException {
    public MismathedInsertArgumentsException() {
        super("The amount of arguments in the insert statement don't match the amount of selected attributes/columns");
    }
}
