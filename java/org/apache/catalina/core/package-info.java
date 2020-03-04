package org.apache.catalina.core;

/**
 * 对于Catalina中的servlet容器，首先需要注意的是，共有4种类型的容器。
 *   1）Engine：表示整个Catalina Servlet引擎
 *   2) Host：表示包含有一个或多个Context容器的虚拟主机
 *   3) Context：表示一个Web应用程序。一个Context可以有多个Wrapper
 *   4) Wrapper：表示一个独立的servlet
 * 它们都继承自container接口。这4个接口的标准实现分别是：
 *   1）StandardEngine类
 *   2）StandardHost类
 *   3）StandardContext类
 *   4）StandardWrapper*类
 * 它们都在org apache.catalina.core包内。
 */
