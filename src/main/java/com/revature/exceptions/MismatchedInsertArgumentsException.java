package com.revature.exceptions;

public class MismatchedInsertArgumentsException extends RuntimeException {
    public MismatchedInsertArgumentsException() {
        super("The amount of arguments in the insert statement don't match the amount of selected attributes/columns");
    }
}
