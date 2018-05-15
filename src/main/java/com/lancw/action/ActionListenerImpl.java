package main.java.com.lancw.action;

import main.java.com.lancw.annotation.MaskAnnotation;
import main.java.com.lancw.plugin.MainFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

/**
 * 项目名称：PackagePlugin
 * 类名称：ActionListenerImpl
 * 类描述：
 * 修改备注：
 * <p>
 * @version 1.0
 */
public class ActionListenerImpl implements ActionListener {

    private final MainFrame frame;

    public ActionListenerImpl(MainFrame frame) {
        this.frame = frame;
    }

    @Override
    @MaskAnnotation
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (null != cmd) {
        		if("showLog".equals(cmd)){
        			 frame.showLog();
        		}else if("packing".equals(cmd)){
        			 frame.packing();
        		}else if("doHttpRequest".equals(cmd)){
        			 frame.doHttpRequest();
        		}
        }
    }

}
