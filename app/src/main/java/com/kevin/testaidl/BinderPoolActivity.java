package com.kevin.testaidl;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Created by Kevin on 2019/4/11<br/>
 * Blog:https://blog.csdn.net/student9128<br/>
 * Describe:<br/>
 */
public class BinderPoolActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private ISecurityCenter mSecurityCenter;
    private ICompute mCompute;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        doWork();
                    }
                }.start();
            }
        });
    }

    private void doWork() {
        BinderPool binderPool = BinderPool.getInstance(this);
        IBinder securityBinder = binderPool.queryBinder(BinderPool.BINDER_SECURITY_CENTER);
        mSecurityCenter = SecurityCenterImpl.asInterface(securityBinder);
        try {
            String encryptStr = mSecurityCenter.encrypt("Hello Android");
            Log.d(TAG, "encryptStr=" + encryptStr);
            String decryptStr = mSecurityCenter.decrypt(encryptStr);
            Log.d(TAG, "decryptStr=" + decryptStr);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "execute compute method");
        IBinder computeBinder = binderPool.queryBinder(BinderPool.BINDER_COMPUTE);
        mCompute = ComputeImpl.asInterface(computeBinder);
        try {
            int addResult = mCompute.add(2, 3);
            Log.d(TAG, "2+3=" + addResult);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
