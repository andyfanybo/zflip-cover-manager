import local.covermanager.Repository;
import java.util.*;
public class RepositoryTest {
    static void check(boolean value) {if(!value)throw new AssertionError();}
    public static void main(String[] args) {
        String[] f=new String[28];Arrays.fill(f,"0");f[12]="";f[27]="";
        f[20]="com.example.old;com.example.work";f[25]="com.example.old,0;com.example.work,10";
        String original=String.join("/",f);
        String added=new Repository(original).change("com.example.newapp",0,true);
        check(new Repository(added).contains("com.example.newapp",0));
        String[] out=added.split("/",-1);for(int i=0;i<28;i++)if(i!=20&&i!=25)check(f[i].equals(out[i]));
        check(added.equals(new Repository(added).change("com.example.newapp",0,true)));
        check(original.equals(new Repository(added).change("com.example.newapp",0,false)));
        String multi=new Repository(original).change("com.example.work",0,true);
        String removed=new Repository(multi).change("com.example.work",0,false);
        check(removed.equals(original));check(new Repository(removed).contains("com.example.work",10));
        boolean rejected=false;try{new Repository("bad");}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        f[25]="com.example.bad,notUser";rejected=false;try{new Repository(String.join("/",f));}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        System.out.println("PASS: format validation, unrelated fields, idempotence, add/remove, profile preservation");
    }
}
