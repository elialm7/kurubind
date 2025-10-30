package com.roelias.kurubind.core;

public interface Handler {

    Object handleWrite(Object javaValue);
    Object handleRead(Object dbValue);
}
