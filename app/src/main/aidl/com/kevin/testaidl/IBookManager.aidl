// IBookManager.aidl
package com.kevin.testaidl;
import com.kevin.testaidl.Book;
import com.kevin.testaidl.IOnNewBookArrivedListener;
// Declare any non-default types here with import statements

interface IBookManager {

void addBook(in Book b);
List<Book> getBookList();
void registerListener(IOnNewBookArrivedListener listener);
void unRegisterListener(IOnNewBookArrivedListener listener);

}
