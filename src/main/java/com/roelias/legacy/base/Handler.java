package com.roelias.legacy.base;

import com.roelias.legacy.metadata.FieldMetadata;

public interface Handler {

    Object handleWrite(Object javaValue, FieldMetadata fieldMeta);

    Object handleRead(Object dbValue, FieldMetadata fieldMeta);
}
