package com.netdimen.buganalysis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netdimen.buganalysis.config.Config;

/**
 * 
 * @author martin.wang
 *
 */
public class POIUtils {

	public static String getCellValue(final Workbook wb, final Cell cell) {

		final Cell cell_temp = (cell != null && cell.getCellType() == Cell.CELL_TYPE_FORMULA) ? wb.getCreationHelper()
		                                                                                          .createFormulaEvaluator()
		                                                                                          .evaluateInCell(cell) : cell;
		return getCellValue(cell_temp);
	}

	public static String getCellValue(final Cell cell) {

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

	/**
	 * Read a column from excel (include startIndex and endIndex)
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @param rowIndex_start
	 *            :include
	 * @param rowIndex_end
	 *            :exclude
	 * @return
	 */
	private static ArrayList<String>
	        getColumnFromExcel(final HSSFSheet sheet, final int columnIndex, final int rowIndex_start, final int rowIndex_end) {

		final ArrayList<String> columnData = Lists.newArrayList();
		for (int i = rowIndex_start; i < rowIndex_end; i++) {
			final Row row = sheet.getRow(i);
			final String data = (row != null && row.getCell(columnIndex) != null) ? POIUtils.getCellValue(row.getCell(columnIndex)) : "";
			columnData.add(data);
		}
		return columnData;
	}

	private static ArrayList<String> getColumnFromExcel(final HSSFSheet sheet, final int columnIndex, final int rowIndex_start) {

		return getColumnFromExcel(sheet, columnIndex, rowIndex_start, sheet.getPhysicalNumberOfRows());
	}

	private static ArrayList<String> getColumnFromExcel(final HSSFSheet sheet, final int columnIndex) {

		return getColumnFromExcel(sheet, columnIndex, 0);
	}

	public static ArrayList<BugReport> getBugReportFromExcel(final String fileName) {

		ArrayList<BugReport> result = Lists.newArrayList();
		try {
			if (new File(fileName).exists()) {
				final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(fileName));
				final HSSFSheet sheet = wb.getSheetAt(0);

				for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
					final Row row = sheet.getRow(i);
					result.add(new BugReport(POIUtils.getCellValue(row.getCell(0)),
					                         POIUtils.getCellValue(row.getCell(1)),
					                         POIUtils.getCellValue(row.getCell(2))));
				}
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;

	}
	
	public static ArrayList<String> getColumnFromExcel(final String fileName, final int columnIndex){
		return getColumnFromExcel(fileName,"", columnIndex);
	}
	
	public static ArrayList<String> getColumnFromExcel(final String fileName) {

		return getColumnFromExcel(fileName, "", 0);
	}

	public static ArrayList<String> getColumnFromExcel(final String fileName, final String sheetName) {

		return getColumnFromExcel(fileName, sheetName, 0);
	}

	public static ArrayList<String> getColumnFromExcel(final String fileName, final String sheetName, final int columnIndex) {

		ArrayList<String> result = Lists.newArrayList();
		try {
			if (new File(fileName).exists()) {
				final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(fileName));
				final HSSFSheet sheet = (sheetName != null && !sheetName.equals("")) ? wb.getSheet(sheetName) : wb.getSheetAt(0);
				result = getColumnFromExcel(sheet, columnIndex);
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	private static ArrayList<String> getRowFromExcel(final HSSFSheet sheet, final int rowIndex, final int columnIndex_start) {

		final ArrayList<String> rowData = Lists.newArrayList();
		final Row row = sheet.getRow(rowIndex);
		for (int i = columnIndex_start; i < row.getPhysicalNumberOfCells(); i++) {
			final Cell cell = row.getCell(i);
			final String cellValue = POIUtils.getCellValue(cell);
			final boolean result = cellValue.equals("") ? false : rowData.add(cellValue);
		}
		return rowData;
	}

	private static ArrayList<String> getRowFromExcel(final HSSFSheet sheet, final int rowIndex) {

		return getRowFromExcel(sheet, rowIndex, 0);
	}

	public static ArrayList<String> getRowFromExcel(final String fileName, final String sheetName, final int rowIndex) {

		ArrayList<String> result = Lists.newArrayList();
		try {
			if (new File(fileName).exists()) {
				final HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(fileName));
				final HSSFSheet sheet = (sheetName != null && !sheetName.equals("")) ? wb.getSheet(sheetName) : wb.getSheetAt(0);
				result = getRowFromExcel(sheet, rowIndex);
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	public static ArrayList<String> getRowFromExcel(final String fileName) {

		return getRowFromExcel(fileName, "", 0);
	}

	public static ArrayList<String> getRowFromExcel(final String fileName, final String sheetName) {

		return getRowFromExcel(fileName, sheetName, 0);
	}

	public static HSSFSheet getSheetByName(final HSSFWorkbook wb, final String strSheetName) {

		return wb.getSheet(strSheetName);
	}

	/**
	 * Load data from Excel and organize them into key-values pair. In Excel, Row 1 = Field List; Column = Values for
	 * each field.
	 * 
	 * @param srcFile
	 * @return
	 */
	public static Map<String, Set<String>> getExcelDataAsKeyValuePair(final String srcFile) {

		Map<String, Set<String>> field_values = null;
		try {
			final HashMultimap<String, String> multimap = HashMultimap.create();

			final FileInputStream file = new FileInputStream(srcFile);
			final HSSFWorkbook wb = new HSSFWorkbook(file);

			// 1. read all data from sheet: first row defines fields, each column keeps values for the field
			for (int i = 0; i < wb.getNumberOfSheets(); i++) {
				final HSSFSheet sheet = wb.getSheetAt(i);

				final int rowIndex = 0;
				final ArrayList<String> fields = POIUtils.getRowFromExcel(sheet, rowIndex);

				for (int j = 0; j < fields.size(); j++) {
					String field = fields.get(j);
					int columnIndex = j;
					ArrayList<String> values = POIUtils.getColumnFromExcel(sheet, columnIndex);
					for (String value : values) {
						multimap.put(field, value);
					}
				}
			}

			field_values = Maps.transformValues(multimap.asMap(), new Function<Collection<String>, Set<String>>() {

				@Override
				public Set<String> apply(Collection<String> from) {

					return new HashSet<String>(from);
				}
			});
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
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
	public static Map<String, ArrayList<String>> getColumnPairs(final HSSFSheet sheet,
	                                                            final int dataRowIndex_start,
	                                                            final int key_columnIndex,
	                                                            final int value_columnIndex) {

		ListMultimap<String, String> multimap = ArrayListMultimap.create();
		for (int i = dataRowIndex_start; i < sheet.getPhysicalNumberOfRows(); i++) {
			final Row row = sheet.getRow(i); // for each row
			final Cell cell = (row != null ? row.getCell(key_columnIndex) : null);
			final String key = (row != null && row.getCell(key_columnIndex) != null ? cell.getRichStringCellValue().getString() : null);
			final String value = (row != null && row.getCell(value_columnIndex) != null ? cell.getRichStringCellValue().getString() : null);
			multimap.put(key, value);
		}

		return Maps.transformValues(multimap.asMap(), new Function<Collection<String>, ArrayList<String>>() {

			@Override
			public ArrayList<String> apply(Collection<String> from) {

				return new ArrayList<String>(from);
			}
		});
	}

	/**
	 * Return number of non-empty rows in a specific column
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @return
	 */
	public static int NumRows(final HSSFSheet sheet, final int columnIndex) {

		int rowNum = 0;
		for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
			final Row row = sheet.getRow(i);
			if (row != null) {
				rowNum++;
			} else {
				break;// return when encountering first empty cell
			}
		}
		return rowNum;
	}

	/**
	 * Return row number of a specific value in a column
	 * 
	 * @param sheet
	 * @param columnIndex
	 * @param search_keyword
	 * @return
	 */
	public static int getRowNum(final HSSFSheet sheet, final int columnIndex, final String search_keyword) {

		int rowNum = -1;
		for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
			final Row row = sheet.getRow(i); // for each row
			if (row != null) {
				final Cell cell = row.getCell(columnIndex); // for key column
				if (cell != null) {
					final String value = cell.getRichStringCellValue().getString();
					if (value.equals(search_keyword)) {
						rowNum = i;
						break;
					}
				}
			}
		}
		return rowNum;
	}

	private static void writeToExcel(final HSSFSheet sheet, final ArrayList<ArrayList<String>> data) {

		for (int i = 0; i < data.size(); i++) {
			final HSSFRow row = sheet.createRow(i);
			for (int j = 0; j < data.get(i).size(); j++) {
				final Cell cell = row.createCell(j);
				cell.setCellValue(data.get(i).get(j));
			}
		}
	}

	public static void writeToExcel(final String fileName, final String sheetName, final ArrayList<ArrayList<String>> data) {

		try {
			boolean fileExist = new File(fileName).exists();
			final HSSFWorkbook wb = fileExist ? new HSSFWorkbook(new FileInputStream(fileName)) : new HSSFWorkbook();
			final HSSFSheet sheet = fileExist ? wb.getSheet(sheetName) : wb.createSheet(sheetName);
			final ArrayList<ArrayList<String>> dataList = Lists.newArrayList(data);
			writeToExcel(sheet, dataList);
			final FileOutputStream outFile = new FileOutputStream(fileName);
			wb.write(outFile);
			outFile.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveSummaryReportToExcel(final ArrayList<ArrayList<String>> data) {

		final String sheetName = Config.getInstance().getProperty("bug.analysis.report.sheet.summary");
		HSSFSheet sheet = getReportTemplate().getSheet(sheetName);
		if (sheet == null) {
			sheet = getReportTemplate().createSheet(sheetName);
		}
		writeToExcel(sheet, data);
	}

	public HSSFWorkbook getReportTemplate() {

		HSSFWorkbook workBook = null;
		final File template = new File(Config.getInstance().getProperty("bug.analysis.report.template.sheet"));
		try {
			if (template.exists()) {
				template.delete();
			}
			template.createNewFile();
			workBook = new HSSFWorkbook(new FileInputStream(template));
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return workBook;
	}
}
