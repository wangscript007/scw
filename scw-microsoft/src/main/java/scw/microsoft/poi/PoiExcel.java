package scw.microsoft.poi;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Workbook;

import scw.microsoft.Excel;
import scw.microsoft.WritableExcel;
import scw.microsoft.WritableSheet;

public class PoiExcel implements Excel, WritableExcel {
	private final Workbook workbook;
	private final OutputStream outputStream;

	public PoiExcel(Workbook workbook) {
		this(workbook, null);
	}

	PoiExcel(Workbook workbook, OutputStream outputStream) {
		this.workbook = workbook;
		this.outputStream = outputStream;
	}

	public void close() throws IOException {
		if(outputStream != null){
			workbook.write(outputStream);
			outputStream.flush();
		}
		workbook.close();
	}

	public Workbook getWorkbook() {
		return workbook;
	}

	public WritableSheet getSheet(int sheetIndex) {
		if (sheetIndex >= workbook.getNumberOfSheets()) {
			return null;
		}
		
		org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(sheetIndex);
		if (sheet == null) {
			return null;
		}

		return new PoiSheet(sheet);
	}

	public WritableSheet getSheet(String sheetName) {
		org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet(sheetName);
		if (sheet == null) {
			return null;
		}

		return new PoiSheet(sheet);
	}

	public int getNumberOfSheets() {
		return workbook.getNumberOfSheets();
	}

	public void flush() throws IOException {
	}

	public WritableSheet createSheet(String sheetName) {
		org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(sheetName);
		return new PoiSheet(sheet);
	}

	public WritableSheet createSheet() {
		org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet();
		return new PoiSheet(sheet);
	}

	public void removeSheet(int sheetIndex) {
		workbook.removeSheetAt(sheetIndex);
	}
}
