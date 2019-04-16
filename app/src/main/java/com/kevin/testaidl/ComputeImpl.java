package com.kevin.testaidl;

import android.os.RemoteException;

/**
 * Created by Kevin on 2019/4/11<br/>
 * Blog:https://blog.csdn.net/student9128<br/>
 * Describe:<br/>
 */
public class ComputeImpl extends ICompute.Stub {
    @Override
    public int add(int a, int b) throws RemoteException {
        return a+b;
    }
}
