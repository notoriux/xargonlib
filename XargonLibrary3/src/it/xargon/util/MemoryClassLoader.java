package it.xargon.util;

import java.util.HashMap;

public class MemoryClassLoader extends ClassLoader {
   private HashMap<String, Class<?>> icache=null;
   
   public MemoryClassLoader() {
      super(Thread.currentThread().getContextClassLoader());
      icache=new HashMap<String, Class<?>>();
   }
   
   public void addClass(String classname, byte[] classbytes) {
      Class<?> generatedclass=defineClass(classname, classbytes, 0, classbytes.length);
      icache.put(classname, generatedclass);
   }
   
   public void removeClass(String classname) {
      icache.remove(classname);
   }
   
   public boolean contains(String classname) {
      return icache.containsKey(classname);
   }
   
   public String[] getClassList() {
      return icache.keySet().toArray(new String[icache.size()]);
   }
   
   protected Class<?> findClass(String name) throws ClassNotFoundException {
      Class<?> resultclass=icache.get(name);
      if (resultclass==null) resultclass=super.findClass(name);
      return resultclass;
   }
}
