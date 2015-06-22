package com.marakana.android.logservice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.marakana.android.service.log.ILogListener;
import com.marakana.android.service.log.ILogService;
import com.marakana.android.lib.log.LibLog;
import com.marakana.android.lib.log.LibLogException;

class ILogServiceImpl extends ILogService.Stub {
  private static final String TAG = "ILogServiceImpl";
  private static final int INCREMENTAL_TIMEOUT = 2 * 1000;
  private static final boolean DEBUG = false;
  private final Map<IBinder, ListenerTracker> listeners = 
    new HashMap<IBinder, ListenerTracker>();
  private final AtomicInteger flushCounter = new AtomicInteger();
  private final Context context;
  private final LibLog libLog;
  private LogServiceThread logServiceThread;


  ILogServiceImpl(Context context) {
    this.context = context;
    this.libLog = new LibLog();
  }
  
  protected void finalize() throws Throwable {
    this.libLog.close();
    super.finalize();
  }

  public void flushLog() {
    this.context.enforceCallingOrSelfPermission(
      Manifest.permission.FLUSH_LOG, "Flush somewhere else");
    if (DEBUG) Slog.d(TAG, "Flushing log.");
    this.libLog.flushLog();
    this.flushCounter.incrementAndGet();
  }

  public int getUsedLogSize() {
    if (DEBUG) Slog.d(TAG, "Getting used log size.");
    return this.libLog.getUsedLogSize();
  }

  public int getTotalLogSize() {
    if (DEBUG) Slog.d(TAG, "Getting total log size.");
    return this.libLog.getTotalLogSize();
  }

  public void register(ILogListener listener) throws RemoteException {
    if (listener != null) {
      IBinder binder = listener.asBinder();
      synchronized(this.listeners) {
        if (this.listeners.containsKey(binder)) {
          Slog.w(TAG, "Ignoring duplicate listener: " + binder);
        } else {
          ListenerTracker listenerTracker = new ListenerTracker(listener);
          binder.linkToDeath(listenerTracker, 0);
          this.listeners.put(binder, listenerTracker);
          if (DEBUG) Slog.d(TAG, "Registered listener: " + binder);
          if (this.logServiceThread == null) {
            if (DEBUG) Slog.d(TAG, "Starting thread");
            this.logServiceThread = new LogServiceThread();
            this.logServiceThread.start();
          }
        }
      }
    } 
  }
  
  public void unregister(ILogListener listener) {
    if (listener != null) {
      IBinder binder = listener.asBinder();
      synchronized(this.listeners) {
        ListenerTracker listenerTracker = 
          this.listeners.remove(binder);
        if (listenerTracker == null) {
          Slog.w(TAG, "Ignoring unregistered listener: " + binder);
        } else {
          if (DEBUG) Slog.d(TAG, "Unregistered listener: " + binder);
          binder.unlinkToDeath(listenerTracker, 0);
          if (this.logServiceThread != null && this.listeners.isEmpty()) {
            if (DEBUG) Slog.d(TAG, "Stopping thread");
            this.logServiceThread.interrupt();
            this.logServiceThread = null;
          }
        }
      }
    }
  }
  
  protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    if (this.context.checkCallingOrSelfPermission(android.Manifest.permission.DUMP) != 
        PackageManager.PERMISSION_GRANTED) {
      pw.print("Permission Denial: can't dump ILogService from from pid=");
      pw.print(Binder.getCallingPid());
      pw.print(", uid=");
      pw.println(Binder.getCallingUid());
      return;
    } else if (args.length > 0 && args[0] != null) {
      if (args[0].equals("flush-count")) {
        pw.println(this.flushCounter.get());
      } else if (args[0].equals("used-size")) {
        pw.println(this.getUsedLogSize());
      } else if (args[0].equals("total-size")) {
        pw.println(this.getTotalLogSize());
      } else if (args[0].equals("listeners")) {
        pw.println(this.listeners.size());
      } else {
        pw.println("Usage: ILogService [flush-count|used-size|total-size|listeners]");
      }
    } else {
      pw.println("ILogServiceState:");
      pw.print("Flush count: ");
      pw.println(this.flushCounter.get());
      pw.print("Used log size: ");
      pw.println(this.getUsedLogSize());
      pw.print("Total log size: ");
      pw.println(this.getTotalLogSize());
      pw.print("Listeners: ");
      pw.println(this.listeners.size());
    }
  }

  private final class ListenerTracker implements IBinder.DeathRecipient {
    private final ILogListener listener;
    
    public ListenerTracker(ILogListener listener) {
      this.listener = listener;
    }

    public ILogListener getListener() {
      return this.listener;
    }

    public void binderDied() {
      ILogServiceImpl.this.unregister(this.listener);
    }
  }

  private final class LogServiceThread extends Thread {
    private int mLastKnownSize;

    public void run() {
      while(!Thread.interrupted()) {
        try {
          if (DEBUG) Slog.d(TAG, "Waiting for log data");
          int usedSize = ILogServiceImpl.this.getUsedLogSize();

          if (mLastKnownSize != usedSize) {
            mLastKnownSize = ILogServiceImpl.this.getUsedLogSize();
            if (DEBUG) Slog.d(TAG, "Log data changed. Used data is now at " + mLastKnownSize);  

            synchronized(ILogServiceImpl.this.listeners) {
              for (Map.Entry<IBinder, ListenerTracker> entry : ILogServiceImpl.this.listeners.entrySet()) {
                ILogListener listener = entry.getValue().getListener();
                try {
                  if (DEBUG) Slog.d(TAG, "Notifying listener: " +  entry.getKey());
                  listener.onUsedLogSizeChange(mLastKnownSize);
                } catch (RemoteException e) {
                  Slog.e(TAG, "Failed to update listener: " + entry.getKey(), e); 
                  ILogServiceImpl.this.unregister(listener);
                }
              }
            }  
          } else {
            //No change, take a rest before polling again
            try {
              Thread.sleep(INCREMENTAL_TIMEOUT);
            } catch (InterruptedException e2) {
              break;
            }
          }
        } catch (LibLogException e) {
          Slog.e(TAG, "Oops", e);
          try {
            Thread.sleep(INCREMENTAL_TIMEOUT);
          } catch (InterruptedException e2) {
            break;
          }
        }
      }
    } 
  }
}
