package local.covermanager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final String SELF = "local.covermanager";
    private final int accent = Color.rgb(141,229,214);
    private TextView status;
    private LinearLayout rows;
    private EditText search;
    private Button filter;
    private boolean selectedOnly;
    private boolean supported;
    private boolean permission;
    private Repository repository;
    private List<ResolveInfo> apps = new ArrayList<>();
    private final Map<String,String> labels = new HashMap<>();
    private int user;
    private android.content.SharedPreferences prefs;
    private String notice = "";
    private boolean authorizing;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Shizuku.OnRequestPermissionResultListener shizukuPermission=(code,result)->{
        if(code!=42)return;
        if(result==PackageManager.PERMISSION_GRANTED)bindGrantService();
        else {authorizing=false;notice="Shizuku 授权被拒绝，可在 Shizuku 中重新允许";render();}
    };
    private Shizuku.UserServiceArgs grantArgs;
    private ServiceConnection grantConnection;
    private final Runnable grantTimeout=()->{if(authorizing){authorizing=false;releaseGrantService();notice="等待授权服务超时，请确认 Shizuku 仍在运行后重试";render();}};
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String value, int sp) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(Color.WHITE); return t;
    }
    private Button button(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextSize(12); b.setAllCaps(false);
        b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(5),0,dp(5),0);
        b.setOnClickListener(v -> action.run()); return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17,24,27));
        getWindow().setNavigationBarColor(Color.rgb(17,24,27));
        // Android allocates a range of 100000 UIDs to each user/profile.
        user=android.os.Process.myUid()/100000;
        prefs=getSharedPreferences("history",MODE_PRIVATE);
        Shizuku.addRequestPermissionResultListener(shizukuPermission);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17,24,27)); root.setPadding(dp(12),dp(5),dp(12),dp(5));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("副屏应用管理",18); title.setTypeface(null,Typeface.BOLD); title.setTextColor(accent);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button("授权 / 入口",()->setup()),new LinearLayout.LayoutParams(dp(94),dp(33)));root.addView(header);
        status=text("读取中…",11); status.setPadding(0,dp(3),0,dp(3)); root.addView(status);
        search=new EditText(this); search.setSingleLine(true); search.setTextSize(13); search.setHint("搜索应用或包名");
        search.setTextColor(Color.WHITE); search.setHintTextColor(Color.LTGRAY); root.addView(search,new LinearLayout.LayoutParams(-1,dp(40)));
        LinearLayout toolbar=new LinearLayout(this);
        filter=button("全部应用",()->{selectedOnly=!selectedOnly;filter.setText(selectedOnly?"已添加":"全部应用"); render();});
        toolbar.addView(filter,new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button("刷新",()->reload()),new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button("撤销",()->undo()),new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button("说明",()->help()),new LinearLayout.LayoutParams(0,dp(36),1)); root.addView(toolbar);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){render();} public void afterTextChanged(Editable e){}});
    }
    @Override protected void onResume() { super.onResume(); reload(); }
    @Override protected void onDestroy(){Shizuku.removeRequestPermissionResultListener(shizukuPermission);releaseGrantService();super.onDestroy();}
    private String read() { return Settings.Secure.getString(getContentResolver(),Repository.KEY); }
    private boolean writable() {return supported && permission && repository!=null;}
    private void reload() {
        permission=checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")==PackageManager.PERMISSION_GRANTED;
        try { supported="9.1.09".equals(getPackageManager().getPackageInfo("com.samsung.android.multistar",0).versionName); }
        catch(Exception e){supported=false;}
        try { repository=new Repository(read()); } catch(Exception e){repository=null;notice=e.getMessage();}
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found=getPackageManager().queryIntentActivities(i,0);
        labels.clear(); apps.clear(); Set<String> seen=new HashSet<>();
        for(ResolveInfo r:found) {String p=r.activityInfo.packageName; if(seen.add(p)){apps.add(r);labels.put(p,r.loadLabel(getPackageManager()).toString());}}
        java.text.Collator collator=java.text.Collator.getInstance(java.util.Locale.CHINA);
        apps.sort((a,b)->collator.compare(labels.get(a.activityInfo.packageName),labels.get(b.activityInfo.packageName)));
        render();
    }
    private void render() {
        if(rows==null)return; rows.removeAllViews();
        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT); int selected=0,shown=0;
        for(ResolveInfo r:apps) {
            String p=r.activityInfo.packageName; boolean added=repository!=null&&repository.contains(p,user);
            if(added) selected++;
            String label=labels.get(p);
            if(selectedOnly&&!added)continue;
            if(!q.isEmpty()&&!label.toLowerCase(Locale.ROOT).contains(q)&&!p.toLowerCase(Locale.ROOT).contains(q))continue;
            shown++;
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,dp(3),0,dp(3));
            ImageView icon=new ImageView(this); icon.setImageDrawable(r.loadIcon(getPackageManager())); row.addView(icon,new LinearLayout.LayoutParams(dp(29),dp(29)));
            LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(7),0,0,0);
            TextView name=text(label,14); name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END); names.addView(name);
            TextView pkg=text(p,9);pkg.setTextColor(Color.LTGRAY);pkg.setSingleLine(true);pkg.setEllipsize(TextUtils.TruncateAt.MIDDLE);names.addView(pkg);
            row.addView(names,new LinearLayout.LayoutParams(0,dp(43),1)); names.setGravity(Gravity.CENTER_VERTICAL);
            names.setOnClickListener(v->launch(p));
            CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
            check.setContentDescription("添加到副屏："+label); check.setChecked(added);check.setEnabled(writable()&&!SELF.equals(p));
            check.setOnClickListener(v->change(p,check.isChecked())); row.addView(check,new LinearLayout.LayoutParams(dp(43),dp(43)));rows.addView(row);
            View line=new View(this);line.setBackgroundColor(Color.rgb(42,51,55));rows.addView(line,new LinearLayout.LayoutParams(-1,dp(.5f)));
        }
        String message=!supported?"仅支持 MultiStar 9.1.09，当前为只读":!permission?"点右上角：Shizuku 或 ADB 授权":repository==null?notice:"已添加 "+selected+" · 当前显示 "+shown;
        status.setText(message+(notice.isEmpty()?"":"\n"+notice));
        if(shown==0)rows.addView(text("没有匹配的应用",14));
    }
    private void change(String p, boolean add) {
        try {
            if(!writable())throw new IllegalStateException("尚未获得写入权限");
            String before=read(); String after=new Repository(before).change(p,user,add);
            if(before.equals(after)){notice="名单已是目标状态";render();return;}
            if(!before.equals(read()))throw new IllegalStateException("设置刚被其他程序修改，请刷新后重试");
            if(!Settings.Secure.putString(getContentResolver(),Repository.KEY,after)||!after.equals(read()))throw new IllegalStateException("保存未成功，请刷新后检查");
            if(!SELF.equals(p))prefs.edit().putString("before",before).putString("after",after).commit();
            else prefs.edit().remove("before").remove("after").commit();
            repository=new Repository(after);
            notice="已保存；图标未更新时，切换一次副屏小组件";
            Toast.makeText(this,add?"已添加到副屏":"已从副屏移除",Toast.LENGTH_SHORT).show();
        }catch(Exception e){notice=e.getMessage();try{repository=new Repository(read());}catch(Exception ignored){repository=null;}}
        render();
    }
    private void undo() {
        String before=prefs.getString("before",null), after=prefs.getString("after",null);
        if(before==null||after==null){Toast.makeText(this,"还没有可撤销的修改",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setMessage("撤销本工具上一次应用名单修改？").setNegativeButton("取消",null).setPositiveButton("撤销",(d,w)->{
            try {
                if(!writable())throw new IllegalStateException("当前不允许写入");
                if(!after.equals(read()))throw new IllegalStateException("名单已被其他程序修改，无法安全撤销");
                if(!Settings.Secure.putString(getContentResolver(),Repository.KEY,before)||!before.equals(read()))throw new IllegalStateException("撤销未成功");
                prefs.edit().remove("before").remove("after").commit();notice="已撤销；可切换一次副屏小组件刷新";
            }catch(Exception e){notice=e.getMessage();} reload();
        }).show();
    }
    private void launch(String p) {
        try {
            Intent i=getPackageManager().getLaunchIntentForPackage(p);if(i==null)throw new IllegalStateException("应用没有可启动的入口");
            ActivityOptions options=ActivityOptions.makeBasic();options.setLaunchDisplayId(getDisplay().getDisplayId());startActivity(i,options.toBundle());
        }catch(Exception e){Toast.makeText(this,"启动失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void help() {
        String info="勾选右侧方框添加到 MultiStar 副屏名单，取消勾选移除。点应用名称可尝试在当前屏幕打开。新装应用点“刷新”即可列出。\n\n保存后若 MultiStar 图标未更新，请切换到其他副屏小组件再回来。\n\n方式一：电脑通过 ADB 安装并授权。\n方式二：手机安装 APK，在 Shizuku 已运行时点右上角“授权 / 入口”，选择 Shizuku 授权并允许。只执行一次本工具权限授予，之后无需持续连接。\n\nShizuku 需为 v13 或更新版。仅安装而未启动 Shizuku 无法授权；工具也需要能被首次打开。\n\n仅适配 MultiStar 9.1.09，不申请联网权限。撤销会检查名单是否被其他程序更改。";
        TextView content=text(info,13); content.setPadding(dp(14),dp(8),dp(14),dp(8)); content.setTextIsSelectable(true);
        ScrollView s=new ScrollView(this);s.addView(content);new AlertDialog.Builder(this).setTitle("使用说明 · v0.2").setView(s).setPositiveButton("知道了",null).show();
    }
    private void setup(){
        new AlertDialog.Builder(this).setTitle("首次设置").setItems(new String[]{"通过 Shizuku 授权并添加入口","已有 ADB 权限：添加 / 修复入口","查看安装说明"},(d,which)->{
            if(which==0)authorizeWithShizuku();else if(which==1)repairEntry();else help();
        }).setNegativeButton("取消",null).show();
    }
    private void repairEntry(){
        reload();if(!writable()){notice="请先完成授权，且确认 MultiStar 版本兼容";render();return;}
        change(SELF,true);
    }
    private void authorizeWithShizuku(){
        if(authorizing)return;
        try {
            if(!supported||repository==null)throw new IllegalStateException("先确认 MultiStar 9.1.09 和配置格式兼容");
            if(!Shizuku.pingBinder())throw new IllegalStateException("Shizuku 未运行，请先到 Shizuku 启动服务，再返回此处");
            if(Shizuku.getVersion()<13)throw new IllegalStateException("需要 Shizuku v13 或更新版本");
            authorizing=true;notice="等待 Shizuku 授权…";render();
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED)bindGrantService();
            else if(Shizuku.shouldShowRequestPermissionRationale())throw new IllegalStateException("请在 Shizuku 的授权应用列表中允许本工具");
            else Shizuku.requestPermission(42);
        }catch(Exception e){authorizing=false;notice=e.getMessage();render();}
    }
    private void bindGrantService(){
        try {
            grantArgs=new Shizuku.UserServiceArgs(new ComponentName(this,GrantService.class)).daemon(false).processNameSuffix("grant").version(2);
            grantConnection=new ServiceConnection(){
                @Override public void onServiceConnected(ComponentName name,IBinder binder){
                    new Thread(()->{
                        String error=null;Parcel input=Parcel.obtain(),output=Parcel.obtain();
                        try {input.writeInterfaceToken(GrantService.DESCRIPTOR);if(!binder.transact(GrantService.GRANT,input,output,0))throw new IllegalStateException("授权服务未响应");output.readException();output.readString();}
                        catch(Exception e){error=e.getMessage();}finally{input.recycle();output.recycle();}
                        final String result=error;
                        handler.post(()->{
                            if(isDestroyed())return;
                            authorizing=false;releaseGrantService();reload();
                            if(result!=null){notice="授权失败："+result;render();}
                            else if(!permission){notice="权限尚未生效，请刷新后重试";render();}
                            else {repairEntry();notice="Shizuku 已完成授权；"+notice;render();}
                        });
                    },"ShizukuGrant").start();
                }
                @Override public void onServiceDisconnected(ComponentName name){if(authorizing){authorizing=false;notice="Shizuku 授权服务意外断开，请重试";render();}}
            };
            Shizuku.bindUserService(grantArgs,grantConnection);
            handler.postDelayed(grantTimeout,20000);
        }catch(Exception e){authorizing=false;releaseGrantService();notice=e.getMessage();render();}
    }
    private void releaseGrantService(){
        handler.removeCallbacks(grantTimeout);
        if(grantArgs!=null&&grantConnection!=null)try{Shizuku.unbindUserService(grantArgs,grantConnection,true);}catch(Exception ignored){}
        grantArgs=null;grantConnection=null;
    }
}
