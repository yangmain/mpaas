//package ghost.framework.module.util;
//
//import ghost.framework.util.Assert;
//import ghost.framework.util.ClassUtils;
//import ghost.framework.util.StringUtils;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.net.*;
//
///**
// * package: ghost.framework.module.util
// *
// * @Author: 郭树灿{guoshucan-pc}
// * @link: 手机:13715848993, QQ 27048384
// * @Description:
// * @Date: 2019-11-15:1:39
// */
//public abstract class ResourceUtils {
//    public static final String CLASSPATH_URL_PREFIX = "classpath:";
//    public static final String FILE_URL_PREFIX = "file:";
//    public static final String JAR_URL_PREFIX = "jar:";
//    public static final String WAR_URL_PREFIX = "war:";
//    public static final String URL_PROTOCOL_FILE = "file";
//    public static final String URL_PROTOCOL_JAR = "jar";
//    public static final String URL_PROTOCOL_WAR = "war";
//    public static final String URL_PROTOCOL_ZIP = "zip";
//    public static final String URL_PROTOCOL_WSJAR = "wsjar";
//    public static final String URL_PROTOCOL_VFSZIP = "vfszip";
//    public static final String URL_PROTOCOL_VFSFILE = "vfsfile";
//    public static final String URL_PROTOCOL_VFS = "vfs";
//    public static final String JAR_FILE_EXTENSION = ".jar";
//    public static final String JAR_URL_SEPARATOR = "!/";
//    public static final String WAR_URL_SEPARATOR = "*/";
//
//    public ResourceUtils() {
//    }
//
//    public static boolean isUrl(String resourceLocation) {
//        if (resourceLocation == null) {
//            return false;
//        } else if (resourceLocation.startsWith("classpath:")) {
//            return true;
//        } else {
//            try {
//                new URL(resourceLocation);
//                return true;
//            } catch (MalformedURLException var2) {
//                return false;
//            }
//        }
//    }
//
//    public static URL getURL(String resourceLocation) throws FileNotFoundException {
//        Assert.notNull(resourceLocation, "Resource location must not be null");
//        if (resourceLocation.startsWith("classpath:")) {
//            String path = resourceLocation.substring("classpath:".length());
//            ClassLoader cl = ClassUtils.getDefaultClassLoader();
//            URL url = cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path);
//            if (url == null) {
//                String description = "class path resource [" + path + "]";
//                throw new FileNotFoundException(description + " cannot be resolved to URL because it does not exist");
//            } else {
//                return url;
//            }
//        } else {
//            try {
//                return new URL(resourceLocation);
//            } catch (MalformedURLException var6) {
//                try {
//                    return (new File(resourceLocation)).toURI().toURL();
//                } catch (MalformedURLException var5) {
//                    throw new FileNotFoundException("Resource location [" + resourceLocation + "] is neither a URL not a well-formed file path");
//                }
//            }
//        }
//    }
//
//    public static File getFile(String resourceLocation) throws FileNotFoundException {
//        Assert.notNull(resourceLocation, "Resource location must not be null");
//        if (resourceLocation.startsWith("classpath:")) {
//            String path = resourceLocation.substring("classpath:".length());
//            String description = "class path resource [" + path + "]";
//            ClassLoader cl = ClassUtils.getDefaultClassLoader();
//            URL url = cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path);
//            if (url == null) {
//                throw new FileNotFoundException(description + " cannot be resolved to absolute file path because it does not exist");
//            } else {
//                return getFile(url, description);
//            }
//        } else {
//            try {
//                return getFile(new URL(resourceLocation));
//            } catch (MalformedURLException var5) {
//                return new File(resourceLocation);
//            }
//        }
//    }
//
//    public static File getFile(URL resourceUrl) throws FileNotFoundException {
//        return getFile(resourceUrl, "URL");
//    }
//
//    public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
//        Assert.notNull(resourceUrl, "Resource URL must not be null");
//        if (!"file".equals(resourceUrl.getProtocol())) {
//            throw new FileNotFoundException(description + " cannot be resolved to absolute file path because it does not reside in the file system: " + resourceUrl);
//        } else {
//            try {
//                return new File(toURI(resourceUrl).getSchemeSpecificPart());
//            } catch (URISyntaxException var3) {
//                return new File(resourceUrl.getFile());
//            }
//        }
//    }
//
//    public static File getFile(URI resourceUri) throws FileNotFoundException {
//        return getFile(resourceUri, "URI");
//    }
//
//    public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
//        Assert.notNull(resourceUri, "Resource URI must not be null");
//        if (!"file".equals(resourceUri.getScheme())) {
//            throw new FileNotFoundException(description + " cannot be resolved to absolute file path because it does not reside in the file system: " + resourceUri);
//        } else {
//            return new File(resourceUri.getSchemeSpecificPart());
//        }
//    }
//
//    public static boolean isFileURL(URL url) {
//        String protocol = url.getProtocol();
//        return "file".equals(protocol) || "vfsfile".equals(protocol) || "vfs".equals(protocol);
//    }
//
//    public static boolean isJarURL(URL url) {
//        String protocol = url.getProtocol();
//        return "jar".equals(protocol) || "war".equals(protocol) || "zip".equals(protocol) || "vfszip".equals(protocol) || "wsjar".equals(protocol);
//    }
//
//    public static boolean isJarFileURL(URL url) {
//        return "file".equals(url.getProtocol()) && url.getPath().toLowerCase().endsWith(".jar");
//    }
//
//    public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
//        String urlFile = jarUrl.getFile();
//        int separatorIndex = urlFile.indexOf("!/");
//        if (separatorIndex != -1) {
//            String jarFile = urlFile.substring(0, separatorIndex);
//
//            try {
//                return new URL(jarFile);
//            } catch (MalformedURLException var5) {
//                if (!jarFile.startsWith("/")) {
//                    jarFile = "/" + jarFile;
//                }
//
//                return new URL("file:" + jarFile);
//            }
//        } else {
//            return jarUrl;
//        }
//    }
//
//    public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
//        String urlFile = jarUrl.getFile();
//        int endIndex = urlFile.indexOf("*/");
//        if (endIndex != -1) {
//            String warFile = urlFile.substring(0, endIndex);
//            if ("war".equals(jarUrl.getProtocol())) {
//                return new URL(warFile);
//            }
//
//            int startIndex = warFile.indexOf("war:");
//            if (startIndex != -1) {
//                return new URL(warFile.substring(startIndex + "war:".length()));
//            }
//        }
//
//        return extractJarFileURL(jarUrl);
//    }
//
//    public static URI toURI(URL url) throws URISyntaxException {
//        return toURI(url.toString());
//    }
//
//    public static URI toURI(String location) throws URISyntaxException {
//        return new URI(StringUtils.replace(location, " ", "%20"));
//    }
//
//    public static void useCachesIfNecessary(URLConnection con) {
//        con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
//    }
//}