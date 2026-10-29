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
    private boolean multistarAvailable;
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
        else {authorizing=false;notice=s("shizuku_denied");render();}
    };
    private Shizuku.UserServiceArgs grantArgs;
    private ServiceConnection grantConnection;
    private final Runnable grantTimeout=()->{if(authorizing){authorizing=false;releaseGrantService();notice=s("grant_timeout");render();}};

    private String s(String key,Object... args){
        int id=getResources().getIdentifier(key,"string",getPackageName());
        if(id==0)throw new IllegalStateException("Missing string: "+key);
        return getString(id,args);
    }
    private String errorText(String message){
        if(message==null)return s("save_failed");
        for(String key:new String[]{"cm_config_missing","cm_config_format","cm_list_format","cm_target_invalid","cm_grant_timeout","cm_grant_output"})
            if(message.contains(key))return s(key);
        return message;
    }
    @Override protected void attachBaseContext(Context base){
        String choice=base.getSharedPreferences("ui",MODE_PRIVATE).getString("language","");
        if(!choice.isEmpty()){
            android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());
            config.setLocale(Locale.forLanguageTag(choice));base=base.createConfigurationContext(config);
        }
        super.attachBaseContext(base);
    }
    private void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle(s("language_title"))
            .setItems(new String[]{s("system_language"),"简体中文","English"},(dialog,which)->{
                getSharedPreferences("ui",MODE_PRIVATE).edit().putString("language",new String[]{"","zh","en"}[which]).apply();recreate();
            }).setNegativeButton(s("cancel"),null).show();
    }
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
        TextView title=text(s("app_name"),18); title.setTypeface(null,Typeface.BOLD); title.setTextColor(accent);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button(s("setup"),()->setup()),new LinearLayout.LayoutParams(dp(94),dp(33)));root.addView(header);
        status=text(s("loading"),11); status.setPadding(0,dp(3),0,dp(3)); root.addView(status);
        search=new EditText(this); search.setSingleLine(true); search.setTextSize(13); search.setHint(s("search"));
        search.setTextColor(Color.WHITE); search.setHintTextColor(Color.LTGRAY); root.addView(search,new LinearLayout.LayoutParams(-1,dp(40)));
        LinearLayout toolbar=new LinearLayout(this);
        filter=button(s("all"),()->{selectedOnly=!selectedOnly;filter.setText(selectedOnly?s("added"):s("all")); render();});
        toolbar.addView(filter,new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button(s("refresh"),()->reload()),new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button(s("undo"),()->undo()),new LinearLayout.LayoutParams(0,dp(36),1));
        toolbar.addView(button(s("help"),()->help()),new LinearLayout.LayoutParams(0,dp(36),1)); root.addView(toolbar);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){render();} public void afterTextChanged(Editable e){}});
    }
    @Override protected void onResume() { super.onResume(); reload(); }
    @Override protected void onDestroy(){Shizuku.removeRequestPermissionResultListener(shizukuPermission);releaseGrantService();super.onDestroy();}
    private String read() { return Settings.Secure.getString(getContentResolver(),Repository.KEY); }
    private boolean writable() {return multistarAvailable && permission && repository!=null;}
    private void reload() {
        permission=checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")==PackageManager.PERMISSION_GRANTED;
        try { multistarAvailable=getPackageManager().getApplicationInfo("com.samsung.android.multistar",0).enabled; }
        catch(Exception e){multistarAvailable=false;}
        try { repository=new Repository(read()); } catch(Exception e){repository=null;notice=errorText(e.getMessage());}
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found=getPackageManager().queryIntentActivities(i,0);
        labels.clear(); apps.clear(); Set<String> seen=new HashSet<>();
        for(ResolveInfo r:found) {String p=r.activityInfo.packageName; if(seen.add(p)){apps.add(r);labels.put(p,SELF.equals(p)?s("app_name"):r.loadLabel(getPackageManager()).toString());}}
        java.text.Collator collator=java.text.Collator.getInstance(getResources().getConfiguration().getLocales().get(0));
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
            check.setContentDescription(s("add_description",label)); check.setChecked(added);check.setEnabled(writable()&&!SELF.equals(p));
            check.setOnClickListener(v->change(p,check.isChecked())); row.addView(check,new LinearLayout.LayoutParams(dp(43),dp(43)));rows.addView(row);
            View line=new View(this);line.setBackgroundColor(Color.rgb(42,51,55));rows.addView(line,new LinearLayout.LayoutParams(-1,dp(.5f)));
        }
        String message=!multistarAvailable?s("read_only"):!permission?s("needs_grant"):repository==null?notice:s("counts",selected,shown);
        status.setText(message+(notice.isEmpty()?"":"\n"+notice));
        if(shown==0)rows.addView(text(s("no_matches"),14));
    }
    private void change(String p, boolean add) {
        try {
            if(!writable())throw new IllegalStateException(s("no_permission"));
            String before=read(); String after=new Repository(before).change(p,user,add);
            if(before.equals(after)){notice=s("no_change");render();return;}
            if(!before.equals(read()))throw new IllegalStateException(s("concurrent_change"));
            if(!Settings.Secure.putString(getContentResolver(),Repository.KEY,after)||!after.equals(read()))throw new IllegalStateException(s("save_failed"));
            if(!SELF.equals(p))prefs.edit().putString("before",before).putString("after",after).commit();
            else prefs.edit().remove("before").remove("after").commit();
            repository=new Repository(after);
            notice=s("saved");
            Toast.makeText(this,add?s("toast_added"):s("toast_removed"),Toast.LENGTH_SHORT).show();
        }catch(Exception e){notice=errorText(e.getMessage());try{repository=new Repository(read());}catch(Exception ignored){repository=null;}}
        render();
    }
    private void undo() {
        String before=prefs.getString("before",null), after=prefs.getString("after",null);
        if(before==null||after==null){Toast.makeText(this,s("no_undo"),Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setMessage(s("undo_confirm")).setNegativeButton(s("cancel"),null).setPositiveButton(s("undo"),(d,w)->{
            try {
                if(!writable())throw new IllegalStateException(s("not_writable"));
                if(!after.equals(read()))throw new IllegalStateException(s("undo_conflict"));
                if(!Settings.Secure.putString(getContentResolver(),Repository.KEY,before)||!before.equals(read()))throw new IllegalStateException(s("undo_failed"));
                prefs.edit().remove("before").remove("after").commit();notice=s("undone");
            }catch(Exception e){notice=errorText(e.getMessage());} reload();
        }).show();
    }
    private void launch(String p) {
        try {
            Intent i=getPackageManager().getLaunchIntentForPackage(p);if(i==null)throw new IllegalStateException(s("no_launch"));
            ActivityOptions options=ActivityOptions.makeBasic();options.setLaunchDisplayId(getDisplay().getDisplayId());startActivity(i,options.toBundle());
        }catch(Exception e){Toast.makeText(this,s("launch_failed",errorText(e.getMessage())),Toast.LENGTH_LONG).show();}
    }
    private void help() {
        String info=s("help_text");
        TextView content=text(info,13); content.setPadding(dp(14),dp(8),dp(14),dp(8)); content.setTextIsSelectable(true);
        ScrollView s=new ScrollView(this);s.addView(content);new AlertDialog.Builder(this).setTitle(s("help_title")).setView(s).setPositiveButton(s("ok"),null).show();
    }
    private void setup(){
        new AlertDialog.Builder(this).setTitle(s("setup_title")).setItems(new String[]{s("shizuku_option"),s("adb_option"),s("instructions_option"),s("language")},(d,which)->{
            if(which==0)authorizeWithShizuku();else if(which==1)repairEntry();else if(which==2)help();else chooseLanguage();
        }).setNegativeButton(s("cancel"),null).show();
    }
    private void repairEntry(){
        reload();if(!writable()){notice=s("grant_first");render();return;}
        change(SELF,true);
    }
    private void authorizeWithShizuku(){
        if(authorizing)return;
        try {
            if(!multistarAvailable||repository==null)throw new IllegalStateException(s("compat_first"));
            if(!Shizuku.pingBinder())throw new IllegalStateException(s("shizuku_stopped"));
            if(Shizuku.getVersion()<13)throw new IllegalStateException(s("shizuku_version"));
            authorizing=true;notice=s("waiting_grant");render();
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED)bindGrantService();
            else if(Shizuku.shouldShowRequestPermissionRationale())throw new IllegalStateException(s("shizuku_settings"));
            else Shizuku.requestPermission(42);
        }catch(Exception e){authorizing=false;notice=errorText(e.getMessage());render();}
    }
    private void bindGrantService(){
        try {
            grantArgs=new Shizuku.UserServiceArgs(new ComponentName(this,GrantService.class)).daemon(false).processNameSuffix("grant").version(3);
            grantConnection=new ServiceConnection(){
                @Override public void onServiceConnected(ComponentName name,IBinder binder){
                    new Thread(()->{
                        String error=null;Parcel input=Parcel.obtain(),output=Parcel.obtain();
                        try {input.writeInterfaceToken(GrantService.DESCRIPTOR);if(!binder.transact(GrantService.GRANT,input,output,0))throw new IllegalStateException(s("service_no_response"));output.readException();output.readString();}
                        catch(Exception e){error=e.getMessage();}finally{input.recycle();output.recycle();}
                        final String result=error;
                        handler.post(()->{
                            if(isDestroyed())return;
                            authorizing=false;releaseGrantService();reload();
                            if(result!=null){notice=s("grant_failed",errorText(result));render();}
                            else if(!permission){notice=s("grant_not_effective");render();}
                            else {repairEntry();notice=s("grant_done",notice);render();}
                        });
                    },"ShizukuGrant").start();
                }
                @Override public void onServiceDisconnected(ComponentName name){if(authorizing){authorizing=false;notice=s("service_disconnected");render();}}
            };
            Shizuku.bindUserService(grantArgs,grantConnection);
            handler.postDelayed(grantTimeout,20000);
        }catch(Exception e){authorizing=false;releaseGrantService();notice=errorText(e.getMessage());render();}
    }
    private void releaseGrantService(){
        handler.removeCallbacks(grantTimeout);
        if(grantArgs!=null&&grantConnection!=null)try{Shizuku.unbindUserService(grantArgs,grantConnection,true);}catch(Exception ignored){}
        grantArgs=null;grantConnection=null;
    }
}
