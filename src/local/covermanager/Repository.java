package local.covermanager;

import java.util.*;

/** Format verified against MultiStar 9.1.09; preserve all unrelated fields verbatim. */
public final class Repository {
    public static final String KEY = "multistar_setting_repository";
    private final String[] fields;
    public Repository(String value) {
        if (value == null) throw new IllegalArgumentException("尚未找到 MultiStar 配置");
        fields = value.split("/", -1);
        if (fields.length != 28 || !fields[27].isEmpty())
            throw new IllegalArgumentException("配置格式不匹配，已停止写入");
        validate(fields[20], false);
        validate(fields[25], true);
    }
    private static boolean pkg(String p) {
        return p.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
    private static List<String> entries(String s) {
        return s.equals("0") || s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split(";", -1)));
    }
    private static void validate(String value, boolean withUser) {
        for (String s : entries(value)) {
            String[] pair = s.split(",", -1);
            if ((!withUser && (pair.length != 1 || !pkg(s))) ||
                (withUser && (pair.length != 2 || !pkg(pair[0]) || !pair[1].matches("[0-9]+"))))
                throw new IllegalArgumentException("应用名单格式异常，已停止写入");
        }
    }
    public boolean contains(String p, int user) { return entries(fields[25]).contains(p + "," + user); }
    public String change(String p, int user, boolean add) {
        if (!pkg(p) || user < 0) throw new IllegalArgumentException("无效的应用或用户 ID");
        List<String> legacy = entries(fields[20]);
        List<String> modern = entries(fields[25]);
        String item = p + "," + user;
        if (add) {
            if (!legacy.contains(p)) legacy.add(p);
            if (!modern.contains(item)) modern.add(item);
        } else {
            modern.removeIf(item::equals);
            boolean otherUser = modern.stream().anyMatch(s -> s.startsWith(p + ","));
            if (!otherUser) legacy.removeIf(p::equals);
        }
        String[] copy = fields.clone();
        copy[20] = legacy.isEmpty() ? "0" : String.join(";", legacy);
        copy[25] = modern.isEmpty() ? "0" : String.join(";", modern);
        return String.join("/", copy);
    }
}
