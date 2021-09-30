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

    /**
     * 将list按quantity切分为若干list分组
     *
     * @param list
     * @param limit
     * @return
     */
    public static <T> List<List<T>> groupListByLimit(final List<T> list, int limit) {
        if (limit <= 0) {
            new IllegalArgumentException("Wrong limit.");
        }
        List<List<T>> wrapList = null;
        if (list != null && list.size() > 0) {
            int group = list.size() % limit == 0 ? list.size() / limit : list.size() / limit + 1;
            wrapList = new ArrayList<>(group);
            for (int i = 0; i < group; i++) {
                wrapList.add(list.subList(i * limit, Math.min((i + 1) * limit, list.size())));
            }
        }

        return wrapList;
    }
}
