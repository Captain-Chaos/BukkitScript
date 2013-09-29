/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.event;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.pepsoft.bukkit.bukkitscript.context.Context;

/**
 *
 * @author pepijn
 */
public class EventManager {
    public Event createEvent(String descriptor, Context context) {
        if ((descriptor == null) || (context == null)) {
            throw new NullPointerException();
        }
        descriptor = descriptor.trim();
        if (descriptor.isEmpty()) {
            throw new IllegalArgumentException();
        }
        StringTokenizer tokenizer = new StringTokenizer(descriptor, ".");
        Object node = context;
        StringBuilder path = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (path.length() > 0) {
                path.append('.');
            }
            path.append(token);
            node = getChildNode(node, token);
            if (node == null) {
                throw new IllegalArgumentException(path + " is null");
            }
        }
        if (! (node instanceof Event)) {
            throw new IllegalArgumentException(descriptor + " is not an event");
        }
        return (Event) node;
    }
    
    private Object getChildNode(Object node, String token) {
        ClassInfo classInfo = getClassInfo(node.getClass());

        // Check if token describes a JavaBean property
        PropertyDescriptor propertyDescriptor = classInfo.properties.get(token);
        if (propertyDescriptor != null) {
            try {
                return propertyDescriptor.getReadMethod().invoke(node);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied reading property \"" + token + "\" of node of type " + node.getClass(), e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException("Exception thrown reading property \"" + token + "\" of node of type " + node.getClass(), cause);
                }
            }
        }

        // Check if token describes an accessible field
        Field field = classInfo.fields.get(token);
        if (field != null) {
            try {
                return field.get(node);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied reading field \"" + token + "\" of node of type " + node.getClass(), e);
            }
        }

        // Check if the node has a get(String) method
        if (classInfo.getMethod != null) {
            try {
                return classInfo.getMethod.invoke(node, token);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied invoking get(\"" + token + "\") on node of type " + node.getClass(), e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException("Exception thrown invoking get(\"" + token + "\") on node of type " + node.getClass(), cause);
                }
            }
        }

        // Give up
        throw new IllegalArgumentException("Unknown token \"" + token + "\" in descriptor");
    }
    
    private ClassInfo getClassInfo(Class<?> clazz) {
        ClassInfo classInfo = classInfoCache.get(clazz);
        if (classInfo == null) {
            classInfo = new ClassInfo(clazz);
            classInfoCache.put(clazz, classInfo);
        }
        return classInfo;
    }
    
    private final Map<Class<?>, ClassInfo> classInfoCache = new HashMap<Class<?>, ClassInfo>();
    
    static class ClassInfo {
        ClassInfo(Class<?> clazz) {
            // Get JavaBean properties
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                for (PropertyDescriptor propertyDescriptor: beanInfo.getPropertyDescriptors()) {
                    properties.put(propertyDescriptor.getName(), propertyDescriptor);
                }
            } catch (IntrospectionException e) {
                throw new RuntimeException("Introspection exception while analysing class " + clazz.getName(), e);
            }
            
            // Get public fields
            for (Field field: clazz.getFields()) {
                fields.put(field.getName(), field);
            }
            
            // Check if the node has a get(String) method
            Method myGetMethod = null;
            try {
                myGetMethod = clazz.getMethod("get", String.class);
            } catch (NoSuchMethodException e) {
                // Continue
            }
            getMethod = myGetMethod;
        }
        
        final Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();
        final Map<String, Field> fields = new HashMap<String, Field>();
        final Method getMethod;
    }
}