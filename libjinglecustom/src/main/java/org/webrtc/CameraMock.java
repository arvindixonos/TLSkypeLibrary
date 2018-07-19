package org.webrtc;

import android.hardware.Camera;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CameraMock {

    public static Camera.Parameters getEmptyParameters() {
        return getEmptyParameters(getUninitializedCamera());
    }

    public static Camera.Parameters getEmptyParameters(final Camera camera) {
        Camera.Parameters parameters = null;

        if (camera != null) {
            try {
                final Method method = Camera.class.getDeclaredMethod("getEmptyParameters", null);
                final Object object = method.invoke(null, null);
                if (object instanceof Camera.Parameters) {
                    parameters = (Camera.Parameters) object;
                } else {
                    throw new RuntimeException();
                }
            } catch (NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException | RuntimeException e) {

                try {
                    final Constructor<Camera.Parameters> constructor = Camera.Parameters.class
                            .getDeclaredConstructor(null);
                    constructor.setAccessible(true);
                    parameters = constructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException
                        | InstantiationException | IllegalAccessException e1) {
                }
            }
        }

        return parameters;
    }

    public static Camera getUninitializedCamera() {
        Camera camera = null;

        try {
            final Method method = Camera.class.getDeclaredMethod("openUninitialized", null);
            final Object object = method.invoke(null, null);
            if (object instanceof Camera) {
                camera = (Camera) object;
            } else {
                throw new RuntimeException();
            }
        } catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | RuntimeException e) {

            try {
                final Constructor<Camera> constructor = Camera.class.getDeclaredConstructor(null);
                constructor.setAccessible(true);
                camera = constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException
                    | InstantiationException | IllegalAccessException e1) {
            }
        }


        return camera;
    }
}
