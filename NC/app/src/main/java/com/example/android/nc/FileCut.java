package com.example.android.nc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.example.android.nc.MainActivity;

public class FileCut {

	static {
		System.loadLibrary("native-lib");
	}
    private String dirParentPath;
	private String savePath;
	private ArrayList<String> resultFileList=new ArrayList<String>();
	private  String path ;
	private String filename ;
	private String ext ;
	private File dir;
	public FileCut() {
		
	}
	public FileCut(  String path , String filename ,  String ext   ) {
		this.path=path;
		this.filename=filename;
		this.ext=ext;
		}

	//编码
	public int Encode(){
		int K=4,N=7;
		int i, j;
		//创建文件目录，存放分割之后的文件
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            dirParentPath=Environment.getRootDirectory().getPath();//SD卡不可用
        else
            dirParentPath=Environment.getExternalStorageDirectory().getPath();
		dir=new File(dirParentPath+"/cutfiles/");
		if(!dir.exists())
		{
			dir.mkdirs();
		}
		savePath = dir + "/" ;
		// 建立输入流
		File inFile = new File(path);
		FileInputStream fis=null;
		try {
			/************************************************************************/
			/** Step 1. Read the file                                               */
			/************************************************************************/
			fis = new FileInputStream(inFile);
			int nSize=fis.available();//获取文件大小字节
			int nLen=nSize/K+(nSize%K!=0?1:0);//把文件平均分成K份，每份长度为nLen个字节
			int bytesRead=0;
			int bytesToRead=K*nLen;
			byte[] buffer = new byte[bytesToRead];
			for (i = 0; i<bytesToRead; i++)//先把buffer数组置0
			{
				buffer[i] = 0;
			}

			//读取文件字节到一个数组中
			while(bytesRead<bytesToRead) {
				int result=fis.read(buffer,bytesRead,bytesToRead-bytesRead);//复制文件到buffer数组
				if(result==-1)
					break;
				bytesRead+=result;
			}
			/*将buffer转存为jpg文件
			OutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath()+"/files/temp.jpg"));
			out.write(buffer);
			out.flush();
			out.close();*/
			byte[][] Mat= Encode(buffer,N,K,nLen);
			/*转为二维数组并保存
			OutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath()+"/files/temp.jpg"));
			for( i=0;i<K;i++)
				for(j=0;j<nLen;j++)
				{ out.write(Mat[i][j]);

				}
			out.flush();
			out.close();*/
			//把矩阵matrix2分开存入N个encodeFile文件
			// 以"demo"+num+".db"方式来命名小文件即分割后为demo1.db，demo2.db，。。。。。。
			for (i = 1; i <=N ; i++) {
				FileOutputStream fos = new FileOutputStream(new File(savePath + filename + i + ext));
				resultFileList.add(filename + i + ext);
				fos.write(Mat[i-1], 0, 1+K+nLen);
				// 文件读取结束
				fos.close();

			}
			return N;
//			//文件数据存入二维数组中K*nLen
//			byte[][] Buf;
//			Buf=new byte[K][];
//			pos=0;
//			for (i = 0; i < K; i++){
//				Buf[i] = new byte[nLen];
//				for (j = 0; j < nLen; j++){
//					Buf[i][j] =buffer[pos++]; //把buffer数组存入二维数组Buf
//				}
//			}
//			/************************************************************************/
//			/* Step 2. Get code matrix(N*K)                                         */
//			/************************************************************************/
//			byte[][] encodeMatrix;          //编码矩阵N*K
//			encodeMatrix = new byte[N][];
//			for (i = 0; i < N; i++){
//				encodeMatrix[i] = new byte[K];
//			}
//			java.util.Random r=new java.util.Random();
//			for (i = 0; i < N; i++)      //生成随机矩阵
//			{
//				for (j = 0; j < K; j++)
//				{
//					encodeMatrix[i][j] =(byte)(r.nextInt()% 256);
//				}
//			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}}
		return -1;

	}

	//再编码
	public ArrayList<String> Reencode(ArrayList<String> partFileList){
		/************************************************************************/
		/*  Start decoding.                                                     */
		/************************************************************************/
		int nPart = partFileList.size();           //用来存放编码前分成相同大小的文件个数
		int nLength = 0;         //用来存放文件的长度
		int i,j;
		//获取文件路径
		String dirParentPath2;
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			dirParentPath2=Environment.getRootDirectory().getPath();//SD卡不可用
		else
			dirParentPath2=Environment.getExternalStorageDirectory().getPath();
		String dir2=dirParentPath2+"/cutfiles/";

		File inFile = new File(dir2+"/"+partFileList.get(0));
		FileInputStream fis=null;
		try {
			fis = new FileInputStream(inFile);
			nLength = fis.available();			  //获取每个编码文件的长度
			fis.close();

			byte[][] MAT;                         //用来存放nPart个编码文件，是一个nPart*nLength的矩阵
			MAT = new byte[nPart][];
			for (i = 0; i < nPart; i++){
				MAT[i] = new byte[nLength];
			}
			for (i = 0; i < nPart; i++)
			{
				inFile = new File(dir2+"/"+partFileList.get(i));
				fis = new FileInputStream(inFile);
				int bytesRead=0;
				int bytesToRead=nLength;
				//读取文件字节到一个数组中
				while(bytesRead<bytesToRead) {
					int result=fis.read(MAT[i],bytesRead,bytesToRead-bytesRead);//复制文件到buffer数组
					if(result==-1)
						break;
					bytesRead+=result;
				}
				delete(dir2+"/"+partFileList.get(i));
			}
			byte[][] MAT2=Reencode(MAT,nPart,nLength);
			System.out.println(nLength+","+nPart+","+MAT2.length+","+MAT2[0].length);
			//把矩阵matrix2分开存入N个encodeFile文件
			// 以"demo"+num+".db"方式来命名小文件即分割后为demo1.db，demo2.db，。。。。。。
			for (i = 0; i <nPart ; i++) {
				FileOutputStream fos = new FileOutputStream(new File(dir2+"/"+partFileList.get(i)+"_re_encodeFile.db"));
				resultFileList.add(partFileList.get(i)+"_re_encodeFile.db");
				fos.write(MAT2[i], 0, nLength);
				// 文件读取结束
				fos.close();

			}
			return resultFileList;
		}catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}}
		return resultFileList;
	}

	//恢复
    public void Recover( ArrayList<String> partFileList, String dst) throws IOException {
		/************************************************************************/
	        /*  Start decoding.                                                     */
		/************************************************************************/
		int nPart = partFileList.size();           //用来存放编码前分成相同大小的文件个数
		int nLength = 0;         //用来存放文件的长度
		int i,j;
		//获取文件路径
		String dirParentPath2;
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			dirParentPath2=Environment.getRootDirectory().getPath();//SD卡不可用
		else
			dirParentPath2=Environment.getExternalStorageDirectory().getPath();
		String dir2=dirParentPath2+"/cutfiles/";
		//String dir2=dirPare ntPath2+"/files/";
		File inFile = new File(dir2+"/"+partFileList.get(0));
		FileInputStream fis=null;
		try {
			fis = new FileInputStream(inFile);
			nLength = fis.available();
			fis.close();
			byte[][] MAT;                         //用来存放nPart个解码文件，是一个nPart*nLength的矩阵
			MAT = new byte[nPart][];

			for (i = 0; i < nPart; i++){
				MAT[i] = new byte[nLength];
			}
			for (i = 0; i < nPart; i++)
			{
				inFile = new File(dir2+"/"+partFileList.get(i));
				fis = new FileInputStream(inFile);
				int bytesRead=0;
				int bytesToRead=nLength;
				//读取文件字节到一个数组中
				while(bytesRead<bytesToRead) {
					int result=fis.read(MAT[i],bytesRead,bytesToRead-bytesRead);//复制文件到buffer数组
					if(result==-1)
						break;
					bytesRead+=result;
				}
			}
			byte[][] MAT2=Decode(MAT,nPart,nLength);
			System.out.println(nLength+","+nPart+","+MAT2.length+","+MAT2[0].length);
			OutputStream out = new FileOutputStream(new File(dst));
			for( i=0;i<partFileList.size();i++){
				out.write(MAT2[i],0,nLength-nPart-1);
				out.flush();
			}
			// 把所有小文件都进行写操作后才关闭输出流，这样就会合并为一个文件了
			out.close();

		}catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}}


    }

	public int Cut(){
		//int split = 1024 * 1024;  
	    byte[] buf = new byte[1024];  
	    int num = 1;
        //创建文件目录，存放分割之后的文件
	     dir=new File(Environment.getExternalStorageDirectory().getPath()+"/cutfiles/");
		if(!dir.exists())
		{
			dir.mkdirs();
		}
	    savePath = dir + "/" ;
	    // 建立输入流  
	    File inFile = new File(path);  
	    try {
			FileInputStream fis = new FileInputStream(inFile);
			int split=fis.available()/3;
			while (true) {  
			    // 以"demo"+num+".db"方式来命名小文件即分割后为demo1.db，demo2.db，。。。。。。  
			    FileOutputStream fos = new FileOutputStream(new File(savePath+filename+ num + ext));
			   resultFileList.add(filename+ num + ext);
			    for (int i = 0; i < split / buf.length; i++) {  
			        int read = fis.read(buf);  
			        fos.write(buf, 0, read);  
			        // 判断大文件读取是否结束  
			        if (read < buf.length) {  
			            fis.close();  
			            fos.close();  
			            return num;  
			        }  
			    }  
			    fos.close();  
			    num++; 
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
	    try {
		} catch (Exception e) {
			e.printStackTrace();
		}}
		return -1;
		
	}
	public void mergeApkFile( ArrayList<String> partFileList, String dst) throws IOException {  
	 {  
	        OutputStream out = new FileOutputStream(new File(dst));  
	        byte[] buffer = new byte[1024];  
	        InputStream in;  
	        int readLen = 0;  
	        
	        for(int i=0;i<partFileList.size();i++){  
	            // 获得输入流  
	            in = new FileInputStream(new File(Environment.getExternalStorageDirectory().getPath()+"/files/")+"/"+partFileList.get(i)); 
	            Log.i("log", partFileList.size()+"---"+partFileList.get(i));
	            while((readLen = in.read(buffer)) != -1){  
	                out.write(buffer, 0, readLen);  
	            }  
	            out.flush();  
	            in.close();  
	        }  
	        // 把所有小文件都进行写操作后才关闭输出流，这样就会合并为一个文件了  
	        out.close();  
	    }  
	}

	public boolean delete(String path){
		if(TextUtils.isEmpty(path)){
			Log.e("","删除文件不存在");
			return true;
		}
		File file=new File(path);
		if(file.exists()){
			boolean flag=file.delete();
			Log.e("","删除文件"+flag);
		}else {
			Log.e("","删除文件不存在");
		}
		return true;
	}
	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native byte[][] Encode(byte[] buffer,int N,int K,int nLen);
    public native byte[][] Decode(byte[][] buffer,int nPart,int nLength);
    public native byte[][] Reencode(byte[][] buffer,int nPart,int nLength);
}
