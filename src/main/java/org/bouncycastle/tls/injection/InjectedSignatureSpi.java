package org.bouncycastle.tls.injection;


import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public class InjectedSignatureSpi extends java.security.SignatureSpi
{
    public interface Factory {
        java.security.SignatureSpi newInstance();
    }
    private static Map<Class, Factory> algFactories
        = new HashMap<>(); // the same map for all instances

    public static void addFactory(Class publicKeyClass, Factory factory) {
        algFactories.put(publicKeyClass, factory);
    }

    private java.security.SignatureSpi delegate = null; // will be initialized in engineInitVerify

    public InjectedSignatureSpi()
    {
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException
    {
        Factory f = algFactories.get(publicKey.getClass());
        if (f == null) {
            throw new InvalidKeyException("No known SignatureSpi for the passed public key of type "+publicKey.getClass().getName());
        }

        this.delegate = f.newInstance();
        try {
            Method m = delegate.getClass().getMethod("engineInitVerify", PublicKey.class);
            m.setAccessible(true);
            m.invoke(delegate, publicKey);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        try {
            Method m = delegate.getClass().getMethod("engineInitSign", PrivateKey.class);
            m.setAccessible(true);
            m.invoke(delegate, privateKey);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        try {
            Method m = delegate.getClass().getMethod("engineUpdate", Byte.TYPE);
            m.setAccessible(true);
            m.invoke(delegate, b);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        try {
            Method m = delegate.getClass().getMethod("engineUpdate", Array.class, Integer.TYPE, Integer.TYPE);
            m.setAccessible(true);
            m.invoke(delegate, b);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        try {
            Method m = delegate.getClass().getMethod("engineSign");
            m.setAccessible(true);
            return (byte[])m.invoke(delegate);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        try {
            Method m = delegate.getClass().getMethod("engineVerify", Array.class);
            m.setAccessible(true);
            return (boolean) m.invoke(delegate, sigBytes);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        try {
            Method m = delegate.getClass().getMethod("engineSetParameter", String.class, Object.class);
            m.setAccessible(true);
            m.invoke(delegate, param, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        try {
            Method m = delegate.getClass().getMethod("engineGetParameter", String.class);
            m.setAccessible(true);
            return m.invoke(delegate, param);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
