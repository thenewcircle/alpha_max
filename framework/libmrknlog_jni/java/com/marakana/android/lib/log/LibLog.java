package com.marakana.android.lib.log;

public class LibLog {
  
  public LibLog() {
    this.init();
  }
    
  @Override
  protected void finalize() {
    this.close();
  }

  private native void init() throws LibLogException;
  public native void close();  
  public native void flushLog() throws LibLogException;
  public native int getTotalLogSize() throws LibLogException;
  public native int getUsedLogSize() throws LibLogException;

  static {
     System.loadLibrary("mrknlog_jni");
  }
}

