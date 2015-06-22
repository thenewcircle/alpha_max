package com.marakana.android.service.log;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog; /* This is to avoid generating events for ourselves */
import java.util.HashMap;

public class LogManager {
  private static final String TAG = "LogManager";
  private static final String REMOTE_SERVICE_NAME = ILogService.class.getName();
  private static final boolean DEBUG = false; // change to true to enable debugging
  
  private HashMap<LogListener, ListenerTransport> mListeners = 
    new HashMap<LogListener, ListenerTransport>();

  private class ListenerTransport extends ILogListener.Stub {      // <1>
    private LogListener mListener;

    public ListenerTransport(LogListener listener) {
      mListener = listener;
    }

    private void clearListener() {
      mListener = null;
    }

    @Override
    public void onUsedLogSizeChange(final int usedLogSize) {       // <2>
      if (DEBUG) Slog.d(TAG, "onUsedLogSizeChange: " + usedLogSize);

      Message message = mHandler.obtainMessage();
      message.arg1 = usedLogSize;
      mHandler.sendMessage(message);
    }

    private final Handler mHandler = new Handler() {               // <3>
      @Override
      public void handleMessage(Message message) {                 // <4>
        if (mListener == null) return;

        int usedLogSize = message.arg1;
        if (DEBUG) Slog.d(TAG, "Notifying local listener: " + usedLogSize);
        mListener.onUsedLogSizeChange(usedLogSize);                // <6>
      }
    };
  }

  //Singleton instance
  private static LogManager sInstance;

  private final ILogService mService;
  
  public static synchronized LogManager getInstance() {
    if (sInstance == null) {
      sInstance = new LogManager();
    }
    return sInstance;
  }
    
  private LogManager() {
    Log.d(TAG, "Connecting to ILogService by name [" + REMOTE_SERVICE_NAME + "]");
    mService = ILogService.Stub.asInterface(
      ServiceManager.getService(REMOTE_SERVICE_NAME));             // <7>
    if (mService == null) {
      throw new IllegalStateException("Failed to find ILogService by name [" + REMOTE_SERVICE_NAME + "]");
    }
  }   
  
  public void flushLog() {
    try {
      if (DEBUG) Slog.d(TAG, "Flushing log.");
      mService.flushLog();                                         // <8>
    } catch (RemoteException e) { 
      throw new RuntimeException("Failed to flush log", e);
    }
  }
  
  public int getTotalLogSize() {
    try {
      if (DEBUG) Slog.d(TAG, "Getting total log size.");
      return mService.getTotalLogSize();                           // <8>
    } catch (RemoteException e) {
      throw new RuntimeException("Failed to get total log size", e);
    }
  }
  
  public int getUsedLogSize() {
    try {
      if (DEBUG) Slog.d(TAG, "Getting used log size.");
      return mService.getUsedLogSize();                            // <8>
    } catch (Exception e) {
      throw new RuntimeException("Failed to get used log size", e);
    }
  }
  
  public void register(LogListener listener) {
    if (listener == null) return;

    if (mListeners.containsKey(listener)) {
      Log.w(TAG, "Already registered: " + listener);
    } else {
      synchronized (mListeners) {                                  // <5>
        ListenerTransport wrapper = new ListenerTransport(listener);
        try {
          if (DEBUG) Log.d(TAG, "Registering remote listener.");
          mService.register(wrapper);                              // <8>
        } catch (RemoteException e) {
          throw new RuntimeException("Failed to register " + listener, e);
        }

        mListeners.put(listener, wrapper);
      }
    }
  }
  
  public void unregister(LogListener listener) {
    if (listener == null) return;

    if (!mListeners.containsKey(listener)) {
      Log.w(TAG, "Not registered: " + listener);
    } else {
      synchronized (mListeners) {                                  // <5>
        ListenerTransport wrapper = mListeners.remove(listener);
      
        if (wrapper != null) {
          try {
            if (DEBUG) Log.d(TAG, "Unregistering remote listener.");
            mService.unregister(wrapper);                          // <8>
          } catch (RemoteException e) {
            throw new RuntimeException("Failed to unregister " + listener, e);
          }
        }

        wrapper.clearListener();                                   // <9>
      }
    }
  }
}
