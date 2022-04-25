package cn.wildfirechat.utils;
import java.lang.reflect.Array;


/**
 * 数组的工具
 * @author Created by wenbiao.xie on 2015/11/17.
 */
public class ArrayUtils {

    /**
     * 根据数组类型的class创建对应类型的数组
     * @param <T> 目标类型
     * @param clazz
     * @param length 数组长度
     * @return
     */
    public static <T> T[] newArrayByArrayClass(Class<T[]> clazz, int length) {
        return (T[]) Array.newInstance(clazz.getComponentType(), length);
    }

    /**
     * 根据普通类型的class创建数组
     * @param <T> 目标类型
     * @param clazz
     * @param length 数组长度
     * @return
     */
    public static <T> T[] newArrayByClass(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }

    public static <T> boolean empty(T[] array) {
        return (array == null || array.length == 0);
    }

    public static <T> int length(T[] array) {
        return (array == null?0: array.length);
    }

    public static <T> T[] combineArray(T[] array1, T[] array2)
    {
        if (empty(array1)) {
            return array2;
        }
        else if (empty(array2)) {
            return array1;
        }

        T[] arrayOfObject = copyOfRange(array1, 0, array1.length + array2.length);
        System.arraycopy(array2, 0, arrayOfObject, array1.length, array2.length);
        return arrayOfObject;
    }

    public static <T> T[] copyOfRange(T[] array, int start, int end)
    {
        if (start > end)
            throw new IllegalArgumentException();
        int length = array == null ? 0: array.length;

        if ((start < 0) || (start >= length))
            throw new ArrayIndexOutOfBoundsException("copyOfRange start >= length");

        int count = end - start;
        int k = Math.min(count, length - start);

        T[] results = (T[]) Array.newInstance(array.getClass().getComponentType(), count);
        System.arraycopy(array, start, results, 0, k);
        return results;
    }

    public static <T> T[] insert(T[] array, int index, T paramT)
    {
        T[] results = (T[]) Array.newInstance(array.getClass().getComponentType(),
                1 + array.length);
        for (int i = 0; i< results.length; i++) {
            if (i < index) {
                results[i] = array[i];
            }
            else if (i == index) {
                results[i] = paramT;
            }
            else {
                results[i] = array[i-1];
            }
        }

        return results;
    }

    public static void main(String[] args) {
        // 判断一个Class是否是数组类型，可以用Class实例的isArray方法。
        String[] byArray = newArrayByArrayClass(String[].class, 10);
        String[] byOne = newArrayByClass(String.class, 10);
    }

}