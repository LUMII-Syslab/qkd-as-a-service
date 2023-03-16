package org.bouncycastle.tls.injection.signaturespi;


import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 *
 * #pqc-tls #injection
 *
 * @author Sergejs Kozlovics
 */
public class InjectedSignatureSpi extends java.security.SignatureSpi
{
    public interface Factory {
        java.security.SignatureSpi newInstance(PublicKey publicKey);
    }
    //private static Map<Class, Factory> algFactories
      //  = new HashMap<>(); // the same map for all instances
    private static Vector<Factory> factories = new Vector<>();

    public static void addFactory(Factory factory) {
        factories.add(factory);
        //algFactories.put(publicKeyClass, factory);
    }

    private java.security.SignatureSpi delegate = null; // will be initialized in engineInitVerify

    public InjectedSignatureSpi()
    {
    }

    private Method findDirectOrInheritedMethod(Class c, String methodName, Class... args) {
        Method m = null;
        while (c!=null) {
            System.out.println(" ===> " + c.getName());
            for (Method mm : c.getDeclaredMethods()) {
                if (mm.getName().equals(methodName) && (args.length == mm.getParameterTypes().length))
                    m = mm;
            }
/*            try {
                m = c.getMethod(methodName, args);
            } catch (Exception ee) {
                ee.printStackTrace();
            }*/
            if (m!=null)
                break;
            c = c.getSuperclass();
        }
        return m;
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException
    {

        delegate = null; // not found yet
        for (Factory f : factories) {
            try {
                delegate = f.newInstance(publicKey);
            }
            catch (Exception e) {
                e.printStackTrace();
                // SignatureSpi could not been created with this factory, continue with the next one
            }
            if (delegate != null)
                break;
        }
        if (delegate == null) {
            throw new InvalidKeyException("No known SignatureSpi for the passed public key of type "+publicKey.getClass().getName());
        }

        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineInitVerify", PublicKey.class);
        if (m==null)
            throw new RuntimeException("Method engineInitVerify not found");

        try {
            m.setAccessible(true);
            m.invoke(delegate, publicKey);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineInitSign", PrivateKey.class);
        if (m==null)
            throw new RuntimeException("Method engineInitSign not found");

        try {
            m.setAccessible(true);
            m.invoke(delegate, privateKey);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineUpdate", Byte.TYPE);
        if (m==null)
            throw new RuntimeException("Method engineUpdate(1) not found");

        try {
            m.setAccessible(true);
            m.invoke(delegate, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineUpdate", Array.class, Integer.TYPE, Integer.TYPE);
        if (m==null)
            throw new RuntimeException("Method engineUpdate(3) not found");
        try {
            m.setAccessible(true);
            m.invoke(delegate, b, off, len);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineSign");
        if (m==null)
            throw new RuntimeException("Method engineSign not found");

        try {
            m.setAccessible(true);
            return (byte[])m.invoke(delegate);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineVerify", Array.class);
        if (m==null)
            throw new RuntimeException("Method engineVerify not found");

        try {
            m.setAccessible(true);
            return (boolean) m.invoke(delegate, sigBytes);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineSetParameter", String.class, Object.class);
        if (m==null)
            throw new RuntimeException("Method engineSetParameter not found");

        try {
            m.setAccessible(true);
            m.invoke(delegate, param, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        Class c = delegate.getClass(); // searching for the method in the class or in base classes
        Method m = findDirectOrInheritedMethod(c, "engineGetParameter", String.class);
        if (m==null)
            throw new RuntimeException("Method engineGetParameter not found");

        try {
            m.setAccessible(true);
            return m.invoke(delegate, param);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
