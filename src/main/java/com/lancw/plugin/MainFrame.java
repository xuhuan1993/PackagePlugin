/*
 * 文 件 名:  MainFrame.java
 * 版    权:  Copyright YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  lancw
 * 修改时间:  2014-5-6
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */
package main.java.com.lancw.plugin;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.CremeCoffeeSkin;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.datePicker.DatePicker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import main.java.com.lancw.action.ActionListenerImpl;
import main.java.com.lancw.action.ListSelectionListenerImpl;
import main.java.com.lancw.handler.AnnotationInvocationHandler;
import main.java.com.lancw.model.HttpConfig;
import main.java.com.lancw.model.SvnFilePath;
import main.java.com.lancw.util.EncodeUtils;
import main.java.com.lancw.util.FileUtil;
import main.java.com.lancw.util.FormatUtil;
import main.java.com.lancw.util.PropertiesUtil;
import main.java.com.lancw.util.SerializableUtil;

/**
 *
 * @author xuhuan
 */
public class MainFrame extends javax.swing.JFrame {

	static {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	private String lastDatabase = "";
	private boolean forceQuery = false;
	private SVNClientManager manager;
	private final DatePicker enDatePicker;
	private final DatePicker beginDatePicker;
	private final ActionListenerImpl actionlistenerImpl;
	private final ListSelectionListenerImpl listSelectionListenerImpl;
	private final JFileChooser jfc = new JFileChooser();
	private final List<String> logs = new ArrayList<String>();
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private static final Set<SvnFilePath> SVNFILEPATHS = new LinkedHashSet<SvnFilePath>();
	public static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
	private final HashMap<Long, Map<String, SVNLogEntryPath>> detailData = new LinkedHashMap<Long, Map<String, SVNLogEntryPath>>();

	/**
	 * Creates new form MainFrame
	 */
	public MainFrame() {
		long time = System.currentTimeMillis();
		initComponents();
		beginDatePicker = new DatePicker(this, true, beginDate);
		enDatePicker = new DatePicker(this, true, endDate);
		actionlistenerImpl = new ActionListenerImpl(this);
		listSelectionListenerImpl = new ListSelectionListenerImpl(this);
		initData();
		LOGGER.log(Level.INFO, "数据初始化完成，耗时{0}毫秒", (System.currentTimeMillis() - time));
	}

	private void initData() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				setMinimumSize(new Dimension(800, 600));
				setTitle("辅助工具 v1.0");
				setLocationRelativeTo(null);
				setDefaultCloseOperation(EXIT_ON_CLOSE);
				Calendar c = Calendar.getInstance();
				sdf.applyPattern("yyyy-MM-dd");

				endDate.setText(sdf.format(c.getTime()));
				c.add(Calendar.DATE, -2);
				beginDate.setText(sdf.format(c.getTime()));

				historyList.setModel(new DefaultListModel());
				historyList.addListSelectionListener(listSelectionListenerImpl);

				svnName.setText(PropertiesUtil.getProperty(SVN_USER_NAME));
				svnPassword.setText(PropertiesUtil.getProperty(SVN_PASSWORD));
				outputPath.setText(PropertiesUtil.getProperty(OUTPUT_PATH));
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				String dir = PropertiesUtil.getProperty(PROJECT_PATH);
				jfc.setCurrentDirectory(new File(dir));
				path.setText(dir);
				isWebProject.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(IS_WEB_PROJECT, "true")));
				rootNameProject.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(IS_ADD_PROJECT_NAME, "false")));
				createFileList.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(IS_CREATE_FILE_LIST, "true")));
				xmlToInf.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(IS_XML_TO_WEB_INF, "false")));
				detailTable.getColumnModel().getColumn(0).setPreferredWidth(600);
				ButtonGroup bg1 = new ButtonGroup();
				bg1.add(radioDES3);
				bg1.add(radioMD5);
				bg1.add(radioSHA);
				ButtonGroup bg2 = new ButtonGroup();
				bg2.add(radioGet);
				bg2.add(radioPost);

				logTable.getSelectionModel().addListSelectionListener(listSelectionListenerImpl);
				detailTable.getSelectionModel().addListSelectionListener(listSelectionListenerImpl);
				initLogs();
				initItem();
				if (System.getProperty("java.version").startsWith("1.8")) {
					logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
					detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
				}
			}
		});
	}

	/**
	 * 通过此方法添加的监听将通过代理 调用前显示遮罩
	 * <p>
	 */
	public void initActionPerform() {
		InvocationHandler handler = new AnnotationInvocationHandler(this, this.getActionlistenerImpl());
		ActionListenerImpl c = this.getActionlistenerImpl();
		ActionListener impl = (ActionListener) Proxy.newProxyInstance(c.getClass().getClassLoader(),
				c.getClass().getInterfaces(), handler);
		showLogBtn.addActionListener(impl);
		packBtn.addActionListener(impl);
		httpRequestBtn.addActionListener(impl);
	}

	public JList getHistoryList() {
		return historyList;
	}

	public void historyListChange(ListSelectionEvent e) {
		try {
			HttpConfig hc = FileUtil.getConfig((String) historyList.getSelectedValue());
			if (hc == null) {
				return;
			}
			historyName.setText(hc.getName());
			url.setText(hc.getUrl());
			header.setText(hc.getHeaderStr());
			data.setText(hc.getParameterStr());
			toJson.setSelected(hc.getPackHead());
			postXML.setSelected(hc.getSendXML());
			toLower.setSelected(hc.getLowercaseEncode());
			encodeField.setText(hc.getEncodeFieldName());
			contentType.setSelectedItem(hc.getContentType() == null ? "default" : hc.getContentType());
			key.setText(hc.getEncodeKey());
			String method = hc.getRequestType();
			String encode = hc.getEncodeType();
			if ("get".equals(method)) {
				radioGet.setSelected(true);
			} else {
				radioPost.setSelected(true);
			}
			if (null != encode) {
				if("sha".equals(encode)){
					radioSHA.setSelected(true);
				}else if("md5".equals(encode)){
					radioMD5.setSelected(true);
				}else{
					radioDES3.setSelected(true);
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 表选择事件处理
	 * <p>
	 * 
	 * @param e
	 */
	public void tableValueChanged(ListSelectionEvent e) {
		DefaultListSelectionModel t = (DefaultListSelectionModel) e.getSource();
		if (t.equals(detailTable.getSelectionModel())) {
			if (detailTable.getSelectedRowCount() > 0 && e.getValueIsAdjusting()) {
				statusLabel.setText("共选择了" + logTable.getSelectedRowCount() + "条日志，共选择了"
						+ detailTable.getSelectedRowCount() + "个文件（不选择默认全部[" + detailTable.getRowCount() + "]）");
			}
		} else if (t.equals(logTable.getSelectionModel())) {
			if (logTable.getSelectedRowCount() > 0 && e.getValueIsAdjusting()) {
				int[] cols = logTable.getSelectedRows();
				DefaultTableModel mod = (DefaultTableModel) detailTable.getModel();
				mod.setRowCount(0);
				Map<String, SVNLogEntryPath> map = new HashMap<String, SVNLogEntryPath>();
				for (int i : cols) {
					i = logTable.convertRowIndexToModel(i);// 排序后要转移行号
					Object key1 = logTable.getModel().getValueAt(i, 1);
					map.putAll(detailData.get(Long.valueOf(key1.toString())));
				}
				for (Map.Entry<String, SVNLogEntryPath> entrySet : map.entrySet()) {
					SVNLogEntryPath path1 = entrySet.getValue();
					mod.addRow(new Object[] { path1.getPath(), getType(path1.getType()), path1.getCopyPath(),
							getCopyRevision(path1.getCopyRevision()) });
				}
				statusLabel.setText("共选择了" + logTable.getSelectedRowCount() + "条日志，共选择了"
						+ detailTable.getSelectedRowCount() + "个文件（不选择默认全部[" + detailTable.getRowCount() + "]）");
			}
		}
	}

	public static void addFilePath(SvnFilePath path) {
		SVNFILEPATHS.add(path);
	}


	/**
	 * 初始化日志记录
	 */
	private void initLogs() {
		try {
			File file = new File(System.getProperty("user.dir") + "/logs");
			if (!file.exists()) {
				file.mkdirs();
			}
			FileHandler fileHandler = new FileHandler(file.getPath() + "/log.log");
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(fileHandler);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 显示svn日志
	 */
	public void showLog() {
		long time = System.currentTimeMillis();
		String userName = svnName.getText();
		String password = new String(svnPassword.getPassword());
		String pathStr = path.getText();
		String begin = beginDate.getText();
		String end = endDate.getText();
		try {
			if (userName == null || userName.isEmpty()) {
				throw new Exception("svn用户名不能为空！");
			}
			if (password.isEmpty()) {
				throw new Exception("svn密码不能为空！");
			}
			if (pathStr == null || pathStr.isEmpty()) {
				throw new Exception("项目路径不能为空！");
			}

			saveProperties();
			DefaultSVNOptions options = new DefaultSVNOptions();
			manager = SVNClientManager.newInstance(options, userName, password); // 如果需要用户名密码
			SVNLogClient logClient = manager.getLogClient();
			detailData.clear();
			logs.clear();
			((DefaultTableModel) logTable.getModel()).setRowCount(0);
			((DefaultTableModel) detailTable.getModel()).setRowCount(0);
			sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
			begin += " 00:00:00";
			end += " 23:59:59";
			String[] strs = pathStr.split(";");
			for (String string : strs) {
				File proj = new File(string);
				DirEntryHandler handler = new DirEntryHandler(proj.getName()); // 在svn
				logClient.doLog(new File[] { proj }, SVNRevision.UNDEFINED, SVNRevision.create(sdf.parse(begin)),
						SVNRevision.create(sdf.parse(end)), false, true, 1000, handler); // 列出当前svn地址的目录，对每个文件进行处理
			}
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(this, ex.getLocalizedMessage());
		} finally {
			LOGGER.log(Level.INFO, "\u65e5\u5fd7\u52a0\u8f7d\u5b8c\u6210\uff0c\u7528\u65f6{0}\u6beb\u79d2",
					(System.currentTimeMillis() - time));
		}
	}

	/**
	 * 更新打包详情信息
	 * <p>
	 * 
	 * @param msg
	 */
	public static void updateInfo(final String msg) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				info.append(msg + System.getProperty("line.separator"));
				info.invalidate();
			}
		});
	}

	/**
	 * 初始化httpRequest中已存储的http请求信息及添加输入框右键菜单监听事件
	 */
	private void initItem() {
		Integer index = Integer.valueOf(PropertiesUtil.getProperty(HISTORY_ITEM_INDEX, "0"));
		DefaultListModel lm = (DefaultListModel) historyList.getModel();
		if (!new File(System.getProperty("user.dir") + "/httpConfig.xml").exists()) {
			for (int i = 1; i <= index; i++) {
				lm.addElement(PropertiesUtil.getProperty(HISTORY_NAME + i));
				historyName.setText(PropertiesUtil.getProperty(HISTORY_NAME + i));
				url.setText(PropertiesUtil.getProperty(HISTORY_URL + i));
				header.setText(PropertiesUtil.getProperty(HISTORY_HEADER + i));
				data.setText(PropertiesUtil.getProperty(HISTORY_DATA + i));
				toJson.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(HISTORY_PACKAGE + i)));
				postXML.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(IS_POST_XML + i)));
				toLower.setSelected(Boolean.valueOf(PropertiesUtil.getProperty(HISTORY_TO_LOWER_CASE + i)));
				encodeField.setText(PropertiesUtil.getProperty(HISTORY_FIELD + i));
				key.setText(PropertiesUtil.getProperty(HISTORY_KEY + i));
				String method = PropertiesUtil.getProperty(HISTORY_METHOD + i);
				String encode = PropertiesUtil.getProperty(HISTORY_ENCODE + i);

				HttpConfig hc = new HttpConfig(PropertiesUtil.getProperty(HISTORY_NAME + i), url.getText(),
						charset.getSelectedItem().toString(), header.getText(), data.getText(), method, null);
				hc.setSendXML(postXML.isSelected());
				hc.setEncodeKey(key.getText());
				hc.setEncodeType(encode);
				hc.setEncodeFieldName(encodeField.getText());
				hc.setLowercaseEncode(toLower.isSelected());
				hc.setPackHead(toJson.isSelected());
				try {
					FileUtil.saveHttpConfig(hc);
				} catch (Exception ex) {
					Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
				}

			}
		}
		initHistoryItem();
		MouseListener.addListener(svnName);
		MouseListener.addListener(path);
		MouseListener.addListener(info);
		MouseListener.addListener(historyName);
		MouseListener.addListener(url);
		MouseListener.addListener(header);
		MouseListener.addListener(data);
		MouseListener.addListener(respBody);
		MouseListener.addListener(encodeField);
		MouseListener.addListener(key);
	}

	private void initHistoryItem() {
		DefaultListModel lm = (DefaultListModel) historyList.getModel();
		lm.clear();
		try {
			FileUtil.getConfig("");// 初始化数据
			String hisFilter = historyFilter.getText();
			for (Map.Entry<String, HttpConfig> entry : FileUtil.CONFIG_MAP.entrySet()) {
				String key1 = entry.getKey();
				if (hisFilter != null && !hisFilter.isEmpty()) {
					HttpConfig hc = entry.getValue();
					if (!key1.toLowerCase().contains(hisFilter.toLowerCase())
							&& !hc.getUrl().toLowerCase().contains(hisFilter.toLowerCase())) {
						continue;
					}
				}
				if (key1.contains("登录")) {
					lm.insertElementAt(key1, 0);
				} else {
					lm.addElement(key1);
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		tab = new javax.swing.JTabbedPane();
		jPanel7 = new javax.swing.JPanel();
		packageTabPanel = new javax.swing.JTabbedPane();
		jPanel1 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		beginDate = new javax.swing.JTextField();
		jLabel2 = new javax.swing.JLabel();
		path = new javax.swing.JTextField();
		choosePath = new javax.swing.JButton();
		jLabel3 = new javax.swing.JLabel();
		svnName = new javax.swing.JTextField();
		jLabel4 = new javax.swing.JLabel();
		svnPassword = new javax.swing.JPasswordField();
		jSplitPane1 = new javax.swing.JSplitPane();
		jScrollPane1 = new javax.swing.JScrollPane();
		logTable = new javax.swing.JTable();
		jScrollPane2 = new javax.swing.JScrollPane();
		detailTable = new javax.swing.JTable();
		jLabel5 = new javax.swing.JLabel();
		endDate = new javax.swing.JTextField();
		showLogBtn = new javax.swing.JButton();
		packBtn = new javax.swing.JButton();
		isWebProject = new javax.swing.JCheckBox();
		rootNameProject = new javax.swing.JCheckBox();
		xmlToInf = new javax.swing.JCheckBox();
		createFileList = new javax.swing.JCheckBox();
		statusLabel = new javax.swing.JLabel();
		jLabel17 = new javax.swing.JLabel();
		logFilterName = new javax.swing.JTextField();
		jLabel19 = new javax.swing.JLabel();
		outputPath = new javax.swing.JTextField();
		chooseOutputPath = new javax.swing.JButton();
		jPanel2 = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		info = new javax.swing.JTextArea();
		jPanel3 = new javax.swing.JPanel();
		jLabel6 = new javax.swing.JLabel();
		url = new javax.swing.JTextField();
		jLabel7 = new javax.swing.JLabel();
		jLabel8 = new javax.swing.JLabel();
		jScrollPane4 = new javax.swing.JScrollPane();
		header = new javax.swing.JTextArea();
		jScrollPane5 = new javax.swing.JScrollPane();
		data = new javax.swing.JTextArea();
		jLabel9 = new javax.swing.JLabel();
		jScrollPane6 = new javax.swing.JScrollPane();
		respBody = new javax.swing.JTextArea();
		radioPost = new javax.swing.JRadioButton();
		jLabel10 = new javax.swing.JLabel();
		radioGet = new javax.swing.JRadioButton();
		httpRequestBtn = new javax.swing.JButton();
		jLabel11 = new javax.swing.JLabel();
		radioSHA = new javax.swing.JRadioButton();
		radioMD5 = new javax.swing.JRadioButton();
		radioDES3 = new javax.swing.JRadioButton();
		jLabel12 = new javax.swing.JLabel();
		encodeField = new javax.swing.JTextField();
		jLabel13 = new javax.swing.JLabel();
		key = new javax.swing.JTextField();
		toJson = new javax.swing.JCheckBox();
		jLabel14 = new javax.swing.JLabel();
		jLabel15 = new javax.swing.JLabel();
		historyName = new javax.swing.JTextField();
		saveHistory = new javax.swing.JButton();
		charset = new javax.swing.JComboBox();
		toLower = new javax.swing.JCheckBox();
		postXML = new javax.swing.JCheckBox();
		keepSession = new javax.swing.JCheckBox();
		tokenFieldName = new javax.swing.JTextField();
		analysisResult = new javax.swing.JCheckBox();
		jScrollPane10 = new javax.swing.JScrollPane();
		historyList = new javax.swing.JList();
		historyFilter = new javax.swing.JTextField();
		contentType = new javax.swing.JComboBox();
		databaseInfoBar = new javax.swing.JLabel();
		modelPackage = new javax.swing.JTextField();
		daoPackage = new javax.swing.JTextField();
		sqlMapPackage = new javax.swing.JTextField();
		outPath = new javax.swing.JTextField();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new java.awt.Dimension(932, 631));

		tab.setMinimumSize(new java.awt.Dimension(900, 600));
		tab.setName(""); // NOI18N
		tab.setPreferredSize(new java.awt.Dimension(900, 600));

		jPanel7.setMinimumSize(new java.awt.Dimension(900, 600));
		jPanel7.setPreferredSize(new java.awt.Dimension(900, 600));
		jPanel7.setLayout(new java.awt.GridLayout(1, 1));

		jLabel1.setText("起始日期");

		beginDate.setEditable(false);
		beginDate.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				beginDateMouseClicked(evt);
			}
		});

		jLabel2.setText("项目地址");

		path.setToolTipText("可以同时选择多个项目，需svn客户端检出，eclipse插件检出的无效");

		choosePath.setText("浏览");
		choosePath.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				choosePathActionPerformed(evt);
			}
		});

		jLabel3.setText("SVN用户");

		jLabel4.setText("SVN密码");

		jSplitPane1.setDividerLocation(150);
		jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

		logTable.setAutoCreateRowSorter(true);
		logTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

		}, new String[] { "projectName", "Revision", "Author", "Date", "Message" }) {
			boolean[] canEdit = new boolean[] { false, false, false, false, false };

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return canEdit[columnIndex];
			}
		});
		jScrollPane1.setViewportView(logTable);

		jSplitPane1.setTopComponent(jScrollPane1);

		detailTable.setAutoCreateRowSorter(true);
		detailTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

		}, new String[] { "Path", "Action", "Copy from path", "Revision" }) {
			boolean[] canEdit = new boolean[] { false, false, false, false };

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return canEdit[columnIndex];
			}
		});
		jScrollPane2.setViewportView(detailTable);

		jSplitPane1.setRightComponent(jScrollPane2);

		jLabel5.setText("截止日期");

		endDate.setEditable(false);
		endDate.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				endDateMouseClicked(evt);
			}
		});

		showLogBtn.setText("查看");
		showLogBtn.setActionCommand("showLog");

		packBtn.setText("打包");
		packBtn.setActionCommand("packing");

		isWebProject.setSelected(true);
		isWebProject.setText("class打包到WEB-INF");
		isWebProject.setVisible(false);

		rootNameProject.setText("根路径为项目名");

		xmlToInf.setText("生成脚本（功能暂未实现）");

		createFileList.setText("生成说明文件");
		createFileList.setToolTipText("生成打包的说明文件到txt");

		statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

		jLabel17.setText("日志过滤");

		logFilterName.setToolTipText("只显示用户名，消息包含过滤内容的日志");

		jLabel19.setText("输出目录");

		chooseOutputPath.setText("浏览");
		chooseOutputPath.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chooseOutputPathActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout
				.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(
										statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.addGroup(jPanel1Layout.createSequentialGroup()
										.addGroup(
												jPanel1Layout
														.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(jPanel1Layout.createSequentialGroup()
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																		.addComponent(jLabel3).addComponent(jLabel1))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.TRAILING,
																				false)
																		.addComponent(beginDate,
																				javax.swing.GroupLayout.Alignment.LEADING,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				120, Short.MAX_VALUE)
																		.addComponent(svnName,
																				javax.swing.GroupLayout.Alignment.LEADING))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.TRAILING)
																		.addComponent(jLabel4).addComponent(jLabel5))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING,
																				false)
																		.addComponent(svnPassword).addComponent(endDate,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				100,
																				javax.swing.GroupLayout.PREFERRED_SIZE))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.TRAILING,
																				false)
																		.addGroup(jPanel1Layout.createSequentialGroup()
																				.addComponent(xmlToInf)
																				.addPreferredGap(
																						javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																				.addComponent(rootNameProject)
																				.addPreferredGap(
																						javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																				.addComponent(isWebProject)
																				.addPreferredGap(
																						javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																				.addComponent(createFileList))
																		.addGroup(jPanel1Layout.createSequentialGroup()
																				.addComponent(jLabel17)
																				.addPreferredGap(
																						javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																				.addComponent(logFilterName))))
														.addGroup(jPanel1Layout.createSequentialGroup()
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																		.addComponent(jLabel2).addComponent(jLabel19))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING,
																				false)
																		.addComponent(path,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				647, Short.MAX_VALUE)
																		.addComponent(outputPath))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(jPanel1Layout
																		.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																		.addGroup(
																				javax.swing.GroupLayout.Alignment.TRAILING,
																				jPanel1Layout.createSequentialGroup()
																						.addComponent(choosePath)
																						.addPreferredGap(
																								javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																						.addComponent(showLogBtn))
																		.addGroup(
																				javax.swing.GroupLayout.Alignment.TRAILING,
																				jPanel1Layout.createSequentialGroup()
																						.addComponent(chooseOutputPath)
																						.addPreferredGap(
																								javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																						.addComponent(packBtn)))))
										.addGap(0, 0, Short.MAX_VALUE))
								.addComponent(jSplitPane1)).addContainerGap()));
		jPanel1Layout.setVerticalGroup(
				jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout
						.createSequentialGroup().addGap(8, 8, 8).addGroup(jPanel1Layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel1Layout.createSequentialGroup()
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(svnName, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel3))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(beginDate, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel1)))
								.addGroup(jPanel1Layout.createSequentialGroup()
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel4)
												.addComponent(svnPassword, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel17).addComponent(logFilterName,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel5)
												.addComponent(endDate, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(xmlToInf).addComponent(rootNameProject)
												.addComponent(isWebProject).addComponent(createFileList))
										.addGap(5, 5, 5)
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(choosePath).addComponent(showLogBtn)
												.addComponent(path, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel2))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(chooseOutputPath).addComponent(packBtn)
												.addComponent(outputPath, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel19))))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(statusLabel,
								javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));

		packageTabPanel.addTab("选择打包文件", jPanel1);

		info.setEditable(false);
		info.setColumns(20);
		info.setRows(5);
		jScrollPane3.setViewportView(info);

		javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
		jPanel2.setLayout(jPanel2Layout);
		jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel2Layout.createSequentialGroup().addContainerGap()
						.addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 902, Short.MAX_VALUE)
						.addContainerGap()));
		jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel2Layout.createSequentialGroup().addContainerGap()
						.addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
						.addContainerGap()));

		packageTabPanel.addTab("打包详情", jPanel2);

		jPanel7.add(packageTabPanel);

		tab.addTab("打增量包", jPanel7);

		jPanel3.setMinimumSize(new java.awt.Dimension(900, 600));
		jPanel3.setPreferredSize(new java.awt.Dimension(900, 600));

		jLabel6.setText("请求地址");

		jLabel7.setText("头部参数");

		jLabel8.setText("请求参数");

		header.setColumns(20);
		header.setLineWrap(true);
		header.setRows(1);
		jScrollPane4.setViewportView(header);

		data.setColumns(20);
		data.setLineWrap(true);
		data.setRows(5);
		jScrollPane5.setViewportView(data);

		jLabel9.setText("返回内容");

		respBody.setEditable(false);
		respBody.setColumns(20);
		respBody.setLineWrap(true);
		respBody.setRows(5);
		jScrollPane6.setViewportView(respBody);

		radioPost.setSelected(true);
		radioPost.setText("post");

		jLabel10.setText("请求方式");

		radioGet.setText("get");

		httpRequestBtn.setText("执行");
		httpRequestBtn.setActionCommand("doHttpRequest");

		jLabel11.setText("加密方式");

		radioSHA.setSelected(true);
		radioSHA.setText("sha1");

		radioMD5.setText("md5");

		radioDES3.setText("des3");

		jLabel12.setText("加密字段");

		jLabel13.setText("加密key");

		toJson.setSelected(true);
		toJson.setText("头部参数打包成token包的JSON串");

		jLabel14.setText("历史记录");

		jLabel15.setText("记录名称");

		saveHistory.setText("保存记录");
		saveHistory.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				saveHistoryActionPerformed(evt);
			}
		});

		charset.setModel(
				new javax.swing.DefaultComboBoxModel(new String[] { "UTF-8", "GBK", "gb18030", "iso-8859-1" }));

		toLower.setText("加密串小写（MD5,SHA1）");

		postXML.setText("POST XML");

		keepSession.setText("保持本次token");
		keepSession.setToolTipText("后续请求中头部参数中token将替换为本次的token");

		tokenFieldName.setText("token");
		tokenFieldName.setToolTipText("保持的token将替换掉头部参数为该名称字段对应参数值");

		analysisResult.setText("生成markdown文档");

		historyList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		historyList.setDragEnabled(true);
		historyList.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				historyListMouseClicked(evt);
			}
		});
		jScrollPane10.setViewportView(historyList);

		historyFilter.setToolTipText("根据名称或url过滤接口列表");
		historyFilter.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				historyFilterKeyReleased(evt);
			}
		});

		contentType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "default", "text/xml",
				"application/json", "multipart/form-data", "application/x-www-form-urlencoded" }));

		javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
		jPanel3.setLayout(jPanel3Layout);
		jPanel3Layout
				.setHorizontalGroup(
						jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
								jPanel3Layout
										.createSequentialGroup()
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(
														jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 208,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addGroup(jPanel3Layout.createSequentialGroup()
														.addComponent(jLabel14)
														.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(historyFilter,
																javax.swing.GroupLayout.PREFERRED_SIZE, 156,
																javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout
														.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
														.addComponent(jLabel9).addComponent(jLabel6)
														.addComponent(jLabel7).addComponent(jLabel8)
														.addComponent(jLabel15))
														.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(jPanel3Layout
																.createParallelGroup(
																		javax.swing.GroupLayout.Alignment.LEADING)
																.addComponent(jScrollPane5)
																.addGroup(jPanel3Layout.createSequentialGroup()
																		.addComponent(historyName,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				240,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(saveHistory)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(keepSession)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(tokenFieldName,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				100,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(charset,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addGap(0, 27, Short.MAX_VALUE))
																.addComponent(
																		jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING)
																.addComponent(url).addComponent(jScrollPane4)))
												.addGroup(jPanel3Layout
														.createSequentialGroup().addGroup(jPanel3Layout
																.createParallelGroup(
																		javax.swing.GroupLayout.Alignment.LEADING)
																.addComponent(jLabel10).addComponent(jLabel12)
																.addComponent(jLabel11))
														.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(jPanel3Layout
																.createParallelGroup(
																		javax.swing.GroupLayout.Alignment.LEADING)
																.addGroup(jPanel3Layout.createSequentialGroup()
																		.addComponent(radioGet).addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(radioPost).addGroup(jPanel3Layout
																				.createParallelGroup(
																						javax.swing.GroupLayout.Alignment.LEADING)
																				.addGroup(jPanel3Layout
																						.createSequentialGroup()
																						.addPreferredGap(
																								javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(httpRequestBtn))
																				.addGroup(jPanel3Layout
																						.createSequentialGroup()
																						.addGap(49, 49, 49)
																						.addComponent(analysisResult)
																						.addPreferredGap(
																								javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																						.addComponent(postXML)
																						.addPreferredGap(
																								javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																						.addComponent(contentType,
																								javax.swing.GroupLayout.PREFERRED_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.PREFERRED_SIZE)
																						.addGap(0, 0,
																								Short.MAX_VALUE))))
																.addGroup(jPanel3Layout.createSequentialGroup()
																		.addComponent(encodeField,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				223,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(jLabel13)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(key))
																.addGroup(jPanel3Layout.createSequentialGroup()
																		.addComponent(radioMD5)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(radioDES3)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(radioSHA)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(toJson)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(toLower)
																		.addGap(0, 0, Short.MAX_VALUE)))))
										.addContainerGap()));
		jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel3Layout.createSequentialGroup().addGap(0, 0, 0)
						.addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel14).addComponent(jLabel15)
								.addComponent(historyName, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(saveHistory)
								.addComponent(charset, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(keepSession)
								.addComponent(tokenFieldName, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(historyFilter, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel3Layout.createSequentialGroup()
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel6)
												.addComponent(url, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
										.addGap(10, 10, 10)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 76,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel7))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel8))
										.addGap(7, 7, 7)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addGroup(jPanel3Layout.createSequentialGroup().addComponent(jLabel9)
														.addGap(0, 0, Short.MAX_VALUE))
												.addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 233,
														Short.MAX_VALUE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(radioPost).addComponent(jLabel10).addComponent(radioGet)
												.addComponent(analysisResult).addComponent(postXML)
												.addComponent(contentType, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(radioSHA).addComponent(radioMD5).addComponent(radioDES3)
												.addComponent(jLabel11).addComponent(toJson).addComponent(toLower))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel3Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(encodeField, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel12).addComponent(jLabel13).addComponent(key,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addComponent(jScrollPane10))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(httpRequestBtn).addContainerGap()));

		tab.addTab("接口调试", jPanel3);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 932, Short.MAX_VALUE)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(tab, javax.swing.GroupLayout.DEFAULT_SIZE, 932, Short.MAX_VALUE)));
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 631, Short.MAX_VALUE)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(
								tab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void choosePathActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_choosePathActionPerformed
		String out = path.getText();
		if (out != null && !out.isEmpty()) {
			jfc.setCurrentDirectory(new File(out.split(";")[0]));
		}
		jfc.setMultiSelectionEnabled(true);
		int opt = jfc.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == opt) {
			File[] files = jfc.getSelectedFiles();
			String paths = "";
			for (File file : files) {
				paths += file.getAbsolutePath() + ";";
			}
			path.setText(paths.substring(0, paths.length() - 1));
		}
	}// GEN-LAST:event_choosePathActionPerformed

	private void endDateMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_endDateMouseClicked
		Point p = endDate.getLocationOnScreen();
		p.y += endDate.getHeight();
		enDatePicker.setLocation(p);
		enDatePicker.setVisible(true);
	}// GEN-LAST:event_endDateMouseClicked

	private void beginDateMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_beginDateMouseClicked
		Point p = beginDate.getLocationOnScreen();
		p.y += beginDate.getHeight();
		beginDatePicker.setLocation(p);
		beginDatePicker.setVisible(true);
	}// GEN-LAST:event_beginDateMouseClicked

	private void saveHistoryActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_saveHistoryActionPerformed
		String name = historyName.getText();
		if (name == null || name.isEmpty()) {
			JOptionPane.showMessageDialog(rootPane, "记录名称不能为空");
			return;
		}
		try {
			HttpConfig hc = new HttpConfig(name, url.getText(), charset.getSelectedItem().toString(), header.getText(),
					data.getText(), radioPost.isSelected() ? "post" : "get", contentType.getSelectedItem().toString());
			hc.setSendXML(postXML.isSelected());
			hc.setEncodeKey(key.getText());
			hc.setEncodeType(radioDES3.isSelected() ? "des3" : radioMD5.isSelected() ? "md5" : "sha");
			hc.setEncodeFieldName(encodeField.getText());
			hc.setLowercaseEncode(toLower.isSelected());
			hc.setPackHead(toJson.isSelected());
			boolean isNew = FileUtil.saveHttpConfig(hc);
			JOptionPane.showMessageDialog(rootPane, "保存成功");
			if (isNew) {
				DefaultListModel lm = (DefaultListModel) historyList.getModel();
				lm.addElement(name);
				historyList.setSelectedIndex(lm.getSize() - 1);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(rootPane, "保存失败：" + ex.getLocalizedMessage());
		}
	}// GEN-LAST:event_saveHistoryActionPerformed

	private void chooseOutputPathActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chooseOutputPathActionPerformed
		String out = outputPath.getText();
		if (out != null && !out.isEmpty()) {
			jfc.setCurrentDirectory(new File(out));
		}
		jfc.setMultiSelectionEnabled(false);
		int opt = jfc.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == opt) {
			outputPath.setText(jfc.getSelectedFile().getAbsolutePath());
		}
	}// GEN-LAST:event_chooseOutputPathActionPerformed

	private void fileChooserActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fileChooserActionPerformed
		String out = outPath.getText();
		if (out != null && !out.isEmpty()) {
			jfc.setCurrentDirectory(new File(out));
		}
		jfc.setMultiSelectionEnabled(false);
		int opt = jfc.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == opt) {
			outPath.setText(jfc.getSelectedFile().getAbsolutePath());
		}
	}// GEN-LAST:event_fileChooserActionPerformed

	private void historyListMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_historyListMouseClicked
		if (MouseEvent.BUTTON3 == evt.getButton()) {
			int index = historyList.getSelectedIndex();
			if (index < 0) {
				return;
			}
			int opr = JOptionPane.showConfirmDialog(rootPane, "确认要删除当前历史记录吗？");
			if (opr == JOptionPane.OK_OPTION) {
				try {
					FileUtil.removeHttpConfig((String) historyList.getSelectedValue());
					DefaultListModel dlm = (DefaultListModel) historyList.getModel();
					dlm.removeElementAt(index);
				} catch (Exception ex) {
					Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}// GEN-LAST:event_historyListMouseClicked

	private void historyFilterKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_historyFilterKeyReleased
		initHistoryItem();
	}// GEN-LAST:event_historyFilterKeyReleased

	public void doHttpRequest() {
		Long[] uids = new Long[] {};
		String tmp = data.getText();
		if (uids.length == 0) {
			doHttpRequest(data.getText());
		} else {// 批量调用某个接口，userId为可变参数
			for (Long uid : uids) {
				doHttpRequest(tmp + "&userId=" + uid.toString());
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * 发http请求
	 *
	 * @param pp
	 */
	public void doHttpRequest(String pp) {
		try {
			HttpClient client = new HttpClient();
			String urlstr = url.getText();
			HttpMethod method = null;
			boolean fileFlag = false;
			ArrayList<Part> parts = new ArrayList<Part>();
			String ct = this.contentType.getSelectedItem().toString();
			if (urlstr != null && !urlstr.isEmpty()) {
				String datastr = pp == null ? data.getText() : pp;
				System.out.println("请求参数：" + datastr);
				if (radioGet.isSelected()) {
					method = new GetMethod(urlstr);
					if (datastr != null && !datastr.isEmpty()) {
						method.setQueryString(datastr);
						respBody.append("queryString:" + datastr + "\r\n");
					}
				} else {
					method = new PostMethod(urlstr);
					if (datastr != null && !datastr.isEmpty()) {
						if (postXML.isSelected()) {
							RequestEntity ent = new StringRequestEntity(datastr, "default".equals(ct) ? "text/xml" : ct,
									charset.getSelectedItem().toString());
							((PostMethod) method).setRequestEntity(ent);
						} else {
							String[] strs = datastr.split("&");
							for (String ss : strs) {
								ss = ss.replace("==", "####");
								String[] tmp = ss.split("=");
								if (tmp.length == 2) {
									tmp[1] = tmp[1].replace("####", "==");
									PostMethod post = (PostMethod) method;
									post.addParameter(tmp[0], tmp[1]);
									respBody.append(tmp[0] + "=" + tmp[1] + "\r\n");
									if (tmp[0].startsWith("FILE_")) {
										fileFlag = true;
										parts.add(new FilePart(tmp[0].replace("FILE_", ""), new File(tmp[1])));
									} else {
										parts.add(new StringPart(tmp[0],
												URLEncoder.encode(tmp[1], (String) charset.getSelectedItem())));
									}
								} else {
									respBody.append("忽略空参数：" + tmp[0] + "\r\n");
								}
							}
						}
					}
				}
			} else {
				Thread.sleep(100);
				throw new Exception("请求地址不能为空");
			}
			if (fileFlag) {
				PostMethod post = (PostMethod) method;
				Part[] ps = new Part[parts.size()];
				for (int i = 0; i < parts.size(); i++) {
					ps[i] = parts.get(i);
				}
				MultipartRequestEntity fileEntity = new MultipartRequestEntity(ps, post.getParams());
				post.setRequestEntity(fileEntity);
			}
			String headerstr = header.getText();
			if (headerstr != null && !headerstr.isEmpty()) {
				Random random = new Random();
				String nonce = random.nextInt(999999) + "";
				headerstr = headerstr.replaceAll("random", nonce);
				sdf.applyPattern("yyyyMMddHHmmss");
				String timestamp = sdf.format(new Date());
				respBody.append("nonce=" + nonce + "\r\ntimestamp=" + timestamp + "\r\n");
				headerstr = headerstr.replaceAll("now", timestamp);
				String[] hs = headerstr.split("&");
				HashMap<String, String> token = new HashMap<String, String>();
				for (String s : hs) {
					String[] tmp = s.split("=");
					if (tmp.length >= 2) {
						if (tmp.length > 2) {
							for (int i = 2; i < tmp.length; i++) {
								tmp[1] += "=" + tmp[i];
							}
						}
						method.addRequestHeader(tmp[0], getEncodeString(tmp[1], tmp[0]));
						if (toJson.isSelected()) {
							token.put(tmp[0], getEncodeString(tmp[1], tmp[0]));
						}
					}
					respBody.append("header:" + method.getRequestHeader(tmp[0]));
				}
				if (toJson.isSelected()) {

					method.addRequestHeader("token", gson.toJson(token));
					respBody.append("header:" + method.getRequestHeader("token"));
				}
			}
			method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, (String) charset.getSelectedItem());
			method.getParams().setParameter(HttpMethodParams.HTTP_ELEMENT_CHARSET, (String) charset.getSelectedItem());
			method.getParams().setParameter("http.protocol.uri-charset", (String) charset.getSelectedItem());
			client.executeMethod(method);
			String result = method.getResponseBodyAsString();
			respBody.append("请求状态:" + method.getStatusCode() + "\r\n");
			if (method.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				respBody.append("跳转地址：" + method.getResponseHeader("Location").getValue() + "\r\n");
			}
			JsonObject json = null;
			if (keepSession.isSelected()) {
				json = gson.fromJson(result.substring(1, result.length() - 1), JsonObject.class);
				if (json.has("result") && json.get("result") instanceof JsonObject) {
					sessionToken = json.getAsJsonObject("result").get("token").getAsString();
					keepSession.setSelected(false);
				}
			}
			respBody.append(FormatUtil.formatJsonOutput(result)
					+ "\r\n----------------------------------------------------\r\n");
			if (analysisResult.isSelected()) {
				String tmp = result.startsWith("\"") ? result.substring(1, result.length() - 1) : result;
				json = gson.fromJson(tmp, JsonObject.class);
				StringBuilder sb = new StringBuilder();
				String[] adds = urlstr.split("/");
				int idx = urlstr.indexOf(adds[3]);
				sb.append("**简要描述：**\n\n- ").append(historyName.getText()).append("\n\n**请求URL：**\n\n- ` ")
						.append(urlstr.substring(idx));
				sb.append(" `\n\n**请求方式：**\n\n- ").append(radioPost.isSelected() ? "POST" : "GET")
						.append(" \n\n**参数：** \n\n|参数名|必选|类型|说明|\n|:----|:---|:-----|-----|\n");
				String param = data.getText();
				if (param != null && !param.trim().isEmpty()) {
					if (ct.contains("json")) {
						JsonObject jo = gson.fromJson(param, JsonObject.class);
						for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
							removeArrayEle(entry.getValue());
						}
						analysisJsonStr(sb, jo);
					} else {
						String[] pms = param.split("&");
						for (String pm : pms) {
							sb.append("|").append(pm.split("=")[0]).append("|  |String|  |\n");
						}
					}
				}
				for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
					removeArrayEle(entry.getValue());
				}
				sb.append("**返回示例**\n\n```\n").append(FormatUtil.formatJsonOutput(json.toString()))
						.append("\n```\n\n**返回参数说明** \n\n|参数名|类型|说明|备注|\n|:-----|:-----|-----| |\n");
				analysisJsonStr(sb, json);
				sb.append(" **备注** \n\n- 更多返回错误代码请看首页的错误代码描述");
				FileWriter writer = new FileWriter(historyName.getText() + ".txt");
				try {
					writer.write(sb.toString());
					writer.flush();
				}finally{
					if(null != writer){
						writer.close();
					}
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
			respBody.append("异常" + ex.getLocalizedMessage());
		}
	}

	private void analysisJsonStr(StringBuilder sb, Object json) {
		LinkedHashMap<Object, Object> map = new LinkedHashMap<Object, Object>();
		Object jo = json;
		if (jo instanceof JsonObject) {
			FileUtil.analysisJson(map, (JsonObject) jo, "");
		} else if (jo instanceof JsonArray) {
			JsonArray ja = (JsonArray) jo;
			Object subjo = ja.get(0);
			if (subjo instanceof JsonObject) {
				FileUtil.analysisJson(map, (JsonObject) subjo, "");
			}
		} else {
			sb.append("**result有误").append(jo);
		}
		analysisMap(map, sb, "");
	}

	/**
	 * 清除集合中其他元素。只保留第一个
	 *
	 * @param val
	 */
	private void removeArrayEle(Object val) {
		if (val instanceof JsonArray) {
			JsonArray arr = (JsonArray) val;
			int len = arr.size();
			for (int i = 0; i < len; i++) {
				if (i > 0) {
					arr.remove(1);
				} else {
					Object v = arr.get(i);
					removeArrayEle(v);
				}
			}
		} else if (val instanceof JsonObject) {
			JsonObject jo = (JsonObject) val;
			for (Entry<String,JsonElement> next : jo.entrySet()) {
				removeArrayEle(next.getValue());
			}
		}
	}

	/**
	 * 解析TreeMap生成表格
	 *
	 * @param map
	 * @param sheet
	 */
	private static void analysisMap(LinkedHashMap<Object, Object> map, StringBuilder sb, String key1) {
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof LinkedHashMap) {
				sb.append("|").append(key).append("|").append(getValueType(value)).append("|  |  |\n");
			} else if (!key.equals(key1)) {
				sb.append("|").append(key).append("|").append(getValueType(value)).append("|  |  |\n");
			}
		}
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key.equals(key1)) {
				continue;
			}
			if (value instanceof LinkedHashMap) {
				sb.append("\n\n**").append(key).append("参数说明** \n\n|参数名|类型|说明|备注|\n|:-----|:-----|-----| |\n");
				analysisMap((LinkedHashMap) value, sb, key.toString());
			}
		}
	}

	private static String getValueType(Object value1) {
		if (value1 instanceof JsonPrimitive) {
			JsonPrimitive jp = (JsonPrimitive) value1;
			if (jp.isNumber() && jp.toString().contains(".")) {
				return "Double";
			}
			return jp.isBoolean() ? "Boolean" : jp.isNumber() ? "Integer" : jp.isString() ? "String" : "String";
		} else if (value1 instanceof JsonObject || value1 instanceof JsonArray || value1 instanceof LinkedHashMap) {
			return "Object[data]";
		}
		return "String";
	}

	/**
	 * 打包
	 */
	public void packing() {
		saveProperties();
		if (detailTable.getRowCount() == 0) {
			JOptionPane.showMessageDialog(rootPane, "未选择任何需要打包的文件。");
			return;
		}
		String outpath = outputPath.getText();
		if (outpath == null || outpath.isEmpty()) {
			JOptionPane.showMessageDialog(rootPane, "未选择输出目录。");
			return;
		}
		packageTabPanel.setSelectedIndex(1);
		final long time = System.currentTimeMillis();

		String jarfile = null;
		try {
			updateInfo("处理中，请稍等....");
			int[] sel = detailTable.getSelectedRows();
			int cot = detailTable.getModel().getRowCount();
			DefaultTableModel mod = (DefaultTableModel) detailTable.getModel();
			SVNFILEPATHS.clear();
			String[] paths = path.getText().split(";");
			if (sel.length > 0) {
				for (int i : sel) {
					int index = detailTable.convertRowIndexToModel(i);
					String val = mod.getValueAt(index, 0).toString();
					if (!"Deleted".equals(mod.getValueAt(index, 1))) {
						SVNFILEPATHS.add(new SvnFilePath(val, paths));
					} else {
						updateInfo("已忽略删除操作：" + val);
					}
				}
			} else if (cot > 0) {
				for (int i = 0; i < cot; i++) {
					int index = detailTable.convertRowIndexToModel(i);
					String val = mod.getValueAt(index, 0).toString();
					if (!"Deleted".equals(mod.getValueAt(index, 1))) {
						SVNFILEPATHS.add(new SvnFilePath(val, paths));
					} else {
						updateInfo("已忽略删除操作：" + val);
					}
				}
			}
			sdf.applyPattern("yyyyMMdd");
			File projectPath = new File(path.getText().split(";")[0]);
			jarfile = outpath + File.separator + projectPath.getName() + "_项目名称_模块名称_" + sdf.format(new Date()) + "_发起人_第几次_接收人";
			int count, innerClass;
			if (createFileList.isSelected()) {
				String from = System.getProperty("user.dir") + "/说明.txt";
				String to = jarfile + "/说明.txt";
				FileUtil.copy(from, to);
			}
			count = 0;
			innerClass = 0;
			for (SvnFilePath sfp : SVNFILEPATHS) {
				String str = sfp.getPath();
				if (str == null) {
					updateInfo("文件不存在或为目录：" + str);
					continue;
				}
				if (isWebProject.isSelected()) {
					
				}
				if (xmlToInf.isSelected()) {
					
				}
				if (rootNameProject.isSelected()) {
					str = projectPath.getName() + "/" + str;
				}
				updateInfo("正在创建：" + str.replace("\\", "/") );
				String path = jarfile + File.separator + str;
				FileUtil.copy(sfp.getLocalFilePath(), path);
				count++;
				
				File f = new File(sfp.getLocalFilePath());
				if (f.isFile()) {
					File sp = f.getParentFile();
					String name = f.getName();
					if (name.contains(".class")) {
						name = name.substring(0, name.indexOf('.'));
						for (File file : sp.listFiles()) {
							if (file.isFile()) {
								if (file.getName().contains(name + "$")) {// 将内部类一并复制
									String tgFile = new File(path).getParentFile().getAbsolutePath() + "/" + file.getName();
									updateInfo("正在创建：" + str.replace("\\", "/").substring(0, str.lastIndexOf("/") + 1) + file.getName() + "[内部类]" );
									FileUtil.copy(file.getAbsolutePath(), tgFile);
									innerClass++;
								}
							}
						}
					}
				}
				Thread.sleep(10);
			}
			updateInfo("打包成功：" + jarfile.replace("\\", "/") + " 共计：" + (count + innerClass) + "个文件,其中包含" + count + "个文件" + innerClass + "个内部类，用时" + (System.currentTimeMillis() - time) + "毫秒");
		} catch (Exception e) {
			FileUtil.deleteDirectory(jarfile);
			LOGGER.log(Level.SEVERE, null, e);
			updateInfo("打包失败：" + e.getLocalizedMessage());
		} finally {
			try {
				SerializableUtil.serializable(FileUtil.FILE_TREE);
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	private String getEncodeString(String str, String fieldName) {
		String encode = encodeField.getText();
		String headTokenFieldName = tokenFieldName.getText();
		if (fieldName.equals(headTokenFieldName) && !sessionToken.isEmpty()) {
			return sessionToken;
		}
		if (!fieldName.equals(encode)) {
			return str;
		}
		String result = encode;
		if (encode != null && !encode.isEmpty()) {
			if (radioSHA.isSelected()) {
				result = EncodeUtils.encodeBySHA(str);
			} else if (radioDES3.isSelected()) {
				result = EncodeUtils.encryptByDES3(str, key.getText());
			} else if (radioMD5.isSelected()) {
				result = EncodeUtils.encodeByMD5(str);
			}
		}
		if (!radioDES3.isSelected()) {
			if (toLower.isSelected()) {
				result = result.toLowerCase();
			} else {
				result = result.toUpperCase();
			}
		}
		return result;
	}

	public ActionListenerImpl getActionlistenerImpl() {
		return actionlistenerImpl;
	}

	public ListSelectionListenerImpl getListSelectionListenerImpl() {
		return listSelectionListenerImpl;
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		/*
		 * Create and display the form
		 */
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				SubstanceLookAndFeel.setSkin(new CremeCoffeeSkin());
				MainFrame frame = new MainFrame();
				frame.initActionPerform();
				// frame.initListSelectionListener();
				frame.setVisible(true);
			}
		});
	}

	private void saveProperties() {
		PropertiesUtil.saveProperty(SVN_USER_NAME, svnName.getText());
		PropertiesUtil.saveProperty(OUTPUT_PATH, outputPath.getText());
		PropertiesUtil.saveProperty(SVN_PASSWORD, new String(svnPassword.getPassword()));
		PropertiesUtil.saveProperty(PROJECT_PATH, path.getText());
		PropertiesUtil.saveProperty(IS_ADD_PROJECT_NAME, rootNameProject.isSelected() + "");
		PropertiesUtil.saveProperty(IS_CREATE_FILE_LIST, createFileList.isSelected() + "");
		PropertiesUtil.saveProperty(IS_WEB_PROJECT, isWebProject.isSelected() + "");
		PropertiesUtil.saveProperty(IS_XML_TO_WEB_INF, xmlToInf.isSelected() + "");
	}

	class DirEntryHandler implements ISVNLogEntryHandler {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		private String projectName = "";

		public DirEntryHandler(String projectName) {
			this.projectName = projectName;
		}

		@Override
		public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
			String filter = logFilterName.getText().toLowerCase();
			String author = logEntry.getAuthor().toLowerCase();
			String msg = logEntry.getMessage().toLowerCase();
			if (filter != null && !filter.isEmpty() && !(author.contains(filter) || msg.contains(filter))) {
				return;
			}
			detailData.put(logEntry.getRevision(), logEntry.getChangedPaths());
			String str1 = projectName + "|" + logEntry.getRevision() + "|" + logEntry.getAuthor() + "|" + sdf.format(logEntry.getDate()) + "|" + logEntry.getMessage();
			logs.add(str1);
			((DefaultTableModel) logTable.getModel()).setRowCount(0);
			((DefaultTableModel) detailTable.getModel()).setRowCount(0);
			for (int i = logs.size() - 1; i >= 0; i--) {
				String str = logs.get(i);
				((DefaultTableModel) logTable.getModel()).addRow(str.split("\\|"));
			}
			for (Object obj : logEntry.getChangedPaths().entrySet()) {
				Map.Entry<String, SVNLogEntryPath> entry = (Map.Entry<String, SVNLogEntryPath>)obj;
				SVNLogEntryPath path = entry.getValue();
				((DefaultTableModel) detailTable.getModel()).addRow(new Object[] { path.getPath(), getType(path.getType()), path.getCopyPath(), getCopyRevision(path.getCopyRevision()) });
			}
		}

	}

	private String getCopyRevision(Long version) {
		return version < 0 ? "" : version.toString();
	}

	private String getType(char type) {
		switch (type) {
		case 'A':
			return "Added";
		case 'M':
			return "Modified";
		case 'D':
			return "Deleted";
		case 'R':
			return "Replaced";
		default:
			return "";
		}
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JCheckBox analysisResult;
	private javax.swing.JTextField beginDate;
	private javax.swing.JComboBox charset;
	private javax.swing.JButton chooseOutputPath;
	private javax.swing.JButton choosePath;
	private javax.swing.JComboBox contentType;
	private javax.swing.JCheckBox createFileList;
	private javax.swing.JTextField daoPackage;
	private javax.swing.JTextArea data;
	private javax.swing.JLabel databaseInfoBar;
	private javax.swing.JTable detailTable;
	private javax.swing.JTextField encodeField;
	private javax.swing.JTextField endDate;
	private javax.swing.JTextArea header;
	private javax.swing.JTextField historyFilter;
	private javax.swing.JList historyList;
	private javax.swing.JTextField historyName;
	private javax.swing.JButton httpRequestBtn;
	private static javax.swing.JTextArea info;
	private javax.swing.JCheckBox isWebProject;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel10;
	private javax.swing.JLabel jLabel11;
	private javax.swing.JLabel jLabel12;
	private javax.swing.JLabel jLabel13;
	private javax.swing.JLabel jLabel14;
	private javax.swing.JLabel jLabel15;
	private javax.swing.JLabel jLabel17;
	private javax.swing.JLabel jLabel19;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel7;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JScrollPane jScrollPane10;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JScrollPane jScrollPane4;
	private javax.swing.JScrollPane jScrollPane5;
	private javax.swing.JScrollPane jScrollPane6;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JCheckBox keepSession;
	private javax.swing.JTextField key;
	private javax.swing.JTextField logFilterName;
	private javax.swing.JTable logTable;
	private javax.swing.JTextField modelPackage;
	private javax.swing.JTextField outPath;
	private javax.swing.JTextField outputPath;
	private javax.swing.JButton packBtn;
	private javax.swing.JTabbedPane packageTabPanel;
	private javax.swing.JTextField path;
	private javax.swing.JCheckBox postXML;
	private javax.swing.JRadioButton radioDES3;
	private javax.swing.JRadioButton radioGet;
	private javax.swing.JRadioButton radioMD5;
	private javax.swing.JRadioButton radioPost;
	private javax.swing.JRadioButton radioSHA;
	private javax.swing.JTextArea respBody;
	private javax.swing.JCheckBox rootNameProject;
	private javax.swing.JButton saveHistory;
	private javax.swing.JButton showLogBtn;
	private javax.swing.JTextField sqlMapPackage;
	private javax.swing.JLabel statusLabel;
	private javax.swing.JTextField svnName;
	private javax.swing.JPasswordField svnPassword;
	private javax.swing.JTabbedPane tab;
	private javax.swing.JCheckBox toJson;
	private javax.swing.JCheckBox toLower;
	private javax.swing.JTextField tokenFieldName;
	private javax.swing.JTextField url;
	private javax.swing.JCheckBox xmlToInf;
	// End of variables declaration//GEN-END:variables
	private final String HISTORY_NAME = "historyName";
	private final String HISTORY_URL = "historyUrl";
	private final String HISTORY_HEADER = "historyHeader";
	private final String HISTORY_DATA = "historyData";
	private final String HISTORY_METHOD = "historyMethod";
	private final String HISTORY_ENCODE = "historyEncode";
	private final String HISTORY_PACKAGE = "historyPackage";
	private final String HISTORY_FIELD = "historyField";
	private final String HISTORY_KEY = "historyKey";
	private final String HISTORY_TO_LOWER_CASE = "historyToLowerCase";
	private final String HISTORY_ITEM_INDEX = "itemIndex";
	private final String SVN_USER_NAME = "userName";
	private final String SVN_PASSWORD = "password";
	private final String PROJECT_PATH = "projectPath";
	private final String IS_WEB_PROJECT = "isWebProject";
	private final String IS_ADD_PROJECT_NAME = "isAddProjectName";
	private final String IS_CREATE_FILE_LIST = "isCreateFileList";
	private final String IS_XML_TO_WEB_INF = "isXMLToWebInf";
	private final String IS_POST_XML = "isPostXML";
	private final String OUTPUT_PATH = "outputPath";
	private static String sessionToken = "";
	private static Gson gson = new GsonBuilder().create();
}
