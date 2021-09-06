package com.ttyc.redis.shard.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yuanzl
 * @date 2021/8/31 3:36 下午
 */
public class StringUtil {
    private final static Pattern linePattern = Pattern.compile("-(\\w)");

    /** 驼峰转下划线,效率比上面高 */
    public static String convertStr(String str) {
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void setField(Object obj,String fileName,String val){
        try {
            Field field = obj.getClass().getDeclaredField(fileName);
            field.setAccessible(true);
            if(field.getType().equals(List.class)){
                List<String> list = Optional.ofNullable((List)field.get(obj)).orElse(new ArrayList());
                list.add((String) val);
                field.set(obj,list);
            }else if(field.getType().equals(Boolean.class)){
                field.set(obj,Boolean.parseBoolean(val));
            }else if(field.getType().equals(Long.class)){
                field.set(obj,Long.parseLong(val));
            }else if(field.getType().equals(Integer.class)){
                field.set(obj,Integer.parseInt(val));
            }else{
                field.set(obj,val);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e){
            e.printStackTrace();
        }
    }
}
