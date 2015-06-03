package com.mycompany.myapp.poi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import com.mycompany.myapp.config.Config;

/**
 * 
 * @author martin.wang
 *
 */
public class DataUtils {
	private static String LINESEPARATOR="\n";
	
	public static String decodeURL(String URL){
		String result = "";
		try {
			result = URLDecoder.decode(URL, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return result;
	}
	
	public static Calendar strToCalendarDate(String dateStr){
		Calendar cal = Calendar.getInstance();
		try {
			Date date = new SimpleDateFormat(Config.dateFormat).parse(dateStr);
			cal.setTime(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cal;
	}

	public static String calendarDateToString(Calendar date){
		String dateStr = new SimpleDateFormat(Config.dateFormat).format(date);
		return dateStr;
	}



	public static String dateToString(Date date){
		String dateStr = new SimpleDateFormat(Config.dateFormat).format(date);
		return dateStr;
	}

	public static String getTimeStamp(){
		Date now = new Date();
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now);
		return timeStamp;
	}
	
	/**Save content into a specific path
	 * 
	 * @param content
	 * @param filePath
	 * @param overwrite
	 */
	public static void saveToFile(String content, String filePath, boolean overwrite){
		File tmp = new File(filePath);
		if (tmp.exists() && overwrite) {
			tmp.delete();
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
			bw.write(content);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
