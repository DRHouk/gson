package com.google.gson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * This class contains some test fixtures for Parameterized types. These classes should ideally
 * belong either in the common or functional package, but they are placed here because they need
 * access to package protected elements of com.google.gson.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ParamterizedTypeFixtures {

  public static class MyParameterizedType<T> {
    public final T value;
    public MyParameterizedType(T value) {
      this.value = value;
    }
    public T getValue() {
      return value;
    }

    public String getExpectedJson() {
      String valueAsJson = getExpectedJson(value);
      return String.format("{\"value\":%s}", valueAsJson);
    }

    private String getExpectedJson(Object obj) {
      Class<?> clazz = obj.getClass();
      if (Primitives.isWrapperType(Primitives.wrap(clazz))) {
        return obj.toString();
      } else if (obj.getClass().equals(String.class)) {
        return "\"" + obj.toString() + "\"";
      } else {
        // Try invoking a getExpectedJson() method if it exists
        try {
          Method method = clazz.getMethod("getExpectedJson");
          Object results = method.invoke(obj);
          return (String) results;
        } catch (SecurityException e) {
          throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public int hashCode() {
      return value == null ? 0 : value.hashCode();
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MyParameterizedType other = (MyParameterizedType) obj;
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }
  }

  public static class MyParameterizedTypeInstanceCreator<T>
      implements InstanceCreator<MyParameterizedType<T>>{
    private final T instanceOfT;
    /**
     * Caution the specified instance is reused by the instance creator for each call.
     * This means that the fields of the same objects will be overwritten by Gson.
     * This is usually fine in tests since there we deserialize just once, but quite
     * dangerous in practice.
     *
     * @param instanceOfT
     */
    public MyParameterizedTypeInstanceCreator(T instanceOfT) {
      this.instanceOfT = instanceOfT;
    }
    public MyParameterizedType<T> createInstance(Type type) {
      return new MyParameterizedType<T>(instanceOfT);
    }
  }

  public static class MyParameterizedTypeAdapter<T>
  implements JsonSerializer<MyParameterizedType<T>>, JsonDeserializer<MyParameterizedType<T>> {
    @SuppressWarnings("unchecked")
    public static<T> String getExpectedJson(MyParameterizedType<T> obj) {
      Class<T> clazz = (Class<T>) obj.value.getClass();
      boolean addQuotes = !clazz.isArray() && !Primitives.unwrap(clazz).isPrimitive();
      StringBuilder sb = new StringBuilder("{\"");
      sb.append(obj.value.getClass().getSimpleName()).append("\":");
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append(obj.value.toString());
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append("}");
      return sb.toString();
    }

    public JsonElement serialize(MyParameterizedType<T> src, Type classOfSrc,
        JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      T value = src.getValue();
      json.add(value.getClass().getSimpleName(), context.serialize(value));
      return json;
    }

    @SuppressWarnings("unchecked")
    public MyParameterizedType<T> deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Type genericClass = TypeUtils.getActualTypeForFirstTypeVariable(typeOfT);
      TypeInfo typeInfo = new TypeInfo(genericClass);
      String className = typeInfo.getRawClass().getSimpleName();
      T value = (T) json.getAsJsonObject().get(className).getAsObject();
      if (typeInfo.isPrimitive()) {
        PrimitiveTypeAdapter typeAdapter = new PrimitiveTypeAdapter();
        value = (T) typeAdapter.adaptType(value, typeInfo.getRawClass());
      }
      return new MyParameterizedType<T>(value);
    }
  }

}