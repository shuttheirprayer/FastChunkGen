package com.misanthropy.fastchunkgen.threading.worldgen.common;

import com.ibm.asyncutil.locks.AsyncLock;

public interface IWorldGenLockable {

    AsyncLock getWorldGenSingleThreadedLock();

}
