package com.example.android.nc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class SocketManager {
	private ServerSocket server;
	private ServerSocket s;
	private ServerSocket ss;
	private Handler handler = null;
	private WifiApAdmin wifiAp;
	private Context context;
	private WifiManager mwfmanager;
	private String path;// 文件保存路径
	private String fname;// 单个文件名
	private int i = 0;
	private int recN;

	private ArrayList<String> kk; // 保存连入手机IP
	private ArrayList<String> k;// 保存连入手机IP
	private ArrayList<String> rec;// 保存已接受文件个数
	private ArrayList<String> k2;// 保存连入手机IP
	private File dir;
	private int size;// 单个文件大小
	private boolean flag = true;// 控制是否接收
	private boolean f = true;// 初始化进度条
	private boolean flag1=false;

	public SocketManager(Handler handler, Context context, final String fpath) {
		this.handler = handler;
		this.context = context;

		mwfmanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		dir = new File(Environment.getExternalStorageDirectory().getPath() + "/files/");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		SendMessage(1, 9999);
	}

	
	public void recfile() {
		int port = 9999;
		try {
			s = new ServerSocket(9000);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(int i=0;i<1;i++) {
			try {
				server = new ServerSocket(port-i);
				final int j=i;
				
				Thread receiveFileThread = new Thread(new Runnable() {
					@Override
					public void run() {
					//	SendMessage(0, 9999-j);
						while (flag) {// 接收文件

							ReceiveFile();
						}
					}
				});
			
				receiveFileThread.start();
				
			} catch (Exception e) {
				SendMessage(0, e.getMessage());
			}
		

		
		}
		rec = new ArrayList<String>();

	}
	public void recfile3() {
		int port = 9999;
		try {
			s = new ServerSocket(9000);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(int i=0;i<4;i++) {
			try {
				server = new ServerSocket(port-i);
				final int j=i;
				
				Thread receiveFileThread = new Thread(new Runnable() {
					@Override
					public void run() {
						SendMessage(0, 9999-j);
						while (flag) {// 接收文件

							ReceiveFile();
						}
					}
				});
			
				receiveFileThread.start();
				
			} catch (Exception e) {
				SendMessage(0, e.getMessage());
			}
		

		
		}
		rec = new ArrayList<String>();

	}

	void SendMessage(int what, Object obj) {
		if (handler != null) {
			Message.obtain(handler, what, obj).sendToTarget();
		}
	}

	// 接收文件
	void ReceiveFile() {

		try {
			// 接收文件名
			ServerSocket serverm=server;
			Socket name = serverm.accept();
			InputStream nameStream = name.getInputStream();
			InputStreamReader streamReader = new InputStreamReader(nameStream);
			BufferedReader br = new BufferedReader(streamReader);
			String fileName1 = br.readLine();
			String n = fileName1.substring(0, fileName1.indexOf("#"));
			int recnum = Integer.parseInt(n);
			if (f) {
				SendMessage(4, recnum);
				f = false;
			}
			int recsize = Integer.parseInt(fileName1.substring(fileName1
					.lastIndexOf("b") + 1));
			size = recsize;
			String fileName = fileName1.substring(
					fileName1.lastIndexOf("#") + 1,
					fileName1.lastIndexOf("b") + 1);
			 SendMessage(0, "---" + recsize);

			String savePath = dir + "/" + fileName;
			path = savePath;
			File f = new File(savePath);

			br.close();
			streamReader.close();
			nameStream.close();
			name.close();

			Socket ss = s.accept();
			OutputStream os = ss.getOutputStream();
			OutputStreamWriter outputWriter = new OutputStreamWriter(os);
			BufferedWriter bwName = new BufferedWriter(outputWriter);
			if (f.exists())
				bwName.write("y"+GetMacAddress());
			else
				bwName.write("n"+GetMacAddress());
			bwName.close();
			outputWriter.close();
			os.close();
			fname = fileName;
			if (f.exists()) {
				SendMessage(0, "文件存在:" + fileName);
				SendMessage(5, fileName1.substring(fileName1.indexOf("#") + 1,
						fileName1.lastIndexOf("#")));
				System.out.println(fileName1.substring(
						fileName1.indexOf("#") + 1, fileName1.lastIndexOf("#"))
						+ "===");
			} else {
				// SendMessage(0, "正在接收:" + fileName);

				// 接收文件数据
				Socket data = serverm.accept();
				InputStream dataStream = data.getInputStream();


				FileOutputStream file = new FileOutputStream(savePath, false);
				byte[] buffer = new byte[1024];
				int size = -1;
				while ((size = dataStream.read(buffer)) != -1) {
					file.write(buffer, 0, size);
				}
				file.close();
				dataStream.close();
				data.close();

				SendMessage(0, fileName + " 接收完成");
				SendMessage(5, fileName1.substring(fileName1.indexOf("#") + 1,
						fileName1.lastIndexOf("#")));
			}

			rec.add(fileName);

			/*合并文件*/
			if (rec.size() == recnum) {
//				new FileCut().mergeApkFile(rec,
//						dir + "/" + fname.substring(0, fname.length() - 4));
				new FileCut().Recover(rec,
						dir + "/" + fname.substring(0, fname.length() - 4));
				mwfmanager.setWifiEnabled(false);
				wifiAp = new WifiApAdmin(context);
				while (i < 5000) {
					i++;
				}

				wifiAp.startWifiAp(Constant.HOST_SPOT_SSID,
						Constant.HOST_SPOT_PASS_WORD);

				Thread sendThread = new Thread(new Runnable() {
					@Override
					public void run() {
						wifiAp = new WifiApAdmin(context);
						wifiAp.startWifiAp(Constant.HOST_SPOT_SSID,
								Constant.HOST_SPOT_PASS_WORD);
						SendFile2(rec);
						flag = false;
					}
				});
				sendThread.start();
			}

		} catch (Exception e) {
			SendMessage(0, "接收错误:\n" + e.getMessage());
			File f = new File(path);
			if (f.length() != size)
				f.delete();
		}

	}

	public void SendFile1(final ArrayList<String> fileName,final ArrayList<String> path) {
		ArrayList<String> r = getConnectedIP();
		k = new ArrayList<String>();
		SendMessage(0, "等待手机连接！！");
		FileCut fc = new FileCut(path.get(0), fileName.get(0), ".db");
//		final int num = fc.Cut();
		final int num = fc.Encode();
		while (true) {
			r = getConnectedIP();
			if (r.size() > 0){
				try {
					for (final String ip : r)
					{
						if (k.contains(ip)) {

						} else {
							k.add(ip);
							new Thread() {
								@Override
								public void run() {
									try {
										SendMessage(0, "IP为" + ip + "的手机正在连接");
										currentThread().sleep(3000);
									} catch (Exception e1) {
										e1.printStackTrace();
									}
									for (int j = 1; j < num + 1; j++) {
										try{
											Socket name = new Socket(ip, 9999);
											OutputStream outputName = name.getOutputStream();
											OutputStreamWriter outputWriter = new OutputStreamWriter(outputName);
											BufferedWriter bwName = new BufferedWriter(outputWriter);
											File dir1 = new File(Environment.getExternalStorageDirectory().getPath() + "/cutfiles/");
											String s = dir1 + "/" + fileName.get(0) + j + ".db";
											File f = new File(s);
											bwName.write(num + "#" + j + "#" + fileName.get(0) + j + ".db" + f.length());
											bwName.close();
											outputWriter.close();
											outputName.close();
											name.close();
											//	 SendMessage(0, "正在发送" +
											//	 fileName.get(0)+j);
											Socket rec = new Socket(ip, 9000);
											InputStream is = rec.getInputStream();
											InputStreamReader streamReader = new InputStreamReader(is);
											BufferedReader br = new BufferedReader(streamReader);
											String re = br.readLine();
											String r = re.substring(0, 1);
											String mac=re.substring(1);
											br.close();
											streamReader.close();
											rec.close();
											if (r.equals("y")) {
												SendMessage(0, fileName.get(0) + j + " 发送到" + mac + "完成");
											} else if (r.equals("n")) {
												Socket data = new Socket(ip, 9999);
												OutputStream outputData = data.getOutputStream();
												FileInputStream fileInput = new FileInputStream(s);
												int size = -1;
												byte[] buffer = new byte[1024];
												while ((size = fileInput.read(buffer, 0, 1024)) != -1) {
													outputData.write(buffer, 0, size);
												}
												outputData.close();
												fileInput.close();
												data.close();
												SendMessage(0, fileName.get(0) + j + " 发送到" + mac + "完成");
											}
										}
										catch(Exception e){
											e.getMessage();
										}
									}
								}
							}.start();
						}
					}
				} catch (Exception e) {
				}
			}

		}

	}
	public void SendFile3(final ArrayList<String> fileName,
			final ArrayList<String> path) {
		ArrayList<String> r = getConnectedIP();
		k = new ArrayList<String>();
		SendMessage(0, "等待手机连接！！");
		FileCut fc = new FileCut(path.get(0), fileName.get(0), ".db");
		final int num = fc.Cut();
		while (true) {
			r = getConnectedIP();
			if (r.size() > 0)
				try {
					for (final String ip : r) {
						if (k.contains(ip)) {
						} else {
							k.add(ip);
							new Thread() {
								@Override
								public void run() {
									try {
										SendMessage(0, "IP为" + ip + "的手机正在连接");
										currentThread().sleep(3000);
									} catch (Exception e1) {
										e1.printStackTrace();
									}
									for (int jj = 1; jj < num + 1; jj++) {
										final int j=jj;
										new Thread(){
											public void run() {
												try{
													Socket name = new Socket(ip, 10000-j);
													OutputStream outputName = name
															.getOutputStream();
													OutputStreamWriter outputWriter = new OutputStreamWriter(
															outputName);
													BufferedWriter bwName = new BufferedWriter(
															outputWriter);

													File dir1 = new File(
															Environment
																	.getExternalStorageDirectory()
																	.getPath()
																	+ "/cutfiles/");
													String s = dir1 + "/"
															+ fileName.get(0) + j
															+ ".db";
													File f = new File(s);

													bwName.write(num + "#" + j + "#"
															+ fileName.get(0) + j
															+ ".db" + f.length());

													bwName.close();
													outputWriter.close();
													outputName.close();
													name.close();
												//	 SendMessage(0, "正在发送" +
												//	 fileName.get(0)+j);

													Socket rec = new Socket(ip, 9000);
													InputStream is = rec
															.getInputStream();
													InputStreamReader streamReader = new InputStreamReader(
															is);
													BufferedReader br = new BufferedReader(
															streamReader);
													String re = br.readLine();
													String r = re.substring(0, 1);
													String mac=re.substring(1);
													br.close();
													streamReader.close();
													rec.close();

													if (r.equals("y")) {
														SendMessage(0, fileName.get(0)
																+ j + " 发送到" + mac
																+ "完成");
														
													} else if (r.equals("n")) {
														Socket data = new Socket(ip,
																10000-j);
														OutputStream outputData = data
																.getOutputStream();

														FileInputStream fileInput = new FileInputStream(
																s);

														int size = -1;
														byte[] buffer = new byte[1024];
														while ((size = fileInput.read(
																buffer, 0, 1024)) != -1) {
															outputData.write(buffer, 0,
																	size);
														}
														outputData.close();
														fileInput.close();
														data.close();
														SendMessage(0, fileName.get(0)
																+ j + " 发送到" + mac
																+ "完成");
													}
												}
												catch(Exception e){
													e.getMessage();
												}
												
											};
										}.start();

									
												};

								};

							}.start();

						}
					}
				} catch (Exception e) {
				}
		}

	}
	public void SendFile2(final ArrayList<String> partFileList) {
		ArrayList<String> r2 = getConnectedIP();
		k2 = new ArrayList<String>();
		SendMessage(0, "等待手机连接！！");
		while (true) {
			r2 = getConnectedIP();

			if (r2.size() > 0)

				try {
					System.out.println(r2.size() + "ge");
					for (final String ip : r2) {
						if (k2.contains(ip)) {
						} else {

							k2.add(ip);
							new Thread() {
								public void run() {
									try {
										SendMessage(0, "IP为" + ip + "的手机正在连接");
										currentThread().sleep(3000);
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}

									for (int j = 1; j < partFileList.size() + 1; j++) {

										try {
											Socket name = new Socket(ip, 9999);
											OutputStream outputName = name
													.getOutputStream();
											OutputStreamWriter outputWriter = new OutputStreamWriter(
													outputName);
											BufferedWriter bwName = new BufferedWriter(
													outputWriter);

											File dir1 = new File(
													Environment
															.getExternalStorageDirectory()
															.getPath()
															+ "/files/");
											String s = dir1 + "/"
													+ partFileList.get(j - 1);
											File f = new File(s);

											bwName.write(partFileList.size()
													+ "#" + j + "#"
													+ partFileList.get(j - 1)
													+ f.length());

											bwName.close();
											outputWriter.close();
											outputName.close();
											name.close();
											// SendMessage(0, "正在发送" +
											// partFileList.get(j-1));

											Socket rec = new Socket(ip, 9000);
											InputStream is = rec
													.getInputStream();
											InputStreamReader streamReader = new InputStreamReader(
													is);
											BufferedReader br = new BufferedReader(
													streamReader);
											String re = br.readLine();
											String r = re.substring(0, 1);
											String mac=re.substring(1);
											rec.close();

											if (r.equals("y")) {
											} else if (r.equals("n")) {
												Socket data = new Socket(ip,
														9999);
												OutputStream outputData = data
														.getOutputStream();

												FileInputStream fileInput = new FileInputStream(
														s);

												int size = -1;
												byte[] buffer = new byte[1024];
												while ((size = fileInput.read(
														buffer, 0, 1024)) != -1) {
													outputData.write(buffer, 0,
															size);
												}
												outputData.close();
												fileInput.close();
												data.close();
												SendMessage(0,
														partFileList.get(j - 1)
																+ " 发送到" + mac
																+ "完成");
											}
										} catch (Exception e) {
											e.printStackTrace();
											SendMessage(0,
													"发送错误:\n" + e.getMessage());
										}
									}

								};

							}.start();

						}
					}
				} catch (Exception e) {
				}
		}

	}


	public ArrayList<String> getConnectedIP() {
		ArrayList<String> connectedIP = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					"/proc/net/arp"));
			String line;
			br.readLine();
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4) {
					String ip = splitted[0];
					// Log.i("log", ip);
					connectedIP.add(ip);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connectedIP;
	}
	
	public String GetMacAddress() {     
	    WifiInfo wifiInfo = mwfmanager.getConnectionInfo();     
	    String i = wifiInfo.getMacAddress();
	    return i;     
	}	
}
