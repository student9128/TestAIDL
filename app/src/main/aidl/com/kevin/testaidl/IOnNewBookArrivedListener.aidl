// IOnNewBookArrivedListener.aidl
package com.kevin.testaidl;
import com.kevin.testaidl.Book;

// Declare any non-default types here with import statements

interface IOnNewBookArrivedListener {
   void onNewBookArrived(in Book newBook);
}
