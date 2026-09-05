package jp.muimi.onigame;
public enum OniType {
    DAKKO("堕狐"), KISHIN("鬼神"), JAKUTSUKI("蛇窟姫");
    public final String display;
    OniType(String display) { this.display = display; }
    public static OniType parse(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) { case "dakko", "堕狐" -> DAKKO; case "kishin", "鬼神" -> KISHIN; case "jakutsuki", "蛇窟姫", "ジャクツキ" -> JAKUTSUKI; default -> null; };
    }
}
