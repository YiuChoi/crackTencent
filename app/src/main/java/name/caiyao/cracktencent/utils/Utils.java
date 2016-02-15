package name.caiyao.cracktencent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by 蔡小木 on 2016/1/7 0007.
 */
public class Utils {

    private static String[] binaryPaths = new String[]{"/system/bin/", "/system/xbin/"};
    private static String savePath = "/mnt/sdcard/cracktencent/";
    private static String qqSavePath = savePath + "qq/";
    private static String weixinSavePath = savePath + "weixin/";
    public static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences("Utils", Context.MODE_PRIVATE);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        sharedPreferences.edit().putString("IMEI", telephonyManager.getDeviceId()).apply();
        SQLiteDatabase.loadLibs(context);
        ArrayList<String> commands = new ArrayList<>();
        commands.add("cd /mnt/sdcard/");
        commands.add("mkdir cracktencent");
        commands.add("cd cracktencent");
        commands.add("mkdir qq");
        commands.add("mkdir weixin");
        runSu(commands);
    }

    public static void copyDatabase(Context context) {
        String QQDbPath = "/data/data/com.tencent.mobileqq/databases";
        File file = new File(QQDbPath);
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 777 " + QQDbPath);
        Utils.runSu(commands);
        commands.clear();
        File[] files = file.listFiles();
        for (File f : files) {
            if (!f.isDirectory()) {
                String fileName = f.getName();
                String[] fileNameSplit = fileName.split("\\.");
                if (fileNameSplit.length > 0 && fileNameSplit[fileNameSplit.length - 1].equals("db")) {
                    Pattern pattern = Pattern.compile("[0-9]*");
                    if (pattern.matcher(fileName.substring(0, fileName.lastIndexOf("."))).matches()) {
                        commands.add("chmod 777 " + f.getPath());
                        Utils.runSu(commands);
                        commands.clear();
                        if (Utils.findBinary("busybox")) {
                            commands.add("busybox cp -r " + f.getPath() + " " + qqSavePath + fileName);
                        } else {
                            commands.add("cat " + f.getPath() + " > " + qqSavePath + fileName);
                        }
                        Utils.runSu(commands);
                        commands.clear();
                    }
                }
            }
        }

        String weixinDbPath = "/data/data/com.tencent.mm/MicroMsg";
        String weixinSp = "/data/data/com.tencent.mm/shared_prefs/system_config_prefs.xml";
        String crackSp = "/data/data/name.caiyao.cracktencent/shared_prefs/system_config_prefs.xml";
        commands.add("chmod 777 " + weixinDbPath);
        if (Utils.findBinary("busybox")) {
            commands.add("busybox cp -r " + weixinSp + " " + crackSp);
        } else {
            commands.add("cat " + weixinSp + " > " + crackSp);
        }
        commands.add("chmod 777 " + crackSp);
        Utils.runSu(commands);
        commands.clear();
        SharedPreferences sp = context.getSharedPreferences(
                "system_config_prefs", 0);
        String str = String.valueOf(sp.getInt("default_uin", 0));
        if (str.equals("0")) {
            Log.i("TAG", "通过sharepreference获取uin失败");
            if (Utils.findBinary("busybox")) {
                commands.add("busybox cp -r " + weixinSp + " " + weixinSavePath + "uin.xml");
            } else {
                commands.add("cat " + weixinSp + " > " + weixinSavePath + "uin.xml");
            }
            str = getUin(weixinDbPath + "uin.xml");
            if (str.equals("")) {
                Log.i("TAG", "无法获取uin");
                return;
            }
        }
        Log.i("TAG", "weixin uid is :" + str);

        String key = sharedPreferences.getString("IMEI", "") + str;
        Log.i("TAG", "key:" + key);
        String keyRes = Utils.MD5(key).substring(0, 7);
        Log.i("TAG", "pwd:" + keyRes);

        File wcf = new File(weixinDbPath);
        files = wcf.listFiles();
        for (File f : files) {
            if (f.isDirectory() && (f.getName().length() == 32)) {
                String dbPath = f.getAbsolutePath();
                String infoDbPath = dbPath + "/EnMicroMsg.db";
                commands.add("chmod 777 " + infoDbPath);
                if (Utils.findBinary("busybox")) {
                    commands.add("busybox cp -r " + infoDbPath + " " + weixinSavePath + "EnMicroMsg.db");
                } else {
                    commands.add("dd if=" + infoDbPath + " of=" + weixinSavePath + "EnMicroMsg.db");
                }
                Utils.runSu(commands);
                commands.clear();
                if (new File(weixinSavePath + "EnMicroMsg.db").exists()) {
                    File databaseFile = context.getDatabasePath(weixinSavePath + "EnMicroMsg.db");
                    File deDatabaseFile = context.getDatabasePath(weixinSavePath + "de.db");
                    if (deDatabaseFile.exists()){
                        deDatabaseFile.delete();
                    }
                    SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
                        public void preKey(SQLiteDatabase database) {
                        }

                        public void postKey(SQLiteDatabase database) {
                            database.rawExecSQL("PRAGMA cipher_migrate;");  //最关键的一句！！！
                        }
                    };
                    SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, keyRes, null, hook);
                    Cursor c = database.query("message", null, null, null, null, null, null);
                    while (c.moveToNext()) {
                        int _id = c.getInt(c.getColumnIndex("msgId"));
                        String name = c.getString(c.getColumnIndex("content"));
                        Log.i("db", "_id=>" + _id + ", content=>" + name);
                    }
                    c.close();
                    database.rawExecSQL(String.format("ATTACH DATABASE '%s' as plaintext KEY '';",
                            weixinSavePath + "de.db"));
                    database.rawExecSQL("SELECT sqlcipher_export('plaintext');");
                    database.rawExecSQL("DETACH DATABASE plaintext;");
                    database.close();
                }
            }
        }
    }

    public static String getUin(String xmlPath) {
        XmlPullParser xmlPullParser = Xml.newPullParser();
        try {
            InputStream inputStream = new FileInputStream(xmlPath);
            xmlPullParser.setInput(inputStream, "utf-8");
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String getName = xmlPullParser.getName();
                if ("int".equals(getName)) {
                    if (("default_uin").equals(xmlPullParser.getAttributeValue(null, "name")))
                        return xmlPullParser.getAttributeValue(null, "value");
                } else {
                    xmlPullParser.nextTag();
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String decrypt(String s) {
        char[] imeiArr = sharedPreferences.getString("IMEI", "").toCharArray();
        int imeiLen = imeiArr.length;
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        char[] enArr = s.toCharArray();
        int enLen = enArr.length;
        for (int i = 0; i < enLen; i++) {
            enArr[i] = (char) (enArr[i] ^ imeiArr[i % imeiLen]);
        }
        return new String(enArr);
    }

    public static String decrypt(byte[] bytes) {
        char[] imeiArr = sharedPreferences.getString("IMEI", "").toCharArray();
        int imeiLen = imeiArr.length;
        if (bytes == null) {
            return "";
        }
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ imeiArr[i % imeiLen]);
        }
        return new String(bytes);
    }

    public static String MD5(String s) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes());
            int i;
            StringBuilder buf = new StringBuilder();
            byte[] b = md5.digest();
            for (byte aB : b) {
                i = aB;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean runSu(ArrayList<String> commands) {
        BufferedOutputStream bufferedOutputStream = null;
        BufferedReader bufferedReader = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            bufferedOutputStream = new BufferedOutputStream(process.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            for (String command : commands) {
                Log.i("TAG", "command run:" + command);
                bufferedOutputStream.write((command + " 2>&1\n").getBytes());
            }
            bufferedOutputStream.write("exit\n".getBytes());
            bufferedOutputStream.flush();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.i("TAG", "command out:" + line);
            }
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean findBinary(String binaryName) {
        if (!TextUtils.isEmpty(binaryName)) {
            for (String path : binaryPaths) {
                File file = new File(path + binaryName);
                if (file.exists())
                    return true;
            }
        }
        return false;
    }
}
