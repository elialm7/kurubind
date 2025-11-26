package com.roelias.kurubind.base;

import com.roelias.kurubind.metadata.FieldMetadata;

public interface Handler {

    Object handleWrite(Object javaValue, FieldMetadata fieldMeta);

    Object handleRead(Object dbValue, FieldMetadata fieldMeta);
}
