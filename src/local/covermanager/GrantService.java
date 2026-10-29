package local.covermanager;

import android.content.Context;
import android.os.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

/** Shizuku UserService: only grants this app its declared development permission. */
public final class GrantService extends Binder {
    public static final String DESCRIPTOR="local.covermanager.GrantService";
    public static final int GRANT=IBinder.FIRST_CALL_TRANSACTION;
    private final int allowedUid;
    public GrantService(Context context) {
        allowedUid=context.getApplicationInfo().uid;
        attachInterface(null,DESCRIPTOR);
    }
    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if(code==INTERFACE_TRANSACTION)return super.onTransact(code,data,reply,flags);
        int caller=Binder.getCallingUid();
        if(caller!=allowedUid && caller!=0 && caller!=2000)throw new SecurityException("Unexpected caller");
        if(code==16777115){System.exit(0);return true;}
        if(code!=GRANT)return super.onTransact(code,data,reply,flags);
        data.enforceInterface(DESCRIPTOR);
        try {
            // No caller-supplied package, permission, user, or shell command is accepted.
            java.lang.Process process=new ProcessBuilder("/system/bin/pm","grant","--user",Integer.toString(allowedUid/100000),
                "local.covermanager","android.permission.WRITE_SECURE_SETTINGS").redirectErrorStream(true).start();
            if(!process.waitFor(12,TimeUnit.SECONDS)){process.destroy();throw new IOException("授权命令超时，请重试");}
            ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[512];int count;
            try(InputStream stream=process.getInputStream()){while((count=stream.read(buffer))!=-1){if(output.size()+count>8192)throw new IOException("返回内容过长");output.write(buffer,0,count);}}
            if(process.exitValue()!=0)throw new IOException(output.toString("UTF-8").trim());
            reply.writeNoException();reply.writeString("权限授予完成");
        } catch(Exception e){reply.writeException(new IllegalStateException(e.getMessage(),e));}
        return true;
    }
}
