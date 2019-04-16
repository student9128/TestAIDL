package com.kevin.testaidl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.*;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private IBookManager mBookManager = null;
    private boolean mBound = false;
    private List<Book> mBooks;
    private final String TAG = getClass().getSimpleName();
    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;

    int count = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG, "receive new book: " + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };
    private DeathRecipient deathRecipient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                count++;
//                if (count / 2 == 0) {

//                } else {
//                    addBook();
//                }
                //开子线程防止服务器端进行耗时操作，app出现ANR
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mBookManager != null) {
                                List<Book> bookList = mBookManager.getBookList();
                                for (Book b : bookList) {
                                    Log.d(TAG, "已存在的书本：名字：" + b.getName() + "价格：" + b.getPrice());
                                }
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        Intent intent = new Intent(this, AIDLService.class);
        deathRecipient = new DeathRecipient() {

            @Override
            public void binderDied() {
                if (mBookManager == null) {
                    return;
                }
                mBookManager.asBinder().unlinkToDeath(deathRecipient, 0);
                mBookManager = null;
                attemptToBindService();//Binder die后，重新绑定服务
            }
        };

        bindService(intent, mSC, Context.BIND_AUTO_CREATE);
    }

    public void addBook() {
        if (!mBound) {
            attemptToBindService();
            Toast.makeText(this, "当前与服务的处于未连接状态，正在尝试连", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBookManager == null)
            return;
        Book book = new Book();
        book.setName("App研发录");
        book.setPrice(30);
        try {
            mBookManager.addBook(book);
            Log.d(getLocalClassName(), "添加的课本：name:" + book.getName() + "\tprice:" + book.getPrice());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void attemptToBindService() {
        Intent intent = new Intent();
        intent.setAction("com.kevin.aidl");
        intent.setPackage("com.kevin.testaidl");
        bindService(intent, mSC, Context.BIND_AUTO_CREATE);
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (!mBound) {
//            attemptToBindService();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mBound) {
//            unbindService(mSC);
//            mBound = false;
//        }
//    }


    @Override
    protected void onDestroy() {
        if (mBookManager != null && mBookManager.asBinder().isBinderAlive()) {
            try {
                Log.i(TAG, "unregister listner:" + mOnNewBookArrivedListener);
                mBookManager.unRegisterListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mSC);
        super.onDestroy();
    }

    private ServiceConnection mSC = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(getLocalClassName(), "service connected");
            try {
                service.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBookManager = IBookManager.Stub.asInterface(service);
            mBound = true;
            if (mBookManager != null) {
                try {
                    Book book = new Book();
                    book.setPrice(99);
                    book.setName("Test");
                    mBookManager.addBook(book);
                    mBooks = mBookManager.getBookList();
                    for (Book b : mBooks) {

                        Log.d(TAG, "已存在的书本：名字：" + b.getName() + "价格：" + b.getPrice());
                    }
                    mBookManager.registerListener(mOnNewBookArrivedListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(getLocalClassName(), "service disconnected");
            mBound = false;
        }
    };
    //运行中Binder线程池中，不能在它里面访问UI相关的内容，如果要访问，使用Handler切换到UI线程
    // Binder可能会意外死亡，使用DeathRecipient监听，当Binder死亡是binderDied方法中重新连接服务
    //或者在onServiceDisconnected重新连接服务
    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book newBook) throws RemoteException {
            //
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, newBook).sendToTarget();
        }
    };
}
