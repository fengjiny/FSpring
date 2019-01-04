package com.fjy.framework.servlet;

import com.fjy.framework.anotation.FAutowired;
import com.fjy.framework.anotation.FController;
import com.fjy.framework.anotation.FRequestMapping;
import com.fjy.framework.anotation.FService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class FDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LOCATION = "contextConfigLocation";

    //保存所有配置信息
    private Properties p = new Properties();

    //保存所有被扫描到的类名
    private List<String> classNames = new ArrayList<String>();

    //IOC容器,保存所有初始化的bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存所有url和method的映射关系
    private Map<String , Method> handlerMapping = new HashMap<String, Method>();

    public FDispatcherServlet() {super();}


    /**
     * 初始化,加载配置文件
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));

        //初始化所有相关类的实例，保存到IOC容器
        doInstance();

        //依赖注入
        doAutoWired();

        //构造HandlerMapping
        initHandlerMapping();

        System.out.println("My mvc framework is init");
//        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {



        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]", "").replaceAll(",\\s", "\r\n"));
        }
//        super.doPost(req, resp);
    }

    private void doLoadConfig(String location) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            //读取配置文件
            p.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fis) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doScanner(String packgeName) {
        //将所有包路径转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packgeName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹，继续递归
            if (file.isDirectory()) {
                doScanner(packgeName + "." + file.getName());
            } else {
                classNames.add(packgeName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }


    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(FController.class)) {
                    //默认将首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if(clazz.isAnnotationPresent(FService.class)) {
                    FService service = clazz.getAnnotation(FService.class);
                    String beanName = service.value();

                    //如果用户设置了名字，就用用户设置的
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //如果用户没设置，就按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     * @param str
     * @return
     */
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(FAutowired.class)) {continue;}

                FAutowired autowired = field.getAnnotation(FAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);  //设置私有属性的访问权限
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(FController.class)) continue;

            String baseUrl = "";
            //获取controller的url配置
            if (clazz.isAnnotationPresent(FRequestMapping.class)) {
                FRequestMapping requestMapping = clazz.getAnnotation(FRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //获取method的url的配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //忽略非FRequestMapping的注解
                if (!method.isAnnotationPresent(FRequestMapping.class)) continue;

                //映射URL
                FRequestMapping requestMapping = method.getAnnotation(FRequestMapping.class);
                String url = (baseUrl + requestMapping.value().replaceAll("/+", "/"));
                handlerMapping.put(url, method);
                System.out.println("mapped" + url + "," + method);
            }
        }
    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        if (this.handlerMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数列表
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; ++i) {
            //
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", "");
                    paramValues[i] = value;
                }
            }
        }
        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());

            method.invoke(this.ioc.get(beanName), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
