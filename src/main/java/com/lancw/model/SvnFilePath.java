package main.java.com.lancw.model;

import main.java.com.lancw.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
*
* @author xuhuan
*/
public class SvnFilePath {

   private String path;
   private String localFilePath;
   private List<String> pathDirs = new ArrayList<String>();

   /**
    *
    * @param path svn路径
    * @param dirs 打包项目地址数组
    */
   public SvnFilePath(String path, String[] dirs) {
       pathDirs.clear();
       if (dirs != null) {
           String[] tmp = path.split("/");
           path = "";
           for (int i = 2; i < tmp.length; i++) {
               if (i == 2 && tmp[i].contains(".")) {
                   i++;
               }
               path += tmp[i] + "/";
           }
           path = path.replace("src/", "Liems/WEB-INF/classes/");//改成适用Liems项目的路径
           path = path.replace(".java", ".class");
           path = path.substring(path.indexOf("Liems"), path.length() - 1);
       }

       this.path = path;
       Collections.addAll(pathDirs, this.path.split("/"));
       localFilePath = dirs == null ? path : localFilePath;
       if (!(path.endsWith(".classpath") || path.endsWith(".project") || path.endsWith(".mymetadata") || path.endsWith(".bak")) && dirs != null) {
           for (String dir : dirs) {
               dir = dir.replaceAll("\\\\", "/");
               String output = FileUtil.readXML(dir, "output");
               String[] srcs = FileUtil.readXML(dir, "src").split(",");
               if (output != null) {
                   String[] tmpStr = output.replaceAll("\\\\", "/").split("/");
                   List<String> tmpList = new ArrayList<String>();
                   int index = -1;
                   for (String src : srcs) {
                       if ("resources".equals(src) || src.isEmpty()) {
                           continue;
                       }
                       index = pathDirs.indexOf(src);
                       if (index > 0) {
                           break;
                       }
                   }
                   if (index > 0) {//如果路径中包含src目录则将src目录替换成输出目录
                       tmpList.addAll(pathDirs.subList(0, index));
                       Collections.addAll(tmpList, tmpStr);
                       tmpList.addAll(pathDirs.subList(index + 1, pathDirs.size()));
                       pathDirs = tmpList;
                   }
               }
               File file = new File(dir + "/" + Arrays.toString(pathDirs.toArray(new String[pathDirs.size()])).replace("[", "").replace("]", "").replace(", ", "/"));
               file = file.exists() ? file : FileUtil.searchOneFile(dir, pathDirs.toArray(new String[pathDirs.size()]), pathDirs.size() + 3);
               if (file != null && file.exists()) {
                   localFilePath = file.getAbsolutePath();
                   localFilePath = localFilePath.replaceAll("\\\\", "/");
                   int index = localFilePath.indexOf("classes");
                   if (index >= 0) {
                       localFilePath = localFilePath.substring(index + "classes".length());
                   } else {
                       String[] subPath = dir.split("/");
                       String pat = subPath[subPath.length - 1];
                       index = localFilePath.lastIndexOf(pat);
                       if (index >= 0) {
                           localFilePath = localFilePath.substring(index + pat.length());
                       }
                   }
                   localFilePath = localFilePath.replace("src/", "");
                   if (!localFilePath.contains(":")) {
                       localFilePath = System.getProperty("user.dir") + "/" + "temp" + localFilePath;
                   }
                   if (!file.isDirectory()) {//将文件复制到临时目录
                       FileUtil.copy(file.getAbsolutePath(), localFilePath);
                   } else {
                       localFilePath = null;
                   }
                   break;
               }
           }
       }
   }

   /**
    * 本地路径，复制到临时目录之后的路径
    *
    * @return
    */
   public String getLocalFilePath() {
       return localFilePath;
   }

   public void setLocalFilePath(String localFilePath) {
       this.localFilePath = localFilePath;
   }

   @Override
   public boolean equals(Object obj) {
       if (obj instanceof SvnFilePath) {
           SvnFilePath filePath = (SvnFilePath) obj;
           return filePath.getPath().equals(this.path);
       } else {
           return false;
       }
   }

   @Override
   public int hashCode() {
       return this.path.hashCode();
   }

   public String getPath() {
       return path;
   }

}
