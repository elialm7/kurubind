package com.roelias.kurubind.core;

import com.roelias.kurubind.metadata.FieldMetadata;

public interface Handler {

    Object handleWrite(Object javaValue, FieldMetadata fieldMeta);
    Object handleRead(Object dbValue, FieldMetadata fieldMeta);
}
