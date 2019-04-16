// ISecurityCenter.aidl
package com.kevin.testaidl;

// Declare any non-default types here with import statements

interface ISecurityCenter {
String encrypt(String content);
String decrypt(String password);

}
