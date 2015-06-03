package com.mycompany.myapp.poi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;



/**
 * 
 * @author martin.wang
 *
 */
public class POIUtils {

	
	public static String getCellValue(Workbook wb, Cell cell) {

		FormulaEvaluator evaluator = wb.getCreationHelper()
				.createFormulaEvaluator();
		String cellValue = "";
		if (cell != null) {
			if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
				cell = evaluator.evaluateInCell(cell);
			}
			cellValue = getCellValue(cell);
		}
		return cellValue;
	}

	public static String getCellValue(Cell cell) {
		String cellValue = "";

		if (cell != null) {
			switch (cell.getCellType()) {
			case Cell.CELL_TYPE_STRING:
				cellValue = cell.getRichStringCellValue().getString();
				break;

			case Cell.CELL_TYPE_NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					Date date = cell.getDateCellValue();
					cellValue = DataUtils.dateToString(date);
				} else {
					cell.setCellType(Cell.CELL_TYPE_STRING); // treat numeric
																// cells as
																// String type
					cellValue = cell.getRichStringCellValue().getString();
				}
				break;

			case Cell.CELL_TYPE_BOOLEAN:
				cellValue = cell.getBooleanCellValue() + "";
				break;

			case Cell.CELL_TYPE_FORMULA:
			default:
				cellValue = "";
				break;
			}
		}

		return cellValue;
	}

	
	/**Read a column from excel (include startIndex and endIndex)
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @param rowIndex_start:include
	 * @param rowIndex_end:exclude
	 * @return
	 */
	public static ArrayList<String> getColumnFromExcel(HSSFSheet sheet,
			int columnIndex, int rowIndex_start, int rowIndex_end){
		
		ArrayList<String> columnData = new ArrayList<String>();
		for (int i = rowIndex_start; i <= rowIndex_end; i++) {
			Row row = sheet.getRow(i);
			if (row != null) {
				Cell cell = row.getCell(columnIndex);
				if (cell != null) {
					String cellValue = POIUtils.getCellValue(cell);
					if (!cellValue.equals("")) {
						columnData.add(cellValue);
					}else{
						columnData.add("");
					}
				}else{
					columnData.add("");
				}
			}else{
				columnData.add("");
			}

		}

		return columnData;
	}
	
	public static ArrayList<String> getColumnFromExcel(HSSFSheet sheet,
			int columnIndex, int rowIndex_start) {
		
		ArrayList<String> columnData = new ArrayList<String>();
		int rows = sheet.getPhysicalNumberOfRows();
		for (int i = rowIndex_start; i < rows; i++) {
			// System.out.println(i);
			Row row = sheet.getRow(i);
			if (row != null) {
				Cell cell = row.getCell(columnIndex);
				if (cell != null) {
					String cellValue = POIUtils.getCellValue(cell);
					if (!cellValue.equals("")) {
						columnData.add(cellValue);
					}
				}
			}

		}

		return columnData;
	}

	public static ArrayList<String> getColumnFromExcel(HSSFSheet sheet,
			int columnIndex) {
		return getColumnFromExcel(sheet, columnIndex, 0);
	}

	public static ArrayList<String> getRowFromExcel(HSSFSheet sheet,
			int rowIndex, int columnIndex_start) {
		ArrayList<String> rowData = new ArrayList<String>();
		Row row = sheet.getRow(rowIndex);
		Cell cell = null;
		String cellValue = "";
		for (int i = columnIndex_start; i < row.getPhysicalNumberOfCells(); i++) {
			cell = row.getCell(i);
			cellValue = POIUtils.getCellValue(cell);
			if (!cellValue.equals("")) {
				rowData.add(cellValue);
			}
		}

		return rowData;
	}

	public static ArrayList<String> getRowFromExcel(HSSFSheet sheet,
			int rowIndex) {
		return getRowFromExcel(sheet, rowIndex, 0);
	}

	public static HSSFSheet getSheetByName(HSSFWorkbook wb, String strSheetName) {
		return wb.getSheet(strSheetName);
	}

	/**Load data from Excel and organize them into key-values pair.
	 * In Excel, 
	 * Row 1 = Field List; Column = Values for each field.
	 * 
	 * @param srcFile
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> getExcelDataAsKeyValuePair(String srcFile){
		HashMap<String, ArrayList<String>> field_values = new HashMap<String, ArrayList<String>>();
		
		try {
			FileInputStream file = new FileInputStream(srcFile);
			HSSFWorkbook wb = new HSSFWorkbook(file);
			
			int sheetNo = wb.getNumberOfSheets();
			
			//1. read all data from sheet: first row defines fields, each column keeps values for the field
			for(int i = 0; i < sheetNo; i ++){
				HSSFSheet sheet = wb.getSheetAt(i);
				
				int rowIndex = 0;
				ArrayList<String> fields = POIUtils.getRowFromExcel(sheet, rowIndex);
		
				for(int j = 0; j < fields.size(); j ++){
					String field = fields.get(j);
					int columnIndex = j;
					ArrayList<String> values = POIUtils.getColumnFromExcel(sheet, columnIndex);
					ArrayList<String> values_existing = null;
					
					if(field_values.containsKey(field)){
						values_existing = field_values.get(field);
						values_existing.addAll(values);
					}else{
						values_existing = values;
					}
					field_values.put(field, values_existing);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return field_values;
	}
	
	/**
	 * 
	 * @param sheet
	 * @param dataRowIndex_start
	 * @param key_columnIndex
	 * @param value_columnIndex
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> getColumnPairs(
			HSSFSheet sheet, int dataRowIndex_start, int key_columnIndex,
			int value_columnIndex) {
		HashMap<String, ArrayList<String>> key_values_map = new HashMap<String, ArrayList<String>>();

		ArrayList<String> values = null;

		Row row;
		Cell cell;
		String key = "", value = "";

		for (int i = dataRowIndex_start; i < sheet.getPhysicalNumberOfRows(); i++) {
			row = sheet.getRow(i); // for each row
			if (row != null) {
				cell = row.getCell(key_columnIndex); // for key column
				if (cell != null) {
					key = cell.getRichStringCellValue().getString();
				}

				cell = row.getCell(value_columnIndex); // for value column
				if (cell != null) {
					value = cell.getRichStringCellValue().getString();
				}
			}

			if (!key.equals("") && !value.equals("")) {

				if (key_values_map.containsKey(key)) {
					values = key_values_map.get(key);
				} else {
					values = new ArrayList<String>();
				}

				if (!values.contains(value)) {
					values.add(value);
				}

				key_values_map.put(key, values);
				key = "";
				value = "";
			}
		}

		return key_values_map;
	}
	
	/**Return number of non-empty rows in a specific column
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @return
	 */
	public static int NumRows(HSSFSheet sheet, int columnIndex){
		int rowNum = 0;
		for(int i = 0; i < sheet.getPhysicalNumberOfRows(); i++){
			Row row = sheet.getRow(i);
			if(row!= null){
				rowNum++;
			}else{
				break;//return when encountering first empty cell
			}
		}
		
		return rowNum;
	}

	/**Return row number of a specific value in a column
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @param search_keyword
	 * @return
	 */
	public static int getRowNum(HSSFSheet sheet, int columnIndex,
			String search_keyword) {
		Row row;
		Cell cell;
		String value = "";

		int rowNum = -1;
		for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
			row = sheet.getRow(i); // for each row
			if (row != null) {
				cell = row.getCell(columnIndex); // for key column
				if (cell != null) {
					value = cell.getRichStringCellValue().getString();
					if (value.equals(search_keyword)) {
						rowNum = i;
						break;
					}
				}
			}

		}
		return rowNum;
	}

	public static void writeExcelData(String fileName, String sheetName,
			int keyword_ColumnIndex, String keyword_search,
			int data_ColumnIndex, String data) {
		try {
			FileInputStream file = new FileInputStream(fileName);
			HSSFWorkbook wb = new HSSFWorkbook(file);

			HSSFSheet sheet = wb.getSheet(sheetName);

			int rowNum = POIUtils.getRowNum(sheet, keyword_ColumnIndex,
					keyword_search);
			HSSFRow row = sheet.getRow(rowNum);
			Cell cell = row.getCell(data_ColumnIndex);
			if (cell == null) {
				cell = row.createCell(data_ColumnIndex);
			}
			cell.setCellValue(data);
			file.close();

			FileOutputStream outFile = new FileOutputStream(fileName);
			wb.write(outFile);
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
