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
           path = path.replace("src/", "Liems/WEB-INF/classes/");//改成适用Liems项目的路径
           path = path.replace(".java", ".class");
           path = path.substring(path.indexOf("/Liems/") + 1);
       }

       this.path = path;
       Collections.addAll(pathDirs, this.path.split("/"));
       localFilePath = dirs == null ? path : localFilePath;
       if (!(path.endsWith(".classpath") || path.endsWith(".project") || path.endsWith(".mymetadata") || path.endsWith(".bak")) && dirs != null) {//忽略这几类后缀的文件
           for (String dir : dirs) {
               dir = dir.replaceAll("\\\\", "/");
               File file = new File(dir + "/" + Arrays.toString(pathDirs.toArray(new String[pathDirs.size()])).replace("[", "").replace("]", "").replace(", ", "/"));
               file = file.exists() ? file : FileUtil.searchOneFile(dir, pathDirs.toArray(new String[pathDirs.size()]), pathDirs.size() + 3);
               if (file != null && file.exists()) {
                   localFilePath = file.getAbsolutePath();
                   localFilePath = localFilePath.replaceAll("\\\\", "/");
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
