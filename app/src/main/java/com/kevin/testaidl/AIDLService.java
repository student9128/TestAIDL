package com.kevin.testaidl;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Kevin on 2019/3/21<br/>
 * Blog:https://blog.csdn.net/student9128<br/>
 * Describe:<br/>
 * 服务端
 */
public class AIDLService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    //    private List<Book> mBooks = new ArrayList<>();//aidl只支持arrayList
    private CopyOnWriteArrayList<Book> mBooks = new CopyOnWriteArrayList<>();//保证线程同步
    //使用RemoteCallbackList存储 listener，这样可以解注册
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();
    private AtomicBoolean mIsServiceDestroyed = new AtomicBoolean(false);

    private IBookManager.Stub mBookManager = new IBookManager.Stub() {

        @Override
        public void addBook(Book b) throws RemoteException {
            synchronized (this) {
                if (mBooks == null) {
//                    mBooks = new ArrayList<Book>();
                    mBooks = new CopyOnWriteArrayList<>();
                }
                if (b == null) {
                    Log.e(TAG, "Book is null in In");
                    b = new Book();
                }
                b.setPrice(123);
                if (!mBooks.contains(b)) {
                    mBooks.add(b);
                }
                //打印mBooks列表，观察客户端传过来的值
                StringBuilder sb = new StringBuilder();
                for (Book boo : mBooks) {
                    sb.append("book: name is " + boo.getName() + ",price is" + boo.getPrice());
                }
                Log.e(TAG, "invoking addBooks() method , now the list is : " + sb.toString());
            }
        }

        @Override
        public List<Book> getBookList() throws RemoteException {
//            SystemClock.sleep(10000);
            synchronized (this) {
                if (mBooks != null) {
                    return mBooks;
                }
            }
            return new CopyOnWriteArrayList<>();
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
//            if (!mListenerList.contains(listener)) {
//                mListenerList.add(listener);
//            }else {
//                Log.d(TAG, "listener already exists...");
//            }
//            Log.d(TAG, "registerListener,size:" + mListenerList.size());
            mListenerList.register(listener);
        }

        @Override
        public void unRegisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
//            if (mListenerList.contains(listener)) {
//                mListenerList.remove(listener);
//            } else {
//                Log.d(TAG, "not found the listener,cannot unregister");
//            }
//            Log.d(TAG, "unRegisterListener,current size ：" + mListenerList.size());
            mListenerList.unregister(listener);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            //验证权限
            int i = checkCallingOrSelfPermission("com.kevin.testaidl.permission.ACCESS_BOOK_SERVICE");
            if (i == PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, "权限不被允许");
                return false;
            }
            //验证包名
            String packageName = null;
            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
            if (packages != null && packages.length > 0) {
                packageName = packages[0];
            }
            if (!packageName.startsWith("com.kevin")) {
                Log.i(TAG, "包名不对");
                return false;
            }
            return super.onTransact(code, data, reply, flags);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Book book = new Book();
        book.setName("Android ADIL 测试");
        book.setPrice(33);
        mBooks.add(book);
        Log.w(TAG, "onCreate");
        new Thread(new ServiceWorker()).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        //在这里校验权限，如果客户端没有声明权限会一直连接不上，但是不知道为什么，不会有提示
//        int i = checkCallingOrSelfPermission("com.kevin.testaidl.permission.ACCESS_BOOK_SERVICE");
//        if (i == PackageManager.PERMISSION_DENIED) {
//            return null;
//        }
        Log.i(TAG, String.format("on bind,intent=%s", intent.toString()));
        return mBookManager;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed.set(true);
        super.onDestroy();
    }

    private void onNewBookArrived(Book book) throws RemoteException {
        mBooks.add(book);
        int N = mListenerList.beginBroadcast();
        Log.d(TAG, "onNewBookArrived,notify listeners:" + N);
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener listener = mListenerList.getBroadcastItem(i);
            if (listener != null) {
                listener.onNewBookArrived(book);
            }
            Log.d(TAG, "onNewBookArrived,notify listener:" + listener);
            mListenerList.finishBroadcast();
        }
    }

    private class ServiceWorker implements Runnable {

        @Override
        public void run() {
            while (!mIsServiceDestroyed.get()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookId = mBooks.size() + 1;
                Book newBook = new Book(bookId, "new book#" + bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
