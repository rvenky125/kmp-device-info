package com.famas.kmp_device_info.resolver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.famas.kmp_device_info.DeviceInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

/**
 * Instance Id resolver's single purpose is to get the device's Instance Id
 * author: Andres Aguilar
 */
public class DeviceIdResolver {

  private final Context context;

  public DeviceIdResolver(Context context) {
    this.context = context;
  }

  public String getInstanceIdSync() {
    String instanceId = getInstanceIdFromPrefs();

    if (!Objects.equals(instanceId, Build.UNKNOWN)) {
      return instanceId;
    }

    try {
      instanceId = getFirebaseInstanceId();
      setInstanceIdInPrefs(instanceId);
      return instanceId;
    } catch (ClassNotFoundException ignored) {
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
      System.err.println("N/A: Unsupported version of com.google.firebase:firebase-iid in your project.");
    }

    try {
      instanceId = getGmsInstanceId();
      setInstanceIdInPrefs(instanceId);
      return instanceId;
    } catch (ClassNotFoundException ignored) {
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
      System.err.println("N/A: Unsupported version of com.google.android.gms.iid in your project.");
    }

    instanceId = getUUIDInstanceId();
    setInstanceIdInPrefs(instanceId);
    return instanceId;
  }

  String getUUIDInstanceId() {
    return UUID.randomUUID().toString();
  }

  String getInstanceIdFromPrefs() {
    SharedPreferences prefs = DeviceInfo.getRNDISharedPreferences(context);
    return prefs.getString("instanceId", Build.UNKNOWN);
  }

  void setInstanceIdInPrefs(String instanceId) {
    SharedPreferences.Editor editor = DeviceInfo.getRNDISharedPreferences(context).edit();
    editor.putString("instanceId", instanceId);
    editor.apply();
  }

  String getGmsInstanceId() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Class<?> clazz = Class.forName("com.google.android.gms.iid.InstanceID");
    Method method = clazz.getDeclaredMethod("getInstance", Context.class);
    Object obj = method.invoke(null, context.getApplicationContext());
    assert obj != null;
    Method method1 = obj.getClass().getMethod("getId");
    return (String) method1.invoke(obj);
  }

  String getFirebaseInstanceId() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Class<?> clazz = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
    Method method = clazz.getDeclaredMethod("getInstance");
    Object obj = method.invoke(null);
    assert obj != null;
    Method method1 = obj.getClass().getMethod("getId");
    return (String) method1.invoke(obj);
  }
}
