package cn.wildfirechat.utils;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 反映工具类
 * Created by wenbiao.xie on 2016/1/4.
 */
public final class InvokeUtil {
    private static final String TAG = "InvokeUtil";

    public static <T> T newEmptyInstance(Class<? extends T> paramClass)
    {
        try
        {
            return newEmptyInstanceOrThrow(paramClass);
        }
        catch (Exception localException)
        {
            Log.w(TAG, "Meet exception when make instance as a " + paramClass.getSimpleName(),
                    localException);
        }
        return null;
    }

    private static Object getDefaultValue(Class<?> paramClass)
    {
        if ((Integer.TYPE.equals(paramClass)) || (Integer.class.equals(paramClass))
                || (Byte.TYPE.equals(paramClass)) || (Byte.class.equals(paramClass))
                || (Short.TYPE.equals(paramClass)) || (Short.class.equals(paramClass))
                || (Long.TYPE.equals(paramClass)) || (Long.class.equals(paramClass))
                || (Double.TYPE.equals(paramClass)) || (Double.class.equals(paramClass))
                || (Float.TYPE.equals(paramClass)) || (Float.class.equals(paramClass)))
            return Integer.valueOf(0);
        if ((Boolean.TYPE.equals(paramClass)) || (Boolean.class.equals(paramClass)))
            return Boolean.valueOf(false);
        if ((Character.TYPE.equals(paramClass)) || (Character.class.equals(paramClass)))
            return Character.valueOf('\000');
        return null;
    }

    public static <T> T newEmptyInstanceOrThrow(Class<? extends T> paramClass) throws IllegalAccessException,
        InvocationTargetException, InstantiationException {
        int i = 0;
        Constructor[] constructors = paramClass.getDeclaredConstructors();
        if ((constructors == null) || (constructors.length == 0))
            throw new IllegalArgumentException("Can't get even one available constructor for " + paramClass);

        for (Constructor constructor: constructors) {
            constructor.setAccessible(true);
            Class[] arrayOfClass = constructor.getParameterTypes();
            if ((arrayOfClass == null) || (arrayOfClass.length == 0)) {
                return (T) constructor.newInstance(new Object[0]);
            }
        }

        Constructor localConstructor = constructors[0];
        localConstructor.setAccessible(true);
        Class[] paramClasses = localConstructor.getParameterTypes();
        Object[] params = new Object[paramClasses.length];
        while (i < paramClasses.length)
        {
            params[i] = getDefaultValue(paramClasses[i]);
            i++;
        }

        return (T) localConstructor.newInstance(params);
    }

    public static <T> T newInstanceOrThrow(Class<? extends T> clz, Object...params) throws IllegalAccessException,
        InvocationTargetException, InstantiationException {
        Constructor[] constructors = clz.getDeclaredConstructors();
        if ((constructors == null) || (constructors.length == 0))
            throw new IllegalArgumentException("Can't get even one available constructor for " + clz);

        Class[] paramClasses = new Class[params.length];
        Constructor found = null;
        for (Constructor constructor: constructors) {

            Class[] arrayOfClass = constructor.getParameterTypes();
            if (arrayOfClass.length != params.length)
                continue;
            if (params.length == 0) {
                found = constructor;
                break;
            }
            boolean matched = true;
            for (int i = 0; i < params.length; i++) {
                int v = instanceOf(params[0], arrayOfClass[i]);
                if (v == INSTANCE_DENIED) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                found = constructor;
                break;
            }
        }

        if (found != null) {
            found.setAccessible(true);
            return (T) found.newInstance(params);
        }

        throw new NoSuchElementException("no Constructor match it!!");
    }

    public static Object invokeMethod(Object o, String methodName, Object...params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = matchMethod(o.getClass(), methodName, params);
        if (method == null)
            throw new NoSuchMethodException("class " + o.getClass().getCanonicalName() +
                    " cannot find method " + methodName);

        Object out = method.invoke(o, params);
        return out;
    }

    public static Object invokeStaticMethod(String className, String methodName, Object...params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class clz = Class.forName(className);
        Method method = matchMethod(clz, methodName, params);
        if (method == null)
            throw new NoSuchMethodException("class " + className +
                    " cannot find method " + methodName);

        Object out = method.invoke(null, params);
        return out;
    }

    public static Object invokeStaticMethod(Class clz, String methodName, Object...params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = matchMethod(clz, methodName, params);
        if (method == null)
            throw new NoSuchMethodException("class " + clz.getCanonicalName() +
                    " cannot find method " + methodName);

        Object out = method.invoke(null, params);
        return out;
    }

    private static Class getObjectClass(Object o) {
        Class clz = o.getClass();
        Class inner = wrappedClass(clz);
        if (inner != null && inner.isPrimitive()) {
            return inner;
        }

        return clz;
    }

    public static boolean isWrapClass(Class clz) {
        Class inner = wrappedClass(clz);
        if (inner != null)
            return inner.isPrimitive();

        return false;
    }

    public static Class wrappedClass(Class clz) {
        try {
            return ((Class) clz.getField("TYPE").get(null));
        } catch (Exception e) {
            return null;
        }
    }

    public static Method[] methodsForName(Class clz, String name) {
        Method[] methods = clz.getDeclaredMethods();
        if (methods == null || methods.length == 0)
            return null;

        List<Method> out = new ArrayList<>();
        for (Method method: methods) {
            if (method.getName().equals(name)) {
                out.add(method);
            }
        }

        if (out.size() == 0)
            return null;

        return out.toArray(new Method[0]);
    }

    public static Method matchMethod(Class clz, String name, Object...params) {
        Method[] methods = methodsForName(clz, name);
        if (methods == null || methods.length == 0)
            return null;

        Method found = null;
        int maxMatch = 0;
        for (Method method: methods) {
            int v = matchMethodParameterTypes(method, params);
            if ( v > maxMatch) {
                maxMatch = v;
                found = method;
            }
        }

        if (maxMatch == METHOD_MATCH_NONE)
            return null;

        if ((maxMatch & METHOD_MATCH_PUBLIC) == 0 ) {
            found.setAccessible(true);
        }

        return found;
    }

    private final static int INSTANCE_DENIED = 0;
    private final static int INSTANCE_OK = 1;
    private final static int INSTANCE_CONV = 2;

    private static int instanceOf(Object o, Class<?> clz) {
        if ( o == null ) {
            // 基本类型不允许null对象
            if (clz.isPrimitive()) return INSTANCE_DENIED;

            // 空对象可匹配任何对象类型
            return INSTANCE_OK;
        }

        if (clz.isPrimitive()) {

            if (clz == void.class)
                return INSTANCE_DENIED;

            Class wclz = wrappedClass(o.getClass());
            // 非封装类型对象
            if (wclz == null)
                return INSTANCE_DENIED;

            // 基本类型与封装类型完全匹配
            if (wclz == clz)
                return INSTANCE_OK;

            // 基本类型与封装类型完全不匹配
            if (clz == long.class && wclz == int.class)
                return INSTANCE_CONV;

            if (clz == double.class && (wclz == float.class || wclz == long.class || wclz == int.class) )
                return INSTANCE_CONV;

            if (clz == float.class && wclz == int.class)
                return INSTANCE_CONV;

            if (clz == int.class && (wclz == byte.class || wclz == short.class || wclz == char.class) )
                return INSTANCE_CONV;

            return INSTANCE_DENIED;
        }

        return clz.isInstance(o)?INSTANCE_OK: INSTANCE_DENIED;
    }

    private final static int METHOD_MATCH_NONE = 0;
    private final static int METHOD_MATCH_PUBLIC = 0x01;
    private final static int METHOD_MATCH_PARAMS_TYPE = 0x02;
    private final static int METHOD_MATCH_STRICTLY = METHOD_MATCH_PUBLIC | METHOD_MATCH_PARAMS_TYPE;
    private static int matchMethodParameterTypes(Method method, Object...params) {
        Class[] types = method.getParameterTypes();
        int tlen = ArrayUtils.length(types);
        int plen = ArrayUtils.length(params);
        int value = METHOD_MATCH_NONE;

        if (tlen != plen) {
            return METHOD_MATCH_NONE;
        }

        if (plen > 0) {
            int[] pos = new int[plen];
            int size = 0;
            for (int i= 0; i< plen; i++) {
                Object p = params[i];
                int v = instanceOf(p, types[i]);
                if (v == INSTANCE_DENIED) {
                    return METHOD_MATCH_NONE;
                }

                else if (v == INSTANCE_OK)
                    continue;

                else
                    pos[size++] = i;
            }

            if (size > 0) {

                for (int index: pos) {
                    Object p = params[index];

                    if (p instanceof Number) {
                        Number n = (Number) p;
                        if (types[index] == int.class) {
                            params[index] = n.intValue();
                        }
                        else if (types[index] == long.class) {
                            params[index] = n.longValue();
                        }
                        else if (types[index] == double.class) {
                            params[index] = n.doubleValue();
                        }
                        else if (types[index] == float.class) {
                            params[index] = n.floatValue();
                        }
                        else if (types[index] == byte.class) {
                            params[index] = n.byteValue();
                        }
                        else if (types[index] == short.class) {
                            params[index] = n.shortValue();
                        }
                    }

                    else if (p instanceof Character) {
                        char c = (Character)p;
                        if (types[index] == int.class) {
                            params[index] = (int)c;
                        }
                        else if (types[index] == long.class) {
                            params[index] = (long)c;
                        }

                        else if (types[index] == byte.class) {
                            params[index] = (byte)c;
                        }
                        else if (types[index] == short.class) {
                            params[index] = (short)c;
                        }
                    }
                }
            }
        }



        value |= METHOD_MATCH_PARAMS_TYPE;

        if (Modifier.isPublic(method.getModifiers())) {
            value |= METHOD_MATCH_PUBLIC;
        }

        return value;
    }

    public static Object valueOfField(Object o, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        if (TextUtils.isEmpty(fieldName))
            throw new IllegalArgumentException("param fieldName is empty");
        Class clz = o.getClass();
        Field field = fieldByNameRecursive(clz, fieldName);
        if (!Modifier.isPublic (field.getModifiers()) ) {
            field.setAccessible(true);
        }

        return field.get(o);
    }

    @SuppressWarnings("unchecked")
    public static <T> Object valueOfField(T o, Class superClass, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        if (TextUtils.isEmpty(fieldName))
            throw new IllegalArgumentException("param fieldName is empty");

        Class clz = o.getClass();
        if (superClass == null)
            superClass = clz;

        else if (!superClass.isAssignableFrom(clz))
            throw new IllegalArgumentException("superClass not match the object o " + clz.getCanonicalName());

        Field field = superClass.getDeclaredField(fieldName);
        if (!Modifier.isPublic (field.getModifiers()) ) {
            field.setAccessible(true);
        }

        return field.get(o);
    }

    public static Object valueOfStaticField(Class clz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        if (TextUtils.isEmpty(fieldName))
            throw new IllegalArgumentException("param fieldName is empty");
        Field field = fieldByNameRecursive(clz, fieldName);
        if (!Modifier.isPublic (field.getModifiers()) ) {
            field.setAccessible(true);
        }

        return field.get(null);
    }

    public static void setValueOfField(Object o, String fieldName, Object value) throws NoSuchFieldException,
        IllegalAccessException {
        if (TextUtils.isEmpty(fieldName))
            throw new IllegalArgumentException("param fieldName is empty");
        Class clz = o.getClass();
        Field field = fieldByNameRecursive(clz, fieldName);
        if (!Modifier.isPublic (field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
            field.setAccessible(true);
        }

        field.set(o, value);
    }

    public static void setStaticValueOfField(Class clz, String fieldName, Object value) throws NoSuchFieldException,
        IllegalAccessException {
        if (TextUtils.isEmpty(fieldName))
            throw new IllegalArgumentException("param fieldName is empty");
        Field field = fieldByNameRecursive(clz, fieldName);
        if (!Modifier.isPublic (field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
            field.setAccessible(true);
        }

        field.set(null, value);
    }

    public static Field[] fieldsByClassRecursive(Class clz, Class memberClz) throws NoSuchFieldException {
        Class target = clz;
        ArrayList<Field> all = null;

        while (!target.equals(Object.class)) {
            Field[] fields = target.getDeclaredFields();
            if (!ArrayUtils.empty(fields)) {
                for (Field field: fields) {
                    if (field.getDeclaringClass().equals(memberClz)) {
                        if (all == null)
                            all = new ArrayList<>();

                        all.add(field);
                    }
                }
            }

            target = clz.getSuperclass();
        }

        if (all == null || all.isEmpty())
            throw new NoSuchFieldException("no such field for class " + memberClz.getName());

        return all.toArray(new Field[0]);
    }

    public static Field fieldByNameRecursive(Class clz, String fieldName) throws NoSuchFieldException {

        Class target = clz;
        while (!target.equals(Object.class)) {
            try {
                Field field = target.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {
                target = clz.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

//    public static void printAllFields(Class clz) {
//
//        Class target = clz;
//        String prefix = "===";
//        int depth = 1;
//        String p = null;
//        while (!target.equals(Object.class)) {
//            Field[] fields = target.getDeclaredFields();
//            p = StringUtils.repeat(prefix, depth);
//            NLog.i(TAG, "%s%s Fields:", p, target.getName());
//            if (fields != null && fields.length > 0) {
//                for(int i= 0; i< fields.length; i++) {
//                    NLog.i(TAG,"%s Field[%d]: %s%s %s", p, i,
//                            modifiers(fields[i].getModifiers()),
//                            className(fields[i].getType()),
//                            fields[i].getName()
//                    );
//                }
//            }
//        }
//    }

    private static String className(Class clz) {
        if (clz.isPrimitive()) {
            Class s = wrappedClass(clz);
            return s != null? s.getName(): null;
        } else {
            return clz.getName();
        }
    }

    private static String modifiers(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(modifiers)) {
            sb.append("public ");
        }
        else if (Modifier.isPrivate(modifiers)) {
            sb.append("private ");
        }
        else if (Modifier.isProtected(modifiers)) {
            sb.append("protected ");
        }

        if (Modifier.isFinal(modifiers)) {
            sb.append("final ");
        }

        if (Modifier.isStatic(modifiers)) {
            sb.append("static ");
        }

        if (Modifier.isVolatile(modifiers)) {
            sb.append("volatile ");
        }

        return sb.toString();
    }

   /* public static void test(String a) {
        System.out.println("test " + a);
    }*/

    public static void main(String[] args) {
        String test = "okabc";
        try {
            Object value = 1f;
            System.out.println(String.class.isInstance(value));
            Object o = InvokeUtil.invokeMethod(test, "equals", 1);
            System.out.println(o);

            InvokeUtil.invokeStaticMethod(InvokeUtil.class, "test", new Object[]{null});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
