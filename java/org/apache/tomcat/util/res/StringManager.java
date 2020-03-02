/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.res;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An internationalization / localization helper class which reduces
 * the bother of handling ResourceBundles and takes care of the
 * common cases of message formating which otherwise require the
 * creation of Object arrays and such.
 *
 * <p>The StringManager operates on a package basis. One StringManager
 * per package can be created and accessed via the getManager method
 * call.
 *
 * <p>The StringManager will look for a ResourceBundle named by
 * the package name given plus the suffix of "LocalStrings". In
 * practice, this means that the localized information will be contained
 * in a LocalStrings.properties file located in the package
 * directory of the class path.
 *
 * <p>Please see the documentation for java.util.ResourceBundle for
 * more information.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Mel Martinez [mmartinez@g1440.com]
 * @see java.util.ResourceBundle
 */

/**
 * Tomcat处理错误消息的方法是将错误消息存储在一个properties文件中，便于读取和编辑。
 * 但是Tomcat中有几百个类。若是将所有类使用的错误消息都存储在一个大的properties属
 * 性文件中，并维护这个文件将会是一场噩梦。为了避免这种情况，Tomcat将properties文
 * 件划分到不同的包中。例如，org.apache.catahna.connector包下的properties属
 * 性文件包含该包中任何类可能抛出的所有的异常消息。
 *
 * 每个properties文件都是用org.apache.tomcat.util.StringManager类的一个实
 * 例来处理的。当Tomcat运行时·会产生StringManager类的很多实例，每个实例都会读取某
 * 个包下的指定properties文件。此外，由于Tomcat非常受欢迎，因此对错误消息进行国际
 * 化处理也是有必要的，当前共有三种语言得到支持。使用英文版错误消息的properties文件
 * 命名为 LocalStnngs.properties其他两种语言是西班牙语和日语，错误消息文件分别名
 * 为LocalStringses.properties和LocalStnngs_Ja.properties当包中的某个类
 * 需要在其包内的properties文件中查找错娱消息时，它会先获取对应的 StringManager实例。
 * 但是，同一个包下的许多类会使用同一个StringManager实例，若是为每个要查找错误消息的
 * 对象创建一个StringManger实例是很浪费资源的。因此，设计StringManager类的实例被
 * 包内所有的对象共享。若你对设计模式比较熟悉的话，你可能已经猜到了，StringManager是
 * 单例类。StringManager只有一个私有的构造函数，这样就不能在类的外部通过关键字new来
 * 实例化它了。只能通过调用其公共静态方法getManager()来获得其实例，该方法需要一个指明
 * 了包名的参数。每个StringManager实例都会以这个包名作为其键，存储在一个Hashtable中。
 */
public class StringManager {

    private static int LOCALE_CACHE_SIZE = 10;

    /**
     * The ResourceBundle for this StringManager.
     */
    private final ResourceBundle bundle;
    private final Locale locale;


    /**
     * Creates a new StringManager for a given package. This is a
     * private method and all access to it is arbitrated by the
     * static getManager method call so that only one StringManager
     * per package will be created.
     *
     * @param packageName Name of package to create StringManager for.
     */
    private StringManager(String packageName, Locale locale) {
        String bundleName = packageName + ".LocalStrings";
        ResourceBundle bnd = null;
        try {
            bnd = ResourceBundle.getBundle(bundleName, locale);
        } catch (MissingResourceException ex) {
            // Try from the current loader (that's the case for trusted apps)
            // Should only be required if using a TC5 style classloader structure
            // where common != shared != server
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                try {
                    bnd = ResourceBundle.getBundle(bundleName, locale, cl);
                } catch (MissingResourceException ex2) {
                    // Ignore
                }
            }
        }
        bundle = bnd;
        // Get the actual locale, which may be different from the requested one
        if (bundle != null) {
            Locale bundleLocale = bundle.getLocale();
            if (bundleLocale.equals(Locale.ROOT)) {
                this.locale = Locale.ENGLISH;
            } else {
                this.locale = bundleLocale;
            }
        } else {
            this.locale = null;
        }
    }


    /**
     * Get a string from the underlying resource bundle or return null if the
     * String is not found.
     *
     * @param key to desired resource String
     *
     * @return resource String matching <i>key</i> from underlying bundle or
     *         null if not found.
     *
     * @throws IllegalArgumentException if <i>key</i> is null
     */
    public String getString(String key) {
        if (key == null){
            String msg = "key may not have a null value";
            throw new IllegalArgumentException(msg);
        }

        String str = null;

        try {
            // Avoid NPE if bundle is null and treat it like an MRE
            if (bundle != null) {
                str = bundle.getString(key);
            }
        } catch (MissingResourceException mre) {
            //bad: shouldn't mask an exception the following way:
            //   str = "[cannot find message associated with key '" + key +
            //         "' due to " + mre + "]";
            //     because it hides the fact that the String was missing
            //     from the calling code.
            //good: could just throw the exception (or wrap it in another)
            //      but that would probably cause much havoc on existing
            //      code.
            //better: consistent with container pattern to
            //      simply return null.  Calling code can then do
            //      a null check.
            str = null;
        }

        return str;
    }


    /**
     * Get a string from the underlying resource bundle and format
     * it with the given set of arguments.
     *
     * @param key  The key for the required message
     * @param args The values to insert into the message
     *
     * @return The request string formatted with the provided arguments or the
     *         key if the key was not found.
     */
    public String getString(final String key, final Object... args) {
        String value = getString(key);
        if (value == null) {
            value = key;
        }

        MessageFormat mf = new MessageFormat(value);
        mf.setLocale(locale);
        return mf.format(args, new StringBuffer(), null).toString();
    }


    /**
     * Identify the Locale this StringManager is associated with.
     *
     * @return The Locale associated with the StringManager
     */
    public Locale getLocale() {
        return locale;
    }


    // --------------------------------------------------------------
    // STATIC SUPPORT METHODS
    // --------------------------------------------------------------

    private static final Map<String, Map<Locale,StringManager>> managers =
            new Hashtable<>();


    /**
     * Get the StringManager for a given class. The StringManager will be
     * returned for the package in which the class is located. If a manager for
     * that package already exists, it will be reused, else a new
     * StringManager will be created and returned.
     *
     * @param clazz The class for which to retrieve the StringManager
     *
     * @return The instance associated with the package of the provide class
     */
    public static final StringManager getManager(Class<?> clazz) {
        return getManager(clazz.getPackage().getName());
    }


    /**
     * Get the StringManager for a particular package. If a manager for
     * a package already exists, it will be reused, else a new
     * StringManager will be created and returned.
     *
     * @param packageName The package name
     *
     * @return The instance associated with the given package and the default
     *         Locale
     */
    public static final StringManager getManager(String packageName) {
        return getManager(packageName, Locale.getDefault());
    }


    /**
     * Get the StringManager for a particular package and Locale. If a manager
     * for a package/Locale combination already exists, it will be reused, else
     * a new StringManager will be created and returned.
     *
     * @param packageName The package name
     * @param locale      The Locale
     *
     * @return The instance associated with the given package and Locale
     */
    public static final synchronized StringManager getManager(
            String packageName, Locale locale) {

        Map<Locale,StringManager> map = managers.get(packageName);
        if (map == null) {
            /*
             * Don't want the HashMap to be expanded beyond LOCALE_CACHE_SIZE.
             * Expansion occurs when size() exceeds capacity. Therefore keep
             * size at or below capacity.
             * removeEldestEntry() executes after insertion therefore the test
             * for removal needs to use one less than the maximum desired size
             *
             */
            // accessOrder值表示：false-基于插入顺序；true-基于访问顺序
            map = new LinkedHashMap<Locale,StringManager>(LOCALE_CACHE_SIZE, 1, true) {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<Locale,StringManager> eldest) {
                    if (size() > (LOCALE_CACHE_SIZE - 1)) {
                        return true;
                    }
                    return false;
                }
            };
            managers.put(packageName, map);
        }

        StringManager mgr = map.get(locale);
        if (mgr == null) {
            mgr = new StringManager(packageName, locale);
            map.put(locale, mgr);
        }
        return mgr;
    }


    /**
     * Retrieve the StringManager for a list of Locales. The first StringManager
     * found will be returned.
     *
     * @param packageName      The package for which the StringManager was
     *                         requested
     * @param requestedLocales The list of Locales
     *
     * @return the found StringManager or the default StringManager
     */
    public static StringManager getManager(String packageName,
            Enumeration<Locale> requestedLocales) {
        while (requestedLocales.hasMoreElements()) {
            Locale locale = requestedLocales.nextElement();
            StringManager result = getManager(packageName, locale);
            if (result.getLocale().equals(locale)) {
                return result;
            }
        }
        // Return the default
        return getManager(packageName);
    }
}
